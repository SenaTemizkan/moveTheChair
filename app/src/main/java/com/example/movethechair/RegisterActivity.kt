package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Firebase başlatma
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // UI bileşenlerini ayarla
        nameEditText = findViewById(R.id.editTextName)
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        val registerButton = findViewById<Button>(R.id.buttonRegister)

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()

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

            registerUser(name, email, password)
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        // Kullanıcıyı Firebase Authentication'a kaydet
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                // Kullanıcı başarıyla kimlik doğrulama sistemine kaydedildi
                val user = authResult.user

                // Kullanıcıyı veritabanına ekle
                user?.let {
                    val userId = it.uid
                    val userRef = database.reference.child("users").child(userId)

                    val userData = HashMap<String, Any>()
                    userData["name"] = name
                    userData["email"] = email

                    userRef.setValue(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Veritabanı kaydı başarısız: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Kayıt başarısız: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}