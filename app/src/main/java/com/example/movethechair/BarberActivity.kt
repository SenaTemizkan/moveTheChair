package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*
import kotlin.collections.ArrayList

class BarberActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var barberNameTextView: TextView
    private lateinit var appointmentsRecyclerView: RecyclerView
    private lateinit var logoutButton: Button
    private lateinit var confirmedButton: Button
    private lateinit var appointmentsTitleTextView: TextView
    private lateinit var adapter: AppointmentAdapter
    private val appointmentsList = mutableListOf<AppointmentItem>()
    private var isShowingRequests = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barber)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initializeViews()
        setupClickListeners()
        loadBarberData()
        loadBarberAppointments()
    }

    private fun initializeViews() {
        barberNameTextView = findViewById(R.id.textViewBarberName)
        appointmentsRecyclerView = findViewById(R.id.recyclerViewAppointments)
        logoutButton = findViewById(R.id.buttonLogout)
        confirmedButton = findViewById(R.id.buttonShowConfirmed)
        appointmentsTitleTextView = findViewById(R.id.textViewAppointmentsTitle)

        appointmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AppointmentAdapter(appointmentsList) { appointment, action ->
            handleAppointmentAction(appointment, action)
        }
        appointmentsRecyclerView.adapter = adapter

        appointmentsTitleTextView.text = "Randevu Talepleri"
    }

    private fun setupClickListeners() {
        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        confirmedButton.setOnClickListener {
            isShowingRequests = !isShowingRequests
            updateTitle()
            loadBarberAppointments()
        }
    }

    private fun updateTitle() {
        if (isShowingRequests) {
            appointmentsTitleTextView.text = "Randevu Talepleri"
            confirmedButton.text = "Onaylanmış Randevular"
        } else {
            appointmentsTitleTextView.text = "Onaylanmış Randevular"
            confirmedButton.text = "Randevu Talepleri"
        }
    }

    private fun loadBarberData() {
        val barberId = auth.currentUser?.uid ?: return
        val barberRef = database.reference.child("barbers").child(barberId)

        barberRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    barberNameTextView.text = "Hoş geldiniz, $name!"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BarberActivity, "Kuaför bilgileri alınamadı: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadBarberAppointments() {
        val barberId = auth.currentUser?.uid ?: return
        val appointmentsRef = database.reference.child("appointments")

        appointmentsList.clear()
        adapter.notifyDataSetChanged()

        appointmentsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempAppointments = ArrayList<AppointmentWithoutName>()
                val now = Calendar.getInstance()

                for (userSnapshot in snapshot.children) {
                    val userId = userSnapshot.key ?: continue

                    for (appointmentSnapshot in userSnapshot.children) {
                        val appointmentId = appointmentSnapshot.key ?: continue
                        val appointmentBarberId = appointmentSnapshot.child("barberId").getValue(String::class.java)
                        val dateStr = appointmentSnapshot.child("date").getValue(String::class.java)
                        val timeStr = appointmentSnapshot.child("time").getValue(String::class.java)
                        val service = appointmentSnapshot.child("service").getValue(String::class.java) ?: "Saç Kesim"
                        val status = appointmentSnapshot.child("status").getValue(String::class.java) ?: "beklemede"

                        if (appointmentBarberId == barberId && dateStr != null && timeStr != null) {
                            val isMatchingStatus = if (isShowingRequests) {
                                status == "beklemede"
                            } else {
                                status == "onaylandı"
                            }

                            if (isMatchingStatus) {
                                val formattedDate = dateStr.replace("_", "/")
                                val formattedTime = timeStr.replace("_", ":")

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

                                    if (appointmentDate.after(now) || now.timeInMillis - appointmentDate.timeInMillis < 3600000) {
                                        tempAppointments.add(
                                            AppointmentWithoutName(
                                                id = appointmentId,
                                                userId = userId,
                                                date = formattedDate,
                                                time = formattedTime,
                                                status = status,
                                                service = service
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                tempAppointments.sortBy {
                    val parts = it.date.split("/")
                    val timeParts = it.time.split(":")
                    val cal = Calendar.getInstance()
                    cal.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt(),
                        timeParts[0].toInt(), timeParts[1].toInt())
                    cal.timeInMillis
                }

                if (tempAppointments.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(
                            this@BarberActivity,
                            if (isShowingRequests) "Bekleyen randevu talebi bulunmuyor" else "Onaylanmış randevu bulunmuyor",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    fetchCustomerNames(tempAppointments)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BarberActivity, "Randevu bilgileri alınamadı: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private data class AppointmentWithoutName(
        val id: String,
        val userId: String,
        val date: String,
        val time: String,
        val service: String,
        val status: String
    )

    private fun fetchCustomerNames(tempAppointments: List<AppointmentWithoutName>) {
        val processedCount = intArrayOf(0)
        val totalCount = tempAppointments.size

        for (appointment in tempAppointments) {
            getUserName(appointment.userId) { userName ->
                val appointmentItem = AppointmentItem(
                    id = appointment.id,
                    userId = appointment.userId,
                    customerName = userName,
                    date = appointment.date,
                    time = appointment.time,
                    status = appointment.status,
                    service = appointment.service
                )

                appointmentsList.add(appointmentItem)
                processedCount[0]++

                if (processedCount[0] == totalCount) {
                    runOnUiThread {
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun getUserName(userId: String, callback: (String) -> Unit) {
        val userRef = database.reference.child("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = if (snapshot.exists()) {
                    snapshot.child("name").getValue(String::class.java) ?: "İsimsiz Müşteri"
                } else {
                    "İsimsiz Müşteri"
                }
                callback(name)
            }

            override fun onCancelled(error: DatabaseError) {
                callback("İsimsiz Müşteri")
            }
        })
    }

    private fun handleAppointmentAction(appointment: AppointmentItem, action: String) {
        when (action) {
            "confirm" -> {
                updateAppointmentStatus(appointment.userId, appointment.id, "onaylandı")
            }
            "cancel" -> {
                showCancelConfirmationDialog(appointment)
            }
            "complete" -> {
                updateAppointmentStatus(appointment.userId, appointment.id, "tamamlandı")
            }
        }
    }

    private fun showCancelConfirmationDialog(appointment: AppointmentItem) {
        AlertDialog.Builder(this)
            .setTitle("Randevu İptali")
            .setMessage("${appointment.customerName} için ${appointment.date} tarihli ve ${appointment.time} saatindeki randevuyu iptal etmek istediğinize emin misiniz?")
            .setPositiveButton("Evet, İptal Et") { _, _ ->
                updateAppointmentStatus(appointment.userId, appointment.id, "iptal_edildi")
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun updateAppointmentStatus(userId: String, appointmentId: String, status: String) {
        val appointmentRef = database.reference.child("appointments").child(userId).child(appointmentId)

        appointmentRef.child("status").setValue(status)
            .addOnSuccessListener {
                val statusMessage = when (status) {
                    "onaylandı" -> "Randevu onaylandı"
                    "iptal_edildi" -> "Randevu iptal edildi"
                    "tamamlandı" -> "Randevu tamamlandı olarak işaretlendi"
                    else -> "Randevu durumu güncellendi"
                }
                Toast.makeText(this, statusMessage, Toast.LENGTH_SHORT).show()
                loadBarberAppointments()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Durum güncellenirken hata oluştu: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

data class Appointment(
    val barberId: String = "",
    val date: String = "",
    val time: String = "",
    val service: String = "",
    val status: String = "beklemede"
)

data class AppointmentItem(
    val id: String,
    val userId: String,
    val customerName: String,
    val date: String,
    val time: String,
    val service: String,
    val status: String
)