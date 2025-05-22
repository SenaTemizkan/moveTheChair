package com.example.movethechair

data class BarberUser(
    val Name: String? = null,
    val birthDate: String? = null,
    val birthPlace: String? = null,
    val tc: String? = null,
    val workplaceName: String? = null,
    val workplaceAddress: String? = null,
    val mersisNo: String? = null,
    val taxNumber: String? = null,
    val phone1: String? = null,
    val phone2: String? = null,
    val email: String? = null,
    val iban: String? = null,
    val role: String? = null,
    val approvalStatus: String? = null,

    val uid: String? = null // Firebase UID
)

