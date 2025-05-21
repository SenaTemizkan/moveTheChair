package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            // User is not logged in, redirect to login screen
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // User is logged in, check user type and redirect accordingly
        checkUserTypeAndNavigate()
    }

    private fun checkUserTypeAndNavigate() {
        val userId = auth.currentUser?.uid ?: return

        val userRef = database.reference.child("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userType = snapshot.child("role").value.toString()

                    when (userType) {
                        "barber" -> {
                            // Redirect to barber activity
                            startActivity(Intent(this@MainActivity, BarberActivity::class.java))
                            finish()
                        }
                        "customer" -> {
                            // Redirect to customer activity
                            startActivity(Intent(this@MainActivity, CustomerActivity::class.java))
                            finish()
                        }
                        else -> {
                            // Handle unknown user type (fallback to login)
                            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                            finish()
                        }
                    }
                } else {
                    // User data not found, redirect to login
                    startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Error fetching data, redirect to login
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            }
        })
    }
}