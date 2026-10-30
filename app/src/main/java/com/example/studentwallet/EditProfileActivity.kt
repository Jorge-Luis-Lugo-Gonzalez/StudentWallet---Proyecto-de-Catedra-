package com.example.studentwallet

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EditProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val btnBack = findViewById<ImageButton>(R.id.btnBackEditProfile)
        val flChangePhoto = findViewById<FrameLayout>(R.id.flChangePhoto)
        val etName = findViewById<EditText>(R.id.etEditName)
        val etCareer = findViewById<EditText>(R.id.etEditCareer)
        val etUniversity = findViewById<EditText>(R.id.etEditUniversity)
        val spCurrency = findViewById<Spinner>(R.id.spEditCurrency)
        val tvError = findViewById<TextView>(R.id.tvEditProfileError)
        val btnSave = findViewById<Button>(R.id.btnSaveProfile)

        val currencies = listOf(
            "USD ($)",
            "MXN - Peso Mexicano",
            "EUR (€)",
            "SVC - Dólar / Colón"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCurrency.adapter = adapter

        FirebaseRepository.getUser { currentUser ->
            if (currentUser == null || isFinishing) return@getUser
            etName.setText(currentUser.fullName)
            etCareer.setText(currentUser.career)
            etUniversity.setText(currentUser.university)
            val idx = currencies.indexOfFirst { it.startsWith(currentUser.currency.take(3)) }
            if (idx >= 0) {
                spCurrency.setSelection(idx)
            }
        }

        btnBack.setOnClickListener { finish() }

        flChangePhoto.setOnClickListener {
            Toast.makeText(this, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val career = etCareer.text.toString().trim()
            val university = etUniversity.text.toString().trim()
            val currency = spCurrency.selectedItem.toString()

            if (name.isEmpty() || career.isEmpty() || university.isEmpty()) {
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            tvError.visibility = View.GONE

            btnSave.isEnabled = false
            FirebaseRepository.updateUserProfile(name, career, university, currency) { result ->
                btnSave.isEnabled = true
                result.onSuccess {
                    Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                }.onFailure { error ->
                    Toast.makeText(this, error.message ?: "No se pudo actualizar el perfil", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
