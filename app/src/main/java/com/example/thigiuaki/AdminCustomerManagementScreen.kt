package com.example.thigiuaki

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thigiuaki.model.User
import com.example.thigiuaki.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val BackgroundLight = Color(0xFFFAF9F6)
private val PrimaryMaroon = Color(0xFF8D021F)
private val SecondaryDark = Color(0xFF424242)
private val CardBackground = Color.White
private val ChatUserBubbleColor = PrimaryMaroon.copy(alpha = 0.9f)
private val ChatAdminBubbleColor = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomerManagementScreen() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val adminId = auth.currentUser?.uid ?: ""

    var customers by remember { mutableStateOf(listOf<User>()) }
    var isLoading by remember { mutableStateOf(true) }
    var unreadMap by remember { mutableStateOf(mapOf<String, Int>()) }
    var currentChatCustomer by remember { mutableStateOf<User?>(null) }

    // Load khách
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

    // Lắng nghe tin nhắn chưa đọc
    LaunchedEffect(Unit) {
        db.collection("messages")
            .whereEqualTo("receiverId", adminId)
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshot, _ ->
                val newMap = mutableMapOf<String, Int>()
                snapshot?.documents?.forEach { doc ->
                    val msg = doc.toObject<Message>() ?: return@forEach
                    newMap[msg.senderId] = (newMap[msg.senderId] ?: 0) + 1
                }
                unreadMap = newMap
            }
    }

    val displayCustomers = remember(customers, unreadMap) {
        val newIds = unreadMap.keys.filter { id -> customers.none { it.id == id } }
        val newCustomers = newIds.map { id -> User(id = id, name = "Khách mới", email = "", phone = "") }
        (customers + newCustomers).distinctBy { it.id }
    }

    if (currentChatCustomer != null) {
        AdminChatScreen(customer = currentChatCustomer!!) { currentChatCustomer = null }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý khách hàng", color = PrimaryMaroon) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = PrimaryMaroon) }
            }

            displayCustomers.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { Text("Không có khách hàng nào", color = SecondaryDark) }
            }

            else -> {
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
                                db.collection("users").document(customer.id).update("isBlocked", true)
                            },
                            onMessageClick = { currentChatCustomer = customer }
                        )
                    }
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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(customer.id) {
        db.collection("messages").orderBy("createdAt")
            .addSnapshotListener { snapshot, _ ->
                val allMessages = snapshot?.documents?.mapNotNull { it.toObject<Message>() } ?: emptyList()
                messages = allMessages.filter { msg ->
                    (msg.senderId == customer.id && msg.receiverId == "admin") ||  // khách -> admin
                    (msg.senderRole == "admin" && msg.receiverId == customer.id)     // admin -> khách
                }.sortedBy { it.createdAt }

            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(customer.name.ifBlank { "Chưa có tên" }, color = PrimaryMaroon) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryMaroon) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(messages) { msg ->
                    val isUser = msg.senderRole == "user"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    color = if (isUser) ChatUserBubbleColor else ChatAdminBubbleColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .widthIn(max = 300.dp)
                        ) {
                            Text(msg.content, color = if (isUser) Color.White else Color.Black, fontSize = 14.sp)
                            msg.createdAt?.let { ts ->
                                val sdf = SimpleDateFormat("HH:mm, dd/MM", Locale.getDefault())
                                Text(
                                    sdf.format(ts.toDate()),
                                    fontSize = 10.sp,
                                    color = if (isUser) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Nhập tin nhắn...") },
                    singleLine = false,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        cursorColor = PrimaryMaroon
                    )
                )
                Button(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            val msg = Message(
                                content = messageText.trim(),
                                createdAt = Timestamp.now(),
                                read = false,
                                senderId = adminId,
                                senderName = auth.currentUser?.displayName ?: "Admin",
                                senderRole = "admin",
                                receiverId = customer.id,
                                orderId = ""
                            )
                            db.collection("messages").add(msg)
                            messages = messages + msg.copy(id = "temp_${System.currentTimeMillis()}")
                            messageText = ""
                            scope.launch { listState.animateScrollToItem(messages.size - 1) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryMaroon)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Gửi")
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
    var orderCount by remember { mutableStateOf(0) }
    var showBlockDialog by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val adminId = auth.currentUser?.uid ?: ""

    LaunchedEffect(customer.id) {
        db.collection("orders").whereEqualTo("userId", customer.id).get()
            .addOnSuccessListener { snapshot -> orderCount = snapshot.size() }

        db.collection("messages")
            .whereEqualTo("senderId", customer.id)
            .whereEqualTo("receiverId", adminId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener { snap -> snap.documents.forEach { doc -> db.collection("messages").document(doc.id).update("read", true) } }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(customer.name.ifBlank { "Chưa có tên" }, style = MaterialTheme.typography.titleMedium.copy(color = SecondaryDark))
                        if (unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Text("• Chưa rep ($unreadCount)", color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(customer.email, style = MaterialTheme.typography.bodyMedium.copy(color = SecondaryDark))
                    Text("SĐT: ${customer.phone.ifBlank { "Chưa cập nhật" }}", style = MaterialTheme.typography.bodySmall.copy(color = SecondaryDark.copy(alpha = 0.8f)))
                    Text("Số đơn hàng: $orderCount", style = MaterialTheme.typography.bodySmall.copy(color = SecondaryDark.copy(alpha = 0.8f)))
                }
                Row {
                    IconButton(onClick = onMessageClick) { Icon(Icons.Default.Message, contentDescription = "Nhắn tin", tint = PrimaryMaroon) }
                    IconButton(onClick = { showBlockDialog = true }) { Icon(Icons.Default.Block, contentDescription = "Chặn", tint = Color.Red) }
                }
            }
        }
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Chặn khách hàng", color = PrimaryMaroon) },
            text = { Text("Bạn có chắc chắn muốn chặn khách hàng này?") },
            confirmButton = { Button(onClick = { onBlock(); showBlockDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Chặn") } },
            dismissButton = { TextButton(onClick = { showBlockDialog = false }) { Text("Hủy") } },
            containerColor = CardBackground
        )
    }
}
