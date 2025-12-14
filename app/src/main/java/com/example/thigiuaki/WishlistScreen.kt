package com.example.thigiuaki.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.example.thigiuaki.model.Product as AppProduct // Import Product Model

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
fun WishlistScreen(
    onNavigateToProductDetails: (String) -> Unit,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    var favoriteProductIds by remember { mutableStateOf(listOf<String>()) }
    var products by remember { mutableStateOf(listOf<AppProduct>()) }
    var isLoading by remember { mutableStateOf(true) }

    // ... (LaunchedEffect logic giữ nguyên)
    LaunchedEffect(userId) {
        if (userId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("favorites")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Wishlist", "Error: ${error.message}")
                    isLoading = false
                    return@addSnapshotListener
                }

                val ids = snapshot?.documents
                    ?.mapNotNull { it.getString("productId") }
                    ?: emptyList()

                favoriteProductIds = ids

                if (ids.isNotEmpty()) {
                    val productList = mutableListOf<AppProduct>()
                    var loadedCount = 0

                    ids.forEach { productId ->
                        db.collection("products").document(productId)
                            .get()
                            .addOnSuccessListener { doc ->
                                // Sử dụng AppProduct (Product Model)
                                val product = doc.toObject<AppProduct>()
                                if (product != null) {
                                    productList.add(product.copy(id = doc.id))
                                }
                                loadedCount++
                                if (loadedCount == ids.size) {
                                    products = productList
                                    isLoading = false
                                }
                            }
                            .addOnFailureListener {
                                loadedCount++
                                if (loadedCount == ids.size) {
                                    products = productList
                                    isLoading = false
                                }
                            }
                    }
                } else {
                    products = emptyList()
                    isLoading = false
                }
            }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Danh sách yêu thích", color = PrimaryMaroon) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = PrimaryMaroon
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight
                )
            )
        },
        containerColor = BackgroundLight // Nền chính
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryMaroon)
            }
        } else if (products.isEmpty()) {
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
                        "Chưa có sản phẩm yêu thích",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = SecondaryDark
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Thêm sản phẩm vào yêu thích để xem lại sau",
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp) // Khoảng cách lớn hơn
            ) {
                items(products, key = { it.id }) { product ->
                    WishlistItemCard(
                        product = product,
                        onRemove = {
                            db.collection("favorites")
                                .whereEqualTo("userId", userId)
                                .whereEqualTo("productId", product.id)
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    snapshot.documents.forEach { it.reference.delete() }
                                }
                            products = products.filter { it.id != product.id }
                        },
                        onClick = { onNavigateToProductDetails(product.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun WishlistItemCard(
    product: AppProduct,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), // Bóng đổ sâu hơn
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Tăng padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hình ảnh sản phẩm
            if (product.imageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUrl),
                    contentDescription = product.name,
                    modifier = Modifier.size(90.dp).background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)) // Hình ảnh lớn hơn
                )
                Spacer(Modifier.width(16.dp))
            }

            // Thông tin sản phẩm
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = SecondaryDark
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${product.price.toInt()} VND",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp), // Giá tiền lớn, đậm và màu thương hiệu
                    color = PrimaryMaroon
                )
                if (product.category.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryDark.copy(alpha = 0.6f)
                    )
                }
            }

            // Nút Xóa
            IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Xóa khỏi yêu thích",
                    tint = MaterialTheme.colorScheme.error, // Màu đỏ hủy
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}