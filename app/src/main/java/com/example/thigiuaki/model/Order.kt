package com.example.thigiuaki.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

// LƯU Ý: Đảm bảo class Address cũng có constructor rỗng
// Ví dụ: data class Address(...) { constructor() : this(...) }

data class Order(
    // Các trường String/Double/Boolean
    @get:Exclude @set:Exclude var id: String = "",
    var userId: String = "",
    var subtotal: Double = 0.0,
    var shippingCost: Double = 0.0,
    var tax: Double = 0.0,
    var discount: Double = 0.0,
    var couponCode: String = "",
    var totalAmount: Double = 0.0,
    var deliveryType: String = "home_delivery",
    var orderNotes: String = "",
    var status: String = "pending",
    var isProcessed: Boolean = false,
    var paymentMethod: String = "cash",
    var paymentStatus: String = "pending",
    var trackingNumber: String = "",
    var canCancel: Boolean = true,
    var canReturn: Boolean = false,

    // TRƯỜNG MỚI CHO CHỨC NĂNG HỦY
    var cancellationReason: String = "",

    // CÁC TRƯỜNG DỮ LIỆU LỒNG NHAU
    var items: List<CartItem> = emptyList(), // Sẽ ánh xạ đúng nếu CartItem.kt đã sửa
    var shippingAddress: Address = Address(),

    // CÁC TRƯỜNG THỜI GIAN PHẢI LÀ TIMESTAMP
    var createdAt: Timestamp? = null,
    var orderDate: Timestamp? = null,    // ĐÃ FIX: Phải là Timestamp
    var pickupTime: Timestamp? = null,   // ĐÃ FIX: Phải là Timestamp
    var confirmedAt: Timestamp? = null,
    var deliveredAt: Timestamp? = null,
    var cancelledAt: Timestamp? = null
) {
    // RẤT QUAN TRỌNG: Constructor không đối số cho Firestore
    constructor() : this(
        id = "", userId = "", subtotal = 0.0, shippingCost = 0.0, tax = 0.0, discount = 0.0,
        couponCode = "", totalAmount = 0.0, deliveryType = "home_delivery", orderNotes = "",
        status = "pending", isProcessed = false, paymentMethod = "cash", paymentStatus = "pending",
        trackingNumber = "", canCancel = true, canReturn = false, cancellationReason = "",
        items = emptyList(), shippingAddress = Address(), createdAt = null, orderDate = null,
        pickupTime = null, confirmedAt = null, deliveredAt = null, cancelledAt = null
    )
}