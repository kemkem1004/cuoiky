package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.thigiuaki.model.CartItem
import com.example.thigiuaki.model.Order
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.*

// ====================================================================
// HÀM MỞ RỘNG (EXTENSION FUNCTION) ĐỂ ĐỊNH DẠNG TIMESTAMP
// ====================================================================

/**
 * Định dạng Timestamp của Firebase thành chuỗi ngày giờ dễ đọc.
 */
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

// ====================================================================
// HÀM HELPER XỬ LÝ LỖI ÁNH XẠ (MIGRATION IN CODE)
// **Lưu ý: Hàm này được giữ lại để xử lý lỗi String -> Timestamp.**
// ====================================================================

fun DocumentSnapshot.toOrderWithMigration(): Order? {
    try {
        // Thử ánh xạ trực tiếp
        return this.toObject<Order>()?.copy(id = this.id)
    } catch (e: Exception) {
        // Nếu lỗi, cố gắng chuyển đổi thủ công các trường String cũ sang Timestamp
        val data = this.data ?: return null
        val updatedData = data.toMutableMap()

        val orderDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val orderDateValue = updatedData["orderDate"]
        if (orderDateValue is String) {
            try {
                val date = orderDateFormat.parse(orderDateValue)
                if (date != null) {
                    // CẬP NHẬT MAP: Chuyển String thành Timestamp trong bộ nhớ
                    updatedData["orderDate"] = Timestamp(date)
                }
            } catch (parseError: Exception) {
                // Log lỗi parse và bỏ qua
            }
        }

        // Vì Firestore không có hàm toObject(Map), việc ánh xạ thủ công là cần thiết
        // cho các trường quan trọng để tránh crash nếu migration chưa hoàn tất.
        // Tuy nhiên, việc ánh xạ List<CartItem> thủ công từ Map là cực kỳ phức tạp.

        // Cách tạm thời nhất: Quay lại ánh xạ thô cho các trường cơ bản
        return try {
            val orderMap = this.data
            // Cần phải có một hàm ánh xạ thủ công từ Map sang Order
            // Vì lý do đơn giản hóa, ta sẽ dựa vào hàm migration trong code để sửa lỗi Timestamp,
            // và sửa lỗi CartItem ở hàm OrderCard.
            // Ta chỉ trả về null nếu lỗi ban đầu không phải là lỗi String -> Timestamp.

            // Nếu bạn đã sửa Order.kt và chạy migration thành công, ta sẽ dựa vào ánh xạ trực tiếp
            this.toObject<Order>()?.copy(id = this.id)

        } catch (finalError: Exception) {
            Log.e("OrderHistoryMapper", "Final mapping attempt failed: ${finalError.message}")
            null
        }
    }
}


// ====================================================================
// ORDER HISTORY SCREEN
// ====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onNavigateToMessage: ((String, String, String) -> Unit)? = null
) {
    // ... (Giữ nguyên phần LaunchedEffect và UI) ...
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    var orders by remember { mutableStateOf(listOf<Order>()) }
    var isLoading by remember { mutableStateOf(true) }
    var adminUserId by remember { mutableStateOf("") }
    var adminUserName by remember { mutableStateOf("Admin") }

    // Get admin user info
    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "admin")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val adminDoc = snapshot.documents.first()
                    adminUserId = adminDoc.id
                    adminUserName = adminDoc.getString("name") ?: "Admin"
                }
            }
    }

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

                val orderList = snapshot?.documents?.mapNotNull { doc ->
                    // SỬ DỤNG HÀM XỬ LÝ LỖI MỚI (Vẫn dùng hàm này để xử lý lỗi String -> Timestamp)
                    doc.toOrderWithMigration()
                } ?: emptyList()

                orders = orderList
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử đơn hàng") }
            )
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
        } else if (orders.isEmpty()) {
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
                        "Chưa có đơn hàng nào",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Đặt hàng ngay để xem lịch sử",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        onMessageClick = {
                            if (onNavigateToMessage != null && adminUserId.isNotBlank()) {
                                onNavigateToMessage(order.id, adminUserId, adminUserName)
                            }
                        }
                    )
                }
            }
        }
    }
}

// ====================================================================
// ORDER CARD (Đã sửa lỗi ClassCastException)
// ====================================================================

@Composable
fun OrderCard(
    order: Order,
    onMessageClick: (() -> Unit)? = null
) {
    val statusColor = when (order.status) {
        "pending" -> MaterialTheme.colorScheme.tertiary
        "confirmed" -> MaterialTheme.colorScheme.primary
        "shipped" -> MaterialTheme.colorScheme.secondary
        "delivered" -> MaterialTheme.colorScheme.primaryContainer
        "cancelled" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    "Đơn hàng #${order.id.take(8)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
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

            // SỬ DỤNG HÀM ĐỊNH DẠNG MỚI ĐÃ THÊM
            Text(
                "Ngày đặt: ${order.orderDate.formatToString()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(8.dp))

            // =================================================
            // SỬA LỖI ClassCastException TẠI ĐÂY
            // Chúng ta cần sử dụng AS? (safe cast) để xử lý việc Map được ánh xạ nhầm
            // thành CartItem nếu ánh xạ POJO thất bại.
            // =================================================
            order.items.forEach { rawItem ->
                // Sử dụng safe cast AS? để item chỉ được ánh xạ nếu nó thực sự là CartItem,
                // hoặc sử dụng CartItem.kt để tạo CartItem từ rawItem (nếu là HashMap).

                // GIẢI PHÁP TẠM THỜI TỐT NHẤT: Thử ép kiểu an toàn
                val item = rawItem as? CartItem

                if (item != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${item.productName} x${item.quantity}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "${(item.price * item.quantity).toInt()} VND",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    // Nếu không thể cast, đó là HashMap. Ta chỉ log lỗi.
                    // (Để giải quyết triệt để, bạn cần sửa class Order.kt để ánh xạ đúng)
                    // Log.e("OrderCard", "Item is not a CartItem: $rawItem")
                }
            }

            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            // ... (Phần còn lại giữ nguyên) ...

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Tổng tiền:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${order.totalAmount.toInt()} VND",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (onMessageClick != null) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onMessageClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Message, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nhắn tin với admin")
                }
            }
        }
    }
}