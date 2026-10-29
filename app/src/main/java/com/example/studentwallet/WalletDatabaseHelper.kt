package com.example.studentwallet

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class WalletDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "student_wallet.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                career TEXT NOT NULL,
                university TEXT NOT NULL,
                currency TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                category TEXT NOT NULL,
                date TEXT NOT NULL,
                month_group TEXT NOT NULL,
                payment_method TEXT NOT NULL,
                description TEXT NOT NULL
            )
            """.trimIndent()
        )

        seedDemoData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS transactions")
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    private fun seedDemoData(db: SQLiteDatabase) {
        val userValues = ContentValues().apply {
            put("full_name", "Carlos")
            put("email", "alumno@universidad.edu")
            put("password", "123456")
            put("career", "Ingeniería Informática")
            put("university", "Universidad Don Bosco")
            put("currency", "USD ($)")
        }
        val userId = db.insert("users", null, userValues)

        val secondUser = ContentValues().apply {
            put("full_name", "Carlos")
            put("email", "carlos@email.com")
            put("password", "123456")
            put("career", "Ingeniería Informática")
            put("university", "Universidad Don Bosco")
            put("currency", "USD ($)")
        }
        val secondId = db.insert("users", null, secondUser)

        seedUserFinancialData(db, userId)
        seedUserFinancialData(db, secondId)
    }

    private fun seedUserFinancialData(db: SQLiteDatabase, userId: Long) {
        val sampleTransactions = listOf(
            TransactionItem(0, userId, "Almuerzo", 8.50, "GASTO", "Alimentación", "Hoy", "Agosto 2026", "Efectivo", "Almuerzo"),
            TransactionItem(0, userId, "Transporte", 2.00, "GASTO", "Transporte", "Hoy", "Agosto 2026", "Efectivo", "Transporte universitario"),
            TransactionItem(0, userId, "Salario", 650.00, "INGRESO", "Ingresos", "01 Ago", "Agosto 2026", "Transf.", "Mensualidad y beca"),
            TransactionItem(0, userId, "Almuerzo Cafetería", 12.50, "GASTO", "Alimentación", "15 Ago", "Agosto 2026", "Tarjeta", "Almuerzo Cafetería"),
            TransactionItem(0, userId, "Supermercado mensual", 99.00, "GASTO", "Alimentación", "10 Ago", "Agosto 2026", "Tarjeta", "Compras de despensa"),
            TransactionItem(0, userId, "Abono transporte", 53.00, "GASTO", "Transporte", "05 Ago", "Agosto 2026", "Efectivo", "Rutas del mes"),
            TransactionItem(0, userId, "Cine y salidas", 45.00, "GASTO", "Ocio", "12 Ago", "Agosto 2026", "Tarjeta", "Salidas con amigos"),
            TransactionItem(0, userId, "Cuota y libros", 95.00, "GASTO", "Universidad", "03 Ago", "Agosto 2026", "Transf.", "Materiales de laboratorio"),
            TransactionItem(0, userId, "Internet y datos", 34.50, "GASTO", "Servicios", "04 Ago", "Agosto 2026", "Transf.", "Plan mensual"),
            TransactionItem(0, userId, "Útiles y papelería", 15.00, "GASTO", "Compras", "06 Ago", "Agosto 2026", "Efectivo", "Cuadernos y accesorios")
        )

        for (item in sampleTransactions) {
            val cv = ContentValues().apply {
                put("user_id", item.userId)
                put("title", item.title)
                put("amount", item.amount)
                put("type", item.type)
                put("category", item.category)
                put("date", item.date)
                put("month_group", item.monthGroup)
                put("payment_method", item.paymentMethod)
                put("description", item.description)
            }
            db.insert("transactions", null, cv)
        }
    }

    fun authenticateUser(email: String, password: String): User? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE LOWER(email) = LOWER(?) AND password = ?",
            arrayOf(email.trim(), password)
        )
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = readUserCursor(cursor)
        }
        cursor.close()
        return user
    }

    fun isEmailRegistered(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id FROM users WHERE LOWER(email) = LOWER(?)",
            arrayOf(email.trim())
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun registerUser(user: User): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("full_name", user.fullName)
            put("email", user.email.trim())
            put("password", user.password)
            put("career", user.career)
            put("university", user.university)
            put("currency", user.currency)
        }
        val newId = db.insert("users", null, values)
        if (newId > 0) {
            seedUserFinancialData(db, newId)
        }
        return newId
    }

    fun getUserById(userId: Long): User? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users WHERE id = ?", arrayOf(userId.toString()))
        var user: User? = null
        if (cursor.moveToFirst()) {
            user = readUserCursor(cursor)
        }
        cursor.close()
        return user
    }

    fun updateUserProfile(userId: Long, fullName: String, career: String, university: String, currency: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("full_name", fullName)
            put("career", career)
            put("university", university)
            put("currency", currency)
        }
        return db.update("users", values, "id = ?", arrayOf(userId.toString())) > 0
    }

    fun updateUserCurrency(userId: Long, currency: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("currency", currency)
        }
        return db.update("users", values, "id = ?", arrayOf(userId.toString())) > 0
    }

    fun updateUserPassword(userId: Long, newPassword: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("password", newPassword)
        }
        return db.update("users", values, "id = ?", arrayOf(userId.toString())) > 0
    }

    private fun readUserCursor(cursor: android.database.Cursor): User {
        return User(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
            fullName = cursor.getString(cursor.getColumnIndexOrThrow("full_name")),
            email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
            password = cursor.getString(cursor.getColumnIndexOrThrow("password")),
            career = cursor.getString(cursor.getColumnIndexOrThrow("career")),
            university = cursor.getString(cursor.getColumnIndexOrThrow("university")),
            currency = cursor.getString(cursor.getColumnIndexOrThrow("currency"))
        )
    }

    fun getTransactions(userId: Long): List<TransactionItem> {
        val list = mutableListOf<TransactionItem>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM transactions WHERE user_id = ? ORDER BY id DESC",
            arrayOf(userId.toString())
        )
        while (cursor.moveToNext()) {
            list.add(
                TransactionItem(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    monthGroup = cursor.getString(cursor.getColumnIndexOrThrow("month_group")),
                    paymentMethod = cursor.getString(cursor.getColumnIndexOrThrow("payment_method")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                )
            )
        }
        cursor.close()
        return list
    }

    fun getTransactionById(transactionId: Long): TransactionItem? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM transactions WHERE id = ?", arrayOf(transactionId.toString()))
        var item: TransactionItem? = null
        if (cursor.moveToFirst()) {
            item = TransactionItem(
                id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                userId = cursor.getLong(cursor.getColumnIndexOrThrow("user_id")),
                title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                monthGroup = cursor.getString(cursor.getColumnIndexOrThrow("month_group")),
                paymentMethod = cursor.getString(cursor.getColumnIndexOrThrow("payment_method")),
                description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
            )
        }
        cursor.close()
        return item
    }

    fun insertTransaction(item: TransactionItem): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("user_id", item.userId)
            put("title", item.title)
            put("amount", item.amount)
            put("type", item.type)
            put("category", item.category)
            put("date", item.date)
            put("month_group", item.monthGroup)
            put("payment_method", item.paymentMethod)
            put("description", item.description)
        }
        return db.insert("transactions", null, values)
    }

    fun updateTransaction(item: TransactionItem): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("title", item.title)
            put("amount", item.amount)
            put("type", item.type)
            put("category", item.category)
            put("date", item.date)
            put("month_group", item.monthGroup)
            put("payment_method", item.paymentMethod)
            put("description", item.description)
        }
        return db.update("transactions", values, "id = ?", arrayOf(item.id.toString())) > 0
    }

    fun deleteTransaction(transactionId: Long): Boolean {
        val db = writableDatabase
        return db.delete("transactions", "id = ?", arrayOf(transactionId.toString())) > 0
    }
}
