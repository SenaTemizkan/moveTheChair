package com.example.movethechair

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppointmentAdapter(
    private val appointments: List<AppointmentItem>,
    private val onActionClick: (AppointmentItem, String) -> Unit
) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    class AppointmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val customerNameTextView: TextView = view.findViewById(R.id.textViewCustomerName)
        val dateTimeTextView: TextView = view.findViewById(R.id.textViewDateTime)
        val serviceTextView: TextView = view.findViewById(R.id.textViewService)
        val statusTextView: TextView = view.findViewById(R.id.textViewStatus)
        val confirmButton: Button = view.findViewById(R.id.buttonConfirm)
        val cancelButton: Button = view.findViewById(R.id.buttonCancel)
        val completeButton: Button = view.findViewById(R.id.buttonComplete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.customerNameTextView.text = appointment.customerName
        holder.dateTimeTextView.text = "${appointment.date} - ${appointment.time}"
        holder.serviceTextView.text = appointment.service

        when (appointment.status) {
            "beklemede" -> {
                holder.statusTextView.text = "Beklemede"
                holder.confirmButton.visibility = View.VISIBLE
                holder.cancelButton.visibility = View.VISIBLE
                holder.completeButton.visibility = View.GONE
            }
            "onaylandı" -> {
                holder.statusTextView.text = "✓ Onaylandı"
                holder.confirmButton.visibility = View.GONE
                holder.cancelButton.visibility = View.VISIBLE
                holder.completeButton.visibility = View.VISIBLE
            }
            "iptal_edildi" -> {
                holder.statusTextView.text = "✗ İptal Edildi"
                holder.confirmButton.visibility = View.GONE
                holder.cancelButton.visibility = View.GONE
                holder.completeButton.visibility = View.GONE
            }
            "tamamlandı" -> {
                holder.statusTextView.text = "✓ Tamamlandı"
                holder.confirmButton.visibility = View.GONE
                holder.cancelButton.visibility = View.GONE
                holder.completeButton.visibility = View.GONE
            }
            else -> {
                holder.statusTextView.text = appointment.status
                holder.confirmButton.visibility = View.VISIBLE
                holder.cancelButton.visibility = View.VISIBLE
                holder.completeButton.visibility = View.GONE
            }
        }

        holder.confirmButton.setOnClickListener {
            onActionClick(appointment, "confirm")
        }
        holder.cancelButton.setOnClickListener {
            onActionClick(appointment, "cancel")
        }
        holder.completeButton.setOnClickListener {
            onActionClick(appointment, "complete")
        }
    }

    override fun getItemCount() = appointments.size
}