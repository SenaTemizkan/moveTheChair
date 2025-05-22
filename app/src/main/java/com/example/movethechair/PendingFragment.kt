package com.example.movethechair

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class PendingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BarberAdapter
    private val barberList = mutableListOf<BarberUser>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_pending, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.pendingBarbersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = BarberAdapter(barberList) { user, status ->
            updateApprovalStatus(user, status)
        }
        recyclerView.adapter = adapter

        fetchPendingUsers()
    }

    private fun fetchPendingUsers() {
        val ref = FirebaseDatabase.getInstance().getReference("users")
        ref.orderByChild("approvalStatus").equalTo("pending")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    barberList.clear()
                    for (child in snapshot.children) {
                        val user = child.getValue(BarberUser::class.java)
                        if (user != null) {
                            barberList.add(user.copy(uid = child.key))
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateApprovalStatus(user: BarberUser, status: String) {
        val ref = FirebaseDatabase.getInstance().getReference("users").child(user.uid ?: return)
        ref.child("approvalStatus").setValue(status).addOnSuccessListener {
            barberList.remove(user)
            adapter.notifyDataSetChanged()
            Toast.makeText(requireContext(), "Kullanıcı $status", Toast.LENGTH_SHORT).show()
        }
    }
}