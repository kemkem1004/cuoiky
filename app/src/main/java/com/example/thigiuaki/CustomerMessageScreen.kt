package com.example.thigiuaki

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
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


@Composable
fun CustomerMessageScreen(userId: String) {
    val db = FirebaseFirestore.getInstance()
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        db.collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, _ ->
                val all = snapshot?.documents?.mapNotNull { it.toObject(Message::class.java)?.copy(id = it.id) } ?: emptyList()
                messages = all.filter { msg ->
                    // user gửi cho admin
                    (msg.senderId == userId && msg.receiverId == "admin") ||
                            // admin gửi cho user
                    (msg.senderRole == "admin" && msg.receiverId == userId)
                }.sortedBy { it.createdAt }


                all.filter { it.receiverId == userId && it.read == false }.forEach { msg ->
                    db.collection("messages").document(msg.id).update("read", true)
                }

                scope.launch { listState.scrollToItem(messages.size.coerceAtLeast(1) - 1) }
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.senderRole == "user"

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    // Tên người gửi bên ngoài ô chat
                    Text(
                        text = msg.senderName,
                        fontSize = 12.sp,
                        color = if (isUser) Color(0xFF0D6EFD) else Color.Gray,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    // Ô chat chứa nội dung
                    Column(
                        modifier = Modifier
                            .background(
                                color = if (isUser) Color(0xFF0D6EFD) else Color.LightGray,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            msg.content,
                            color = if (isUser) Color.White else Color.Black,
                            fontSize = 14.sp
                        )

                        // Ngày giờ dưới tin nhắn
                        msg.createdAt?.let { ts ->
                            val sdf = SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault())
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                sdf.format(ts.toDate()),
                                fontSize = 10.sp,
                                color = if (isUser) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Gửi tin nhắn
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Nhập tin nhắn...") }
            )
            Button(onClick = {
                if (messageText.isNotBlank()) {
                    val newMessage = Message(
                        content = messageText,
                        createdAt = Timestamp.now(),
                        read = false,
                        senderId = userId,
                        senderName = "Bạn",
                        senderRole = "user",
                        receiverId = "admin",
                        orderId = ""
                    )
                    db.collection("messages").add(newMessage)
                    messages = messages + newMessage
                    messageText = ""
                    scope.launch { listState.scrollToItem(messages.size - 1) }
                }
            }) {
                Text("Gửi")
            }
        }
    }
}
