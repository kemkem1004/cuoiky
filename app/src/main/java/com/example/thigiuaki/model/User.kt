package com.example.thigiuaki.model

data class User(
    var id: String = "",
    var email: String = "",
    var name: String = "",
    var phone: String = "",
    var role: String = "user", // user, admin
    var avatarUrl: String = ""
)
