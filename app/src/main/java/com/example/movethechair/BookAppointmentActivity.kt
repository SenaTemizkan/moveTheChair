package com.example.movethechair

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class BookAppointmentActivity : AppCompatActivity() {

    private lateinit var barberSpinner: Spinner
    private lateinit var datePicker: DatePicker
    private lateinit var gridTimeSlots: GridLayout
    private lateinit var saveButton: Button
    private var selectedTime: String? = null
    private var selectedService: String = "Saç Kesim"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_appointment)

        barberSpinner = findViewById(R.id.spinnerBarbers)
        datePicker = findViewById(R.id.datePicker)
        gridTimeSlots = findViewById(R.id.gridTimeSlots)
        saveButton = findViewById(R.id.buttonSaveAppointment)

        findViewById<Button>(R.id.buttonHaircut).setOnClickListener {
            selectedService = "Saç Kesim"
        }
        findViewById<Button>(R.id.buttonColor).setOnClickListener {
            selectedService = "Saç Boyama"
        }

        loadBarbers()
        setupTimeSlots()
        saveButton.setOnClickListener { saveAppointment() }
    }

    private fun loadBarbers() {
        val barbers = listOf("Ali", "Ayşe", "Mehmet") // Firebase’den de çekilebilir
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, barbers)
        barberSpinner.adapter = adapter
    }

    private fun setupTimeSlots() {
        val hours = listOf("09:00", "10:00", "11:00", "13:00", "14:00", "15:00")
        for (hour in hours) {
            val button = Button(this).apply {
                text = hour
                setOnClickListener {
                    selectedTime = hour
                    highlightSelected(this)
                }
            }
            gridTimeSlots.addView(button)
        }
    }

    private fun highlightSelected(selectedBtn: Button) {
        for (i in 0 until gridTimeSlots.childCount) {
            val btn = gridTimeSlots.getChildAt(i) as Button
            btn.setBackgroundColor(getColor(android.R.color.darker_gray))
        }
        selectedBtn.setBackgroundColor(getColor(android.R.color.holo_green_light))
    }

    private fun saveAppointment() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val barberName = barberSpinner.selectedItem.toString()
        val date = "${datePicker.dayOfMonth}_${datePicker.month + 1}_${datePicker.year}"
        val time = selectedTime?.replace(":", "_") ?: run {
            Toast.makeText(this, "Lütfen saat seçin", Toast.LENGTH_SHORT).show()
            return
        }

        val appointmentData = mapOf(
            "barberId" to barberName,
            "date" to date,
            "time" to time,
            "service" to selectedService
        )

        FirebaseDatabase.getInstance().reference
            .child("appointments")
            .child(userId)
            .push()
            .setValue(appointmentData)
            .addOnSuccessListener {
                Toast.makeText(this, "Randevu alındı!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Hata: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
