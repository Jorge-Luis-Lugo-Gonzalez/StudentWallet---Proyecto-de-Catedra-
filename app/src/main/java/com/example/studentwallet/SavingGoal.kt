package com.example.studentwallet

data class SavingGoal(
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val targetDate: String
) {
    val percentage: Int
        get() = if (targetAmount > 0) ((savedAmount / targetAmount) * 100).toInt().coerceAtMost(100) else 0

    val remainingAmount: Double
        get() = (targetAmount - savedAmount).coerceAtLeast(0.0)
}
