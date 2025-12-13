package com.example.thigiuaki.model

import com.google.firebase.Timestamp

data class Review(
    var id: String = "",
    var productId: String = "",
    var userId: String = "",
    var userName: String = "",
    var rating: Int = 5, // 1-5 stars
    var comment: String = "",
    var createdAt: Timestamp? = null
)
