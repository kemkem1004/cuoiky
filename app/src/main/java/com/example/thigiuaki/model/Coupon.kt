package com.example.thigiuaki.model

import com.google.firebase.Timestamp

data class Coupon(
    var id: String = "",
    var code: String = "", // e.g., "SAVE20"
    var type: String = "percentage", // "percentage", "fixed", "free_shipping"
    var value: Double = 0.0, // Percentage (0-100) or fixed amount
    var minimumOrderValue: Double = 0.0,
    var maximumDiscount: Double = 0.0, // For percentage coupons
    var validFrom: Timestamp? = null,
    var validUntil: Timestamp? = null,
    var usageLimit: Int = 0, // 0 = unlimited
    var usageCount: Int = 0,
    var perUserLimit: Int = 1, // How many times a user can use it
    var isActive: Boolean = true,
    var applicableCategories: List<String> = emptyList(), // Empty = all categories
    var description: String = ""
)

