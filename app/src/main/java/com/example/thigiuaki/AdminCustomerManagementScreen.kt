package com.example.thigiuaki

import android.util.Log
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
import com.example.thigiuaki.model.User
import com.example.thigiuaki.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomerManagementScreen() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val adminId = auth.currentUser?.uid ?: ""

    var customers by remember { mutableStateOf(listOf<User>()) }
    var isLoading by remember { mutableStateOf(true) }
    var unreadMap by remember { mutableStateOf(mapOf<String, Int>()) } // Lưu số tin chưa rep của từng khách

    var currentChatCustomer by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        // Lấy danh sách khách
        db.collection("users")
            .whereEqualTo("role", "user")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AdminCustomers", "Error: ${error.message}")
                    isLoading = false
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val user = doc.toObject<User>()
                    user?.copy(id = doc.id)
                } ?: emptyList()
                customers = list
                isLoading = false

                // Lấy số tin chưa rep cho từng khách
                list.forEach { customer ->
                    db.collection("messages")
                        .whereEqualTo("senderId", customer.id)
                        .whereEqualTo("receiverId", adminId)
                        .whereEqualTo("isRead", false)
                        .get()
                        .addOnSuccessListener { snap ->
                            unreadMap = unreadMap.toMutableMap().apply {
                                put(customer.id, snap.size())
                            }
                        }
                }
            }
    }

    if (currentChatCustomer != null) {
        AdminChatScreen(customer = currentChatCustomer!!) { currentChatCustomer = null }
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Quản lý khách hàng") }) }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else if (customers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { Text("Không có khách hàng nào") }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sắp xếp khách theo số tin chưa rep giảm dần
                val sortedCustomers = customers.sortedByDescending { unreadMap[it.id] ?: 0 }
                items(sortedCustomers) { customer ->
                    AdminCustomerCard(
                        customer = customer,
                        unreadCount = unreadMap[customer.id] ?: 0,
                        onBlock = {
                            db.collection("users").document(customer.id)
                                .update("isBlocked", true)
                        },
                        onMessageClick = { currentChatCustomer = customer }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminCustomerCard(
    customer: User,
    unreadCount: Int = 0,
    onBlock: () -> Unit,
    onMessageClick: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var orderCount by remember { mutableStateOf(0) }
    var showBlockDialog by remember { mutableStateOf(false) }

    LaunchedEffect(customer.id) {
        db.collection("orders")
            .whereEqualTo("userId", customer.id)
            .get()
            .addOnSuccessListener { snapshot -> orderCount = snapshot.size() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(customer.name.ifBlank { "Chưa có tên" }, style = MaterialTheme.typography.titleMedium)
                        if (unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "• Chưa rep ($unreadCount)",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Text(customer.email, style = MaterialTheme.typography.bodyMedium)
                    Text("SĐT: ${customer.phone.ifBlank { "Chưa cập nhật" }}", style = MaterialTheme.typography.bodySmall)
                    Text("Số đơn hàng: $orderCount", style = MaterialTheme.typography.bodySmall)
                }
                Row {
                    IconButton(onClick = onMessageClick) {
                        Icon(Icons.Default.Message, contentDescription = "Nhắn tin")
                    }
                    IconButton(onClick = { showBlockDialog = true }) {
                        Icon(Icons.Default.Block, contentDescription = "Chặn", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Chặn khách hàng") },
            text = { Text("Bạn có chắc chắn muốn chặn khách hàng này?") },
            confirmButton = {
                Button(onClick = { onBlock(); showBlockDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Chặn") }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Hủy") }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatScreen(customer: User, onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val adminId = auth.currentUser?.uid ?: ""
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var messageText by remember { mutableStateOf("") }

    fun Timestamp?.formatToString(format: String = "HH:mm dd/MM/yyyy"): String {
        return if (this != null) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.format(this.toDate())
            } catch (_: Exception) {
                ""
            }
        } else ""
    }

    LaunchedEffect(customer.id) {
        db.collection("messages")
            .whereIn("senderId", listOf(adminId, customer.id))
            .addSnapshotListener { snapshot, _ ->
                val allMessages = snapshot?.documents?.mapNotNull { it.toObject<Message>() } ?: emptyList()
                messages = allMessages.filter {
                    (it.senderId == adminId && it.receiverId == customer.id) ||
                            (it.senderId == customer.id && it.receiverId == adminId)
                }.sortedBy { it.createdAt } // mới nhất trên cùng
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer.name.ifBlank { "Chưa có tên" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Nhập tin nhắn...") }
                )
                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            val message = hashMapOf(
                                "content" to messageText,
                                "createdAt" to Timestamp.now(),
                                "isRead" to false,
                                "senderId" to adminId,
                                "senderName" to (auth.currentUser?.displayName ?: "Admin"),
                                "senderRole" to "admin",
                                "receiverId" to customer.id,
                                "orderId" to ""
                            )
                            db.collection("messages").add(message)
                            messageText = ""
                        }
                    }
                ) { Text("Gửi") }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(messages) { msg ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Hiển thị ngày giờ phía trên tin nhắn
                    Text(
                        text = msg.createdAt.formatToString(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(4.dp))
                    // Bong bóng chat
                    Row(
                        horizontalArrangement = if (msg.senderRole == "admin") Arrangement.End else Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (msg.senderRole == "admin") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                msg.content,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
