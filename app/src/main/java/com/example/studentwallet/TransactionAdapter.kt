package com.example.studentwallet

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(
    private var items: List<TransactionItem>,
    private var currency: String,
    private val onItemClick: (TransactionItem) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMonthHeader: TextView = view.findViewById(R.id.tvMonthGroupHeader)
        val cardRow: LinearLayout = view.findViewById(R.id.cardMovementRow)
        val flIconBg: FrameLayout = view.findViewById(R.id.flHistoryIconBg)
        val ivIcon: ImageView = view.findViewById(R.id.ivHistoryIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvHistoryTitle)
        val tvSubtitle: TextView = view.findViewById(R.id.tvHistorySubtitle)
        val tvAmount: TextView = view.findViewById(R.id.tvHistoryAmount)
    }

    fun updateList(newItems: List<TransactionItem>, newCurrency: String) {
        items = newItems
        currency = newCurrency
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        val showHeader = position == 0 || items[position - 1].monthGroup != item.monthGroup
        if (showHeader) {
            holder.tvMonthHeader.visibility = View.VISIBLE
            holder.tvMonthHeader.text = item.monthGroup
        } else {
            holder.tvMonthHeader.visibility = View.GONE
        }

        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = "${item.category} • ${item.date}"

        val formatted = CategoryConfig.formatMoney(item.amount, currency)
        if (item.type == "INGRESO") {
            holder.tvAmount.text = "+$formatted"
            holder.tvAmount.setTextColor(Color.parseColor("#388E3C"))
            holder.flIconBg.setBackgroundResource(R.drawable.bg_circle_soft_green)
            holder.ivIcon.setImageResource(R.drawable.ic_university)
            holder.ivIcon.setColorFilter(Color.parseColor("#1B5E20"))
        } else {
            holder.tvAmount.text = "-$formatted"
            holder.tvAmount.setTextColor(Color.parseColor("#C62828"))
            holder.flIconBg.setBackgroundResource(R.drawable.bg_circle_soft_red)
            holder.ivIcon.setImageResource(CategoryConfig.getIconRes(item.category, item.type))
            holder.ivIcon.setColorFilter(Color.parseColor("#B71C1C"))
        }

        holder.cardRow.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
