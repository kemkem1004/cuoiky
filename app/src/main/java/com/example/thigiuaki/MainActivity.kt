package com.example.thigiuaki

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.example.thigiuaki.ui.theme.ThigiuakiTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FIX LỖI UI XML
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Khởi tạo Firebase
        FirebaseApp.initializeApp(this)

        // =============================================================
        // BƯỚC 1: GỌI HÀM CHUYỂN ĐỔI DỮ LIỆU CŨ TỪ STRING SANG TIMESTAMP
        // CHẠY HÀM NÀY MỘT LẦN VÀ XÓA SAU KHI HOÀN TẤT MIGRATION
        // =============================================================
        migrateOldOrderData()

        setContent {
            ThigiuakiApp()
        }
    }
}

// =============================================================
// HÀM HELPER DI CHUYỂN DỮ LIỆU CŨ TRONG FIRESTORE
// =============================================================
fun migrateOldOrderData() {
    val db = FirebaseFirestore.getInstance()
    val collectionRef = db.collection("orders")

    // Định dạng chuỗi ngày giờ bạn đang lưu trong Firestore ("10/12/2025 22:47")
    // Nếu bạn có nhiều định dạng, bạn cần xử lý tất cả
    val stringDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // Định dạng cho pickupTime nếu nó có định dạng khác (ví dụ: "December 2, 2025 at 3:15:52 PM UTC+7")
    // Lưu ý: Việc parse các múi giờ như UTC+7 từ String có thể phức tạp.
    // Nếu pickupTime có định dạng khác, bạn cần tạo SimpleDateFormat khác cho nó.
    // Ví dụ (chỉ là ví dụ, format thực tế có thể khác):
    // val pickupTimeDateFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a z", Locale.ENGLISH)

    collectionRef.get().addOnSuccessListener { snapshot ->
        for (document in snapshot.documents) {
            var needsUpdate = false
            val updateData = mutableMapOf<String, Any>()

            // --- Xử lý orderDate ---
            val orderDateValue = document.get("orderDate")
            if (orderDateValue is String) {
                try {
                    val date = stringDateFormat.parse(orderDateValue)
                    if (date != null) {
                        val newTimestamp = Timestamp(date)
                        updateData["orderDate"] = newTimestamp
                        needsUpdate = true
                    }
                } catch (e: Exception) {
                    Log.e("MIGRATION", "Error parsing orderDate string ${orderDateValue}: ${e.message}")
                }
            }

            // --- Xử lý pickupTime ---
            val pickupTimeValue = document.get("pickupTime")
            if (pickupTimeValue is String) {
                // TẠM THỜI BỎ QUA VÌ ĐỊNH DẠNG "December 2, 2025 at 3:15:52 PM UTC+7"
                // rất khó parse chính xác mà không có format chuẩn.
                // Tốt nhất nên sửa thủ công hoặc nhập lại.
                // Nếu bạn có thể cung cấp format chính xác, tôi sẽ sửa.
                // Hiện tại, ta sẽ bỏ qua để tránh lỗi.
            }

            // Thực hiện cập nhật nếu có trường cần sửa
            if (needsUpdate) {
                document.reference.update(updateData)
                    .addOnSuccessListener {
                        Log.d("MIGRATION", "Successfully updated Timestamp fields for ${document.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("MIGRATION", "Error updating fields for ${document.id}: ${e.message}")
                    }
            }
        }
    }
}
// =============================================================
// KẾT THÚC HÀM HELPER
// =============================================================

// Định nghĩa các trạng thái/màn hình điều hướng
sealed class Screen {
    object RoleSelection : Screen()
    object AdminLogin : Screen()
    object CustomerLogin : Screen()
    object Register : Screen()
    object AdminManager : Screen()
    object CustomerHome : Screen()
    data class ProductDetails(val productId: String) : Screen()
    object Cart : Screen()
    object Checkout : Screen()
    object OrderHistory : Screen()
    object Wishlist : Screen()
    data class Message(val orderId: String, val otherUserId: String, val otherUserName: String) : Screen()
}

@Composable
fun ThigiuakiApp() {
    val auth = FirebaseAuth.getInstance()
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    ThigiuakiTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var currentScreen by remember { mutableStateOf<Screen>(Screen.RoleSelection) }
            var cartItems by remember { mutableStateOf(listOf<com.example.thigiuaki.model.CartItem>()) }

            // Hàm xử lý đăng xuất chung
            val handleLogout: () -> Unit = {
                auth.signOut()
                currentScreen = Screen.RoleSelection
            }

            // Load cart items for checkout
            LaunchedEffect(auth.currentUser?.uid) {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    db.collection("cart")
                        .whereEqualTo("userId", userId)
                        .addSnapshotListener { snapshot, _ ->
                            val items: List<com.example.thigiuaki.model.CartItem> = snapshot?.documents?.mapNotNull { doc ->
                                try {
                                    val item = doc.toObject(com.example.thigiuaki.model.CartItem::class.java)
                                    item?.apply {
                                        id = doc.id
                                    }
                                } catch (e: Exception) {
                                    null
                                }
                            } ?: emptyList()
                            cartItems = items
                        }
                }
            }

            when (currentScreen) {
                is Screen.RoleSelection -> RoleSelectionScreen(
                    onAdminSelected = { currentScreen = Screen.AdminLogin },
                    onCustomerSelected = { currentScreen = Screen.CustomerLogin }
                )

                is Screen.AdminLogin -> LoginScreen(
                    onLoginSuccess = { role ->
                        currentScreen = if (role == "admin") {
                            Screen.AdminManager
                        } else {
                            Screen.CustomerHome
                        }
                    },
                    onBack = { currentScreen = Screen.RoleSelection }
                )

                is Screen.CustomerLogin -> LoginScreen(
                    onLoginSuccess = { role ->
                        currentScreen = Screen.CustomerHome
                    },
                    onBack = { currentScreen = Screen.RoleSelection },
                    onRegisterClick = { currentScreen = Screen.Register }
                )

                is Screen.Register -> RegisterScreen(
                    onRegisterSuccess = { currentScreen = Screen.CustomerHome },
                    onBack = { currentScreen = Screen.CustomerLogin }
                )

                is Screen.AdminManager -> AdminScreen(
                    onLogout = handleLogout
                )

                is Screen.CustomerHome -> CustomerHomeScreen(
                    onLogout = handleLogout,
                    onNavigateToProductDetails = { productId ->
                        currentScreen = Screen.ProductDetails(productId)
                    },
                    onNavigateToCheckout = {
                        if (cartItems.isNotEmpty()) {
                            currentScreen = Screen.Checkout
                        }
                    },
                    onNavigateToWishlist = {
                        currentScreen = Screen.Wishlist
                    },
                    onNavigateToMessage = { orderId, otherUserId, otherUserName ->
                        currentScreen = Screen.Message(orderId, otherUserId, otherUserName)
                    }
                )

                is Screen.ProductDetails -> {
                    val productId = (currentScreen as Screen.ProductDetails).productId
                    ProductDetailsScreen(
                        productId = productId,
                        onBack = { currentScreen = Screen.CustomerHome },
                        onAddToCart = { product, size, color ->
                            val userId = auth.currentUser?.uid ?: return@ProductDetailsScreen
                            val cartItem = com.example.thigiuaki.model.CartItem(
                                productId = product.id,
                                productName = product.name,
                                productImageUrl = product.imageUrl,
                                price = product.price,
                                quantity = 1,
                                selectedSize = size,
                                selectedColor = color,
                                userId = userId
                            )
                            db.collection("cart").add(cartItem)
                            currentScreen = Screen.CustomerHome
                        }
                    )
                }

                is Screen.Cart -> CartScreen(
                    onCheckout = {
                        if (cartItems.isNotEmpty()) {
                            currentScreen = Screen.Checkout
                        }
                    },
                    onNavigateToProductDetails = { productId ->
                        currentScreen = Screen.ProductDetails(productId)
                    }
                )

                is Screen.Checkout -> CheckoutScreen(
                    cartItems = cartItems,
                    onBack = { currentScreen = Screen.Cart },
                    onOrderPlaced = {
                        currentScreen = Screen.CustomerHome
                    }
                )

                is Screen.OrderHistory -> OrderHistoryScreen(
                    onNavigateToMessage = { orderId, otherUserId, otherUserName ->
                        currentScreen = Screen.Message(orderId, otherUserId, otherUserName)
                    }
                )

                is Screen.Message -> {
                    val messageScreen = currentScreen as Screen.Message
                    MessageScreen(
                        orderId = messageScreen.orderId,
                        otherUserId = messageScreen.otherUserId,
                        otherUserName = messageScreen.otherUserName,
                        onBack = { currentScreen = Screen.OrderHistory }
                    )
                }

                is Screen.Wishlist -> WishlistScreen(
                    onNavigateToProductDetails = { productId ->
                        currentScreen = Screen.ProductDetails(productId)
                    },
                    onBack = {
                        currentScreen = Screen.CustomerHome
                    }
                )


            }
        }
    }
}