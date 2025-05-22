package com.example.movethechair

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ApprovedBarberAdapter(
    private val barberList: List<BarberUser>,
    private val onRevokeClick: (BarberUser) -> Unit
) : RecyclerView.Adapter<ApprovedBarberAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.barberName)
        val emailTextView: TextView = itemView.findViewById(R.id.barberEmail)
        val phoneTextView: TextView = itemView.findViewById(R.id.barberPhone)
        val addressTextView: TextView = itemView.findViewById(R.id.barberAddress)
        val revokeButton: Button = itemView.findViewById(R.id.revokeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_approved_barber, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val barber = barberList[position]

        // Mevcut alanlarınıza göre düzenleyin - örnek:
        holder.nameTextView.text = barber.email?.substringBefore("@") ?: "İsim belirtilmemiş"
        holder.emailTextView.text = barber.email ?: "Email belirtilmemiş"
        holder.phoneTextView.text = "Telefon bilgisi yok" // BarberUser'da phone yoksa
        holder.addressTextView.text = "Adres bilgisi yok" // BarberUser'da address yoksa

        holder.revokeButton.setOnClickListener {
            onRevokeClick(barber)
        }
    }

    override fun getItemCount(): Int = barberList.size
}