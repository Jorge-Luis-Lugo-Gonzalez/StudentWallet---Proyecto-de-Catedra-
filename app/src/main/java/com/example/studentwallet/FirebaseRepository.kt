package com.example.studentwallet

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

/**
 * Acceso a Firebase Authentication y Realtime Database.
 *
 * Estructura en Realtime Database (todo cuelga del uid del usuario):
 *
 * users/{uid}/profile       -> fullName, email, career, university, currency
 * users/{uid}/transactions/{id} -> title, amount, type, category, date, monthGroup, paymentMethod, description, createdAt
 * users/{uid}/budgets/{id}  -> category, limitAmount
 * users/{uid}/goals/{id}    -> name, targetAmount, savedAmount, targetDate
 */
object FirebaseRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val database: DatabaseReference by lazy { FirebaseDatabase.getInstance().reference }

    const val DEFAULT_CURRENCY = "USD ($)"

    data class DashboardData(
        val user: User?,
        val transactions: List<TransactionItem>,
        val budgets: List<BudgetItem>,
        val goals: List<SavingGoal>
    ) {
        val totalIncome: Double
            get() = transactions.filter { it.type == "INGRESO" }.sumOf { it.amount }

        val totalExpense: Double
            get() = transactions.filter { it.type == "GASTO" }.sumOf { it.amount }

        val expensesByCategory: Map<String, Double>
            get() = transactions.filter { it.type == "GASTO" }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
                .toMap()

        val currency: String
            get() = user?.currency ?: DEFAULT_CURRENCY
    }

    // ---------------------------------------------------------------------
    // Referencias
    // ---------------------------------------------------------------------

    fun currentUid(): String? = auth.currentUser?.uid

    private fun userRef(): DatabaseReference? = currentUid()?.let { database.child("users").child(it) }

    private fun notLoggedIn(): Exception = Exception("No hay una sesión activa")

    // ---------------------------------------------------------------------
    // Autenticación
    // ---------------------------------------------------------------------

    fun login(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(Exception(authErrorMessage(it)))) }
    }

    fun register(user: User, password: String, onResult: (Result<Unit>) -> Unit) {
        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid == null) {
                    onResult(Result.failure(Exception("No se pudo crear la cuenta")))
                    return@addOnSuccessListener
                }
                database.child("users").child(uid).child("profile")
                    .setValue(profileToMap(user))
                    .addOnSuccessListener { onResult(Result.success(Unit)) }
                    .addOnFailureListener { onResult(Result.failure(Exception(dbErrorMessage(it)))) }
            }
            .addOnFailureListener { onResult(Result.failure(Exception(authErrorMessage(it)))) }
    }

    fun sendPasswordReset(email: String, onResult: (Result<Unit>) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(Exception(authErrorMessage(it)))) }
    }

    fun updatePassword(newPassword: String, onResult: (Result<Unit>) -> Unit) {
        val current = auth.currentUser ?: return onResult(Result.failure(notLoggedIn()))
        current.updatePassword(newPassword)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(Exception(authErrorMessage(it)))) }
    }

    fun logout() {
        auth.signOut()
    }

    // ---------------------------------------------------------------------
    // Perfil
    // ---------------------------------------------------------------------

    fun getUser(onResult: (User?) -> Unit) {
        val ref = userRef() ?: return onResult(null)
        ref.child("profile").get()
            .addOnSuccessListener { onResult(parseUser(it)) }
            .addOnFailureListener { onResult(null) }
    }

    fun updateUserProfile(
        fullName: String,
        career: String,
        university: String,
        currency: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val ref = userRef() ?: return onResult(Result.failure(notLoggedIn()))
        val updates = mapOf<String, Any>(
            "fullName" to fullName,
            "career" to career,
            "university" to university,
            "currency" to currency
        )
        ref.child("profile").updateChildren(updates)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(Exception(dbErrorMessage(it)))) }
    }

    fun updateUserCurrency(currency: String, onResult: (Result<Unit>) -> Unit) {
        val ref = userRef() ?: return onResult(Result.failure(notLoggedIn()))
        ref.child("profile").child("currency").setValue(currency)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(Exception(dbErrorMessage(it)))) }
    }

    // ---------------------------------------------------------------------
    // Dashboard: una sola lectura de todo el nodo del usuario
    // ---------------------------------------------------------------------

    fun loadDashboard(onResult: (Result<DashboardData>) -> Unit) {
        val ref = userRef() ?: return onResult(Result.failure(notLoggedIn()))
        ref.get()
            .addOnSuccessListener { snapshot ->
                val uid = snapshot.key ?: ""
                val transactions = parseTransactions(snapshot.child("transactions"), uid)
                val budgets = parseBudgets(snapshot.child("budgets"), uid, transactions)
                val goals = parseGoals(snapshot.child("goals"), uid)
                onResult(
                    Result.success(
                        DashboardData(parseUser(snapshot.child("profile")), transactions, budgets, goals)
                    )
                )
            }
            .addOnFailureListener { onResult(Result.failure(Exception(dbErrorMessage(it)))) }
    }

    // ---------------------------------------------------------------------
    // Movimientos
    // ---------------------------------------------------------------------

    fun getTransactionById(transactionId: String, onResult: (TransactionItem?) -> Unit) {
        val ref = userRef() ?: return onResult(null)
        ref.child("transactions").child(transactionId).get()
            .addOnSuccessListener { snap ->
                onResult(if (snap.exists()) parseTransaction(snap, currentUid() ?: "") else null)
            }
            .addOnFailureListener { onResult(null) }
    }

    /** Inserta si item.id está vacío; si no, actualiza el movimiento existente. */
    fun saveTransaction(item: TransactionItem, onResult: (Result<Unit>) -> Unit) {
        val ref = userRef() ?: return onResult(Result.failure(notLoggedIn()))
        val txRef = if (item.id.isEmpty()) {
            ref.child("transactions").push()
        } else {
            ref.child("transactions").child(item.id)
        }
        val data = mutableMapOf<String, Any>(
            "title" to item.title,
            "amount" to item.amount,
            "type" to item.type,
            "category" to item.category,
            "date" to item.date,
            "monthGroup" to item.monthGroup,
            "paymentMethod" to item.paymentMethod,
            "description" to item.description
        )
        if (item.id.isEmpty()) {
            data["createdAt"] = System.currentTimeMillis()
        }

        val task = if (item.id.isEmpty()) txRef.setValue(data) else txRef.updateChildren(data)
        task.addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(Exception(dbErrorMessage(it)))) }
    }

    fun deleteTransaction(transactionId: String, onResult: (Result<Unit>) -> Unit) {
        val ref = userRef() ?: return onResult(Result.failure(notLoggedIn()))
        ref.child("transactions").child(transactionId).removeValue()
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(Exception(dbErrorMessage(it)))) }
    }

    // ---------------------------------------------------------------------
    // Presupuestos
    // ---------------------------------------------------------------------

    /** Si ya existe un presupuesto para la categoría lo actualiza; si no, crea uno nuevo. */
    fun saveOrUpdateBudget(category: String, limitAmount: Double, onResult: (Result<Unit>) -> Unit) {
        val ref = userRef() ?: return onResult(Result.failure(notLoggedIn()))
        val budgetsRef = ref.child("budgets")
        budgetsRef.get()
            .addOnSuccessListener { snapshot ->
                val existing = snapshot.children.firstOrNull {
                    it.child("category").getValue(String::class.java) == category
                }
                val target = existing?.ref ?: budgetsRef.push()
                target.setValue(mapOf("category" to category, "limitAmount" to limitAmount))
                    .addOnSuccessListener { onResult(Result.success(Unit)) }
                    .addOnFailureListener { onResult(Result.failure(Exception(dbErrorMessage(it)))) }
            }
            .addOnFailureListener { onResult(Result.failure(Exception(dbErrorMessage(it)))) }
    }

    fun deleteBudget(budgetId: String, onResult: (Result<Unit>) -> Unit) {
        val ref = userRef() ?: return onResult(Result.failure(notLoggedIn()))
        ref.child("budgets").child(budgetId).removeValue()
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(Exception(dbErrorMessage(it)))) }
    }

    // ---------------------------------------------------------------------
    // Metas de ahorro
    // ---------------------------------------------------------------------

    fun insertSavingGoal(goal: SavingGoal, onResult: (Result<Unit>) -> Unit) {
        val ref = userRef() ?: return onResult(Result.failure(notLoggedIn()))
        val data = mapOf(
            "name" to goal.name,
            "targetAmount" to goal.targetAmount,
            "savedAmount" to goal.savedAmount,
            "targetDate" to goal.targetDate
        )
        ref.child("goals").push().setValue(data)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(Exception(dbErrorMessage(it)))) }
    }

    fun addDepositToGoal(goalId: String, additionalAmount: Double, onResult: (Result<Unit>) -> Unit) {
        val ref = userRef() ?: return onResult(Result.failure(notLoggedIn()))
        val savedRef = ref.child("goals").child(goalId).child("savedAmount")
        savedRef.get()
            .addOnSuccessListener { snap ->
                val current = snap.toDouble()
                savedRef.setValue(current + additionalAmount)
                    .addOnSuccessListener { onResult(Result.success(Unit)) }
                    .addOnFailureListener { onResult(Result.failure(Exception(dbErrorMessage(it)))) }
            }
            .addOnFailureListener { onResult(Result.failure(Exception(dbErrorMessage(it)))) }
    }

    fun deleteSavingGoal(goalId: String, onResult: (Result<Unit>) -> Unit) {
        val ref = userRef() ?: return onResult(Result.failure(notLoggedIn()))
        ref.child("goals").child(goalId).removeValue()
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(Exception(dbErrorMessage(it)))) }
    }

    // ---------------------------------------------------------------------
    // Lectura de snapshots
    // ---------------------------------------------------------------------

    private fun DataSnapshot.toDouble(): Double = (value as? Number)?.toDouble() ?: 0.0

    private fun DataSnapshot.str(key: String, default: String = ""): String =
        this.child(key).getValue(String::class.java) ?: default

    private fun profileToMap(user: User): Map<String, Any> = mapOf(
        "fullName" to user.fullName,
        "email" to user.email,
        "career" to user.career,
        "university" to user.university,
        "currency" to user.currency
    )

    private fun parseUser(snap: DataSnapshot): User? {
        if (!snap.exists()) return null
        return User(
            id = currentUid() ?: "",
            fullName = snap.str("fullName"),
            email = snap.str("email", auth.currentUser?.email ?: ""),
            career = snap.str("career"),
            university = snap.str("university"),
            currency = snap.str("currency", DEFAULT_CURRENCY)
        )
    }

    private fun parseTransaction(snap: DataSnapshot, uid: String): TransactionItem = TransactionItem(
        id = snap.key ?: "",
        userId = uid,
        title = snap.str("title"),
        amount = snap.child("amount").toDouble(),
        type = snap.str("type", "GASTO"),
        category = snap.str("category", "Otros"),
        date = snap.str("date"),
        monthGroup = snap.str("monthGroup"),
        paymentMethod = snap.str("paymentMethod", "Efectivo"),
        description = snap.str("description")
    )

    /** Los ids de push() son cronológicos: se invierte para mostrar primero los más recientes. */
    private fun parseTransactions(snap: DataSnapshot, uid: String): List<TransactionItem> =
        snap.children.map { parseTransaction(it, uid) }.reversed()

    private fun parseBudgets(
        snap: DataSnapshot,
        uid: String,
        transactions: List<TransactionItem>
    ): List<BudgetItem> {
        val spentByCategory = transactions.filter { it.type == "GASTO" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        return snap.children.map {
            val category = it.str("category")
            BudgetItem(
                id = it.key ?: "",
                userId = uid,
                category = category,
                limitAmount = it.child("limitAmount").toDouble(),
                spentAmount = spentByCategory[category] ?: 0.0
            )
        }
    }

    private fun parseGoals(snap: DataSnapshot, uid: String): List<SavingGoal> =
        snap.children.map {
            SavingGoal(
                id = it.key ?: "",
                userId = uid,
                name = it.str("name"),
                targetAmount = it.child("targetAmount").toDouble(),
                savedAmount = it.child("savedAmount").toDouble(),
                targetDate = it.str("targetDate")
            )
        }.reversed()

    // ---------------------------------------------------------------------
    // Mensajes de error en español
    // ---------------------------------------------------------------------

    private fun authErrorMessage(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "La contraseña debe tener al menos 6 caracteres"
        is FirebaseAuthUserCollisionException -> "Este correo ya está registrado"
        is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo"
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
        is FirebaseAuthRecentLoginRequiredException ->
            "Por seguridad, cierra sesión y vuelve a ingresar antes de cambiar la contraseña"
        is FirebaseNetworkException -> "Sin conexión a internet. Intenta de nuevo"
        else -> e.localizedMessage ?: "Ocurrió un error inesperado"
    }

    private fun dbErrorMessage(e: Exception): String = when {
        e.message?.contains("Permission denied", ignoreCase = true) == true ->
            "Permiso denegado en la base de datos. Revisa las reglas de Firebase"
        else -> e.localizedMessage ?: "No se pudo completar la operación"
    }
}
