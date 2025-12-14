package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.wear.compose.material.ChipDefaults
import com.example.thigiuaki.model.Order
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.util.* // =================================================================
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
// =================================================================

// *Giả định hàm toDateTimeString() được định nghĩa/import ở nơi khác để tránh lỗi*
fun Timestamp?.toDateTimeString(): String {
    return if (this != null) {
        // Đây là giá trị placeholder. Bạn cần đảm bảo hàm này có sẵn.
        "N/A (Cần format)"
    } else {
        "N/A"
    }
}
// ====================================================================


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderManagementScreen() {
    val db = FirebaseFirestore.getInstance()
    var orders by remember { mutableStateOf(listOf<Order>()) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // State cho chức năng Hủy đơn hàng (Giữ nguyên)
    var showCancelDialog by remember { mutableStateOf<Order?>(null) }
    var cancelReason by remember { mutableStateOf("") }
    var isCancelling by remember { mutableStateOf(false) }

    // Hàm xử lý hủy đơn hàng (Giữ nguyên logic)
    val handleCancelOrder: (Order, String) -> Unit = { orderToCancel, reason ->
        isCancelling = true
        db.collection("orders").document(orderToCancel.id)
            .update(
                "status", "cancelled",
                "cancelledAt", Timestamp.now(),
                "cancellationReason", reason
            )
            .addOnSuccessListener {
                Log.d("AdminOrders", "Order ${orderToCancel.id} cancelled.")
                showCancelDialog = null
                isCancelling = false
                cancelReason = ""
            }
            .addOnFailureListener { e ->
                Log.e("AdminOrders", "Error cancelling order: ${e.message}")
                isCancelling = false
            }
    }

    // LaunchedEffect (Logic tải dữ liệu giữ nguyên)
    LaunchedEffect(selectedStatus) {
        var query = db.collection("orders").orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)

        if (selectedStatus != null) {
            query = query.whereEqualTo("status", selectedStatus)
        }

        query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AdminOrders", "Error: ${error.message}")
                isLoading = false
                return@addSnapshotListener
            }

            // FIX LỖI ĐỌC DỮ LIỆU: Dùng try-catch (Giữ nguyên logic migration của bạn)
            val orderList = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val order = doc.toObject<Order>()
                    order?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("AdminOrders", "Skipping crashing document: ${doc.id}, Error: ${e.message}")
                    null
                }
            } ?: emptyList()

            orders = orderList
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý đơn hàng", color = PrimaryMaroon, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = PrimaryMaroon
                ),
                actions = {
                    IconButton(onClick = { /* Refresh */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới", tint = PrimaryMaroon)
                    }
                }
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Filter chips (Đã tùy chỉnh giao diện)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusOptions = mapOf(
                    null to "Tất cả",
                    "pending" to "Chờ xử lý",
                    "confirmed" to "Đã xác nhận",
                    "shipped" to "Đang giao", // Thêm trạng thái này nếu bạn dùng trong OrderCard
                    "delivered" to "Đã giao",
                    "cancelled" to "Đã hủy"
                )

                statusOptions.forEach { (statusKey, statusLabel) ->
                    FilterChip(
                        selectedStatus == statusKey,
                        { selectedStatus = statusKey },
                        { Text(statusLabel, fontWeight = FontWeight.SemiBold) },

                        border = if (selectedStatus == statusKey) null else BorderStroke(1.dp, Color.LightGray)
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryMaroon)
                }
            } else if (orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Không có đơn hàng nào", style = MaterialTheme.typography.titleMedium, color = SecondaryDark)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(orders) { order ->
                        AdminOrderCard(
                            order = order,
                            // Logic onStatusChange giữ nguyên
                            onStatusChange = { newStatus, isProcessed ->
                                db.collection("orders").document(order.id)
                                    .update(
                                        "status", newStatus,
                                        "isProcessed", isProcessed
                                    )
                                    .addOnSuccessListener {
                                        if (newStatus == "confirmed") {
                                            db.collection("orders").document(order.id)
                                                .update("confirmedAt", Timestamp.now())
                                        } else if (newStatus == "delivered") {
                                            db.collection("orders").document(order.id)
                                                .update("deliveredAt", Timestamp.now())
                                        }
                                    }
                            },
                            onCancelClick = {
                                showCancelDialog = order
                                cancelReason = ""
                            }
                        )
                    }
                }
            }
        }

        // --- Hộp thoại Hủy đơn hàng (Đã tùy chỉnh giao diện) ---
        if (showCancelDialog != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!isCancelling) showCancelDialog = null
                },
                title = { Text("Hủy đơn hàng #${showCancelDialog!!.id.take(8)}", color = PrimaryMaroon) },
                text = {
                    Column {
                        Text("Nhập lý do hủy đơn hàng:", style = MaterialTheme.typography.titleMedium, color = SecondaryDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = cancelReason,
                            onValueChange = { cancelReason = it },
                            label = { Text("Lý do") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCancelling,
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
                            if (cancelReason.isNotBlank()) {
                                handleCancelOrder(showCancelDialog!!, cancelReason)
                            }
                        },
                        enabled = cancelReason.isNotBlank() && !isCancelling,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCancelled)
                    ) {
                        if (isCancelling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Xác nhận hủy", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCancelDialog = null },
                        enabled = !isCancelling,
                        colors = ButtonDefaults.textButtonColors(contentColor = SecondaryDark)
                    ) {
                        Text("Đóng")
                    }
                },
                containerColor = CardBackground
            )
        }
    }
}

@Composable
fun AdminOrderCard(
    order: Order,
    onStatusChange: (String, Boolean) -> Unit,
    onCancelClick: () -> Unit
) {
    val (statusColor, statusText) = when (order.status) {
        "pending" -> Pair(StatusPending, "Chờ xử lý")
        "confirmed" -> Pair(StatusConfirmedShipped, "Đã xác nhận")
        "shipped" -> Pair(StatusConfirmedShipped, "Đang giao") // Thêm trạng thái "shipped" để xử lý
        "delivered" -> Pair(StatusDelivered, "Đã giao")
        "cancelled" -> Pair(StatusCancelled, "Đã hủy")
        else -> Pair(SecondaryDark, order.status)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Hàng 1: ID, Ngày và Trạng thái
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Đơn hàng #${order.id.take(8)}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SecondaryDark
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ngày đặt: ${order.orderDate.toDateTimeString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryDark.copy(alpha = 0.7f)
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, statusColor) // Thêm border cho trạng thái
                ) {
                    Text(
                        text = statusText.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = statusColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            // Chi tiết tiền & Ghi chú
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Tổng tiền:",
                    style = MaterialTheme.typography.titleMedium,
                    color = SecondaryDark
                )
                Text(
                    "${order.totalAmount.toInt()} VND",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
                    color = PrimaryMaroon
                )
            }
            Spacer(Modifier.height(8.dp))

            // Ghi chú/Lý do Hủy
            if (order.status == "cancelled" && (order.cancellationReason ?: "").isNotBlank()) {
                Text(
                    "Lý do hủy: ${order.cancellationReason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusCancelled
                )
            } else if ((order.orderNotes ?: "").isNotBlank()) {
                Text(
                    "Ghi chú: ${order.orderNotes}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryDark.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            // Status change buttons (Đã tùy chỉnh giao diện và logic)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chỉ hiển thị các nút hành động nếu đơn hàng chưa bị hủy và chưa giao
                if (order.status == "pending" || order.status == "confirmed" || order.status == "shipped") {

                    // Nút Hủy (Destructive Action - Luôn có)
                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StatusCancelled
                        ),
                        border = BorderStroke(1.dp, StatusCancelled)
                    ) {
                        Text("HỦY ĐƠN")
                    }

                    if (order.status == "pending") {
                        // Nút xác nhận (Primary Action)
                        Button(
                            onClick = { onStatusChange("confirmed", false) }, // Gửi đi trạng thái confirmed
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryMaroon)
                        ) {
                            Text("XÁC NHẬN")
                        }
                    }

                    if (order.status == "confirmed" || order.status == "shipped") {
                        // Nút Giao/Hoàn thành
                        Button(
                            onClick = { onStatusChange("delivered", true) }, // Gửi đi trạng thái delivered
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusDelivered)
                        ) {
                            Text(if (order.status == "confirmed") "ĐANG GIAO" else "ĐÃ GIAO")
                        }
                    }
                } else if (!order.isProcessed && order.status != "delivered" && order.status != "cancelled") {
                    // Nút Đánh dấu đã xử lý (nếu có các trạng thái trung gian khác)
                    OutlinedButton(
                        onClick = { onStatusChange(order.status, true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Đánh dấu đã xử lý")
                    }
                } else if (order.status == "delivered") {
                    Text("Đơn hàng đã hoàn thành.", style = MaterialTheme.typography.bodyMedium, color = StatusDelivered)
                } else if (order.status == "cancelled") {
                    Text("Đơn hàng đã bị hủy.", style = MaterialTheme.typography.bodyMedium, color = StatusCancelled)
                }
            }
        }
    }
}