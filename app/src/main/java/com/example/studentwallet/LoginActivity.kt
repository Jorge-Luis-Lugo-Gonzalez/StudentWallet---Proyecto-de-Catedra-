package com.example.studentwallet

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val session = SessionManager(this)
        if (session.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val etEmail = findViewById<EditText>(R.id.etLoginEmail)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val lineEmail = findViewById<View>(R.id.lineLoginEmail)
        val linePassword = findViewById<View>(R.id.lineLoginPassword)
        val tvEmailError = findViewById<TextView>(R.id.tvLoginEmailError)
        val tvLoginError = findViewById<TextView>(R.id.tvLoginError)
        val btnTogglePass = findViewById<ImageButton>(R.id.btnToggleLoginPassword)
        val tvForgot = findViewById<TextView>(R.id.tvForgotPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoToRegister)

        btnTogglePass.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                btnTogglePass.setImageResource(R.drawable.ic_eye)
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                btnTogglePass.setImageResource(R.drawable.ic_eye_off)
            }
            etPassword.setSelection(etPassword.text.length)
        }

        tvForgot.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                AlertDialog.Builder(this)
                    .setTitle("Recuperar contraseña")
                    .setMessage("Escribe tu correo en el campo de correo y vuelve a tocar \"¿Olvidaste tu contraseña?\" para recibir el enlace de recuperación.")
                    .setPositiveButton("Entendido", null)
                    .show()
                return@setOnClickListener
            }
            FirebaseRepository.sendPasswordReset(email) { result ->
                val message = result.fold(
                    onSuccess = { "Te enviamos un correo a $email para restablecer tu contraseña." },
                    onFailure = { it.message ?: "No se pudo enviar el correo" }
                )
                AlertDialog.Builder(this)
                    .setTitle("Recuperar contraseña")
                    .setMessage(message)
                    .setPositiveButton("Entendido", null)
                    .show()
            }
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            tvEmailError.visibility = View.GONE
            tvLoginError.visibility = View.GONE
            lineEmail.setBackgroundColor(Color.parseColor("#B0B8C9"))
            linePassword.setBackgroundColor(Color.parseColor("#B0B8C9"))

            var valid = true
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tvEmailError.text = "Correo institucional inválido"
                tvEmailError.visibility = View.VISIBLE
                lineEmail.setBackgroundColor(Color.parseColor("#B71C1C"))
                valid = false
            }

            if (password.isEmpty()) {
                tvLoginError.text = "Ingresa tu contraseña"
                tvLoginError.visibility = View.VISIBLE
                linePassword.setBackgroundColor(Color.parseColor("#B71C1C"))
                valid = false
            }

            if (!valid) return@setOnClickListener

            btnLogin.isEnabled = false
            btnLogin.text = "Ingresando..."

            FirebaseRepository.login(email, password) { result ->
                btnLogin.isEnabled = true
                btnLogin.text = "Iniciar sesión  →"

                result.onSuccess {
                    FirebaseRepository.getUser { user ->
                        val name = user?.fullName?.split(" ")?.firstOrNull() ?: email
                        Toast.makeText(applicationContext, "Bienvenido, $name", Toast.LENGTH_SHORT).show()
                    }
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }.onFailure { error ->
                    tvLoginError.text = error.message ?: "Correo o contraseña incorrectos"
                    tvLoginError.visibility = View.VISIBLE
                    lineEmail.setBackgroundColor(Color.parseColor("#B71C1C"))
                    linePassword.setBackgroundColor(Color.parseColor("#B71C1C"))
                }
            }
        }
    }
}
