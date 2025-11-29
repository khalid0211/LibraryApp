package com.example.mybooks.data.model

import com.google.firebase.Timestamp

data class User(
    val email: String = "",
    val name: String = "",
    val role: String = "none", // viewer, manage, admin
    val status: String = "active",
    val lastLogin: Timestamp? = null
)