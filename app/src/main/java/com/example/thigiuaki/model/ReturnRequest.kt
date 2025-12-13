package com.example.thigiuaki.model

import com.google.firebase.Timestamp

data class ReturnRequest(
    var id: String = "",
    var orderId: String = "",
    var userId: String = "",
    var items: List<ReturnItem> = emptyList(),
    var reason: String = "",
    var type: String = "return", // "return", "exchange"
    var exchangeProductId: String = "", // If exchange
    var status: String = "pending", // pending, approved, rejected, processing, completed
    var requestedAt: Timestamp? = null,
    var processedAt: Timestamp? = null,
    var processedBy: String = "", // Admin userId
    var adminNotes: String = "",
    var refundAmount: Double = 0.0,
    var trackingNumber: String = ""
)

data class ReturnItem(
    var orderItemId: String = "",
    var productId: String = "",
    var productName: String = "",
    var quantity: Int = 0,
    var size: String = "",
    var color: String = ""
)

