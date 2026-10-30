package com.example.studentwallet

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MovementsFragment : Fragment() {

    private lateinit var adapter: TransactionAdapter

    private var allTransactions: List<TransactionItem> = emptyList()
    private var currency: String = FirebaseRepository.DEFAULT_CURRENCY
    private var searchQuery: String = ""
    private var filterDate: String = "Todas"
    private var filterCategory: String = "Todas"
    private var filterType: String = "Todos"
    private var filterAmountMode: Int = 0

    private lateinit var rvMovements: RecyclerView
    private lateinit var llEmpty: LinearLayout
    private lateinit var chipDate: TextView
    private lateinit var chipCategory: TextView
    private lateinit var chipType: TextView
    private lateinit var chipAmount: TextView
    private lateinit var etSearch: EditText

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_movements, container, false)

        etSearch = view.findViewById(R.id.etSearchMovements)
        chipDate = view.findViewById(R.id.chipFilterDate)
        chipCategory = view.findViewById(R.id.chipFilterCategory)
        chipType = view.findViewById(R.id.chipFilterType)
        chipAmount = view.findViewById(R.id.chipFilterAmount)
        llEmpty = view.findViewById(R.id.llEmptyMovements)
        val tvClearFilters = view.findViewById<TextView>(R.id.tvClearFilters)
        rvMovements = view.findViewById(R.id.rvMovements)

        rvMovements.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter(emptyList(), "USD ($)") { item ->
            showTransactionOptionsDialog(item)
        }
        rvMovements.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        chipDate.setOnClickListener { showDateFilterDialog() }
        chipCategory.setOnClickListener { showCategoryFilterDialog() }
        chipType.setOnClickListener { showTypeFilterDialog() }
        chipAmount.setOnClickListener { showAmountFilterDialog() }

        tvClearFilters.setOnClickListener {
            etSearch.setText("")
            filterDate = "Todas"
            filterCategory = "Todas"
            filterType = "Todos"
            filterAmountMode = 0
            updateChipStyles()
            applyFilters()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        FirebaseRepository.loadDashboard { result ->
            if (!isAdded) return@loadDashboard
            result.onSuccess { data ->
                allTransactions = data.transactions
                currency = data.currency
                applyFilters()
            }.onFailure { error ->
                Toast.makeText(requireContext(), error.message ?: "No se pudieron cargar los movimientos", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun applyFilters() {

        var filtered = allTransactions.filter { item ->
            val matchesQuery = searchQuery.isEmpty() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)

            val matchesDate = filterDate == "Todas" ||
                item.date.equals(filterDate, ignoreCase = true) ||
                item.monthGroup.equals(filterDate, ignoreCase = true)

            val matchesCat = filterCategory == "Todas" ||
                item.category.equals(filterCategory, ignoreCase = true)

            val matchesType = when (filterType) {
                "Ingresos" -> item.type == "INGRESO"
                "Gastos" -> item.type == "GASTO"
                else -> true
            }

            val matchesAmount = when (filterAmountMode) {
                1 -> item.amount < 20.0
                2 -> item.amount >= 20.0
                else -> true
            }

            matchesQuery && matchesDate && matchesCat && matchesType && matchesAmount
        }

        if (filterAmountMode == 3) {
            filtered = filtered.sortedByDescending { it.amount }
        }

        adapter.updateList(filtered, currency)

        if (filtered.isEmpty()) {
            llEmpty.visibility = View.VISIBLE
            rvMovements.visibility = View.GONE
        } else {
            llEmpty.visibility = View.GONE
            rvMovements.visibility = View.VISIBLE
        }
    }

    private fun showDateFilterDialog() {
        val options = arrayOf("Todas", "Hoy", "Agosto 2026")
        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por fecha")
            .setItems(options) { _, which ->
                filterDate = options[which]
                updateChipStyles()
                applyFilters()
            }
            .show()
    }

    private fun showCategoryFilterDialog() {
        val options = (listOf("Todas") + CategoryConfig.allCategories).toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por categoría")
            .setItems(options) { _, which ->
                filterCategory = options[which]
                updateChipStyles()
                applyFilters()
            }
            .show()
    }

    private fun showTypeFilterDialog() {
        val options = arrayOf("Todos", "Gastos", "Ingresos")
        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por tipo")
            .setItems(options) { _, which ->
                filterType = options[which]
                updateChipStyles()
                applyFilters()
            }
            .show()
    }

    private fun showAmountFilterDialog() {
        val options = arrayOf(
            "Todos los montos",
            "Menores a $20.00",
            "Mayores o iguales a $20.00",
            "Ordenar de mayor a menor"
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Filtrar por monto")
            .setItems(options) { _, which ->
                filterAmountMode = which
                updateChipStyles()
                applyFilters()
            }
            .show()
    }

    private fun updateChipStyles() {
        chipDate.text = if (filterDate == "Todas") "📅  Fecha" else "📅  $filterDate"
        chipDate.setBackgroundResource(if (filterDate == "Todas") R.drawable.bg_chip_filter else R.drawable.bg_chip_selected)

        chipCategory.text = if (filterCategory == "Todas") "❖  Categoría" else "❖  $filterCategory"
        chipCategory.setBackgroundResource(if (filterCategory == "Todas") R.drawable.bg_chip_filter else R.drawable.bg_chip_selected)

        chipType.text = if (filterType == "Todos") "⇅  Tipo" else "⇅  $filterType"
        chipType.setBackgroundResource(if (filterType == "Todos") R.drawable.bg_chip_filter else R.drawable.bg_chip_selected)

        chipAmount.setBackgroundResource(if (filterAmountMode == 0) R.drawable.bg_chip_filter else R.drawable.bg_chip_selected)
    }

    private fun showTransactionOptionsDialog(item: TransactionItem) {
        val options = arrayOf("Editar movimiento", "Eliminar movimiento")
        AlertDialog.Builder(requireContext())
            .setTitle(item.title)
            .setItems(options) { _, which ->
                if (which == 0) {
                    val intent = Intent(requireContext(), AddTransactionActivity::class.java)
                    intent.putExtra("EXTRA_TRANSACTION_ID", item.id)
                    startActivity(intent)
                } else {
                    showDeleteConfirmationDialog(item)
                }
            }
            .show()
    }

    private fun showDeleteConfirmationDialog(item: TransactionItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("¿Eliminar movimiento?")
            .setMessage("Se eliminará \"${item.title}\" y se actualizará el saldo disponible.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                FirebaseRepository.deleteTransaction(item.id) { result ->
                    if (!isAdded) return@deleteTransaction
                    result.onSuccess {
                        Toast.makeText(requireContext(), "Movimiento eliminado", Toast.LENGTH_SHORT).show()
                        loadData()
                    }.onFailure { error ->
                        Toast.makeText(requireContext(), error.message ?: "No se pudo eliminar", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }
}
