package com.example.studentwallet

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class GoalsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var llGoalsContainer: LinearLayout
    private var selectedDate = "12/31/2026"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        session = SessionManager(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBackGoals)
        llGoalsContainer = findViewById(R.id.llGoalsContainer)
        val etGoalName = findViewById<EditText>(R.id.etGoalName)
        val etGoalTarget = findViewById<EditText>(R.id.etGoalTarget)
        val etGoalInitial = findViewById<EditText>(R.id.etGoalInitial)
        val llGoalDatePicker = findViewById<LinearLayout>(R.id.llGoalDatePicker)
        val tvGoalTargetDate = findViewById<TextView>(R.id.tvGoalTargetDate)
        val tvGoalError = findViewById<TextView>(R.id.tvGoalError)
        val btnSaveGoal = findViewById<Button>(R.id.btnSaveGoal)

        btnBack.setOnClickListener { finish() }

        llGoalDatePicker.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate = String.format("%02d/%02d/%04d", month + 1, dayOfMonth, year)
                    tvGoalTargetDate.text = selectedDate
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnSaveGoal.setOnClickListener {
            val name = etGoalName.text.toString().trim()
            val target = etGoalTarget.text.toString().replace(",", ".").trim().toDoubleOrNull()
            val initial = etGoalInitial.text.toString().replace(",", ".").trim().toDoubleOrNull() ?: 0.0

            if (name.isEmpty() || target == null || target <= 0.0 || initial < 0.0) {
                tvGoalError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            tvGoalError.visibility = View.GONE

            val goal = SavingGoal(
                userId = session.getUserId(),
                name = name,
                targetAmount = target,
                savedAmount = initial,
                targetDate = selectedDate
            )
            btnSaveGoal.isEnabled = false
            FirebaseRepository.insertSavingGoal(goal) { result ->
                btnSaveGoal.isEnabled = true
                result.onSuccess {
                    Toast.makeText(this, "Meta de ahorro creada", Toast.LENGTH_SHORT).show()
                    etGoalName.setText("")
                    etGoalTarget.setText("")
                    etGoalInitial.setText("")
                    tvGoalTargetDate.text = "mm/dd/yyyy"
                    loadGoals()
                }.onFailure { error ->
                    Toast.makeText(this, error.message ?: "No se pudo crear la meta", Toast.LENGTH_LONG).show()
                }
            }
        }

        loadGoals()
    }

    private fun loadGoals() {
        FirebaseRepository.loadDashboard { result ->
            if (isFinishing || isDestroyed) return@loadDashboard
            result.onSuccess { data ->
                renderGoals(data.goals, CategoryConfig.getCurrencySymbol(data.currency))
            }.onFailure { error ->
                Toast.makeText(this, error.message ?: "No se pudieron cargar las metas", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderGoals(goals: List<SavingGoal>, symbol: String) {
        llGoalsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (goal in goals) {
            val card = inflater.inflate(R.layout.item_saving_goal, llGoalsContainer, false)

            val tvName = card.findViewById<TextView>(R.id.tvGoalItemName)
            val tvSaved = card.findViewById<TextView>(R.id.tvGoalSavedAmount)
            val tvTarget = card.findViewById<TextView>(R.id.tvGoalTargetAmount)
            val pbProgress = card.findViewById<ProgressBar>(R.id.pbGoalProgress)
            val tvPercent = card.findViewById<TextView>(R.id.tvGoalPercentText)
            val tvRemaining = card.findViewById<TextView>(R.id.tvGoalRemainingText)
            val btnOptions = card.findViewById<ImageButton>(R.id.btnGoalOptions)

            tvName.text = goal.name
            tvSaved.text = String.format("%s%,.0f", symbol, goal.savedAmount)
            tvTarget.text = String.format("/ %s%,.0f", symbol, goal.targetAmount)
            pbProgress.progress = goal.percentage
            tvPercent.text = "${goal.percentage}% completado"
            tvRemaining.text = if (goal.remainingAmount > 0) {
                String.format("Faltan %s%,.0f", symbol, goal.remainingAmount)
            } else {
                "¡Meta alcanzada!"
            }

            val clickAction = View.OnClickListener {
                showGoalOptionsDialog(goal)
            }
            btnOptions.setOnClickListener(clickAction)
            card.setOnClickListener(clickAction)

            llGoalsContainer.addView(card)
        }
    }

    private fun showGoalOptionsDialog(goal: SavingGoal) {
        val options = arrayOf("Registrar abono a la meta", "Eliminar meta")
        AlertDialog.Builder(this)
            .setTitle(goal.name)
            .setItems(options) { _, which ->
                if (which == 0) {
                    showDepositDialog(goal)
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("¿Eliminar meta?")
                        .setMessage("Se eliminará la meta \"${goal.name}\".")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Confirmar") { _, _ ->
                            FirebaseRepository.deleteSavingGoal(goal.id) { result ->
                                result.onSuccess { loadGoals() }
                                    .onFailure { error ->
                                        Toast.makeText(this, error.message ?: "No se pudo eliminar", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                        .show()
                }
            }
            .show()
    }

    private fun showDepositDialog(goal: SavingGoal) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_goal_deposit, null)
        val etDeposit = dialogView.findViewById<EditText>(R.id.etDepositAmount)

        AlertDialog.Builder(this)
            .setTitle("Abonar a: ${goal.name}")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Abonar") { _, _ ->
                val amount = etDeposit.text.toString().replace(",", ".").trim().toDoubleOrNull()
                if (amount != null && amount > 0.0) {
                    FirebaseRepository.addDepositToGoal(goal.id, amount) { result ->
                        result.onSuccess {
                            Toast.makeText(this, "Abono registrado", Toast.LENGTH_SHORT).show()
                            loadGoals()
                        }.onFailure { error ->
                            Toast.makeText(this, error.message ?: "No se pudo registrar el abono", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }
}
