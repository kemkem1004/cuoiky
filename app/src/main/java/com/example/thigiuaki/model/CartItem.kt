package com.example.thigiuaki.model

// Đảm bảo import các thư viện cần thiết

data class CartItem(
    var id: String = "",
    var userId: String = "",
    var productId: String = "",
    var productName: String = "",
    var productImageUrl: String = "",
    var price: Double = 0.0,
    var quantity: Int = 0,
    var selectedSize: String = "",
    var selectedColor: String = ""
) {
    // RẤT QUAN TRỌNG: Constructor không đối số (cần thiết cho Firebase POJO Deserialization)
    constructor() : this("", "", "", "", "", 0.0, 0, "", "")
}