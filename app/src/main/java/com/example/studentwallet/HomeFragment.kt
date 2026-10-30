package com.example.studentwallet

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    private lateinit var session: SessionManager

    private lateinit var tvGreeting: TextView
    private lateinit var tvAvailableBalance: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var llAlertBanners: LinearLayout
    private lateinit var llRecentMovements: LinearLayout
    private lateinit var llDashboardBudgets: LinearLayout
    private lateinit var donutChart: DonutChartView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        session = SessionManager(requireContext())

        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvAvailableBalance = view.findViewById(R.id.tvAvailableBalance)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense)
        llAlertBanners = view.findViewById(R.id.llAlertBanners)
        llRecentMovements = view.findViewById(R.id.llRecentMovements)
        llDashboardBudgets = view.findViewById(R.id.llDashboardBudgets)
        donutChart = view.findViewById(R.id.donutChart)

        val btnQuickExpense = view.findViewById<LinearLayout>(R.id.btnQuickExpense)
        val btnQuickIncome = view.findViewById<LinearLayout>(R.id.btnQuickIncome)
        val btnQuickHistory = view.findViewById<LinearLayout>(R.id.btnQuickHistory)
        val btnQuickReports = view.findViewById<LinearLayout>(R.id.btnQuickReports)
        val tvSeeAllMovements = view.findViewById<TextView>(R.id.tvSeeAllMovements)

        btnQuickExpense.setOnClickListener {
            val intent = Intent(requireContext(), AddTransactionActivity::class.java)
            intent.putExtra("EXTRA_TYPE", "GASTO")
            startActivity(intent)
        }

        btnQuickIncome.setOnClickListener {
            val intent = Intent(requireContext(), AddTransactionActivity::class.java)
            intent.putExtra("EXTRA_TYPE", "INGRESO")
            startActivity(intent)
        }

        btnQuickHistory.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_movements)
        }

        tvSeeAllMovements.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_movements)
        }

        btnQuickReports.setOnClickListener {
            startActivity(Intent(requireContext(), GoalsActivity::class.java))
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
    }

    private fun refreshDashboard() {
        FirebaseRepository.loadDashboard { result ->
            if (!isAdded) return@loadDashboard
            result.onSuccess { data ->
                renderDashboard(data)
            }.onFailure { error ->
                Toast.makeText(requireContext(), error.message ?: "No se pudo cargar el resumen", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderDashboard(data: FirebaseRepository.DashboardData) {
        val user = data.user
        val currency = data.currency
        val symbol = CategoryConfig.getCurrencySymbol(currency)

        val firstName = user?.fullName?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "estudiante"
        tvGreeting.text = "Hola, $firstName 👋"

        val totalIncome = data.totalIncome
        val totalExpense = data.totalExpense
        val available = totalIncome - totalExpense

        tvAvailableBalance.text = CategoryConfig.formatMoney(available, currency)
        tvTotalIncome.text = "+${CategoryConfig.formatMoney(totalIncome, currency)}"
        tvTotalExpense.text = "-${CategoryConfig.formatMoney(totalExpense, currency)}"

        val budgets = data.budgets
        populateAlerts(budgets)
        populateRecentMovements(data.transactions, currency)
        populateBudgetsSummary(budgets, symbol)

        val expensesByCategory = data.expensesByCategory
        donutChart.setData(
            expensesByCategory.values.toList(),
            CategoryConfig.formatMoney(totalExpense, currency)
        )
    }

    private fun populateAlerts(budgets: List<BudgetItem>) {
        llAlertBanners.removeAllViews()
        if (!session.areNotificationsEnabled()) return

        val density = resources.displayMetrics.density
        val exceeded = budgets.filter { it.percentage >= 100 }
        val warning = budgets.filter { it.percentage in 80..99 }

        for (b in exceeded) {
            val banner = buildAlertView(
                bgRes = R.drawable.bg_alert_red,
                iconRes = R.drawable.ic_cancel_red,
                text = "Has superado tu presupuesto de ${b.category.lowercase()}.",
                textColor = Color.parseColor("#B71C1C"),
                density = density
            )
            llAlertBanners.addView(banner)
        }

        for (b in warning) {
            val banner = buildAlertView(
                bgRes = R.drawable.bg_alert_warning,
                iconRes = R.drawable.ic_warning_amber,
                text = "Has utilizado el ${b.percentage}% de tu presupuesto de ${b.category.lowercase()}.",
                textColor = Color.parseColor("#1A1F36"),
                density = density
            )
            llAlertBanners.addView(banner)
        }
    }

    private fun buildAlertView(
        bgRes: Int,
        iconRes: Int,
        text: String,
        textColor: Int,
        density: Float
    ): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(bgRes)
            setPadding(
                (16 * density).toInt(),
                (14 * density).toInt(),
                (16 * density).toInt(),
                (14 * density).toInt()
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (10 * density).toInt()
            }

            val icon = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams((22 * density).toInt(), (22 * density).toInt())
                setImageResource(iconRes)
            }

            val tv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = (12 * density).toInt()
                }
                this.text = text
                setTextColor(textColor)
                textSize = 14f
            }

            addView(icon)
            addView(tv)
        }
    }

    private fun populateRecentMovements(transactions: List<TransactionItem>, currency: String) {
        llRecentMovements.removeAllViews()
        val recent = transactions.take(3)
        val inflater = LayoutInflater.from(requireContext())

        for (item in recent) {
            val row = inflater.inflate(R.layout.item_recent_transaction, llRecentMovements, false)
            val flIconBg = row.findViewById<FrameLayout>(R.id.flRecentIconBg)
            val ivIcon = row.findViewById<ImageView>(R.id.ivRecentIcon)
            val tvTitle = row.findViewById<TextView>(R.id.tvRecentTitle)
            val tvDate = row.findViewById<TextView>(R.id.tvRecentDate)
            val tvAmount = row.findViewById<TextView>(R.id.tvRecentAmount)

            tvTitle.text = item.title
            tvDate.text = item.date

            val formatted = CategoryConfig.formatMoney(item.amount, currency)
            if (item.type == "INGRESO") {
                tvAmount.text = "+$formatted"
                tvAmount.setTextColor(Color.parseColor("#388E3C"))
                flIconBg.setBackgroundResource(R.drawable.bg_circle_soft_green)
                ivIcon.setImageResource(R.drawable.ic_cat_income)
                ivIcon.setColorFilter(Color.parseColor("#388E3C"))
            } else {
                tvAmount.text = "-$formatted"
                tvAmount.setTextColor(Color.parseColor("#C62828"))
                flIconBg.setBackgroundResource(R.drawable.bg_circle_soft_blue)
                ivIcon.setImageResource(CategoryConfig.getIconRes(item.category, item.type))
                ivIcon.setColorFilter(Color.parseColor("#08207A"))
            }

            row.setOnClickListener {
                (activity as? MainActivity)?.navigateToTab(R.id.nav_movements)
            }

            llRecentMovements.addView(row)
        }
    }

    private fun populateBudgetsSummary(
        budgets: List<BudgetItem>,
        symbol: String
    ) {
        llDashboardBudgets.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        for (b in budgets) {
            val row = inflater.inflate(R.layout.item_dashboard_budget, llDashboardBudgets, false)
            val tvCat = row.findViewById<TextView>(R.id.tvDashBudgetCategory)
            val tvSpent = row.findViewById<TextView>(R.id.tvDashBudgetSpent)
            val tvLimit = row.findViewById<TextView>(R.id.tvDashBudgetLimit)
            val pb = row.findViewById<ProgressBar>(R.id.pbDashBudget)

            tvSpent.text = String.format("%s%.0f", symbol, b.spentAmount)
            tvLimit.text = String.format(" / %s%.0f", symbol, b.limitAmount)
            pb.progress = b.percentage.coerceAtMost(100)

            if (b.percentage >= 100) {
                tvCat.text = "${b.category} ⚠"
                tvCat.setTextColor(Color.parseColor("#B71C1C"))
                tvSpent.setTextColor(Color.parseColor("#B71C1C"))
                pb.progressDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.progress_budget_red)
            } else {
                tvCat.text = b.category
                tvCat.setTextColor(Color.parseColor("#1A1F36"))
                tvSpent.setTextColor(Color.parseColor("#1A1F36"))
                pb.progressDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.progress_budget_blue)
            }

            row.setOnClickListener {
                (activity as? MainActivity)?.navigateToTab(R.id.nav_budgets)
            }

            llDashboardBudgets.addView(row)
        }
    }
}
