package com.example.studentwallet

data class User(
    val id: String = "",
    val fullName: String,
    val email: String,
    val career: String,
    val university: String,
    val currency: String
)
