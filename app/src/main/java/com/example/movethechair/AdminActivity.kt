package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        if (userId == null) {
            finishWithError("Kullanıcı oturumu bulunamadı")
            return
        }

        bottomNavigation = findViewById(R.id.bottomNavigation)
        database = FirebaseDatabase.getInstance().getReference("users").child(userId)

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val role = snapshot.child("role").getValue(String::class.java)
                Log.d("AdminActivity", "Kullanıcı rolü: $role")

                if (role == "admin") {
                    setupBottomNavigation()
                    // Varsayılan olarak pending fragment'ı göster
                    loadFragment(PendingFragment())
                } else {
                    finishWithError("Bu alana sadece admin erişebilir.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminActivity", "Veritabanı hatası: ${error.message}")
                finishWithError("Veritabanı hatası: ${error.message}")
            }
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_pending -> {
                    loadFragment(PendingFragment())
                    true
                }
                R.id.nav_approved -> {
                    loadFragment(ApprovedFragment())
                    true
                }
                R.id.nav_logout -> {
                    performLogout()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun performLogout() {
        auth.signOut()
        Toast.makeText(this, "Çıkış yapıldı", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun finishWithError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e("AdminActivity", message)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}