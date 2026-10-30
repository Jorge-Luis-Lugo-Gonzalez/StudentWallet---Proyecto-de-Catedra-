package com.example.studentwallet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class NewsFragment : Fragment() {

    private lateinit var llLoading: LinearLayout
    private lateinit var llError: LinearLayout
    private lateinit var rvNews: RecyclerView
    private lateinit var adapter: NewsAdapter
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_news, container, false)

        llLoading = view.findViewById(R.id.llNewsLoading)
        llError = view.findViewById(R.id.llNewsError)
        rvNews = view.findViewById(R.id.rvNews)
        val btnRetry = view.findViewById<Button>(R.id.btnRetryNews)
        val btnRefresh = view.findViewById<TextView>(R.id.btnRefreshNews)

        rvNews.layoutManager = LinearLayoutManager(requireContext())
        adapter = NewsAdapter(emptyList()) { article ->
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                startActivity(browserIntent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show()
            }
        }
        rvNews.adapter = adapter

        btnRetry.setOnClickListener {
            loadNewsFromApi(false)
        }

        btnRefresh.setOnClickListener {
            loadNewsFromApi(false)
        }

        btnRefresh.setOnLongClickListener {
            loadNewsFromApi(true)
            true
        }

        loadNewsFromApi(false)

        return view
    }

    private fun loadNewsFromApi(simulateError: Boolean) {
        llLoading.visibility = View.VISIBLE
        llError.visibility = View.GONE
        rvNews.visibility = View.GONE

        executor.execute {
            val result = NewsApiService.fetchAcademicAndFinancialNews(simulateError)
            mainHandler.post {
                if (!isAdded) return@post
                llLoading.visibility = View.GONE

                result.onSuccess { list ->
                    llError.visibility = View.GONE
                    rvNews.visibility = View.VISIBLE
                    adapter.updateArticles(list)
                }.onFailure {
                    rvNews.visibility = View.GONE
                    llError.visibility = View.VISIBLE
                }
            }
        }
    }
}
