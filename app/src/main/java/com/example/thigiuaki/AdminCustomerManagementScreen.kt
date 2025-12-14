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

    var customers by remember { mutableStateOf(listOf<User>()) } // khách từ collection users
    var isLoading by remember { mutableStateOf(true) }
    var unreadMap by remember { mutableStateOf(mapOf<String, Int>()) } // số tin chưa rep theo customerId
    var currentChatCustomer by remember { mutableStateOf<User?>(null) }

    // 1. Fetch danh sách khách 1 lần
    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "user")
            .get()
            .addOnSuccessListener { snap ->
                customers = snap.documents.mapNotNull { it.toObject<User>()?.copy(id = it.id) }
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("AdminCustomer", "Fetch users error: ${e.message}")
                isLoading = false
            }
    }

    // 2. Lắng nghe tin nhắn chưa đọc, cập nhật unreadMap
    LaunchedEffect(Unit) {
        db.collection("messages")
            .whereEqualTo("receiverId", adminId)
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshot, _ ->
                val newUnreadMap = mutableMapOf<String, Int>()
                snapshot?.documents?.forEach { doc ->
                    val msg = doc.toObject<Message>() ?: return@forEach
                    newUnreadMap[msg.senderId] = (newUnreadMap[msg.senderId] ?: 0) + 1
                }
                unreadMap = newUnreadMap
            }
    }

    // 3. Kết hợp danh sách khách: customers + khách mới nhắn nhưng chưa có trong users
    val displayCustomers = remember(customers, unreadMap) {
        val newIds = unreadMap.keys.filter { id -> customers.none { it.id == id } }
        val newCustomers = newIds.map { id ->
            User(id = id, name = "Khách mới", email = "", phone = "")
        }
        (customers + newCustomers).distinctBy { it.id }
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
        } else if (displayCustomers.isEmpty()) {
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
                val sortedCustomers = displayCustomers.sortedByDescending { unreadMap[it.id] ?: 0 }
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

    // Lắng nghe tất cả tin nhắn liên quan customer
    LaunchedEffect(customer.id) {
        db.collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, _ ->
                val allMessages = snapshot?.documents?.mapNotNull { it.toObject<Message>() } ?: emptyList()
                messages = allMessages.filter { msg ->
                    // Tin nhắn user gửi (receiverId = "admin")
                    (msg.senderId == customer.id && msg.receiverId == "admin") ||
// Tin nhắn admin gửi cho user
                            (msg.receiverId == customer.id)
                }.sortedBy { it.createdAt }
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
                                "read" to false,
                                "senderId" to adminId,
                                "senderName" to (auth.currentUser?.displayName ?: "Admin"),
                                "senderRole" to "admin",
                                "receiverId" to customer.id, // gửi về customer
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
                val isAdmin = msg.senderRole == "admin"
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Hiển thị ngày giờ
                    Text(
                        text = msg.createdAt.formatToString(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = if (isAdmin) Arrangement.End else Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                msg.content,
                                color = if (isAdmin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
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



@Composable
fun AdminCustomerCard(
    customer: User,
    unreadCount: Int = 0,
    onBlock: () -> Unit,
    onMessageClick: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val adminId = auth.currentUser?.uid ?: ""
    var orderCount by remember { mutableStateOf(0) }
    var showBlockDialog by remember { mutableStateOf(false) }

    LaunchedEffect(customer.id) {
        // Lấy số đơn hàng
        db.collection("orders")
            .whereEqualTo("userId", customer.id)
            .get()
            .addOnSuccessListener { snapshot -> orderCount = snapshot.size() }

        // Đánh dấu tất cả tin chưa đọc là đã đọc
        db.collection("messages")
            .whereEqualTo("senderId", customer.id)
            .whereEqualTo("receiverId", adminId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { doc ->
                    db.collection("messages").document(doc.id).update("read", true)
                }
            }
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