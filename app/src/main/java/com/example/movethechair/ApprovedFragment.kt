package com.example.movethechair

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ApprovedFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Şimdilik basit bir view döndür
        val textView = TextView(requireContext())
        textView.text = "Onaylanan Kuaförler"
        textView.textSize = 18f
        textView.setPadding(32, 32, 32, 32)
        return textView
    }
}