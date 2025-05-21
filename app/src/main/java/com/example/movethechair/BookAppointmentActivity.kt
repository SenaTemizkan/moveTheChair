package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class BookAppointmentActivity : AppCompatActivity() {

    private lateinit var barberSpinner: Spinner
    private lateinit var serviceSpinner: Spinner
    private lateinit var staffSpinner: Spinner
    private lateinit var dateSpinner: Spinner
    private lateinit var timeSpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var staffLayout: LinearLayout

    private val barberIdMap = mutableMapOf<String, String>()
    private val staffIdMap = mutableMapOf<String, String>()
    private val availableDates = mutableListOf<String>()
    private val availableTimes = mutableListOf<String>()
    private val bookedTimes = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_appointment)

        barberSpinner = findViewById(R.id.spinnerBarbers)
        serviceSpinner = findViewById(R.id.spinnerServices)
        staffSpinner = findViewById(R.id.spinnerStaff)
        dateSpinner = findViewById(R.id.spinnerDateSelection)  // Değiştirildi: spinnerDates -> spinnerDateSelection
        timeSpinner = findViewById(R.id.spinnerTimeSelection)  // Değiştirildi: spinnerTimes -> spinnerTimeSelection
        saveButton = findViewById(R.id.buttonSaveAppointment)
        staffLayout = findViewById(R.id.layoutStaff)

        // 15 günlük tarihleri hazırla
        prepareDates()

        loadBarbers()

        barberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedBarber = barberSpinner.selectedItem.toString()
                loadServicesForBarber(selectedBarber)
                loadStaffForBarber(selectedBarber)
                loadBookedAppointments()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        dateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                loadBookedAppointments()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        staffSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                loadBookedAppointments()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        saveButton.setOnClickListener {
            saveAppointment()
        }
    }

    private fun prepareDates() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())

        for (i in 0..14) { // Bugünden itibaren 15 gün
            availableDates.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val dateAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableDates)
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dateSpinner.adapter = dateAdapter
    }

    private fun prepareTimeSlots() {
        availableTimes.clear()
        val times = listOf("09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00")

        val selectedDate = dateSpinner.selectedItem.toString()
        val today = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

        // Eğer bugün seçilmişse, geçmiş saatleri gösterme
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        times.forEach { time ->
            if (selectedDate != today || time.split(":")[0].toInt() > currentHour) {
                if (!bookedTimes.contains(time)) {
                    availableTimes.add(time)
                }
            }
        }

        val timeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableTimes)
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeSpinner.adapter = timeAdapter
    }

    private fun loadBarbers() {
        val barbersRef = FirebaseDatabase.getInstance().reference.child("barbers")

        barbersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val barberNames = mutableListOf<String>()
                snapshot.children.forEach { barber ->
                    val name = barber.child("name").getValue(String::class.java)
                    val id = barber.key
                    if (name != null && id != null) {
                        barberNames.add(name)
                        barberIdMap[name] = id
                    }
                }

                val adapter = ArrayAdapter(this@BookAppointmentActivity,
                    android.R.layout.simple_spinner_item, barberNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                barberSpinner.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BookAppointmentActivity,
                    "Kuaförler yüklenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadServicesForBarber(barberName: String) {
        val barberId = barberIdMap[barberName] ?: return
        val servicesRef = FirebaseDatabase.getInstance().reference
            .child("barbers").child(barberId).child("services")

        servicesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val serviceNames = mutableListOf<String>()
                snapshot.children.forEach { service ->
                    val serviceName = service.key
                    if (serviceName != null) {
                        serviceNames.add(serviceName)
                    }
                }

                val adapter = ArrayAdapter(this@BookAppointmentActivity,
                    android.R.layout.simple_spinner_item, serviceNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                serviceSpinner.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BookAppointmentActivity,
                    "Hizmetler yüklenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadStaffForBarber(barberName: String) {
        val barberId = barberIdMap[barberName] ?: return
        val staffRef = FirebaseDatabase.getInstance().reference
            .child("barbers").child(barberId).child("staff")

        staffRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val staffNames = mutableListOf<String>("Herhangi biri")
                staffIdMap.clear()

                snapshot.children.forEach { staff ->
                    val name = staff.child("name").getValue(String::class.java)
                    val id = staff.key
                    if (name != null && id != null) {
                        staffNames.add(name)
                        staffIdMap[name] = id
                    }
                }

                val adapter = ArrayAdapter(this@BookAppointmentActivity,
                    android.R.layout.simple_spinner_item, staffNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                staffSpinner.adapter = adapter

                // Eğer çalışan yoksa bu seçeneği gizle
                staffLayout.visibility = if (staffNames.size > 1) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BookAppointmentActivity,
                    "Çalışanlar yüklenemedi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadBookedAppointments() {
        val selectedBarber = barberSpinner.selectedItem?.toString() ?: return
        val barberId = barberIdMap[selectedBarber] ?: return
        val selectedDate = dateSpinner.selectedItem?.toString() ?: return
        val selectedStaff = if (staffSpinner.selectedItemPosition > 0)
            staffSpinner.selectedItem.toString() else null
        val staffId = if (selectedStaff != null) staffIdMap[selectedStaff] else null

        bookedTimes.clear()
        val appointmentsRef = FirebaseDatabase.getInstance().reference.child("appointments")

        appointmentsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { userSnapshot ->
                    userSnapshot.children.forEach { appointment ->
                        val apptBarberId = appointment.child("barberId").getValue(String::class.java)
                        val apptStaffId = appointment.child("staffId").getValue(String::class.java)
                        val apptDate = appointment.child("date").getValue(String::class.java)
                        val apptTime = appointment.child("time").getValue(String::class.java)

                        if (apptBarberId == barberId &&
                            (staffId == null || apptStaffId == staffId) &&
                            apptDate == selectedDate && apptTime != null) {
                            bookedTimes.add(apptTime.replace("_", ":"))
                        }
                    }
                }
                prepareTimeSlots()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BookAppointmentActivity,
                    "Randevu bilgileri alınamadı: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveAppointment() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val barberName = barberSpinner.selectedItem.toString()
        val barberId = barberIdMap[barberName] ?: return
        val serviceName = serviceSpinner.selectedItem.toString()
        val staffName = if (staffSpinner.selectedItemPosition > 0)
            staffSpinner.selectedItem.toString() else null
        val staffId = if (staffName != null) staffIdMap[staffName] else null
        val date = dateSpinner.selectedItem.toString()
        val time = timeSpinner.selectedItem?.toString()?.replace(":", "_") ?: run {
            Toast.makeText(this, "Lütfen saat seçin", Toast.LENGTH_SHORT).show()
            return
        }

        // Bugünden önceki tarihleri kontrol et
        val today = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        if (date < today) {
            Toast.makeText(this, "Geçmiş tarihe randevu alınamaz!", Toast.LENGTH_SHORT).show()
            return
        }

        // Bugün için geçmiş saatleri kontrol et
        if (date == today) {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val selectedHour = time.split("_")[0].toInt()
            if (selectedHour <= currentHour) {
                Toast.makeText(this, "Geçmiş saate randevu alınamaz!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val appointmentsRef = FirebaseDatabase.getInstance().reference.child("appointments")
        val appointmentData = hashMapOf(
            "barberId" to barberId,
            "barberName" to barberName,
            "service" to serviceName,
            "date" to date,
            "time" to time,
            "staffId" to staffId,
            "staffName" to staffName
        )

        appointmentsRef.child(userId).push().setValue(appointmentData)
            .addOnSuccessListener {
                Toast.makeText(this@BookAppointmentActivity,
                    "Randevu başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@BookAppointmentActivity, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this@BookAppointmentActivity,
                    "Hata oluştu: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}