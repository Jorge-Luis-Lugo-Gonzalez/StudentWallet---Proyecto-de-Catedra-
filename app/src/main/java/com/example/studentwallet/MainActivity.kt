package com.example.studentwallet

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var tvToolbarTitle: TextView
    private lateinit var fabMain: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val session = SessionManager(this)
        if (!session.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle)
        val ivAvatar = findViewById<ImageView>(R.id.ivToolbarAvatar)
        fabMain = findViewById(R.id.fabMain)
        bottomNav = findViewById(R.id.bottomNav)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    tvToolbarTitle.text = "Student Wallet"
                    fabMain.visibility = View.VISIBLE
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_movements -> {
                    tvToolbarTitle.text = "Student Wallet"
                    fabMain.visibility = View.VISIBLE
                    loadFragment(MovementsFragment())
                    true
                }
                R.id.nav_budgets -> {
                    tvToolbarTitle.text = "Student Wallet"
                    fabMain.visibility = View.VISIBLE
                    loadFragment(BudgetsFragment())
                    true
                }
                R.id.nav_news -> {
                    tvToolbarTitle.text = "Student Wallet"
                    fabMain.visibility = View.GONE
                    loadFragment(NewsFragment())
                    true
                }
                R.id.nav_profile -> {
                    tvToolbarTitle.text = "Perfil"
                    fabMain.visibility = View.GONE
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        fabMain.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment is BudgetsFragment) {
                currentFragment.showBudgetFormDialog(null)
            } else {
                startActivity(Intent(this, AddTransactionActivity::class.java))
            }
        }

        ivAvatar.setOnClickListener {
            navigateToTab(R.id.nav_profile)
        }

        btnMenu.setOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            popup.menu.add(0, 1, 0, "Metas de Ahorro")
            popup.menu.add(0, 2, 1, "Agregar Movimiento")
            popup.menu.add(0, 3, 2, "Presupuestos")
            popup.menu.add(0, 4, 3, "Noticias Académicas")
            popup.menu.add(0, 5, 4, "Cerrar Sesión")
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    1 -> startActivity(Intent(this, GoalsActivity::class.java))
                    2 -> startActivity(Intent(this, AddTransactionActivity::class.java))
                    3 -> navigateToTab(R.id.nav_budgets)
                    4 -> navigateToTab(R.id.nav_news)
                    5 -> {
                        session.clearSession()
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }
                true
            }
            popup.show()
        }
    }

    fun navigateToTab(itemId: Int) {
        bottomNav.selectedItemId = itemId
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
