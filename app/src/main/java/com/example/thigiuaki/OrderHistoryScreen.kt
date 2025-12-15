package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

// =================================================================
// 1. ĐỊNH NGHĨA MÀU SẮC (Clean Retail Design)
// =================================================================
private val BackgroundLight = Color(0xFFFAF9F6)
private val PrimaryMaroon = Color(0xFF8D021F)
private val SecondaryDark = Color(0xFF424242)
private val CardBackground = Color.White
private val StatusPending = Color(0xFFFFA000)      // Vàng cam
private val StatusConfirmedShipped = Color(0xFF1976D2) // Xanh dương
private val StatusDelivered = Color(0xFF388E3C)     // Xanh lá
private val StatusCancelled = Color(0xFFD32F2F)     // Đỏ hủy
private val GoldStar = Color(0xFFFFC72C)
// =================================================================

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
    // ... (Logic migration giữ nguyên)
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

    // Logic tải dữ liệu (Giữ nguyên)
    LaunchedEffect(userId) {
        if (userId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }

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

        db.collection("reviews")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                reviews = snapshot?.documents?.mapNotNull { it.toObject<Review>()?.copy(id = it.id) } ?: emptyList()
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử đơn hàng", color = PrimaryMaroon) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = PrimaryMaroon) }
        } else if (orders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Chưa có đơn hàng nào",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = SecondaryDark
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Đặt hàng ngay để xem lịch sử",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryDark.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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

        // Dialog đánh giá (Đã áp dụng styling Clean Retail)
        if (showReviewDialog && selectedOrder != null) {
            AlertDialog(
                onDismissRequest = { showReviewDialog = false },
                title = { Text("Đánh giá đơn hàng #${selectedOrder!!.id.take(8)}", color = PrimaryMaroon) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = reviewText,
                            onValueChange = { reviewText = it },
                            label = { Text("Nội dung đánh giá") },
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryMaroon,
                                focusedLabelColor = PrimaryMaroon,
                                cursorColor = PrimaryMaroon
                            )
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Mức đánh giá:", style = MaterialTheme.typography.titleMedium, color = SecondaryDark)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⭐".repeat(rating), style = MaterialTheme.typography.titleLarge.copy(color = GoldStar))
                            Text(" ($rating/5)", style = MaterialTheme.typography.bodyLarge, color = SecondaryDark.copy(alpha = 0.7f))
                        }

                        Slider(
                            value = rating.toFloat(),
                            onValueChange = { rating = it.toInt() },
                            valueRange = 1f..5f,
                            steps = 3,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryMaroon,
                                activeTrackColor = PrimaryMaroon,
                                inactiveTrackColor = PrimaryMaroon.copy(alpha = 0.3f)
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val user = auth.currentUser
                            if (user != null && selectedOrder != null) {


                                db.collection("users").document(user.uid).get()
                                    .addOnSuccessListener { userDoc ->
                                        val userName = userDoc.getString("name") ?: "Người dùng"

                                        val review = hashMapOf(
                                            "userId" to user.uid,
                                            "userName" to userName,
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
                        enabled = reviewText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryMaroon)
                    ) { Text(if (reviews.find { it.orderId == selectedOrder!!.id } != null) "Cập nhật" else "Gửi đánh giá") }
                },
                dismissButton = {
                    TextButton(onClick = { showReviewDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = SecondaryDark)) { Text("Hủy") }
                },
                containerColor = CardBackground
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
    val (statusColor, statusText) = when (order.status) {
        "pending" -> Pair(StatusPending, "Chờ xử lý")
        "confirmed" -> Pair(StatusConfirmedShipped, "Đã xác nhận")
        "shipped" -> Pair(StatusConfirmedShipped, "Đang giao")
        "delivered" -> Pair(StatusDelivered, "Đã giao")
        "cancelled" -> Pair(StatusCancelled, "Đã hủy")
        else -> Pair(SecondaryDark, order.status)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Hàng 1: ID & Trạng thái
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Đơn hàng #${order.id.take(8)}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SecondaryDark
                )
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, statusColor)
                ) {
                    Text(
                        text = statusText.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Ngày đặt: ${order.orderDate.formatToString()}",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryDark.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(12.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            // Chi tiết sản phẩm
            order.items.forEach { rawItem ->
                val item = rawItem as? CartItem
                if (item != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${item.productName} x${item.quantity}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryDark
                        )
                        Text(
                            "${(item.price * item.quantity).toInt()} VND",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            // Tổng tiền
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "TỔNG TIỀN:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = SecondaryDark
                )
                Text(
                    "${order.totalAmount.toInt()} VND",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                    color = PrimaryMaroon // Màu nhấn
                )
            }

            Spacer(Modifier.height(16.dp))

            // Hiển thị đánh giá + nút sửa/đánh giá
            if (review != null) {
                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
                Text("Đánh giá của bạn:", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = PrimaryMaroon)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐".repeat(review.rating), style = MaterialTheme.typography.titleMedium.copy(color = GoldStar))
                    Text(" (${review.rating}/5)", style = MaterialTheme.typography.bodyMedium, color = SecondaryDark.copy(alpha = 0.7f))
                }

                Text(review.comment, style = MaterialTheme.typography.bodyMedium, color = SecondaryDark)

                if (onReviewClick != null) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onReviewClick,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, PrimaryMaroon),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryMaroon)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("SỬA ĐÁNH GIÁ")
                    }
                }
            } else if (order.status == "delivered" && onReviewClick != null) {
                // Chỉ cho phép đánh giá nếu đã giao hàng
                Button(
                    onClick = onReviewClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMaroon)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ĐÁNH GIÁ ĐƠN HÀNG")
                }
            } else if (order.status == "cancelled") {
                Text(
                    "Đơn hàng đã hủy. Không thể đánh giá.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusCancelled
                )
            }
        }
    }
}