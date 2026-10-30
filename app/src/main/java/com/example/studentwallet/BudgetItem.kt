package com.example.studentwallet

data class BudgetItem(
    val id: String = "",
    val userId: String,
    val category: String,
    val limitAmount: Double,
    val spentAmount: Double = 0.0
) {
    val percentage: Int
        get() = if (limitAmount > 0) ((spentAmount / limitAmount) * 100).toInt() else 0

    val remainingAmount: Double
        get() = (limitAmount - spentAmount).coerceAtLeast(0.0)
}
