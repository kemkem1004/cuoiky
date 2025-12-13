package com.example.thigiuaki.model

data class ProductVariant(
    var id: String = "",
    var productId: String = "",
    var size: String = "",
    var color: String = "",
    var sku: String = "", // Stock Keeping Unit
    var stock: Int = 0,
    var price: Double = 0.0, // Variant-specific price (optional)
    var imageUrl: String = "", // Variant-specific image
    var isActive: Boolean = true,
    var reorderPoint: Int = 10 // Low stock threshold
)

