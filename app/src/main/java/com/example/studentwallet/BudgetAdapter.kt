package com.example.studentwallet

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class BudgetAdapter(
    private var items: List<BudgetItem>,
    private var currencySymbol: String,
    private val onItemClick: (BudgetItem) -> Unit
) : RecyclerView.Adapter<BudgetAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardContainer: LinearLayout = view.findViewById(R.id.cardBudgetContainer)
        val flIconBg: FrameLayout = view.findViewById(R.id.flBudgetIconBg)
        val ivIcon: ImageView = view.findViewById(R.id.ivBudgetIcon)
        val tvCategory: TextView = view.findViewById(R.id.tvBudgetCategory)
        val tvBadge: TextView = view.findViewById(R.id.tvBudgetBadge)
        val tvSpent: TextView = view.findViewById(R.id.tvBudgetSpent)
        val tvLimit: TextView = view.findViewById(R.id.tvBudgetLimit)
        val tvRemaining: TextView = view.findViewById(R.id.tvBudgetRemaining)
        val pbCard: ProgressBar = view.findViewById(R.id.pbBudgetCard)
    }

    fun updateData(newItems: List<BudgetItem>, symbol: String) {
        items = newItems
        currencySymbol = symbol
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_budget_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        val pct = item.percentage

        holder.tvCategory.text = item.category
        holder.tvSpent.text = String.format("%s%.0f", currencySymbol, item.spentAmount)
        holder.tvLimit.text = String.format("/ %s%.0f", currencySymbol, item.limitAmount)
        holder.ivIcon.setImageResource(CategoryConfig.getIconRes(item.category))

        holder.pbCard.max = 100
        holder.pbCard.progress = pct.coerceAtMost(100)

        if (pct >= 100) {
            holder.cardContainer.setBackgroundResource(R.drawable.bg_card_budget_exceeded)
            holder.flIconBg.setBackgroundResource(R.drawable.bg_circle_soft_red)
            holder.ivIcon.setColorFilter(Color.parseColor("#B71C1C"))
            holder.tvCategory.setTextColor(Color.parseColor("#B71C1C"))
            holder.tvBadge.text = "⚠ $pct% Excedido"
            holder.tvBadge.setTextColor(Color.parseColor("#B71C1C"))
            holder.tvBadge.setBackgroundResource(R.drawable.bg_quick_expense)
            holder.tvRemaining.text = String.format("Excedido %s%.0f", currencySymbol, item.spentAmount - item.limitAmount)
            holder.tvRemaining.setTextColor(Color.parseColor("#B71C1C"))
            holder.pbCard.progressDrawable = ContextCompat.getDrawable(context, R.drawable.progress_budget_red)
        } else {
            holder.cardContainer.setBackgroundResource(R.drawable.bg_card_rounded)
            holder.flIconBg.setBackgroundResource(R.drawable.bg_circle_soft_blue)
            holder.ivIcon.setColorFilter(Color.parseColor("#08207A"))
            holder.tvCategory.setTextColor(Color.parseColor("#1A1F36"))
            holder.tvBadge.text = "$pct%"
            holder.tvBadge.setTextColor(Color.parseColor("#1A1F36"))
            holder.tvBadge.setBackgroundResource(R.drawable.bg_field_soft)
            holder.tvRemaining.text = String.format("Quedan %s%.0f", currencySymbol, item.remainingAmount)
            holder.tvRemaining.setTextColor(Color.parseColor("#5E677D"))
            holder.pbCard.progressDrawable = ContextCompat.getDrawable(context, R.drawable.progress_budget_blue)
        }

        holder.cardContainer.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
