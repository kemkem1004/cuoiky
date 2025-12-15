package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.wear.compose.material.ChipDefaults // <-- ĐÃ XÓA DÒNG NÀY GÂY LỖI
import androidx.compose.material3.FilterChipDefaults // <-- DÙNG IMPORT CHÍNH XÁC NÀY
import coil.compose.rememberAsyncImagePainter
import com.example.thigiuaki.model.Product
import com.example.thigiuaki.model.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.util.Date
import java.util.concurrent.TimeUnit

// =================================================================
// 1. ĐỊNH NGHĨA MÀU SẮC (Clean Retail Design)
// =================================================================
private val BackgroundLight = Color(0xFFFAF9F6)
private val PrimaryMaroon = Color(0xFF8D021F)
private val SecondaryDark = Color(0xFF424242)
private val CardBackground = Color.White
private val StatusError = Color(0xFFD32F2F)
private val GoldStar = Color(0xFFFFC72C)
// =================================================================


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    productId: String,
    onBack: () -> Unit,
    onAddToCart: (Product, String, String, Int) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    var product by remember { mutableStateOf<Product?>(null) }
    var selectedSize by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }
    var isFavorite by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var reviews by remember { mutableStateOf(listOf<Review>()) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var canReview by remember { mutableStateOf(false) }

    // Load product details (Logic giữ nguyên)
    LaunchedEffect(productId) {
        db.collection("products").document(productId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProductDetails", "Error: ${error.message}")
                    isLoading = false
                    return@addSnapshotListener
                }

                val p = snapshot?.toObject(Product::class.java)
                if (p != null) {
                    product = p.copy(id = snapshot.id)
                    selectedSize = p.sizes.firstOrNull() ?: ""
                    selectedColor = p.colors.firstOrNull() ?: ""
                    isLoading = false
                }
            }
    }

    // Check favorites & Can Review logic (Giữ nguyên)
    LaunchedEffect(productId, userId) {
        if (userId.isNotBlank() && productId.isNotBlank()) {
            db.collection("favorites")
                .whereEqualTo("userId", userId)
                .whereEqualTo("productId", productId)
                .get()
                .addOnSuccessListener { snapshot ->
                    isFavorite = !snapshot.isEmpty
                }

            // Check if user can review (has delivered order within 7 days)
            db.collection("orders")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "delivered")
                .get()
                .addOnSuccessListener { ordersSnapshot ->
                    ordersSnapshot.documents.forEach { orderDoc ->
                        val order = orderDoc.toObject<com.example.thigiuaki.model.Order>()
                        order?.items?.forEach { item ->
                            if (item.productId == productId) {
                                val deliveredAtDate = order.deliveredAt?.toDate()
                                if (deliveredAtDate != null) {
                                    val daysSinceDelivery = TimeUnit.MILLISECONDS.toDays(
                                        Date().time - deliveredAtDate.time
                                    )
                                    if (daysSinceDelivery <= 7) {
                                        // Check if already reviewed
                                        db.collection("reviews")
                                            .whereEqualTo("userId", userId)
                                            .whereEqualTo("productId", productId)
                                            .get()
                                            .addOnSuccessListener { reviewsSnapshot ->
                                                if (reviewsSnapshot.isEmpty) {
                                                    canReview = true
                                                }
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    // Load reviews (Logic giữ nguyên)
    LaunchedEffect(productId) {
        db.collection("reviews")
            .whereEqualTo("productId", productId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val reviewList = snapshot?.documents?.mapNotNull { doc ->
                    val review = doc.toObject<Review>()
                    review?.copy(id = doc.id)
                } ?: emptyList()
                reviews = reviewList
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết sản phẩm", color = PrimaryMaroon) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại", tint = PrimaryMaroon)
                    }
                },
                actions = {
                    if (userId.isNotBlank()) {
                        IconButton(
                            onClick = {
                                if (isFavorite) {
                                    db.collection("favorites")
                                        .whereEqualTo("userId", userId)
                                        .whereEqualTo("productId", productId)
                                        .get()
                                        .addOnSuccessListener { snapshot ->
                                            snapshot.documents.forEach { it.reference.delete() }
                                            isFavorite = false
                                        }
                                } else {
                                    val favoriteData = hashMapOf(
                                        "userId" to userId,
                                        "productId" to productId,
                                        "addedAt" to com.google.firebase.Timestamp.now()
                                    )
                                    db.collection("favorites").add(favoriteData)
                                        .addOnSuccessListener { isFavorite = true }
                                }
                            }
                        ) {
                            Icon(
                                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Yêu thích",
                                tint = if (isFavorite) StatusError else SecondaryDark
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        bottomBar = {
            if (product != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 10.dp, // Độ nổi lớn hơn
                    color = CardBackground
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quantity selector (Đã tùy chỉnh)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                .padding(horizontal = 4.dp)
                        ) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                enabled = quantity > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Giảm", tint = SecondaryDark)
                            }
                            Text(
                                text = "$quantity",
                                modifier = Modifier.padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SecondaryDark
                            )
                            IconButton(
                                onClick = { quantity++ }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tăng", tint = SecondaryDark)
                            }
                        }

                        // Nút Thêm vào giỏ (Thanh toán)
                        Button(
                            onClick = {
                                if (product != null && selectedSize.isNotBlank() && selectedColor.isNotBlank()) {
                                    onAddToCart(product!!, selectedSize, selectedColor, quantity) // <-- thêm quantity
                                }
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(56.dp),
                            enabled = product != null && selectedSize.isNotBlank() && selectedColor.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryMaroon
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("THÊM VÀO GIỎ", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        containerColor = BackgroundLight
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
        } else if (product == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Không tìm thấy sản phẩm", color = SecondaryDark)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .background(CardBackground) // Nền trắng cho khu vực nội dung chính
            ) {
                // Product Image
                if (product!!.imageUrl.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(product!!.imageUrl),
                        contentDescription = product!!.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(450.dp) // Ảnh lớn hơn
                    )
                }

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = product!!.name,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = SecondaryDark
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "${product!!.price.toInt()} VND",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = PrimaryMaroon // Giá tiền nổi bật
                        )
                        if (product!!.rating > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = GoldStar, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", product!!.rating),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = " (${product!!.reviewCount})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SecondaryDark.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))

                    // Size Selection
                    Text(
                        text = "Kích thước:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = SecondaryDark
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        product!!.sizes.forEach { size ->
                            FilterChip(
                                selected = selectedSize == size,
                                onClick = { selectedSize = size },
                                label = { Text(size) },

                                border = if (selectedSize == size) null else ButtonDefaults.outlinedButtonBorder
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Color Selection
                    Text(
                        text = "Màu sắc:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = SecondaryDark
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        product!!.colors.forEach { color ->
                            FilterChip(
                                selected = selectedColor == color,
                                onClick = { selectedColor = color },
                                label = { Text(color) },

                                border = if (selectedColor == color) null else ButtonDefaults.outlinedButtonBorder
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Description
                    Text(
                        text = "Mô tả chi tiết:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryMaroon
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = product!!.description.ifBlank { "Không có mô tả chi tiết cho sản phẩm này." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = SecondaryDark
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Còn lại: ${product!!.stock} sản phẩm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryDark.copy(alpha = 0.7f)
                    )

                    Spacer(Modifier.height(24.dp))
                    Divider(color = Color.LightGray)
                    Spacer(Modifier.height(24.dp))

                    // Reviews Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đánh giá Khách hàng (${reviews.size})",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryMaroon
                        )
                        if (canReview) {
                            TextButton(onClick = { showReviewDialog = true }) {
                                Text("Viết đánh giá", color = PrimaryMaroon, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (reviews.isEmpty()) {
                        Text(
                            text = "Chưa có đánh giá nào cho sản phẩm này.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryDark.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        Spacer(Modifier.height(12.dp))
                        reviews.forEach { review ->
                            ReviewItem(review = review)
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        if (showReviewDialog) {
            ReviewDialog(
                productId = productId,
                onDismiss = { showReviewDialog = false },
                onReviewSubmitted = {
                    showReviewDialog = false
                    canReview = false
                }
            )
        }
    }
}

// XÓA ĐỊNH NGHĨA HÀM KHÔNG CẦN THIẾT NÀY
// private fun ChipDefaults.filterChipColors(...) { ... }

// =================================================================
// ReviewItem (Đã tùy chỉnh)
// =================================================================
@Composable
fun ReviewItem(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundLight),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.userName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = SecondaryDark
                )
                Text(
                    text = "⭐".repeat(review.rating),
                    style = MaterialTheme.typography.titleLarge.copy(color = GoldStar)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyLarge,
                color = SecondaryDark.copy(alpha = 0.9f)
            )
            val createdAtDate = review.createdAt?.toDate()
            if (createdAtDate != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Ngày: ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(createdAtDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryDark.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// =================================================================
// ReviewDialog (Đã tùy chỉnh)
// =================================================================
@Composable
fun ReviewDialog(
    productId: String,
    onDismiss: () -> Unit,
    onReviewSubmitted: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    val userName = auth.currentUser?.displayName ?: "Khách hàng"

    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Viết đánh giá", color = PrimaryMaroon) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Chọn số sao:", style = MaterialTheme.typography.titleMedium, color = SecondaryDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { star ->
                        Button(
                            onClick = { rating = star },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (star <= rating) GoldStar else Color.LightGray
                            ),
                            modifier = Modifier.weight(1f),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Nhận xét của bạn") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryMaroon,
                        focusedLabelColor = PrimaryMaroon,
                        cursorColor = PrimaryMaroon
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    val reviewData = hashMapOf(
                        "productId" to productId,
                        "userId" to userId,
                        "userName" to userName,
                        "rating" to rating,
                        "comment" to comment,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                    db.collection("reviews").add(reviewData)
                        .addOnSuccessListener {
                            // Update product rating logic (Giữ nguyên)
                            db.collection("reviews")
                                .whereEqualTo("productId", productId)
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    val allReviews = snapshot.documents.mapNotNull { doc ->
                                        doc.getLong("rating")?.toInt()
                                    }
                                    if (allReviews.isNotEmpty()) {
                                        val avgRating = allReviews.average()
                                        val reviewCount = allReviews.size
                                        db.collection("products").document(productId)
                                            .update(
                                                "rating", avgRating,
                                                "reviewCount", reviewCount
                                            )
                                    }
                                }
                            isLoading = false
                            onReviewSubmitted()
                        }
                        .addOnFailureListener {
                            isLoading = false
                        }
                },
                enabled = !isLoading && comment.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryMaroon)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("GỬI ĐÁNH GIÁ")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = SecondaryDark)) {
                Text("Hủy")
            }
        },
        containerColor = CardBackground
    )
}