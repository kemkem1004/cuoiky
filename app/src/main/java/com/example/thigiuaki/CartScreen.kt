package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.thigiuaki.model.CartItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

// =================================================================
// 1. ĐỊNH NGHĨA MÀU SẮC (Nhất quán với Clean Retail Design)
// =================================================================
private val BackgroundLight = Color(0xFFFAF9F6)
private val PrimaryMaroon = Color(0xFF8D021F)
private val SecondaryDark = Color(0xFF424242)
private val CardBackground = Color.White
// =================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    onNavigateToProductDetails: (String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    var cartItems by remember { mutableStateOf(listOf<CartItem>()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        // ... (Logic tải dữ liệu Firestore giữ nguyên)
        if (userId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("cart")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Cart", "Error: ${error.message}")
                    isLoading = false
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    val item = doc.toObject<CartItem>()
                    item?.copy(id = doc.id)
                } ?: emptyList()

                cartItems = items
                isLoading = false
            }
    }

    val totalAmount = cartItems.sumOf { it.price * it.quantity }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Giỏ hàng của bạn", color = PrimaryMaroon) },
                navigationIcon = {
                    IconButton(onClick = onBack) { // <-- gọi onBack() ở đây
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại", tint = PrimaryMaroon)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = PrimaryMaroon
                )
            )

        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 10.dp, // Bóng chuyên nghiệp hơn
                    color = CardBackground // Nền trắng cho Bottom Bar
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Tổng cộng:",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SecondaryDark
                                )
                                Text(
                                    "${totalAmount.toInt()} VND",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                                    color = PrimaryMaroon // Màu Đỏ Gạch cho tổng tiền
                                )
                            }
                            Button(
                                onClick = onCheckout,
                                modifier = Modifier.height(60.dp).width(160.dp), // Nút lớn hơn
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryMaroon, // Màu Đỏ Gạch
                                    contentColor = Color.White
                                )
                            ) {
                                Text("TIẾN HÀNH THANH TOÁN", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                    }
                }
            }
        },
        containerColor = BackgroundLight // Nền chính Kem nhạt
    ) { paddingValues ->
        if (isLoading) {
            // ... (Hiển thị loading giữ nguyên)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryMaroon)
            }
        } else if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Giỏ hàng trống",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = SecondaryDark
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Thêm sản phẩm vào giỏ hàng để tiếp tục mua sắm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryDark.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp), // Padding đều
                verticalArrangement = Arrangement.spacedBy(16.dp) // Khoảng cách lớn hơn
            ) {
                items(cartItems, key = { it.id }) { item ->
                    CartItemCard(
                        item = item,
                        onRemove = {
                            db.collection("cart").document(item.id).delete()
                                .addOnFailureListener { e -> Log.e("Cart", "Error removing item: ${e.message}") }
                        },
                        onQuantityChange = { newQuantity ->
                            if (newQuantity > 0) {
                                db.collection("cart").document(item.id)
                                    .update("quantity", newQuantity)
                                    .addOnFailureListener { e -> Log.e("Cart", "Error updating quantity: ${e.message}") }
                            } else {
                                db.collection("cart").document(item.id).delete()
                            }
                        },
                        onItemClick = { onNavigateToProductDetails(item.productId) }
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground), // Nền trắng
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Bóng rõ ràng hơn
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Tăng padding bên trong Card
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            if (item.productImageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(item.productImageUrl),
                    contentDescription = item.productName,
                    modifier = Modifier
                        .size(90.dp) // Hình ảnh lớn hơn
                        .clickable { onItemClick() }
                )
                Spacer(Modifier.width(16.dp))
            }

            // Product Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), // Chữ lớn và đậm
                    color = SecondaryDark
                )
                Spacer(Modifier.height(4.dp))
                // Giá sản phẩm
                Text(
                    text = "${item.price.toInt()} VND",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryMaroon
                )
                // Kích thước/Màu sắc
                if (item.selectedSize.isNotBlank() || item.selectedColor.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Phân loại: ${item.selectedSize} / ${item.selectedColor}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryDark.copy(alpha = 0.7f)
                    )
                }

                // Quantity Controls
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút Giảm
                    OutlinedIconButton(
                        onClick = { onQuantityChange(item.quantity - 1) },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = PrimaryMaroon),
                        border = BorderStroke(1.dp, PrimaryMaroon)
                    ) {
                        Text("-", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        text = "${item.quantity}",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = SecondaryDark
                    )
                    // Nút Tăng
                    Button(
                        onClick = { onQuantityChange(item.quantity + 1) },
                        modifier = Modifier.size(36.dp),
                        contentPadding = PaddingValues(0.dp), // Xóa padding mặc định của nút
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryMaroon)
                    ) {
                        Text("+", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }

            // Remove Button
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Xóa",
                    tint = MaterialTheme.colorScheme.error // Giữ màu lỗi mặc định (đỏ)
                )
            }
        }
    }
}