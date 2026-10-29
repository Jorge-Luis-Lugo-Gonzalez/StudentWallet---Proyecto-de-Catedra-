package com.example.studentwallet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    private lateinit var dbHelper: WalletDatabaseHelper
    private lateinit var session: SessionManager

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvCareer: TextView
    private lateinit var tvUniversity: TextView
    private lateinit var tvCurrency: TextView
    private lateinit var tvCurrencyShort: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        dbHelper = WalletDatabaseHelper(requireContext())
        session = SessionManager(requireContext())

        tvName = view.findViewById(R.id.tvProfileName)
        tvEmail = view.findViewById(R.id.tvProfileEmail)
        tvCareer = view.findViewById(R.id.tvProfileCareer)
        tvUniversity = view.findViewById(R.id.tvProfileUniversity)
        tvCurrency = view.findViewById(R.id.tvProfileCurrency)
        tvCurrencyShort = view.findViewById(R.id.tvProfileCurrencyShort)

        val btnEditTop = view.findViewById<FrameLayout>(R.id.btnEditProfileTop)
        val rowEditProfile = view.findViewById<LinearLayout>(R.id.rowEditProfile)
        val rowChangePassword = view.findViewById<LinearLayout>(R.id.rowChangePassword)
        val rowChangeCurrency = view.findViewById<LinearLayout>(R.id.rowChangeCurrency)
        val rowNotifications = view.findViewById<LinearLayout>(R.id.rowNotifications)
        val rowLogout = view.findViewById<LinearLayout>(R.id.rowLogout)

        val openEditProfile = View.OnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }
        btnEditTop.setOnClickListener(openEditProfile)
        rowEditProfile.setOnClickListener(openEditProfile)

        rowChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        rowChangeCurrency.setOnClickListener {
            showCurrencyDialog()
        }

        rowNotifications.setOnClickListener {
            showNotificationsDialog()
        }

        rowLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Deseas cerrar tu sesión actual en Student Wallet?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Cerrar sesión") { _, _ ->
                    session.clearSession()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .show()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val user = dbHelper.getUserById(session.getUserId()) ?: return
        tvName.text = user.fullName
        tvEmail.text = user.email
        tvCareer.text = user.career
        tvUniversity.text = user.university
        tvCurrency.text = user.currency
        tvCurrencyShort.text = user.currency.take(3)
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_change_password, null)
        val etNew = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirm = dialogView.findViewById<EditText>(R.id.etConfirmNewPassword)

        AlertDialog.Builder(requireContext())
            .setTitle("Cambiar contraseña")
            .setView(dialogView)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val p1 = etNew.text.toString()
                val p2 = etConfirm.text.toString()
                when {
                    p1.length < 4 -> {
                        Toast.makeText(requireContext(), "La contraseña debe tener al menos 4 caracteres", Toast.LENGTH_SHORT).show()
                    }
                    p1 != p2 -> {
                        Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        dbHelper.updateUserPassword(session.getUserId(), p1)
                        Toast.makeText(requireContext(), "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun showCurrencyDialog() {
        val currencies = arrayOf(
            "USD ($)",
            "MXN - Peso Mexicano",
            "EUR (€)",
            "SVC - Dólar / Colón"
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar moneda principal")
            .setItems(currencies) { _, which ->
                dbHelper.updateUserCurrency(session.getUserId(), currencies[which])
                loadUserData()
                Toast.makeText(requireContext(), "Moneda actualizada a ${currencies[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showNotificationsDialog() {
        val enabled = session.areNotificationsEnabled()
        val items = arrayOf("Alertas de presupuesto (80% y límite excedido)")
        val checked = booleanArrayOf(enabled)

        AlertDialog.Builder(requireContext())
            .setTitle("Notificaciones")
            .setMultiChoiceItems(items, checked) { _, _, isChecked ->
                checked[0] = isChecked
            }
            .setPositiveButton("Guardar") { _, _ ->
                session.setNotificationsEnabled(checked[0])
                Toast.makeText(requireContext(), "Preferencias guardadas", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
