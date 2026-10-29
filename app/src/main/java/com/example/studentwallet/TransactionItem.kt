package com.example.studentwallet

data class TransactionItem(
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val date: String,
    val monthGroup: String,
    val paymentMethod: String,
    val description: String
)
