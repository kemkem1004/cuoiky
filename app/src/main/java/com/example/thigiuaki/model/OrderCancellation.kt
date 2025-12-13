package com.example.thigiuaki.model

import com.google.firebase.Timestamp

data class OrderCancellation(
    var id: String = "",
    var orderId: String = "",
    var userId: String = "",
    var reason: String = "",
    var status: String = "pending", // pending, approved, rejected
    var requestedAt: Timestamp? = null,
    var processedAt: Timestamp? = null,
    var processedBy: String = "", // Admin userId
    var adminNotes: String = ""
)

