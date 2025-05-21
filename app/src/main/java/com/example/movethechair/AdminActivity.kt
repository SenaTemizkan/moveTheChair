package com.example.movethechair

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        if (userId == null) {
            finishWithError("Kullanıcı oturumu bulunamadı")
            return
        }

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        database = FirebaseDatabase.getInstance().getReference("users").child(userId)

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val role = snapshot.child("role").getValue(String::class.java)
                Log.d("AdminActivity", "Kullanıcı rolü: $role")

                if (role == "admin") {
                    setupViewPager()
                } else {
                    finishWithError("Bu alana sadece admin erişebilir.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminActivity", "Veritabanı hatası: ${error.message}")
                finishWithError("Veritabanı hatası: ${error.message}")
            }
        })
    }

    private fun setupViewPager() {
        val adapter = AdminPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Bekleyen Onaylar"
                1 -> tab.text = "Onaylı Kuaförler"
            }
        }.attach()
    }

    private fun finishWithError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e("AdminActivity", message)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
