package com.example.movethechair

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class CustomerActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var userNameTextView: TextView
    private lateinit var bookAppointmentButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private var isShowingHomeContent = true
    private var currentUserName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initializeViews()
        setupClickListeners()
        setupBottomNav()
        loadUserData()
        loadUserAppointments()
    }

    private fun initializeViews() {
        userNameTextView = findViewById(R.id.textViewUserName)
        bookAppointmentButton = findViewById(R.id.buttonBookAppointment)
        bottomNavigationView = findViewById(R.id.bottomNavigation)
    }

    private fun setupClickListeners() {
        bookAppointmentButton.setOnClickListener {
            startActivity(Intent(this, BookAppointmentActivity::class.java))
        }
    }

    private fun setupBottomNav() {
        bottomNavigationView.setOnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showHomeContent()
                    true
                }
                R.id.nav_account -> {
                    showAccountFragment()
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_home
    }

    private fun showHomeContent() {
        if (!isShowingHomeContent) {
            supportFragmentManager.fragments.forEach {
                supportFragmentManager.beginTransaction().remove(it).commit()
            }

            findViewById<TextView>(R.id.textViewUserName).visibility = View.VISIBLE
            findViewById<TextView>(R.id.textViewAppointmentsTitle).visibility = View.VISIBLE
            findViewById<android.widget.ScrollView>(R.id.scrollView).visibility = View.VISIBLE
            bookAppointmentButton.visibility = View.VISIBLE

            loadUserAppointments()
            isShowingHomeContent = true
        }
    }

    private fun showAccountFragment() {
        findViewById<TextView>(R.id.textViewUserName).visibility = View.GONE
        findViewById<TextView>(R.id.textViewAppointmentsTitle).visibility = View.GONE
        findViewById<android.widget.ScrollView>(R.id.scrollView).visibility = View.GONE
        bookAppointmentButton.visibility = View.GONE

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, AccountFragment())
            .commit()

        isShowingHomeContent = false
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.child("name").getValue(String::class.java)?.let { name ->
                        currentUserName = name
                        // İsmi büyük harfle başlatıyoruz
                        val capitalizedName = capitalizeWords(name)
                        userNameTextView.text = "Hoş geldiniz, $capitalizedName"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Kullanıcı bilgileri alınamadı: ${error.message}")
                }
            })
    }

    private fun loadUserAppointments() {
        val userId = auth.currentUser?.uid ?: return
        val container = findViewById<LinearLayout>(R.id.appointmentsContainer)
        container.removeAllViews()

        database.reference.child("appointments").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    container.removeAllViews()
                    val now = Calendar.getInstance()
                    var hasAppointments = false

                    if (!snapshot.exists()) {
                        showNoAppointmentsMessage(container)
                        return
                    }

                    for (appointment in snapshot.children) {
                        val dateStr = appointment.child("date").getValue(String::class.java)
                        val timeStr = appointment.child("time").getValue(String::class.java)
                        val barberId = appointment.child("barberId").getValue(String::class.java)
                        val service = appointment.child("service").getValue(String::class.java)
                        val staffName = appointment.child("staffName").getValue(String::class.java)
                        val status = appointment.child("status").getValue(String::class.java) ?: "beklemede"

                        if (dateStr != null && timeStr != null) {
                            val appointmentDate = parseAppointmentDate(dateStr, timeStr)
                            if (appointmentDate != null && appointmentDate.after(now)) {
                                createAppointmentCard(
                                    container,
                                    barberId,
                                    service,
                                    staffName,
                                    dateStr,
                                    timeStr,
                                    status
                                )
                                hasAppointments = true
                            }
                        }
                    }

                    if (!hasAppointments) {
                        showNoAppointmentsMessage(container)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Randevu bilgileri alınamadı: ${error.message}")
                }
            })
    }

    private fun parseAppointmentDate(dateStr: String, timeStr: String): Calendar? {
        return try {
            val dateParts = dateStr.split("_")
            val timeParts = timeStr.split("_")
            if (dateParts.size == 3 && timeParts.size == 2) {
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, dateParts[0].toInt())
                    set(Calendar.MONTH, dateParts[1].toInt() - 1)
                    set(Calendar.YEAR, dateParts[2].toInt())
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                    set(Calendar.SECOND, 0)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createAppointmentCard(
        container: LinearLayout,
        barberId: String?,
        service: String?,
        staffName: String?,
        dateStr: String,
        timeStr: String,
        status: String
    ) {
        getBarberName(barberId) { barberName ->
            val cardView = LayoutInflater.from(this)
                .inflate(R.layout.appointment_card_new, container, false) as CardView

            val formattedDate = dateStr.replace("_", "/")
            val formattedTime = timeStr.replace("_", ":")

            // Kuaför adını büyük harfle başlatıyoruz
            val capitalizedBarberName = capitalizeWords(barberName)

            // Randevu detayları
            cardView.findViewById<TextView>(R.id.textViewBarberName).text = "Kuaför: $capitalizedBarberName"
            cardView.findViewById<TextView>(R.id.textViewService).text = buildServiceText(service, staffName)
            cardView.findViewById<TextView>(R.id.textViewDateTime).text = "Tarih ve Saat: $formattedDate - $formattedTime"
            cardView.findViewById<TextView>(R.id.textViewStatusDetail).apply {
                text = "Durum: ${getStatusText(status)}"
                setTextColor(getStatusColor(status))
            }

            container.addView(cardView)
        }
    }

    private fun getBarberName(barberId: String?, callback: (String) -> Unit) {
        if (barberId == null) {
            callback("Bilinmeyen Kuaför")
            return
        }

        database.reference.child("barbers").child(barberId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    callback(snapshot.child("name").getValue(String::class.java) ?: "Bilinmeyen Kuaför")
                }

                override fun onCancelled(error: DatabaseError) {
                    callback("Bilinmeyen Kuaför")
                }
            })
    }

    private fun buildServiceText(service: String?, staffName: String?): String {
        val serviceText = "Hizmet: ${service ?: "Belirtilmemiş"}"
        return if (!staffName.isNullOrEmpty()) {
            val capitalizedStaffName = capitalizeWords(staffName)
            "$serviceText Çalışan: $capitalizedStaffName"
        } else {
            serviceText
        }
    }

    private fun getStatusText(status: String): String {
        return when (status) {
            "beklemede" -> "Beklemede"
            "onaylandı" -> "Onaylandı"
            "iptal_edildi" -> "İptal Edildi"
            "tamamlandı" -> "Tamamlandı"
            else -> status
        }
    }

    private fun getStatusColor(status: String): Int {
        return when (status) {
            "onaylandı" -> ContextCompat.getColor(this, android.R.color.holo_green_dark)
            "iptal_edildi" -> ContextCompat.getColor(this, android.R.color.holo_red_dark)
            "tamamlandı" -> ContextCompat.getColor(this, android.R.color.holo_blue_dark)
            else -> ContextCompat.getColor(this, android.R.color.holo_orange_dark)
        }
    }

    // Kelimelerin ilk harfini büyük yapan yardımcı fonksiyon
    private fun capitalizeWords(text: String): String {
        return text.split(" ").joinToString(" ") { word ->
            if (word.isNotEmpty()) {
                word.lowercase().replaceFirstChar { it.uppercase() }
            } else {
                word
            }
        }
    }

    private fun showNoAppointmentsMessage(container: LinearLayout) {
        TextView(this).apply {
            text = "Henüz randevunuz bulunmamaktadır"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(32), 0, 0)
            container.addView(this)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}