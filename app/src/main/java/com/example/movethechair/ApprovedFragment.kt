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

class ApprovedFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ApprovedBarberAdapter
    private val barberList = mutableListOf<BarberUser>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_approved, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.approvedBarbersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ApprovedBarberAdapter(barberList) { user ->
            revokeApproval(user)
        }
        recyclerView.adapter = adapter

        fetchApprovedUsers()
    }

    private fun fetchApprovedUsers() {
        val ref = FirebaseDatabase.getInstance().getReference("users")
        ref.orderByChild("approvalStatus").equalTo("approved")
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

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Veri yüklenirken hata oluştu", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun revokeApproval(user: BarberUser) {
        val ref = FirebaseDatabase.getInstance().getReference("users").child(user.uid ?: return)
        ref.child("approvalStatus").setValue("pending").addOnSuccessListener {
            barberList.remove(user)
            adapter.notifyDataSetChanged()
            Toast.makeText(requireContext(), "Kullanıcı onayı iptal edildi", Toast.LENGTH_SHORT).show()
        }
    }
}