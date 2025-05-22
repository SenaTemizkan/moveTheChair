package com.example.movethechair

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AddressActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var spinnerIl: Spinner
    private lateinit var spinnerIlce: Spinner
    private lateinit var editTextAdres: EditText
    private lateinit var buttonKaydet: Button
    private lateinit var buttonGuncelle: Button
    private lateinit var textViewMevcutAdres: TextView

    private val ilList = arrayOf("İl Seçin", "Rize", "Samsun")
    private val rizeIlceler = arrayOf("İlçe Seçin", "Merkez", "Ardeşen", "Çayeli", "Fındıklı", "Güneysu", "Hemşin", "İkizdere", "İyidere", "Kalkandere", "Pazar", "Derepazarı")
    private val samsunIlceler = arrayOf("İlçe Seçin", "İlkadım", "Canik", "Tekkeköy", "Atakum", "Bafra", "Çarşamba", "Terme", "Kavak", "Ladik", "Havza", "Vezirköprü", "Alaçam", "Yakakent", "19 Mayıs", "Ayvacık", "Ondokuzmayıs")

    private var selectedIl: String = ""
    private var selectedIlce: String = ""
    private var hasAddress: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            FirebaseApp.initializeApp(this)
            setContentView(R.layout.activity_address)

            auth = FirebaseAuth.getInstance()
            database = FirebaseDatabase.getInstance()

            initializeViews()
            setupSpinners()
            setupClickListeners()
            checkExistingAddress()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Başlatma hatası: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeViews() {
        spinnerIl = findViewById(R.id.spinnerIl)
        spinnerIlce = findViewById(R.id.spinnerIlce)
        editTextAdres = findViewById(R.id.editTextAdres)
        buttonKaydet = findViewById(R.id.buttonKaydet)
        buttonGuncelle = findViewById(R.id.buttonGuncelle)
        textViewMevcutAdres = findViewById(R.id.textViewMevcutAdres)
    }

    private fun setupSpinners() {
        try {
            // İl Spinner Setup
            val ilAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ilList)
            ilAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerIl.adapter = ilAdapter

            // İlçe Spinner için başlangıç değerleri
            val initialIlceList = arrayOf("İlçe Seçin")
            val ilceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, initialIlceList)
            ilceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerIlce.adapter = ilceAdapter

            // İl seçimi değiştiğinde ilçeleri güncelle
            spinnerIl.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    try {
                        if (position in 0 until ilList.size) {
                            when (position) {
                                0 -> {
                                    setupIlceSpinner(arrayOf("İlçe Seçin"))
                                    selectedIl = ""
                                }
                                1 -> {
                                    setupIlceSpinner(rizeIlceler)
                                    selectedIl = "Rize"
                                }
                                2 -> {
                                    setupIlceSpinner(samsunIlceler)
                                    selectedIl = "Samsun"
                                }
                            }
                            selectedIlce = ""
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        showToast("İl seçiminde hata oluştu")
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Spinner başlatmada hata oluştu")
        }
    }

    private fun setupIlceSpinner(ilceler: Array<String>) {
        try {
            val ilceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ilceler)
            ilceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerIlce.adapter = ilceAdapter

            spinnerIlce.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    try {
                        selectedIlce = if (position > 0 && position < ilceler.size) ilceler[position] else ""
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("İlçe spinner hatası")
        }
    }

    private fun setupClickListeners() {
        buttonKaydet.setOnClickListener {
            saveAddress()
        }

        buttonGuncelle.setOnClickListener {
            updateAddress()
        }
    }

    private fun checkExistingAddress() {
        try {
            val userId = auth.currentUser?.uid ?: return

            database.reference.child("users").child(userId).child("address")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        try {
                            if (snapshot.exists()) {
                                hasAddress = true
                                val il = snapshot.child("il").getValue(String::class.java)
                                val ilce = snapshot.child("ilce").getValue(String::class.java)
                                val adresDetay = snapshot.child("adresDetay").getValue(String::class.java)
                                val tamAdres = snapshot.child("tamAdres").getValue(String::class.java)

                                // Mevcut adresi göster
                                tamAdres?.let {
                                    textViewMevcutAdres.text = "Mevcut Adres: $it"
                                    textViewMevcutAdres.visibility = View.VISIBLE
                                }

                                // Buton durumlarını ayarla
                                buttonKaydet.visibility = View.GONE
                                buttonGuncelle.visibility = View.VISIBLE

                                // Form alanlarını doldur
                                il?.let { loadedIl ->
                                    val ilPosition = ilList.indexOf(loadedIl)
                                    if (ilPosition in 0 until ilList.size) {
                                        spinnerIl.setSelection(ilPosition)
                                        selectedIl = loadedIl
                                    }
                                }

                                ilce?.let { loadedIlce ->
                                    spinnerIl.post {
                                        try {
                                            val currentIlceler = when (selectedIl) {
                                                "Rize" -> rizeIlceler
                                                "Samsun" -> samsunIlceler
                                                else -> arrayOf("İlçe Seçin")
                                            }

                                            val ilcePosition = currentIlceler.indexOf(loadedIlce)
                                            if (ilcePosition in 0 until currentIlceler.size) {
                                                spinnerIlce.setSelection(ilcePosition)
                                                selectedIlce = loadedIlce
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }

                                adresDetay?.let {
                                    editTextAdres.setText(it)
                                }
                            } else {
                                hasAddress = false
                                textViewMevcutAdres.visibility = View.GONE
                                buttonKaydet.visibility = View.VISIBLE
                                buttonGuncelle.visibility = View.GONE
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showToast("Adres bilgileri yüklenirken hata oluştu: ${error.message}")
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveAddress() {
        try {
            val adresDetay = editTextAdres.text.toString().trim()

            // Validasyon
            if (selectedIl.isEmpty()) {
                showToast("Lütfen il seçin")
                return
            }

            if (selectedIlce.isEmpty()) {
                showToast("Lütfen ilçe seçin")
                return
            }

            if (adresDetay.isEmpty()) {
                showToast("Lütfen adres detayı girin")
                return
            }

            val userId = auth.currentUser?.uid ?: run {
                showToast("Kullanıcı oturumu bulunamadı")
                return
            }

            // Adres bilgilerini oluştur
            val addressData = hashMapOf(
                "il" to selectedIl,
                "ilce" to selectedIlce,
                "adresDetay" to adresDetay,
                "tamAdres" to "$selectedIl, $selectedIlce - $adresDetay"
            )

            // Firebase'e kaydet
            buttonKaydet.text = "Kaydediliyor..."
            buttonKaydet.isEnabled = false

            database.reference.child("users").child(userId).child("address")
                .setValue(addressData)
                .addOnSuccessListener {
                    showToast("Adres başarıyla kaydedildi")
                    finish()
                }
                .addOnFailureListener { exception ->
                    showToast("Adres kaydedilirken hata oluştu: ${exception.message}")
                    buttonKaydet.text = "Adresi Kaydet"
                    buttonKaydet.isEnabled = true
                }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Kayıt sırasında hata oluştu")
            buttonKaydet.text = "Adresi Kaydet"
            buttonKaydet.isEnabled = true
        }
    }

    private fun updateAddress() {
        try {
            val adresDetay = editTextAdres.text.toString().trim()

            // Validasyon
            if (selectedIl.isEmpty()) {
                showToast("Lütfen il seçin")
                return
            }

            if (selectedIlce.isEmpty()) {
                showToast("Lütfen ilçe seçin")
                return
            }

            if (adresDetay.isEmpty()) {
                showToast("Lütfen adres detayı girin")
                return
            }

            val userId = auth.currentUser?.uid ?: run {
                showToast("Kullanıcı oturumu bulunamadı")
                return
            }

            // Güncellenmiş adres bilgilerini oluştur
            val updatedAddress = hashMapOf(
                "il" to selectedIl,
                "ilce" to selectedIlce,
                "adresDetay" to adresDetay,
                "tamAdres" to "$selectedIl, $selectedIlce - $adresDetay"
            )

            // Firebase'de güncelle
            buttonGuncelle.text = "Güncelleniyor..."
            buttonGuncelle.isEnabled = false

            database.reference.child("users").child(userId).child("address")
                .updateChildren(updatedAddress as Map<String, Any>)
                .addOnSuccessListener {
                    showToast("Adres başarıyla güncellendi")
                    finish()
                }
                .addOnFailureListener { exception ->
                    showToast("Adres güncellenirken hata oluştu: ${exception.message}")
                    buttonGuncelle.text = "Adresi Güncelle"
                    buttonGuncelle.isEnabled = true
                }
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Güncelleme sırasında hata oluştu")
            buttonGuncelle.text = "Adresi Güncelle"
            buttonGuncelle.isEnabled = true
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}