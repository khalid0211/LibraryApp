package com.example.mybooks.data.model

import com.google.firebase.firestore.DocumentId

data class Bookshelf(
    @DocumentId val id: String = "",
    val shelf_id: Int = 0,
    val title: String = "",
    val description: String = ""
)
