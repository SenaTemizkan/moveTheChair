package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.editTextEmail)
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val loginButton = findViewById<Button>(R.id.buttonLogin)
        val registerButton = findViewById<Button>(R.id.buttonGoToRegister) // <- Yeni buton

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val user = auth.currentUser
                        val userId = user?.uid ?: return@addOnSuccessListener

                        val databaseRef = FirebaseDatabase.getInstance().reference
                        databaseRef.child("users").child(userId).get()
                            .addOnSuccessListener { snapshot ->
                                val role = snapshot.child("role").value.toString()

                                when (role) {
                                    "barber" -> {
                                        databaseRef.child("barbers").child(userId).get()
                                            .addOnSuccessListener { barberSnapshot ->
                                                val status = snapshot.child("approvalStatus").value.toString()
                                                if (status != "approved") {
                                                    Toast.makeText(this, "Hesabınız henüz onaylanmadı", Toast.LENGTH_SHORT).show()
                                                    auth.signOut()
                                                } else {
                                                    startActivity(Intent(this, BarberActivity::class.java))
                                                    finish()
                                                }
                                            }
                                    }
                                    "customer" -> {
                                        startActivity(Intent(this, CustomerActivity::class.java))
                                        finish()
                                    }
                                    "admin" -> {
                                        startActivity(Intent(this, AdminActivity::class.java))
                                        finish()
                                    }
                                    else -> {
                                        Toast.makeText(this, "Rol bilgisi hatalı", Toast.LENGTH_SHORT).show()
                                        auth.signOut()
                                    }
                                }
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Giriş başarısız: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Yeni hesap oluşturma butonu listener'ı
        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
