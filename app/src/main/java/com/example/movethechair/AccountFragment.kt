package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class AccountFragment : Fragment() {

    private lateinit var logoutButton: Button
    private lateinit var addressButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        auth = FirebaseAuth.getInstance()
        logoutButton = view.findViewById(R.id.buttonLogoutAccount)
        addressButton = view.findViewById(R.id.buttonAddress)

        logoutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            activity?.finish()
        }

        addressButton.setOnClickListener {
            startActivity(Intent(requireContext(), AddressActivity::class.java))
        }

        return view
    }
}