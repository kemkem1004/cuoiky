package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.thigiuaki.model.CartItem
import com.example.thigiuaki.model.Order
import com.example.thigiuaki.model.Review
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.*

/** Hàm mở rộng để định dạng Timestamp thành chuỗi dễ đọc */
fun Timestamp?.formatToString(format: String = "dd/MM/yyyy HH:mm"): String {
    return if (this != null) {
        try {
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            sdf.format(this.toDate())
        } catch (e: Exception) {
            "Lỗi định dạng"
        }
    } else {
        "N/A"
    }
}

/** Migration xử lý lỗi String -> Timestamp */
fun DocumentSnapshot.toOrderWithMigration(): Order? {
    return try {
        this.toObject<Order>()?.copy(id = this.id)
    } catch (e: Exception) {
        val data = this.data ?: return null
        val updatedData = data.toMutableMap()
        val orderDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val orderDateValue = updatedData["orderDate"]
        if (orderDateValue is String) {
            try {
                val date = orderDateFormat.parse(orderDateValue)
                if (date != null) updatedData["orderDate"] = Timestamp(date)
            } catch (_: Exception) {}
        }
        this.toObject<Order>()?.copy(id = this.id)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    var orders by remember { mutableStateOf(listOf<Order>()) }
    var reviews by remember { mutableStateOf(listOf<Review>()) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog đánh giá
    var showReviewDialog by remember { mutableStateOf(false) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var reviewText by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(5) }

    // Mở dialog đánh giá/sửa đánh giá
    fun openReviewDialog(order: Order, existingReview: Review? = null) {
        selectedOrder = order
        reviewText = existingReview?.comment ?: ""
        rating = existingReview?.rating ?: 5
        showReviewDialog = true
    }

    LaunchedEffect(userId) {
        if (userId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

        // Lấy đơn hàng
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("OrderHistory", "Error: ${error.message}")
                    isLoading = false
                    return@addSnapshotListener
                }
                orders = snapshot?.documents?.mapNotNull { it.toOrderWithMigration() } ?: emptyList()
            }

        // Lấy đánh giá
        db.collection("reviews")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                reviews = snapshot?.documents?.mapNotNull { it.toObject<Review>()?.copy(id = it.id) } ?: emptyList()
                isLoading = false
            }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lịch sử đơn hàng") }) }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else if (orders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Chưa có đơn hàng nào", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Đặt hàng ngay để xem lịch sử", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders, key = { it.id }) { order ->
                    val orderReview = reviews.find { it.orderId == order.id }
                    OrderCard(
                        order = order,
                        review = orderReview,
                        onReviewClick = { openReviewDialog(order, orderReview) }
                    )
                }
            }
        }

        // Dialog đánh giá
        if (showReviewDialog && selectedOrder != null) {
            AlertDialog(
                onDismissRequest = { showReviewDialog = false },
                title = { Text("Đánh giá đơn hàng") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = reviewText,
                            onValueChange = { reviewText = it },
                            label = { Text("Nội dung đánh giá") },
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Đánh giá sao: $rating")
                        Slider(
                            value = rating.toFloat(),
                            onValueChange = { rating = it.toInt() },
                            valueRange = 1f..5f,
                            steps = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val user = auth.currentUser
                            if (user != null && selectedOrder != null) {
                                // Kiểm tra xem đã có review chưa
                                val existingReview = reviews.find { it.orderId == selectedOrder!!.id }
                                if (existingReview != null) {
                                    // Cập nhật review
                                    db.collection("reviews").document(existingReview.id!!)
                                        .update(
                                            mapOf(
                                                "comment" to reviewText,
                                                "rating" to rating,
                                                "createdAt" to Timestamp.now()
                                            )
                                        )
                                } else {
                                    // Tạo mới review
                                    val review = hashMapOf(
                                        "userId" to user.uid,
                                        "userName" to (user.displayName ?: "Người dùng"),
                                        "orderId" to selectedOrder!!.id,
                                        "comment" to reviewText,
                                        "rating" to rating,
                                        "createdAt" to Timestamp.now(),
                                        "adminReply" to null
                                    )
                                    db.collection("reviews").add(review)
                                }
                            }
                            showReviewDialog = false
                        },
                        enabled = reviewText.isNotBlank()
                    ) { Text("Gửi đánh giá") }
                },
                dismissButton = {
                    TextButton(onClick = { showReviewDialog = false }) { Text("Hủy") }
                }
            )
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    review: Review? = null,
    onReviewClick: (() -> Unit)? = null
) {
    val statusColor = when (order.status) {
        "pending" -> MaterialTheme.colorScheme.tertiary
        "confirmed" -> MaterialTheme.colorScheme.primary
        "shipped" -> MaterialTheme.colorScheme.secondary
        "delivered" -> MaterialTheme.colorScheme.primaryContainer
        "cancelled" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Đơn hàng #${order.id.take(8)}", style = MaterialTheme.typography.titleMedium)
                Surface(color = statusColor.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) {
                    Text(
                        text = when (order.status) {
                            "pending" -> "Chờ xử lý"
                            "confirmed" -> "Đã xác nhận"
                            "shipped" -> "Đang giao"
                            "delivered" -> "Đã giao"
                            "cancelled" -> "Đã hủy"
                            else -> order.status
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Ngày đặt: ${order.orderDate.formatToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)

            Spacer(Modifier.height(8.dp))
            order.items.forEach { rawItem ->
                val item = rawItem as? CartItem
                if (item != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.productName} x${item.quantity}", style = MaterialTheme.typography.bodyMedium)
                        Text("${(item.price * item.quantity).toInt()} VND", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tổng tiền:", style = MaterialTheme.typography.titleMedium)
                Text("${order.totalAmount.toInt()} VND", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(8.dp))

            // Hiển thị đánh giá + nút sửa
            if (review != null) {
                Divider()
                Spacer(Modifier.height(4.dp))
                Text("Đánh giá của bạn:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text("⭐".repeat(review.rating), style = MaterialTheme.typography.bodyMedium)
                Text(review.comment, style = MaterialTheme.typography.bodyMedium)

                if (onReviewClick != null) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onReviewClick, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Star, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sửa đánh giá")
                    }
                }
            } else if (onReviewClick != null) {
                Button(onClick = onReviewClick, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Đánh giá đơn hàng")
                }
            }
        }
    }
}
