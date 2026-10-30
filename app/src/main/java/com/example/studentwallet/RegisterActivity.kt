package com.example.studentwallet

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    private var passVisible = false
    private var confirmVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val lblName = findViewById<TextView>(R.id.lblRegName)
        val boxName = findViewById<LinearLayout>(R.id.boxRegName)
        val etName = findViewById<EditText>(R.id.etRegName)
        val iconErrName = findViewById<ImageView>(R.id.iconErrName)
        val tvErrName = findViewById<TextView>(R.id.tvErrName)

        val lblEmail = findViewById<TextView>(R.id.lblRegEmail)
        val boxEmail = findViewById<LinearLayout>(R.id.boxRegEmail)
        val etEmail = findViewById<EditText>(R.id.etRegEmail)
        val iconErrEmail = findViewById<ImageView>(R.id.iconErrEmail)
        val tvErrEmail = findViewById<TextView>(R.id.tvErrEmail)

        val lblPassword = findViewById<TextView>(R.id.lblRegPassword)
        val boxPassword = findViewById<LinearLayout>(R.id.boxRegPassword)
        val etPassword = findViewById<EditText>(R.id.etRegPassword)
        val btnTogglePass = findViewById<ImageButton>(R.id.btnToggleRegPass)

        val lblConfirm = findViewById<TextView>(R.id.lblRegConfirm)
        val boxConfirm = findViewById<LinearLayout>(R.id.boxRegConfirm)
        val etConfirm = findViewById<EditText>(R.id.etRegConfirm)
        val btnToggleConfirm = findViewById<ImageButton>(R.id.btnToggleRegConfirm)
        val tvErrPassword = findViewById<TextView>(R.id.tvErrPassword)

        val lblCareer = findViewById<TextView>(R.id.lblRegCareer)
        val boxCareer = findViewById<LinearLayout>(R.id.boxRegCareer)
        val etCareer = findViewById<EditText>(R.id.etRegCareer)
        val iconErrCareer = findViewById<ImageView>(R.id.iconErrCareer)
        val tvErrCareer = findViewById<TextView>(R.id.tvErrCareer)

        val etUni = findViewById<EditText>(R.id.etRegUni)
        val spCurrency = findViewById<Spinner>(R.id.spRegCurrency)
        val btnCreate = findViewById<Button>(R.id.btnCreateAccount)
        val tvBackLogin = findViewById<TextView>(R.id.tvBackToLogin)

        val currencies = listOf(
            "USD ($)",
            "MXN - Peso Mexicano",
            "EUR (€)",
            "SVC - Dólar / Colón"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCurrency.adapter = adapter

        btnTogglePass.setOnClickListener {
            passVisible = !passVisible
            etPassword.transformationMethod = if (passVisible) {
                HideReturnsTransformationMethod.getInstance()
            } else {
                PasswordTransformationMethod.getInstance()
            }
            etPassword.setSelection(etPassword.text.length)
        }

        btnToggleConfirm.setOnClickListener {
            confirmVisible = !confirmVisible
            etConfirm.transformationMethod = if (confirmVisible) {
                HideReturnsTransformationMethod.getInstance()
            } else {
                PasswordTransformationMethod.getInstance()
            }
            etConfirm.setSelection(etConfirm.text.length)
        }

        tvBackLogin.setOnClickListener {
            finish()
        }

        btnCreate.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString()
            val confirm = etConfirm.text.toString()
            val career = etCareer.text.toString().trim()
            val uni = etUni.text.toString().trim().ifEmpty { "Universidad Don Bosco" }
            val currency = spCurrency.selectedItem.toString()

            resetFieldStyle(lblName, boxName, iconErrName, tvErrName)
            resetFieldStyle(lblEmail, boxEmail, iconErrEmail, tvErrEmail)
            resetFieldStyle(lblPassword, boxPassword, null, null)
            resetFieldStyle(lblConfirm, boxConfirm, null, tvErrPassword)
            resetFieldStyle(lblCareer, boxCareer, iconErrCareer, tvErrCareer)

            var hasError = false

            if (name.isEmpty()) {
                setFieldError(lblName, boxName, iconErrName, tvErrName, "Campos obligatorios vacíos")
                hasError = true
            }

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                setFieldError(lblEmail, boxEmail, iconErrEmail, tvErrEmail, "Correo inválido")
                hasError = true
            }

            if (pass.length < 6) {
                setFieldError(lblPassword, boxPassword, null, null, "")
                setFieldError(lblConfirm, boxConfirm, null, tvErrPassword, "La contraseña debe tener al menos 6 caracteres")
                hasError = true
            } else if (pass != confirm) {
                setFieldError(lblPassword, boxPassword, null, null, "")
                setFieldError(lblConfirm, boxConfirm, null, tvErrPassword, "Contraseñas diferentes")
                hasError = true
            }

            if (career.isEmpty()) {
                setFieldError(lblCareer, boxCareer, iconErrCareer, tvErrCareer, "Campos obligatorios vacíos")
                hasError = true
            }

            if (hasError) return@setOnClickListener

            val newUser = User(
                fullName = name,
                email = email,
                career = career,
                university = uni,
                currency = currency
            )

            val originalText = btnCreate.text
            btnCreate.isEnabled = false
            btnCreate.text = "Creando cuenta..."

            FirebaseRepository.register(newUser, pass) { result ->
                btnCreate.isEnabled = true
                btnCreate.text = originalText

                result.onSuccess {
                    Toast.makeText(this, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }.onFailure { error ->
                    val message = error.message ?: "No se pudo registrar la cuenta"
                    if (message.contains("correo", ignoreCase = true)) {
                        setFieldError(lblEmail, boxEmail, iconErrEmail, tvErrEmail, message)
                    } else if (message.contains("contraseña", ignoreCase = true)) {
                        setFieldError(lblPassword, boxPassword, null, null, "")
                        setFieldError(lblConfirm, boxConfirm, null, tvErrPassword, message)
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun resetFieldStyle(
        label: TextView,
        box: LinearLayout,
        icon: ImageView?,
        errorText: TextView?
    ) {
        label.setTextColor(Color.parseColor("#1A1F36"))
        box.setBackgroundResource(R.drawable.bg_input_normal)
        icon?.visibility = View.GONE
        errorText?.visibility = View.GONE
    }

    private fun setFieldError(
        label: TextView,
        box: LinearLayout,
        icon: ImageView?,
        errorText: TextView?,
        message: String
    ) {
        label.setTextColor(Color.parseColor("#B71C1C"))
        box.setBackgroundResource(R.drawable.bg_input_error)
        icon?.visibility = View.VISIBLE
        if (errorText != null && message.isNotEmpty()) {
            errorText.text = message
            errorText.visibility = View.VISIBLE
        }
    }
}
