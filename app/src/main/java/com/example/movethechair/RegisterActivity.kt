package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var roleRadioGroup: RadioGroup

    // Barber-specific fields
    private lateinit var birthDateEditText: EditText
    private lateinit var birthPlaceEditText: EditText
    private lateinit var tcEditText: EditText
    private lateinit var businessNameEditText: EditText
    private lateinit var businessAddressEditText: EditText
    private lateinit var mersisNoEditText: EditText
    private lateinit var taxNumberEditText: EditText
    private lateinit var phone1EditText: EditText
    private lateinit var phone2EditText: EditText
    private lateinit var ibanEditText: EditText
    private lateinit var barberFieldsLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        nameEditText = findViewById(R.id.editTextName)
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        roleRadioGroup = findViewById(R.id.radioGroupRole)

        // Barber fields
        birthDateEditText = findViewById(R.id.editTextBirthDate)
        birthPlaceEditText = findViewById(R.id.editTextBirthPlace)
        tcEditText = findViewById(R.id.editTextTC)
        businessNameEditText = findViewById(R.id.editTextBusinessName)
        businessAddressEditText = findViewById(R.id.editTextBusinessAddress)
        mersisNoEditText = findViewById(R.id.editTextMersisNo)
        taxNumberEditText = findViewById(R.id.editTextTaxNumber)
        phone1EditText = findViewById(R.id.editTextPhone1)
        phone2EditText = findViewById(R.id.editTextPhone2)
        ibanEditText = findViewById(R.id.editTextIBAN)
        barberFieldsLayout = findViewById(R.id.layoutBarberFields)

        roleRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioBarber) {
                barberFieldsLayout.visibility = View.VISIBLE
            } else {
                barberFieldsLayout.visibility = View.GONE
            }
        }

        findViewById<Button>(R.id.buttonRegister).setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val selectedRoleId = roleRadioGroup.checkedRadioButtonId

            if (selectedRoleId == -1) {
                Toast.makeText(this, "Lütfen bir rol seçin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = when (selectedRoleId) {
                R.id.radioCustomer -> "customer"
                R.id.radioBarber -> "barber"
                else -> ""
            }

            if (name.isEmpty() || email.isEmpty() || password.length < 6) {
                Toast.makeText(this, "Lütfen tüm bilgileri doğru girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (role == "barber") {
                val requiredFields = listOf(
                    birthDateEditText, birthPlaceEditText, tcEditText,
                    businessNameEditText, businessAddressEditText,
                    mersisNoEditText, taxNumberEditText,
                    phone1EditText, ibanEditText
                )

                for (field in requiredFields) {
                    if (field.text.toString().trim().isEmpty()) {
                        field.error = "Bu alan gerekli"
                        field.requestFocus()
                        return@setOnClickListener
                    }
                }
            }

            registerUser(name, email, password, role)
        }
    }

    private fun registerUser(name: String, email: String, password: String, role: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener { authResult ->
            val user = authResult.user
            user?.let {
                val userId = it.uid
                val userRef = database.reference.child("users").child(userId)

                // Kullanıcı temel bilgileri
                val userData = mutableMapOf<String, Any>(
                    "name" to name,
                    "email" to email,
                    "role" to role
                )

                // Eğer berberse detayları ekle + approvalStatus = "pending"
                if (role == "barber") {
                    userData.putAll(
                        mapOf(
                            "birthDate" to birthDateEditText.text.toString().trim(),
                            "birthPlace" to birthPlaceEditText.text.toString().trim(),
                            "tc" to tcEditText.text.toString().trim(),
                            "workplaceName" to businessNameEditText.text.toString().trim(),
                            "workplaceAddress" to businessAddressEditText.text.toString().trim(),
                            "mersisNo" to mersisNoEditText.text.toString().trim(),
                            "taxNumber" to taxNumberEditText.text.toString().trim(),
                            "phone1" to phone1EditText.text.toString().trim(),
                            "phone2" to phone2EditText.text.toString().trim(),
                            "iban" to ibanEditText.text.toString().trim(),
                            "approvalStatus" to "pending" // 👈 burada işte
                        )
                    )
                }

                userRef.setValue(userData).addOnSuccessListener {
                    Toast.makeText(this, "Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }.addOnFailureListener {
                    Toast.makeText(this, "Veritabanı hatası: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Kayıt başarısız: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}
