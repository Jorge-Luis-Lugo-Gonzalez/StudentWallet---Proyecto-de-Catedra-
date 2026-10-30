package com.example.studentwallet

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Calendar

class AddTransactionActivity : AppCompatActivity() {

    private var selectedType = "GASTO"
    private var selectedCategory = "Alimentación"
    private var selectedPayment = "Tarjeta"
    private var selectedDateText = "14 Ago"
    private var selectedMonthGroup = "Agosto 2026"
    private var editingTransactionId = ""

    private val categoryViews = mutableMapOf<String, LinearLayout>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        val session = SessionManager(this)
        val userId = session.getUserId()

        val btnClose = findViewById<ImageButton>(R.id.btnCloseAdd)
        val tvTitle = findViewById<TextView>(R.id.tvAddScreenTitle)
        val tvToggleExpense = findViewById<TextView>(R.id.tvToggleExpense)
        val tvToggleIncome = findViewById<TextView>(R.id.tvToggleIncome)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val llAmountError = findViewById<LinearLayout>(R.id.llAmountError)
        val gridCategories = findViewById<GridLayout>(R.id.gridCategories)
        val llDateSelector = findViewById<LinearLayout>(R.id.llDateSelector)
        val tvSelectedDate = findViewById<TextView>(R.id.tvSelectedDate)
        val btnPayCash = findViewById<TextView>(R.id.btnPayCash)
        val btnPayCard = findViewById<TextView>(R.id.btnPayCard)
        val btnPayTransfer = findViewById<TextView>(R.id.btnPayTransfer)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val btnSave = findViewById<Button>(R.id.btnSaveTransaction)

        setupCategoryGrid(gridCategories)

        val initialType = intent.getStringExtra("EXTRA_TYPE")
        if (initialType == "INGRESO") {
            selectedType = "INGRESO"
        }

        editingTransactionId = intent.getStringExtra("EXTRA_TRANSACTION_ID") ?: ""
        if (editingTransactionId.isNotEmpty()) {
            tvTitle.text = "Editar Movimiento"
            btnSave.text = "Guardar cambios"
            FirebaseRepository.getTransactionById(editingTransactionId) { existing ->
                if (existing == null || isFinishing) return@getTransactionById
                selectedType = existing.type
                selectedCategory = if (existing.category in CategoryConfig.expenseCategories) {
                    existing.category
                } else {
                    "Otros"
                }
                selectedPayment = existing.paymentMethod
                selectedDateText = existing.date
                selectedMonthGroup = existing.monthGroup
                etAmount.setText(String.format("%.2f", existing.amount))
                etDescription.setText(existing.description)
                tvSelectedDate.text = existing.date
                updateTypeToggleUI(tvToggleExpense, tvToggleIncome)
                updateCategorySelectionUI()
                updatePaymentSelectionUI(btnPayCash, btnPayCard, btnPayTransfer)
            }
        }

        updateTypeToggleUI(tvToggleExpense, tvToggleIncome)
        updateCategorySelectionUI()
        updatePaymentSelectionUI(btnPayCash, btnPayCard, btnPayTransfer)

        btnClose.setOnClickListener { finish() }

        tvToggleExpense.setOnClickListener {
            selectedType = "GASTO"
            updateTypeToggleUI(tvToggleExpense, tvToggleIncome)
        }

        tvToggleIncome.setOnClickListener {
            selectedType = "INGRESO"
            updateTypeToggleUI(tvToggleExpense, tvToggleIncome)
        }

        btnPayCash.setOnClickListener {
            selectedPayment = "Efectivo"
            updatePaymentSelectionUI(btnPayCash, btnPayCard, btnPayTransfer)
        }

        btnPayCard.setOnClickListener {
            selectedPayment = "Tarjeta"
            updatePaymentSelectionUI(btnPayCash, btnPayCard, btnPayTransfer)
        }

        btnPayTransfer.setOnClickListener {
            selectedPayment = "Transf."
            updatePaymentSelectionUI(btnPayCash, btnPayCard, btnPayTransfer)
        }

        llDateSelector.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val shortMonths = arrayOf(
                        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
                        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
                    )
                    val fullMonths = arrayOf(
                        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
                    )
                    tvSelectedDate.text = String.format("%02d/%02d/%04d", month + 1, dayOfMonth, year)
                    selectedDateText = String.format("%02d %s", dayOfMonth, shortMonths[month])
                    selectedMonthGroup = "${fullMonths[month]} $year"
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnSave.setOnClickListener {
            val rawAmount = etAmount.text.toString().replace(",", ".").trim()
            val amount = rawAmount.toDoubleOrNull()

            if (amount == null || amount <= 0.0) {
                llAmountError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            llAmountError.visibility = View.GONE

            val desc = etDescription.text.toString().trim()
            val finalCategory = if (selectedType == "INGRESO" && selectedCategory == "Alimentación" && desc.isNotEmpty()) {
                "Ingresos"
            } else {
                selectedCategory
            }
            val finalTitle = if (desc.isNotEmpty()) desc else finalCategory

            val item = TransactionItem(
                id = editingTransactionId,
                userId = userId,
                title = finalTitle,
                amount = amount,
                type = selectedType,
                category = finalCategory,
                date = selectedDateText,
                monthGroup = selectedMonthGroup,
                paymentMethod = selectedPayment,
                description = desc.ifEmpty { finalTitle }
            )

            btnSave.isEnabled = false
            FirebaseRepository.saveTransaction(item) { result ->
                btnSave.isEnabled = true
                result.onSuccess {
                    val message = if (editingTransactionId.isNotEmpty()) "Movimiento actualizado" else "Movimiento guardado"
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    finish()
                }.onFailure { error ->
                    Toast.makeText(this, error.message ?: "No se pudo guardar el movimiento", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupCategoryGrid(grid: GridLayout) {
        grid.removeAllViews()
        categoryViews.clear()
        val density = resources.displayMetrics.density

        for (category in CategoryConfig.expenseCategories) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(
                    (6 * density).toInt(),
                    (12 * density).toInt(),
                    (6 * density).toInt(),
                    (12 * density).toInt()
                )
                background = ContextCompat.getDrawable(this@AddTransactionActivity, R.drawable.bg_category_unselected)
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(
                        (4 * density).toInt(),
                        (4 * density).toInt(),
                        (4 * density).toInt(),
                        (4 * density).toInt()
                    )
                }
                layoutParams = params
            }

            val circle = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams((42 * density).toInt(), (42 * density).toInt())
                background = ContextCompat.getDrawable(this@AddTransactionActivity, R.drawable.bg_circle_blue)
            }

            val icon = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams((22 * density).toInt(), (22 * density).toInt()).apply {
                    gravity = Gravity.CENTER
                }
                setImageResource(CategoryConfig.getIconRes(category))
                setColorFilter(Color.parseColor("#021468"))
            }
            circle.addView(icon)

            val label = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (8 * density).toInt()
                }
                text = category
                textSize = 11f
                setTextColor(Color.parseColor("#1A1F36"))
                gravity = Gravity.CENTER
                maxLines = 1
            }

            card.addView(circle)
            card.addView(label)

            card.setOnClickListener {
                selectedCategory = category
                updateCategorySelectionUI()
            }

            categoryViews[category] = card
            grid.addView(card)
        }
    }

    private fun updateCategorySelectionUI() {
        for ((cat, view) in categoryViews) {
            if (cat == selectedCategory) {
                view.setBackgroundResource(R.drawable.bg_category_selected)
            } else {
                view.setBackgroundResource(R.drawable.bg_category_unselected)
            }
        }
    }

    private fun updateTypeToggleUI(tvExpense: TextView, tvIncome: TextView) {
        if (selectedType == "GASTO") {
            tvExpense.setBackgroundResource(R.drawable.bg_toggle_selected)
            tvExpense.setTextColor(Color.parseColor("#0052CC"))
            tvIncome.background = null
            tvIncome.setTextColor(Color.parseColor("#5E677D"))
        } else {
            tvIncome.setBackgroundResource(R.drawable.bg_toggle_selected)
            tvIncome.setTextColor(Color.parseColor("#0052CC"))
            tvExpense.background = null
            tvExpense.setTextColor(Color.parseColor("#5E677D"))
        }
    }

    private fun updatePaymentSelectionUI(btnCash: TextView, btnCard: TextView, btnTransfer: TextView) {
        val buttons = listOf(
            Pair("Efectivo", btnCash),
            Pair("Tarjeta", btnCard),
            Pair("Transf.", btnTransfer)
        )
        for ((method, btn) in buttons) {
            if (method == selectedPayment) {
                btn.setBackgroundResource(R.drawable.bg_payment_selected)
                btn.setTextColor(Color.WHITE)
            } else {
                btn.setBackgroundResource(R.drawable.bg_payment_unselected)
                btn.setTextColor(Color.parseColor("#1A1F36"))
            }
        }
    }
}
