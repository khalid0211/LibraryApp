package com.example.mybooks.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Book(
    @DocumentId val id: String = "",
    val title: String = "",
    val authors: List<String> = emptyList(),
    val publisher: String = "",
    val isbn: String = "",
    val bookshelf_id: Int = 0,
    val owner_id: String = "",
    val tracking_number: String = "",
    val is_lent: Boolean = false,
    val lent_to: String? = null,
    val lent_by: String? = null,
    val lent_date: String? = null,
    val due_date: Timestamp? = null,
    val preview_url: String? = null,
    val edition: String? = null,
    val page_count: String? = null,
    val publish_date: String? = null,
    val created_at: String? = null
)