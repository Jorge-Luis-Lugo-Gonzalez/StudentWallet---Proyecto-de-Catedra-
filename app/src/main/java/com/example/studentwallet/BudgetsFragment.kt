package com.example.studentwallet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BudgetsFragment : Fragment() {

    private lateinit var dbHelper: WalletDatabaseHelper
    private lateinit var session: SessionManager
    private lateinit var adapter: BudgetAdapter
    private lateinit var tvTotalAssigned: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_budgets, container, false)

        dbHelper = WalletDatabaseHelper(requireContext())
        session = SessionManager(requireContext())

        tvTotalAssigned = view.findViewById(R.id.tvTotalBudgetAssigned)
        val btnOpenGoals = view.findViewById<LinearLayout>(R.id.btnOpenSavingsGoals)
        val rvBudgets = view.findViewById<RecyclerView>(R.id.rvBudgets)

        rvBudgets.layoutManager = LinearLayoutManager(requireContext())
        adapter = BudgetAdapter(emptyList(), "$") { budgetItem ->
            showBudgetFormDialog(budgetItem)
        }
        rvBudgets.adapter = adapter

        btnOpenGoals.setOnClickListener {
            startActivity(Intent(requireContext(), GoalsActivity::class.java))
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        refreshBudgets()
    }

    fun refreshBudgets() {
        if (!isAdded) return
        val userId = session.getUserId()
        val user = dbHelper.getUserById(userId)
        val currency = user?.currency ?: "USD ($)"
        val symbol = CategoryConfig.getCurrencySymbol(currency)

        val budgets = dbHelper.getBudgetsWithSpent(userId)
        val totalAssigned = budgets.sumOf { it.limitAmount }
        tvTotalAssigned.text = CategoryConfig.formatMoney(totalAssigned, currency)

        adapter.updateData(budgets, symbol)
    }

    fun showBudgetFormDialog(existing: BudgetItem? = null) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_budget_form, null)

        val spCategory = dialogView.findViewById<Spinner>(R.id.spBudgetCategory)
        val etLimit = dialogView.findViewById<EditText>(R.id.etBudgetLimit)
        val tvError = dialogView.findViewById<TextView>(R.id.tvBudgetFormError)

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            CategoryConfig.expenseCategories
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = spinnerAdapter

        if (existing != null) {
            val idx = CategoryConfig.expenseCategories.indexOf(existing.category)
            if (idx >= 0) spCategory.setSelection(idx)
            etLimit.setText(String.format("%.2f", existing.limitAmount))
        }

        val builder = AlertDialog.Builder(requireContext())
            .setTitle(if (existing == null) "Configurar presupuesto" else "Editar presupuesto")
            .setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)

        if (existing != null) {
            builder.setNeutralButton("Eliminar") { _, _ ->
                dbHelper.deleteBudget(existing.id)
                Toast.makeText(requireContext(), "Presupuesto eliminado", Toast.LENGTH_SHORT).show()
                refreshBudgets()
            }
        }

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val category = spCategory.selectedItem.toString()
            val limit = etLimit.text.toString().replace(",", ".").trim().toDoubleOrNull()

            if (limit == null || limit <= 0.0) {
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            dbHelper.saveOrUpdateBudget(session.getUserId(), category, limit)
            Toast.makeText(requireContext(), "Presupuesto guardado", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            refreshBudgets()
        }
    }
}
