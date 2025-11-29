package com.example.mybooks.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Loan(
    @DocumentId val id: String = "",
    val book_id: String = "",
    val borrowed_by_email: String = "",
    val loan_date: Timestamp = Timestamp.now(),
    val due_date: Timestamp? = null,
    val returned_date: Timestamp? = null
)
