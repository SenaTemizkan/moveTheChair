package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Calendar

class BarberActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var barberNameTextView: TextView
    private lateinit var appointmentsRecyclerView: RecyclerView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var tabLayout: TabLayout
    private lateinit var logoImageView: ImageView

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
        setupTabs()
        setupBottomNavigation()
        loadBarberData()
        loadBarberAppointments()
    }

    private fun initializeViews() {
        barberNameTextView = findViewById(R.id.textViewHeader)
        appointmentsRecyclerView = findViewById(R.id.recyclerViewAppointments)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        tabLayout = findViewById(R.id.tabLayout)
        logoImageView = findViewById(R.id.imageViewLogo)

        adapter = AppointmentAdapter(appointmentsList) { appointment, action ->
            when (action) {
                "onayla" -> onaylaRandevu(appointment)
                "iptal" -> iptalRandevuDialoguGoster(appointment)
                "tamamlandi" -> tamamlandiIsaretle(appointment)
            }
        }
        appointmentsRecyclerView.layoutManager = LinearLayoutManager(this)
        appointmentsRecyclerView.adapter = adapter
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Bekleyen Randevular"))
        tabLayout.addTab(tabLayout.newTab().setText("Onaylanan Randevular"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        isShowingRequests = true
                        loadBarberAppointments()
                    }
                    1 -> {
                        isShowingRequests = false
                        loadBarberAppointments()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_appointments -> true
                R.id.nav_services -> {
                    startActivity(Intent(this, HizmetYonetimActivity::class.java))
                    true
                }
                R.id.nav_staff -> {
                    startActivity(Intent(this, StaffActivity::class.java))
                    true
                }
                R.id.nav_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadBarberData() {
        val barberId = auth.currentUser?.uid ?: return
        val barberRef = database.reference.child("barbers").child(barberId)

        barberRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    barberNameTextView.text = name ?: "Kuaför Paneli"
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@BarberActivity,
                    "Kuaför bilgileri alınamadı",
                    Toast.LENGTH_SHORT
                ).show()
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
                            if ((isShowingRequests && status == "beklemede") ||
                                (!isShowingRequests && status == "onaylandı")) {

                                val formattedDate = dateStr.replace("_", "/")
                                val formattedTime = timeStr.replace("_", ":")

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

                if (tempAppointments.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(
                            this@BarberActivity,
                            if (isShowingRequests) "Bekleyen randevu bulunmuyor"
                            else "Onaylanmış randevu bulunmuyor",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    fetchCustomerNames(tempAppointments)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@BarberActivity,
                    "Randevular yüklenemedi",
                    Toast.LENGTH_SHORT
                ).show()
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
        val newAppointmentsList = mutableListOf<AppointmentItem>()

        for (appointment in tempAppointments) {
            database.reference.child("users").child(appointment.userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: "Misafir"

                        newAppointmentsList.add(
                            AppointmentItem(
                                id = appointment.id,
                                userId = appointment.userId,
                                customerName = name,
                                date = appointment.date,
                                time = appointment.time,
                                status = appointment.status,
                                service = appointment.service
                            )
                        )

                        if (newAppointmentsList.size == tempAppointments.size) {
                            runOnUiThread {
                                appointmentsList.clear()
                                appointmentsList.addAll(newAppointmentsList.sortedBy {
                                    it.date + it.time
                                })
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        // Hata durumunda isimsiz ekle
                        newAppointmentsList.add(
                            AppointmentItem(
                                id = appointment.id,
                                userId = appointment.userId,
                                customerName = "Misafir",
                                date = appointment.date,
                                time = appointment.time,
                                status = appointment.status,
                                service = appointment.service
                            )
                        )
                    }
                })
        }
    }

    private fun onaylaRandevu(appointment: AppointmentItem) {
        database.reference.child("appointments")
            .child(appointment.userId).child(appointment.id)
            .child("status").setValue("onaylandı")
            .addOnSuccessListener {
                Toast.makeText(this, "Randevu onaylandı", Toast.LENGTH_SHORT).show()
                loadBarberAppointments()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Onaylama başarısız", Toast.LENGTH_SHORT).show()
            }
    }

    private fun iptalRandevuDialoguGoster(appointment: AppointmentItem) {
        AlertDialog.Builder(this)
            .setTitle("Randevu İptali")
            .setMessage("${appointment.customerName} müşterisinin ${appointment.date} tarihli randevusunu iptal etmek istiyor musunuz?")
            .setPositiveButton("İptal Et") { _, _ ->
                database.reference.child("appointments")
                    .child(appointment.userId).child(appointment.id)
                    .child("status").setValue("iptal_edildi")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Randevu iptal edildi", Toast.LENGTH_SHORT).show()
                        loadBarberAppointments()
                    }
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun tamamlandiIsaretle(appointment: AppointmentItem) {
        database.reference.child("appointments")
            .child(appointment.userId).child(appointment.id)
            .child("status").setValue("tamamlandı")
            .addOnSuccessListener {
                Toast.makeText(this, "Randevu tamamlandı olarak işaretlendi", Toast.LENGTH_SHORT).show()
                loadBarberAppointments()
            }
    }
}

data class AppointmentItem(
    val id: String,
    val userId: String,
    val customerName: String,
    val date: String,
    val time: String,
    val service: String,
    val status: String
)