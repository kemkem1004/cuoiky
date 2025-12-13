package com.example.thigiuaki.model

data class Address(
    var id: String = "",
    var fullName: String = "",
    var phone: String = "",
    var street: String = "",
    var city: String = "",
    var district: String = "",
    var ward: String = "",
    var isDefault: Boolean = false,
    var userId: String = ""
)
