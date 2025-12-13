package com.example.thigiuaki.model

data class Product(
    var id: String = "",
    var name: String = "",
    var price: Double = 0.0,
    var type: String = "",
    var category: String = "", // Men, Women, Kids, Accessories
    var subCategory: String = "", // e.g., "T-Shirts", "Jeans"
    var brand: String = "",
    var material: String = "", // e.g., "Cotton", "Polyester"
    var imageUrl: String = "",
    var images: List<String> = emptyList(), // Multiple images
    var description: String = "",
    var sizes: List<String> = listOf("S", "M", "L", "XL"), // Available sizes
    var colors: List<String> = listOf("Black", "White", "Blue"), // Available colors
    var stock: Int = 0, // Total stock quantity (deprecated - use variants)
    var rating: Double = 0.0, // Average rating
    var reviewCount: Int = 0, // Number of reviews
    var hasVariants: Boolean = false, // If true, use ProductVariant collection
    var createdAt: com.google.firebase.Timestamp? = null,
    var updatedAt: com.google.firebase.Timestamp? = null,
    var isActive: Boolean = true,
    var tags: List<String> = emptyList() // For search and recommendations
)
