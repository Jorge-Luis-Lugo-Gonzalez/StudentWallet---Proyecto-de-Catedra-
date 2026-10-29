package com.example.studentwallet


object CategoryConfig {

    val expenseCategories = listOf(
        "Alimentación",
        "Transporte",
        "Ocio",
        "Servicios",
        "Universidad",
        "Compras",
        "Salud",
        "Otros"
    )

    val allCategories = expenseCategories + listOf("Ingresos")

    fun getIconRes(category: String, type: String = "GASTO"): Int {
        if (type == "INGRESO" || category.equals("Ingresos", ignoreCase = true)) {
            return R.drawable.ic_cat_income
        }
        return when (category) {
            "Alimentación", "Comida" -> R.drawable.ic_cat_food
            "Transporte" -> R.drawable.ic_cat_transport
            "Ocio" -> R.drawable.ic_cat_leisure
            "Servicios" -> R.drawable.ic_cat_services
            "Universidad" -> R.drawable.ic_cat_university
            "Compras" -> R.drawable.ic_cat_shopping
            "Salud" -> R.drawable.ic_cat_health
            else -> R.drawable.ic_cat_other
        }
    }

    fun getCircleBgRes(category: String, type: String): Int {
        if (type == "INGRESO") {
            return R.drawable.bg_circle_soft_green
        }
        return when (category) {
            "Alimentación", "Comida" -> R.drawable.bg_circle_soft_blue
            "Transporte" -> R.drawable.bg_circle_soft_blue
            "Ocio" -> R.drawable.bg_circle_soft_red
            else -> R.drawable.bg_circle_soft_blue
        }
    }

    fun getCurrencySymbol(currencyText: String): String {
        return when {
            currencyText.contains("EUR") -> "€"
            currencyText.contains("GBP") -> "£"
            else -> "$"
        }
    }

    fun formatMoney(amount: Double, currencyText: String = "USD ($)"): String {
        val symbol = getCurrencySymbol(currencyText)
        return String.format("%s%,.2f", symbol, amount)
    }
}
