package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.thigiuaki.model.Product
import com.example.thigiuaki.model.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    productId: String,
    onBack: () -> Unit,
    onAddToCart: (Product, String, String) -> Unit
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

    // Load product details
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

    // Check if product is in favorites
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
                                // Avoid smart-cast issue by using a local val
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
    
    // Load reviews
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
                title = { Text("Chi tiết sản phẩm") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (userId.isNotBlank()) {
                        IconButton(
                            onClick = {
                                if (isFavorite) {
                                    // Remove from favorites
                                    db.collection("favorites")
                                        .whereEqualTo("userId", userId)
                                        .whereEqualTo("productId", productId)
                                        .get()
                                        .addOnSuccessListener { snapshot ->
                                            snapshot.documents.forEach { it.reference.delete() }
                                            isFavorite = false
                                        }
                                } else {
                                    // Add to favorites
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
                                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (product != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quantity selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- }
                            ) {
                                Text("-")
                            }
                            Text(
                                text = "$quantity",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(
                                onClick = { quantity++ }
                            ) {
                                Text("+")
                            }
                        }
                        
                        Button(
                            onClick = {
                                if (product != null && selectedSize.isNotBlank() && selectedColor.isNotBlank()) {
                                    onAddToCart(product!!, selectedSize, selectedColor)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            enabled = product != null && selectedSize.isNotBlank() && selectedColor.isNotBlank()
                        ) {
                            Text("Mua thêm")
                        }
                        
                        Button(
                            onClick = {
                                if (product != null && selectedSize.isNotBlank() && selectedColor.isNotBlank()) {
                                    // Add to cart and navigate to checkout
                                    onAddToCart(product!!, selectedSize, selectedColor)
                                    // Note: Navigation to checkout should be handled by parent
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            enabled = product != null && selectedSize.isNotBlank() && selectedColor.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Thanh toán")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (product == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Không tìm thấy sản phẩm")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Product Image
                if (product!!.imageUrl.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(product!!.imageUrl),
                        contentDescription = product!!.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )
                }

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = product!!.name,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${product!!.price.toInt()} VND",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (product!!.rating > 0) {
                            Text(
                                text = "⭐ ${String.format("%.1f", product!!.rating)} (${product!!.reviewCount})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Quick actions: add to cart & toggle favorite
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (selectedSize.isNotBlank() && selectedColor.isNotBlank()) {
                                    onAddToCart(product!!, selectedSize, selectedColor)
                                    Log.d("ProductDetails", "Add to cart quick action for product ${product!!.id}")
                                } else {
                                    Log.w("ProductDetails", "Cannot add to cart: missing size/color")
                                }
                            },
                            enabled = selectedSize.isNotBlank() && selectedColor.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Thêm vào giỏ")
                        }
                        
                        if (userId.isNotBlank()) {
                            OutlinedButton(
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
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Yêu thích",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (isFavorite) "Đã yêu thích" else "Yêu thích")
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Size Selection
                    Text(
                        text = "Kích thước:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        product!!.sizes.forEach { size ->
                            FilterChip(
                                selected = selectedSize == size,
                                onClick = { selectedSize = size },
                                label = { Text(size) }
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Color Selection
                    Text(
                        text = "Màu sắc:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        product!!.colors.forEach { color ->
                            FilterChip(
                                selected = selectedColor == color,
                                onClick = { selectedColor = color },
                                label = { Text(color) }
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Description
                    Text(
                        text = "Mô tả:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = product!!.description.ifBlank { "Không có mô tả" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Còn lại: ${product!!.stock} sản phẩm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Reviews Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đánh giá (${reviews.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (canReview) {
                            TextButton(onClick = { showReviewDialog = true }) {
                                Text("Viết đánh giá")
                            }
                        }
                    }
                    
                    if (reviews.isEmpty()) {
                        Text(
                            text = "Chưa có đánh giá nào",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        reviews.forEach { review ->
                            ReviewItem(review = review)
                            Spacer(Modifier.height(8.dp))
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

@Composable
fun ReviewItem(review: Review) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.userName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "⭐".repeat(review.rating),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium
            )
            val createdAtDate = review.createdAt?.toDate()
            if (createdAtDate != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Ngày: ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(createdAtDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

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
        title = { Text("Viết đánh giá") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Đánh giá:")
                    (1..5).forEach { star ->
                        TextButton(
                            onClick = { rating = star }
                        ) {
                            Text(
                                if (star <= rating) "⭐" else "☆",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Nhận xét") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
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
                            // Update product rating
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
                enabled = !isLoading && comment.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Gửi")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
