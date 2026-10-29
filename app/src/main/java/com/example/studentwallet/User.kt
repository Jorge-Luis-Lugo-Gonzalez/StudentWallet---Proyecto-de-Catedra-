package com.example.studentwallet

data class User(
    val id: Long = 0,
    val fullName: String,
    val email: String,
    val password: String,
    val career: String,
    val university: String,
    val currency: String
)
