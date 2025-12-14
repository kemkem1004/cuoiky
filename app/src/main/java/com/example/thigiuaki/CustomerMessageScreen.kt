package com.example.thigiuaki

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thigiuaki.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip

// Định nghĩa màu sắc cơ bản
private val PrimaryMaroon = Color(0xFF8D021F)
private val ChatUserBubbleColor = PrimaryMaroon.copy(alpha = 0.9f) // Màu nổi bật hơn cho người dùng
private val ChatAdminBubbleColor = Color(0xFFE0E0E0) // Màu xám nhạt cho Admin

@Composable
fun CustomerMessageScreen(userId: String) {
    val db = FirebaseFirestore.getInstance()
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        // Đăng ký Listener và xử lý tin nhắn như cũ
        db.collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, _ ->
                val all = snapshot?.documents?.mapNotNull { it.toObject(Message::class.java)?.copy(id = it.id) } ?: emptyList()
                messages = all.filter { msg ->
                    (msg.senderId == userId && msg.receiverId == "admin") ||
                            (msg.senderRole == "admin" && msg.receiverId == userId)
                }.sortedBy { it.createdAt }

                // Đánh dấu đã đọc
                all.filter { it.receiverId == userId && it.read == false }.forEach { msg ->
                    db.collection("messages").document(msg.id).update("read", true)
                }

                // Cuộn xuống cuối
                if (messages.isNotEmpty()) {
                    scope.launch { listState.scrollToItem(messages.size - 1) }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
        // Loại bỏ padding 8.dp ở đây để tránh xung đột với Scaffold bên ngoài
        // Nếu bạn không dùng Scaffold, hãy thêm lại padding
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), // Thêm padding xung quanh list
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.senderRole == "user"

                // Container căn lề
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {

                    // Tên người gửi/ vai trò (Tùy chọn: thường ẩn trong chat 1-1 với Admin)
                    /*
                    Text(
                        text = if (isUser) "Bạn" else "Hỗ trợ (Admin)",
                        fontSize = 12.sp,
                        color = if (isUser) ChatUserBubbleColor else Color.Gray,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    */

                    // Ô chat chứa nội dung
                    Column(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium) // Áp dụng bo góc
                            .background(
                                color = if (isUser) ChatUserBubbleColor else ChatAdminBubbleColor
                            )
                            .widthIn(max = 300.dp) // Giới hạn chiều rộng tin nhắn
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            msg.content,
                            color = if (isUser) Color.White else Color.Black,
                            fontSize = 14.sp
                        )

                        // Ngày giờ dưới tin nhắn
                        msg.createdAt?.let { ts ->
                            val sdf = SimpleDateFormat("HH:mm, dd/MM", Locale.getDefault())
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                sdf.format(ts.toDate()),
                                fontSize = 10.sp,
                                color = if (isUser) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.End) // Căn lề phải cho thời gian
                            )
                        }
                    }
                }
            }
        }

        // Gửi tin nhắn
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn...") },
                singleLine = false, // Cho phép nhiều dòng
                shape = MaterialTheme.shapes.large,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = PrimaryMaroon
                )
            )
            Button(
                onClick = {
                    if (messageText.isNotBlank()) {
                        val newMessage = Message(
                            content = messageText.trim(), // Trim để loại bỏ khoảng trắng thừa
                            createdAt = Timestamp.now(),
                            read = false,
                            senderId = userId,
                            senderName = "Bạn", // Đặt tên người gửi
                            senderRole = "user",
                            receiverId = "admin",
                            orderId = ""
                        )
                        db.collection("messages").add(newMessage)

                        // Cập nhật UI tạm thời và cuộn xuống
                        val tempNewMessage = newMessage.copy(id = "temp_${System.currentTimeMillis()}")
                        messages = messages + tempNewMessage
                        messageText = ""
                        scope.launch { listState.animateScrollToItem(messages.size - 1) }
                    }
                },
                modifier = Modifier.height(TextFieldDefaults.MinHeight),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryMaroon),
                enabled = messageText.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Gửi")
            }
        }
    }
}