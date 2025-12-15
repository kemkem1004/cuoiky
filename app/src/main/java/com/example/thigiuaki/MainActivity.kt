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

// IMPORTS CÁC MÀN HÌNH MỚI
import com.example.thigiuaki.ui.screens.*
import com.example.thigiuaki.model.CartItem as AppCartItem // Alias để tránh xung đột tên
import com.example.thigiuaki.model.Product as AppProduct // Alias cho Product model

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        FirebaseApp.initializeApp(this)
        migrateOldOrderData()

        setContent {
            ThigiuakiApp()
        }
    }
}
fun migrateOldOrderData() {
    val db = FirebaseFirestore.getInstance()
    val collectionRef = db.collection("orders")

    // Định dạng chuỗi ngày giờ bạn đang lưu trong Firestore ("10/12/2025 22:47")
    val stringDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

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
                // (Giữ nguyên phần xử lý bị bỏ qua trước đó)
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
// ========================================================
sealed class Screen {
    object Splash : Screen() // <-- ĐÃ THÊM: Màn hình Splash
    object RoleSelection : Screen()
    object AdminLogin : Screen()
    object CustomerLogin : Screen()
    object Register : Screen()
    object AdminManager : Screen()
    object CustomerHome : Screen()
    object ForgotPassword : Screen()

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
            // KHỞI TẠO BẰNG MÀN HÌNH SPLASH
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
            var cartItems by remember { mutableStateOf(listOf<AppCartItem>()) }

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
                            val items: List<AppCartItem> = snapshot?.documents?.mapNotNull { doc ->
                                try {
                                    val item = doc.toObject(AppCartItem::class.java)
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
                // <-- ĐÃ THÊM: Logic cho Splash Screen
                is Screen.Splash -> SplashScreen(
                    onTimeout = {
                        // Sau khi Splash kết thúc, chuyển đến màn hình chọn vai trò
                        currentScreen = Screen.RoleSelection
                    }
                )
                // -->

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
                    onBack = { currentScreen = Screen.RoleSelection },
                    onForgotPasswordClick = { currentScreen = Screen.ForgotPassword }
                )

                is Screen.CustomerLogin -> LoginScreen(
                    onLoginSuccess = { role ->
                        currentScreen = Screen.CustomerHome
                    },
                    onBack = { currentScreen = Screen.RoleSelection },
                    onRegisterClick = { currentScreen = Screen.Register },
                    onForgotPasswordClick = { currentScreen = Screen.ForgotPassword }
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
                        onAddToCart = { product, size, color, quantity -> // <-- thêm quantity
                            val userId = auth.currentUser?.uid ?: return@ProductDetailsScreen
                            val cartItem = AppCartItem(
                                productId = product.id,
                                productName = product.name,
                                productImageUrl = product.imageUrl,
                                price = product.price,
                                quantity = quantity, // <-- dùng quantity đúng
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
                    onBack = { currentScreen = Screen.CustomerHome }, // <-- quay về Home
                    onCheckout = {
                        if (cartItems.isNotEmpty()) currentScreen = Screen.Checkout
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
                is Screen.ForgotPassword -> ForgotPasswordScreen(
                    onBack = { currentScreen = Screen.CustomerLogin }, // quay về login
                    onSuccess = { currentScreen = Screen.CustomerLogin } // sau khi gửi mail xong
                )


            }
        }
    }
}