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
import java.util.Calendar


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
                val now = Calendar.getInstance()
                val appointments = StringBuilder()
                var hasFutureAppointments = false

                for (appointmentSnapshot in snapshot.children) {
                    val dateStr = appointmentSnapshot.child("date").getValue(String::class.java)
                    val timeStr = appointmentSnapshot.child("time").getValue(String::class.java)
                    val barberId = appointmentSnapshot.child("barberId").getValue(String::class.java)
                    val status = appointmentSnapshot.child("status").getValue(String::class.java) ?: "beklemede"

                    if (dateStr != null && timeStr != null) {
                        val dateParts = dateStr.split("_")
                        val timeParts = timeStr.split("_")
                        if (dateParts.size == 3 && timeParts.size == 2) {
                            val appointmentDate = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, dateParts[0].toInt())
                                set(Calendar.MONTH, dateParts[1].toInt() - 1)
                                set(Calendar.YEAR, dateParts[2].toInt())
                                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                                set(Calendar.MINUTE, timeParts[1].toInt())
                                set(Calendar.SECOND, 0)
                            }

                            if (appointmentDate.before(now)) {

                                if (status == "tamamlandı" || now.timeInMillis - appointmentDate.timeInMillis > 24 * 60 * 60 * 1000) {
                                    appointmentSnapshot.ref.removeValue()
                                } else {

                                    getBarberInfo(barberId) { barberName ->
                                        val formattedDate = dateStr.replace("_", "/")
                                        val formattedTime = timeStr.replace("_", ":")
                                        val statusText = getStatusText(status)

                                        appointments.append("- $barberName: $formattedDate saat $formattedTime - $statusText\n")
                                        appointmentsTextView.text = appointments.toString()
                                    }
                                    hasFutureAppointments = true
                                }
                            } else {
                                getBarberInfo(barberId) { barberName ->
                                    val formattedDate = dateStr.replace("_", "/")
                                    val formattedTime = timeStr.replace("_", ":")
                                    val statusText = getStatusText(status)

                                    appointments.append("- $barberName: $formattedDate saat $formattedTime - $statusText\n")
                                    appointmentsTextView.text = appointments.toString()
                                }
                                hasFutureAppointments = true
                            }
                        }
                    }
                }

                if (!hasFutureAppointments) {
                    appointmentsTextView.text = "Henüz gelecekte bir randevunuz bulunmamaktadır."
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Randevu bilgileri alınamadı: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getStatusText(status: String): String {
        return when (status) {
            "beklemede" -> "Beklemede"
            "onaylandı" -> "✓ Onaylandı"
            "iptal_edildi" -> "✗ İptal Edildi"
            "tamamlandı" -> "✓ Tamamlandı"
            else -> status
        }
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