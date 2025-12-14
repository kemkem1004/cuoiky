package com.example.thigiuaki

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thigiuaki.model.Message
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CustomerMessageScreen(userId: String) {
    val db = FirebaseFirestore.getInstance()
    val adminId = "VGThjjo7vnelUsDWBgEQSyMPUHt2" // ID admin thật

    var messages by remember { mutableStateOf(listOf<Message>()) }
    var messageText by remember { mutableStateOf("") }

    // Lấy toàn bộ tin nhắn giữa user và admin
    LaunchedEffect(userId) {
        db.collection("messages")
            .addSnapshotListener { snapshot, _ ->
                val all = snapshot?.documents?.mapNotNull { it.toObject(Message::class.java) } ?: emptyList()
                messages = all.filter {
                    (it.senderId == userId && it.receiverId == adminId) ||
                            (it.senderId == adminId && it.receiverId == userId)
                }.sortedBy { it.createdAt } // sắp xếp từ cũ → mới
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                val isUser = msg.senderId == userId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
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
                        msg.createdAt?.let { ts ->
                            val sdf = SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault())
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
                    val message = hashMapOf(
                        "content" to messageText,
                        "createdAt" to Timestamp.now(),
                        "isRead" to false,
                        "senderId" to userId,
                        "senderName" to "Bạn",
                        "senderRole" to "user",
                        "receiverId" to adminId,
                        "orderId" to ""
                    )
                    db.collection("messages").add(message)
                    messageText = ""
                }
            }) {
                Text("Gửi")
            }
        }
    }
}
