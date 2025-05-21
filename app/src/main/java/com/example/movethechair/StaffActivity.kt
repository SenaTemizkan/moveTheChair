package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class StaffActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var staffRecyclerView: RecyclerView
    private lateinit var addStaffButton: Button
    private lateinit var backButton: Button
    private lateinit var adapter: StaffAdapter
    private val staffList = mutableListOf<Staff>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initializeViews()
        setupClickListeners()
        loadStaffData()
    }

    private fun initializeViews() {
        staffRecyclerView = findViewById(R.id.recyclerViewStaff)
        addStaffButton = findViewById(R.id.buttonAddStaff)
        backButton = findViewById(R.id.buttonBack)

        staffRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StaffAdapter(staffList) { staff, action ->
            when (action) {
                "edit" -> showEditStaffDialog(staff)
                "delete" -> showDeleteConfirmationDialog(staff)
            }
        }
        staffRecyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        addStaffButton.setOnClickListener {
            showAddStaffDialog()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadStaffData() {
        val barberId = auth.currentUser?.uid ?: return
        val staffRef = database.reference.child("barbers").child(barberId).child("staff")

        staffList.clear()
        staffRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (staffSnapshot in snapshot.children) {
                    val staff = staffSnapshot.getValue(Staff::class.java)
                    staff?.let {
                        it.id = staffSnapshot.key ?: ""
                        staffList.add(it)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@StaffActivity, "Çalışan bilgileri alınamadı: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddStaffDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_staff, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Yeni Çalışan Ekle")
            .setView(dialogView)
            .setPositiveButton("Ekle") { _, _ ->

            }
            .setNegativeButton("İptal", null)
            .create()

        dialog.show()


        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = dialogView.findViewById<android.widget.EditText>(R.id.editTextStaffName).text.toString()
            val specialties = dialogView.findViewById<android.widget.EditText>(R.id.editTextSpecialties).text.toString()

            if (name.isBlank()) {
                Toast.makeText(this, "Çalışan adı boş olamaz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (specialties.isBlank()) {
                Toast.makeText(this, "Uzmanlık alanları boş olamaz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val specialtiesList = specialties.split(",").map { it.trim() }

            val newStaff = Staff(
                name = name,
                specialties = specialtiesList,
                barberId = auth.currentUser?.uid ?: ""
            )

            addStaffToDatabase(newStaff)
            dialog.dismiss()
        }
    }

    private fun showEditStaffDialog(staff: Staff) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_staff, null)
        dialogView.findViewById<android.widget.EditText>(R.id.editTextStaffName).setText(staff.name)
        dialogView.findViewById<android.widget.EditText>(R.id.editTextSpecialties).setText(staff.specialties.joinToString(", "))

        val dialog = AlertDialog.Builder(this)
            .setTitle("Çalışanı Düzenle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                // Burada güncelleme işlemi StaffAdapter içinde yapılacak
            }
            .setNegativeButton("İptal", null)
            .create()

        dialog.show()


        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = dialogView.findViewById<android.widget.EditText>(R.id.editTextStaffName).text.toString()
            val specialties = dialogView.findViewById<android.widget.EditText>(R.id.editTextSpecialties).text.toString()

            if (name.isBlank()) {
                Toast.makeText(this, "Çalışan adı boş olamaz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (specialties.isBlank()) {
                Toast.makeText(this, "Uzmanlık alanları boş olamaz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val specialtiesList = specialties.split(",").map { it.trim() }

            val updatedStaff = staff.copy(
                name = name,
                specialties = specialtiesList
            )

            updateStaffInDatabase(updatedStaff)
            dialog.dismiss()
        }
    }

    private fun showDeleteConfirmationDialog(staff: Staff) {
        AlertDialog.Builder(this)
            .setTitle("Çalışan Silme")
            .setMessage("${staff.name} isimli çalışanı silmek istediğinize emin misiniz?")
            .setPositiveButton("Evet, Sil") { _, _ ->
                deleteStaffFromDatabase(staff)
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun addStaffToDatabase(staff: Staff) {
        val barberId = auth.currentUser?.uid ?: return
        val staffRef = database.reference.child("barbers").child(barberId).child("staff").push()

        staffRef.setValue(staff)
            .addOnSuccessListener {
                Toast.makeText(this, "Çalışan başarıyla eklendi", Toast.LENGTH_SHORT).show()
                loadStaffData()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Çalışan eklenirken hata oluştu: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateStaffInDatabase(staff: Staff) {
        val barberId = auth.currentUser?.uid ?: return
        val staffRef = database.reference.child("barbers").child(barberId).child("staff").child(staff.id)

        staffRef.setValue(staff)
            .addOnSuccessListener {
                Toast.makeText(this, "Çalışan bilgileri güncellendi", Toast.LENGTH_SHORT).show()
                loadStaffData()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Güncelleme sırasında hata oluştu: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteStaffFromDatabase(staff: Staff) {
        val barberId = auth.currentUser?.uid ?: return
        val staffRef = database.reference.child("barbers").child(barberId).child("staff").child(staff.id)

        staffRef.removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Çalışan başarıyla silindi", Toast.LENGTH_SHORT).show()
                loadStaffData()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Silme işlemi sırasında hata oluştu: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

data class Staff(
    var id: String = "",
    val name: String = "",
    val specialties: List<String> = listOf(),
    val barberId: String = "",
    val workingHours: Map<String, List<String>> = mapOf()
)