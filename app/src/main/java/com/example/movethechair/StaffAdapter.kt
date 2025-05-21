package com.example.movethechair

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StaffAdapter(
    private val staffList: List<Staff>,
    private val onStaffAction: (Staff, String) -> Unit
) : RecyclerView.Adapter<StaffAdapter.StaffViewHolder>() {

    class StaffViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textViewStaffName)
        val specialtiesTextView: TextView = itemView.findViewById(R.id.textViewSpecialties)
        val editButton: Button = itemView.findViewById(R.id.buttonEdit)
        val deleteButton: Button = itemView.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_staff, parent, false)
        return StaffViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        val currentItem = staffList[position]

        holder.nameTextView.text = currentItem.name
        holder.specialtiesTextView.text = currentItem.specialties.joinToString(", ")

        holder.editButton.setOnClickListener {
            onStaffAction(currentItem, "edit")
        }

        holder.deleteButton.setOnClickListener {
            onStaffAction(currentItem, "delete")
        }
    }

    override fun getItemCount() = staffList.size
}