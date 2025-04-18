package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userNameTextView: TextView
    private lateinit var appointmentsTextView: TextView
    private lateinit var logoutButton: Button
    private lateinit var bookAppointmentButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Kullanıcı giriş yapılmadıysa login sayfasına yönlendir
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initializeViews()
        setupClickListeners()
        loadUserData()
        loadUserAppointments()
    }

    private fun initializeViews() {
        userNameTextView = findViewById(R.id.textViewUserName)
        appointmentsTextView = findViewById(R.id.textViewAppointments)
        logoutButton = findViewById(R.id.buttonLogout)
        bookAppointmentButton = findViewById(R.id.buttonBookAppointment)
    }

    private fun setupClickListeners() {
        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        bookAppointmentButton.setOnClickListener {
            val intent = Intent(this, BookAppointmentActivity::class.java)
            startActivity(intent)
        }

    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.reference.child("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    userNameTextView.text = "Hoş geldiniz, $name!"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Kullanıcı bilgileri alınamadı: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserAppointments() {
        val userId = auth.currentUser?.uid ?: return
        val appointmentsRef = database.reference.child("appointments").child(userId)

        appointmentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val appointments = StringBuilder()
                    appointments.append("Randevularınız:\n\n")

                    for (appointmentSnapshot in snapshot.children) {
                        val date = appointmentSnapshot.child("date").getValue(String::class.java)
                        val time = appointmentSnapshot.child("time").getValue(String::class.java)
                        val barberId = appointmentSnapshot.child("barberId").getValue(String::class.java)

                        // Berber bilgilerini al
                        getBarberInfo(barberId) { barberName ->
                            if (date != null && time != null) {
                                val formattedDate = date.replace("_", "/")
                                val formattedTime = time.replace("_", ":")
                                appointments.append("- $barberName: $formattedDate saat $formattedTime\n")
                            }

                            appointmentsTextView.text = appointments.toString()
                        }
                    }
                } else {
                    appointmentsTextView.text = "Henüz randevunuz bulunmamaktadır."
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Randevu bilgileri alınamadı: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getBarberInfo(barberId: String?, callback: (String) -> Unit) {
        if (barberId == null) {
            callback("Bilinmeyen Kuaför")
            return
        }

        val barberRef = database.reference.child("barbers").child(barberId)
        barberRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val barberName = snapshot.child("name").getValue(String::class.java) ?: "Bilinmeyen Kuaför"
                    callback(barberName)
                } else {
                    callback("Bilinmeyen Kuaför")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback("Bilinmeyen Kuaför")
            }
        })
    }
}