package com.example.movethechair

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HizmetYonetimActivity : AppCompatActivity() {

    private lateinit var ozelHizmetEditText: EditText
    private lateinit var yeniHizmetFiyatEditText: EditText
    private lateinit var yeniHizmetEkleButton: Button
    private lateinit var bilgiTextView: TextView
    private lateinit var mevcutHizmetlerRecyclerView: RecyclerView
    private lateinit var availableHizmetlerRecyclerView: RecyclerView
    private lateinit var emptyStateTextView: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val mevcutHizmetlerList = mutableListOf<HizmetItem>()
    private val availableHizmetlerList = mutableListOf<String>()
    private lateinit var hizmetAdapter: HizmetAdapter
    private lateinit var availableHizmetAdapter: AvailableHizmetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hizmet_yonetim)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        initializeViews()
        setupRecyclerViews()
        loadAvailableServices()
        setupClickListeners()
        loadBarberServices()
    }

    private fun initializeViews() {
        ozelHizmetEditText = findViewById(R.id.editTextOzelHizmet)
        yeniHizmetFiyatEditText = findViewById(R.id.editTextYeniHizmetFiyat)
        yeniHizmetEkleButton = findViewById(R.id.buttonYeniHizmetEkle)
        bilgiTextView = findViewById(R.id.textViewBilgi)
        mevcutHizmetlerRecyclerView = findViewById(R.id.recyclerViewMevcutHizmetler)
        availableHizmetlerRecyclerView = findViewById(R.id.recyclerViewAvailableHizmetler)
        emptyStateTextView = findViewById(R.id.textViewEmptyState)
    }

    private fun setupRecyclerViews() {

        mevcutHizmetlerRecyclerView.layoutManager = LinearLayoutManager(this)
        hizmetAdapter = HizmetAdapter(
            mevcutHizmetlerList,
            onDeleteClick = { hizmet -> deleteService(hizmet) },
            onEditClick = { hizmet -> showEditDialog(hizmet) }
        )
        mevcutHizmetlerRecyclerView.adapter = hizmetAdapter

        availableHizmetlerRecyclerView.layoutManager = LinearLayoutManager(this)
        availableHizmetAdapter = AvailableHizmetAdapter(
            availableHizmetlerList,
            onAddClick = { serviceName, price -> addServiceWithPrice(serviceName, price) }
        )
        availableHizmetlerRecyclerView.adapter = availableHizmetAdapter
    }

    private fun loadAvailableServices() {
        val availableServicesRef = database.reference.child("services")

        availableServicesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                availableHizmetlerList.clear()

                for (serviceSnapshot in snapshot.children) {
                    val serviceName = serviceSnapshot.child("name").getValue(String::class.java)
                    if (serviceName != null) {
                        availableHizmetlerList.add(serviceName)
                    }
                }

                availableHizmetAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HizmetYonetimActivity, "Hizmetler alınamadı: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupClickListeners() {
        yeniHizmetEkleButton.setOnClickListener {
            val yeniHizmetAdi = ozelHizmetEditText.text.toString()
            val yeniHizmetFiyat = yeniHizmetFiyatEditText.text.toString()

            if (yeniHizmetAdi.isEmpty()) {
                Toast.makeText(this, "Lütfen hizmet adı girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (yeniHizmetFiyat.isEmpty()) {
                Toast.makeText(this, "Lütfen fiyat girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val newServiceKey = yeniHizmetAdi.replace(" ", "_").toLowerCase()
            database.reference.child("services").child(newServiceKey).child("name").setValue(yeniHizmetAdi)
                .addOnSuccessListener {

                    addServiceWithPrice(yeniHizmetAdi, yeniHizmetFiyat)


                    ozelHizmetEditText.text.clear()
                    yeniHizmetFiyatEditText.text.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Hizmet eklenirken hata oluştu", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun addServiceWithPrice(serviceName: String, price: String) {
        if (price.isEmpty()) {
            Toast.makeText(this, "Lütfen fiyat girin", Toast.LENGTH_SHORT).show()
            return
        }

        val barberId = auth.currentUser?.uid ?: return
        val hizmetRef = database.reference.child("barbers").child(barberId).child("services").child(serviceName)

        hizmetRef.setValue(price)
            .addOnSuccessListener {
                Toast.makeText(this, "Hizmet başarıyla eklendi", Toast.LENGTH_SHORT).show()
                bilgiTextView.text = "$serviceName: $price ₺ eklendi"
                loadBarberServices() // Eklenen hizmeti görmek için listeyi yenile
            }
            .addOnFailureListener {
                Toast.makeText(this, "Hizmet eklenirken hata oluştu", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadBarberServices() {
        val barberId = auth.currentUser?.uid ?: return
        val servicesRef = database.reference.child("barbers").child(barberId).child("services")

        servicesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mevcutHizmetlerList.clear()

                for (serviceSnapshot in snapshot.children) {
                    val serviceName = serviceSnapshot.key ?: continue
                    val servicePrice = serviceSnapshot.getValue(String::class.java) ?: continue

                    mevcutHizmetlerList.add(HizmetItem(serviceName, servicePrice))
                }

                hizmetAdapter.notifyDataSetChanged()

                if (mevcutHizmetlerList.isEmpty()) {
                    emptyStateTextView.visibility = View.VISIBLE
                    mevcutHizmetlerRecyclerView.visibility = View.GONE
                } else {
                    emptyStateTextView.visibility = View.GONE
                    mevcutHizmetlerRecyclerView.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HizmetYonetimActivity, "Hizmetler yüklenirken hata oluştu: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteService(hizmet: HizmetItem) {
        val barberId = auth.currentUser?.uid ?: return
        val serviceRef = database.reference.child("barbers").child(barberId).child("services").child(hizmet.ad)

        AlertDialog.Builder(this)
            .setTitle("Hizmet Silme")
            .setMessage("${hizmet.ad} hizmetini silmek istediğinize emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                serviceRef.removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "${hizmet.ad} hizmeti silindi", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Hizmet silinirken hata oluştu", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showEditDialog(hizmet: HizmetItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_service, null)
        val editTextFiyat = dialogView.findViewById<EditText>(R.id.editTextDialogFiyat)
        editTextFiyat.setText(hizmet.fiyat)

        AlertDialog.Builder(this)
            .setTitle("${hizmet.ad} Fiyatını Düzenle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val yeniFiyat = editTextFiyat.text.toString()
                if (yeniFiyat.isNotEmpty()) {
                    updateServicePrice(hizmet.ad, yeniFiyat)
                } else {
                    Toast.makeText(this, "Lütfen geçerli bir fiyat girin", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun updateServicePrice(serviceName: String, newPrice: String) {
        val barberId = auth.currentUser?.uid ?: return
        val serviceRef = database.reference.child("barbers").child(barberId).child("services").child(serviceName)

        serviceRef.setValue(newPrice)
            .addOnSuccessListener {
                Toast.makeText(this, "$serviceName fiyatı güncellendi", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Fiyat güncellenirken hata oluştu", Toast.LENGTH_SHORT).show()
            }
    }
}

data class HizmetItem(val ad: String, val fiyat: String)

class HizmetAdapter(
    private val hizmetler: List<HizmetItem>,
    private val onDeleteClick: (HizmetItem) -> Unit,
    private val onEditClick: (HizmetItem) -> Unit
) : RecyclerView.Adapter<HizmetAdapter.HizmetViewHolder>() {

    inner class HizmetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hizmetAdiTextView: TextView = itemView.findViewById(R.id.textViewHizmetAdi)
        val hizmetFiyatiTextView: TextView = itemView.findViewById(R.id.textViewHizmetFiyati)
        val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDeleteHizmet)
        val editButton: ImageButton = itemView.findViewById(R.id.buttonEditHizmet)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HizmetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hizmet, parent, false)
        return HizmetViewHolder(view)
    }

    override fun onBindViewHolder(holder: HizmetViewHolder, position: Int) {
        val hizmet = hizmetler[position]
        holder.hizmetAdiTextView.text = hizmet.ad
        holder.hizmetFiyatiTextView.text = "${hizmet.fiyat} ₺"

        holder.deleteButton.setOnClickListener {
            onDeleteClick(hizmet)
        }

        holder.editButton.setOnClickListener {
            onEditClick(hizmet)
        }
    }

    override fun getItemCount() = hizmetler.size
}


class AvailableHizmetAdapter(
    private val hizmetler: List<String>,
    private val onAddClick: (String, String) -> Unit
) : RecyclerView.Adapter<AvailableHizmetAdapter.AvailableHizmetViewHolder>() {

    inner class AvailableHizmetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hizmetAdiTextView: TextView = itemView.findViewById(R.id.textViewAvailableHizmetAdi)
        val fiyatEditText: EditText = itemView.findViewById(R.id.editTextHizmetFiyati)
        val addButton: ImageButton = itemView.findViewById(R.id.buttonAddHizmet)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvailableHizmetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_available_hizmet, parent, false)
        return AvailableHizmetViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvailableHizmetViewHolder, position: Int) {
        val hizmetAdi = hizmetler[position]
        holder.hizmetAdiTextView.text = hizmetAdi

        holder.addButton.setOnClickListener {
            val fiyat = holder.fiyatEditText.text.toString()
            onAddClick(hizmetAdi, fiyat)
            holder.fiyatEditText.text.clear()
        }
    }

    override fun getItemCount() = hizmetler.size
}