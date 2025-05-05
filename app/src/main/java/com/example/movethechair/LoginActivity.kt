package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        if (auth.currentUser != null) {
            checkUserTypeAndNavigate()
            return
        }

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.buttonLogin).setOnClickListener {
            handleLogin()
        }

        findViewById<Button>(R.id.buttonGoToRegister).setOnClickListener {
            navigateToRegister()
        }
    }

    private fun handleLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        when {
            email.isEmpty() -> {
                emailEditText.error = "E-posta adresi gerekli"
                emailEditText.requestFocus()
            }
            password.isEmpty() -> {
                passwordEditText.error = "Şifre gerekli"
                passwordEditText.requestFocus()
            }
            else -> authenticateUser(email, password)
        }
    }

    private fun authenticateUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    checkUserTypeAndNavigate()
                } else {
                    handleLoginError(task.exception)
                }
            }
    }

    private fun checkUserTypeAndNavigate() {
        val userId = auth.currentUser?.uid ?: return


        val userRef = database.reference.child("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userType = snapshot.child("role").value.toString()
                    if (userType == "barber") {
                        navigateToBarberPanel()
                        Toast.makeText(this@LoginActivity, "Kuaför olarak giriş yapıldı", Toast.LENGTH_SHORT).show()
                    } else if (userType == "customer") {
                        navigateToMain()
                        Toast.makeText(this@LoginActivity, "Müşteri olarak giriş yapıldı", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@LoginActivity, "Bilinmeyen kullanıcı türü: $userType", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Kullanıcı bilgileri veritabanında bulunamadı", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginActivity, "Veri okuma hatası: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun navigateToMain() {
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
            finish()
        }
    }

    private fun navigateToBarberPanel() {
        Intent(this, BarberActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
            finish()
        }
    }

    private fun navigateToRegister() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }

    private fun handleLoginError(exception: Exception?) {
        val errorMessage = when (exception) {
            is FirebaseAuthInvalidUserException -> "Bu e-posta ile kayıtlı kullanıcı bulunamadı"
            is FirebaseAuthInvalidCredentialsException -> "Geçersiz e-posta veya şifre"
            else -> "Giriş başarısız: ${exception?.message ?: "Bilinmeyen hata"}"
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }
}
