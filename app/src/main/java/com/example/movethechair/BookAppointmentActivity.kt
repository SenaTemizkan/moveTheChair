package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import android.view.View
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
    private val barberIdMap = mutableMapOf<String, String>()
    private val hours = listOf("09:00", "10:00", "11:00", "13:00", "14:00", "15:00")

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

        barberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                setupTimeSlots()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        saveButton.setOnClickListener {
            saveAppointment()
        }

        datePicker.setOnDateChangedListenerManual()
    }

    private fun DatePicker.setOnDateChangedListenerManual() {
        val today = Calendar.getInstance()
        val selected = Calendar.getInstance().apply {
            set(datePicker.year, datePicker.month, datePicker.dayOfMonth)
        }

        if (selected.before(today)) {
            gridTimeSlots.removeAllViews()
            Toast.makeText(this@BookAppointmentActivity, "Geçmiş tarihlere randevu alınamaz!", Toast.LENGTH_SHORT).show()
        } else {
            setupTimeSlots()
        }
    }

    private fun loadBarbers() {
        val barbersRef = FirebaseDatabase.getInstance().reference.child("barbers")

        barbersRef.get().addOnSuccessListener { snapshot ->
            val barberNames = mutableListOf<String>()
            snapshot.children.forEach { barber ->
                val name = barber.child("name").getValue(String::class.java)
                val id = barber.key
                if (name != null && id != null) {
                    barberNames.add(name)
                    barberIdMap[name] = id
                }
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, barberNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            barberSpinner.adapter = adapter
        }.addOnFailureListener {
            Toast.makeText(this, "Kuaförler yüklenemedi: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTimeSlots() {
        gridTimeSlots.removeAllViews()

        val selectedName = barberSpinner.selectedItem?.toString() ?: return
        val barberId = barberIdMap[selectedName] ?: return
        val date = "${datePicker.dayOfMonth}_${datePicker.month + 1}_${datePicker.year}"

        val calendar = Calendar.getInstance()
        val selectedDate = Calendar.getInstance().apply {
            set(datePicker.year, datePicker.month, datePicker.dayOfMonth, 0, 0, 0)
        }

        if (selectedDate.before(calendar.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) })) {
            Toast.makeText(this, "Geçmiş tarihler için randevu alınamaz!", Toast.LENGTH_SHORT).show()
            return
        }

        val appointmentsRef = FirebaseDatabase.getInstance().reference.child("appointments")

        appointmentsRef.get().addOnSuccessListener { snapshot ->
            val bookedTimes = mutableSetOf<String>()

            snapshot.children.forEach { userSnapshot ->
                userSnapshot.children.forEach { appointment ->
                    val apptBarberId = appointment.child("barberId").getValue(String::class.java)
                    val apptDate = appointment.child("date").getValue(String::class.java)
                    val apptTime = appointment.child("time").getValue(String::class.java)

                    if (apptBarberId == barberId && apptDate == date && apptTime != null) {
                        bookedTimes.add(apptTime.replace("_", ":"))
                    }
                }
            }

            for (hour in hours) {
                val button = Button(this).apply {
                    text = hour
                    isEnabled = !bookedTimes.contains(hour)
                    alpha = if (isEnabled) 1.0f else 0.4f
                    setOnClickListener {
                        if (isEnabled) {
                            selectedTime = hour
                            highlightSelected(this)
                        }
                    }
                }
                gridTimeSlots.addView(button)
            }
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
        val selectedName = barberSpinner.selectedItem.toString()
        val barberId = barberIdMap[selectedName] ?: return

        val calendar = Calendar.getInstance()
        val selectedDate = Calendar.getInstance().apply {
            set(datePicker.year, datePicker.month, datePicker.dayOfMonth, 0, 0, 0)
        }

        if (selectedDate.before(calendar.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) })) {
            Toast.makeText(this, "Geçmiş tarihe randevu alınamaz!", Toast.LENGTH_SHORT).show()
            return
        }

        val date = "${datePicker.dayOfMonth}_${datePicker.month + 1}_${datePicker.year}"
        val time = selectedTime?.replace(":", "_") ?: run {
            Toast.makeText(this, "Lütfen saat seçin", Toast.LENGTH_SHORT).show()
            return
        }

        val appointmentsRef = FirebaseDatabase.getInstance().reference.child("appointments")

        appointmentsRef.get().addOnSuccessListener { snapshot ->
            var isSlotTaken = false

            snapshot.children.forEach { userSnapshot ->
                userSnapshot.children.forEach { appointment ->
                    val apptBarberId = appointment.child("barberId").getValue(String::class.java)
                    val apptDate = appointment.child("date").getValue(String::class.java)
                    val apptTime = appointment.child("time").getValue(String::class.java)

                    if (apptBarberId == barberId && apptDate == date && apptTime == time) {
                        isSlotTaken = true
                    }
                }
            }

            if (isSlotTaken) {
                Toast.makeText(this, "Bu saat ve kuaförde zaten bir randevu var!", Toast.LENGTH_LONG).show()
            } else {
                val appointmentData = mapOf(
                    "barberId" to barberId,
                    "date" to date,
                    "time" to time,
                    "service" to selectedService
                )

                appointmentsRef.child(userId).push().setValue(appointmentData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Randevu başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Hata oluştu: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
//github deneme
}