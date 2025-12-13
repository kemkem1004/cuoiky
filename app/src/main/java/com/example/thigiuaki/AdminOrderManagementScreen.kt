package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.thigiuaki.model.Order
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.util.* // LƯU Ý: HÀM formatToString ĐÃ ĐƯỢC ĐỔI TÊN THÀNH toDateTimeString TRONG FILE EXTENSIONS.KT


// ====================================================================
// ADMIN ORDER MANAGEMENT SCREEN
// ====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderManagementScreen() {
    val db = FirebaseFirestore.getInstance()
    var orders by remember { mutableStateOf(listOf<Order>()) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // State cho chức năng Hủy đơn hàng
    var showCancelDialog by remember { mutableStateOf<Order?>(null) }
    var cancelReason by remember { mutableStateOf("") }
    var isCancelling by remember { mutableStateOf(false) }

    // Hàm xử lý hủy đơn hàng
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

            // FIX LỖI ĐỌC DỮ LIỆU: Dùng try-catch để bỏ qua tài liệu lỗi ánh xạ (String/Timestamp hoặc HashMap/CartItem)
            val orderList = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val order = doc.toObject<Order>()
                    order?.copy(id = doc.id)
                } catch (e: Exception) {
                    // Log tài liệu gây lỗi và bỏ qua nó
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
                title = { Text("Quản lý đơn hàng") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { selectedStatus = null },
                    label = { Text("Tất cả") }
                )
                FilterChip(
                    selected = selectedStatus == "pending",
                    onClick = { selectedStatus = "pending" },
                    label = { Text("Chờ xử lý") }
                )
                FilterChip(
                    selected = selectedStatus == "confirmed",
                    onClick = { selectedStatus = "confirmed" },
                    label = { Text("Đã xác nhận") }
                )
                FilterChip(
                    selected = selectedStatus == "delivered",
                    onClick = { selectedStatus = "delivered" },
                    label = { Text("Đã giao") }
                )
                FilterChip(
                    selected = selectedStatus == "cancelled",
                    onClick = { selectedStatus = "cancelled" },
                    label = { Text("Đã hủy") }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Không có đơn hàng nào")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders) { order ->
                        AdminOrderCard(
                            order = order,
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
                                // Mở dialog hủy đơn hàng
                                showCancelDialog = order
                                cancelReason = ""
                            }
                        )
                    }
                }
            }
        }

        // --- Hộp thoại Hủy đơn hàng ---
        if (showCancelDialog != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!isCancelling) showCancelDialog = null
                },
                title = { Text("Hủy đơn hàng #${showCancelDialog!!.id.take(8)}") },
                text = {
                    Column {
                        Text("Nhập lý do hủy đơn hàng:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = cancelReason,
                            onValueChange = { cancelReason = it },
                            label = { Text("Lý do") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCancelling
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
                        enabled = cancelReason.isNotBlank() && !isCancelling
                    ) {
                        if (isCancelling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Xác nhận hủy")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCancelDialog = null },
                        enabled = !isCancelling
                    ) {
                        Text("Đóng")
                    }
                }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Đơn hàng #${order.id.take(8)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    color = when (order.status) {
                        "pending" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        "confirmed" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        "delivered" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        "cancelled" -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when (order.status) {
                            "pending" -> "Chờ xử lý"
                            "confirmed" -> "Đã xác nhận"
                            "delivered" -> "Đã giao"
                            "cancelled" -> "Đã hủy"
                            else -> order.status
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            // FIX LỖI HIỂN THỊ: Sử dụng tên hàm mới (toDateTimeString)
            Text("Ngày đặt: ${order.orderDate.toDateTimeString()}")
            Text("Tổng tiền: ${order.totalAmount.toInt()} VND")

            // Hiển thị lý do hủy nếu có
            if (order.status == "cancelled" && order.cancellationReason.isNotBlank()) {
                Text("Lý do hủy: ${order.cancellationReason}", color = MaterialTheme.colorScheme.error)
            } else if (order.orderNotes.isNotBlank()) {
                Text("Ghi chú: ${order.orderNotes}")
            }

            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            // Status change buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chỉ hiển thị các nút hành động nếu đơn hàng chưa bị hủy và chưa giao
                if (order.status == "pending" || order.status == "confirmed") {

                    if (order.status == "pending") {
                        // Nút xác nhận
                        Button(
                            onClick = { onStatusChange("confirmed", true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Xác nhận")
                        }
                    }

                    if (order.status == "confirmed") {
                        // Nút Đã giao
                        Button(
                            onClick = { onStatusChange("delivered", true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Đã giao")
                        }
                    }

                    // Nút Hủy (Chỉ hiển thị nếu chưa giao/chưa hủy)
                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text("Hủy đơn")
                    }

                } else if (!order.isProcessed && order.status != "delivered" && order.status != "cancelled") {
                    // Nút Đánh dấu đã xử lý (nếu có các trạng thái trung gian khác)
                    OutlinedButton(
                        onClick = { onStatusChange(order.status, true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Đánh dấu đã xử lý")
                    }
                }
            }
        }
    }
}