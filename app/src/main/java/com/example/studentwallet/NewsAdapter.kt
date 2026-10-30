package com.example.studentwallet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NewsAdapter(
    private var articles: List<NewsArticle>,
    private val onArticleClick: (NewsArticle) -> Unit
) : RecyclerView.Adapter<NewsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTag: TextView = view.findViewById(R.id.tvNewsTag)
        val tvDate: TextView = view.findViewById(R.id.tvNewsDate)
        val tvTitle: TextView = view.findViewById(R.id.tvNewsTitle)
        val tvSummary: TextView = view.findViewById(R.id.tvNewsSummary)
    }

    fun updateArticles(newList: List<NewsArticle>) {
        articles = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news_article, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = articles[position]
        holder.tvTag.text = item.tag
        holder.tvDate.text = item.date
        holder.tvTitle.text = item.title
        holder.tvSummary.text = item.summary

        holder.itemView.setOnClickListener {
            onArticleClick(item)
        }
    }

    override fun getItemCount(): Int = articles.size
}
