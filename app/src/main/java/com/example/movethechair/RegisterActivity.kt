package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var roleRadioGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        nameEditText = findViewById(R.id.editTextName)
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        roleRadioGroup = findViewById(R.id.radioGroupRole)
        val registerButton = findViewById<Button>(R.id.buttonRegister)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            val selectedRoleId = roleRadioGroup.checkedRadioButtonId
            if (selectedRoleId == -1) {
                Toast.makeText(this, "Lütfen bir rol seçin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = when (selectedRoleId) {
                R.id.radioCustomer -> "customer"
                R.id.radioBarber -> "barber"
                else -> ""
            }

            if (name.isEmpty()) {
                nameEditText.error = "İsim gerekli"
                nameEditText.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                emailEditText.error = "E-posta gerekli"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                passwordEditText.error = "Şifre en az 6 karakter olmalı"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            registerUser(name, email, password, role)
        }
    }

    private fun registerUser(name: String, email: String, password: String, role: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user

                user?.let {
                    val userId = it.uid
                    val userRef = database.reference.child("users").child(userId)

                    val userData = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "role" to role
                    )

                    userRef.setValue(userData)
                        .addOnSuccessListener {
                            if (role == "barber") {

                                val barberRef = database.reference.child("barbers").child(userId)
                                val barberData = hashMapOf(
                                    "name" to name,
                                    "location" to "",
                                    "services" to listOf<String>(),
                                    "availableHours" to listOf<String>()
                                )
                                barberRef.setValue(barberData)
                            }

                            Toast.makeText(this, "Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Veritabanı hatası: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Kayıt başarısız: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

}
