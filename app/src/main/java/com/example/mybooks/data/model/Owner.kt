package com.example.mybooks.data.model

import com.google.firebase.firestore.DocumentId

data class Owner(
    @DocumentId val id: String = "",
    val owner_id: String = "",
    val name: String = ""
)
