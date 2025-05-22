package com.example.movethechair

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BarberAdapter(
    private val barberList: List<BarberUser>,
    private val onStatusChange: (BarberUser, String) -> Unit
) : RecyclerView.Adapter<BarberAdapter.BarberViewHolder>() {

    inner class BarberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val Name: TextView = itemView.findViewById(R.id.Name)
        val toggleDetails: TextView = itemView.findViewById(R.id.toggleDetails)
        val detailsLayout: LinearLayout = itemView.findViewById(R.id.detailsLayout)
        val approveButton: Button = itemView.findViewById(R.id.approveButton)
        val rejectButton: Button = itemView.findViewById(R.id.rejectButton)
        val actionButtons: LinearLayout = itemView.findViewById(R.id.actionButtons)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarberViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expandable_barber, parent, false)
        return BarberViewHolder(view)
    }

    override fun onBindViewHolder(holder: BarberViewHolder, position: Int) {
        val barber = barberList[position]
        holder.Name.text = barber.Name ?: "Ad yok"

        val detailsMap = mapOf(
            R.id.birthDate to "Doğum Tarihi: ${barber.birthDate}",
            R.id.birthPlace to "Doğum Yeri: ${barber.birthPlace}",
            R.id.tc to "TC: ${barber.tc}",
            R.id.workplaceName to "İşyeri: ${barber.workplaceName}",
            R.id.workplaceAddress to "Adres: ${barber.workplaceAddress}",
            R.id.mersisNo to "Mersis No: ${barber.mersisNo}",
            R.id.taxNumber to "Vergi No: ${barber.taxNumber}",
            R.id.phone1 to "Telefon 1: ${barber.phone1}",
            R.id.phone2 to "Telefon 2: ${barber.phone2}",
            R.id.email to "E-posta: ${barber.email}",
            R.id.iban to "IBAN: ${barber.iban}"
        )

        detailsMap.forEach { (id, text) ->
            holder.itemView.findViewById<TextView>(id).text = text
        }

        holder.toggleDetails.setOnClickListener {
            val visible = holder.detailsLayout.visibility == View.VISIBLE
            holder.detailsLayout.visibility = if (visible) View.GONE else View.VISIBLE
            holder.actionButtons.visibility = if (visible) View.GONE else View.VISIBLE
            holder.toggleDetails.text = if (visible) "Detayları Göster" else "Detayları Gizle"
        }

        holder.approveButton.setOnClickListener { onStatusChange(barber, "approved") }
        holder.rejectButton.setOnClickListener { onStatusChange(barber, "rejected") }
    }

    override fun getItemCount() = barberList.size
}