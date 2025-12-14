package com.example.thigiuaki.model

import com.google.firebase.Timestamp

data class Message(
    var id: String = "",
    var orderId: String = "",
    var senderId: String = "",
    var senderName: String = "",
    var senderRole: String = "", // "admin" or "user"
    var receiverId: String = "",
    var content: String = "",
    var createdAt: Timestamp? = null,
    var read: Boolean = false
)

