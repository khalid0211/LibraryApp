package com.example.mybooks.data.repository

import com.example.mybooks.data.model.Owner
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

interface OwnerRepository {
    fun getOwners(): Flow<List<Owner>>
}

class OwnerRepositoryImpl : OwnerRepository {

    private val db: FirebaseFirestore = Firebase.firestore

    override fun getOwners(): Flow<List<Owner>> = flow {
        val snapshot = db.collection("owners").get().await()

        // Manually parse owners to handle any field mismatches
        val owners = snapshot.documents.mapNotNull { doc ->
            try {
                Owner(
                    id = doc.id,
                    owner_id = doc.getString("owner_id") ?: doc.get("ownerId")?.toString() ?: "",
                    name = doc.getString("name") ?: ""
                )
            } catch (e: Exception) {
                android.util.Log.e("OwnerRepository", "Error parsing owner ${doc.id}: ${e.message}")
                android.util.Log.e("OwnerRepository", "Document data: ${doc.data}")
                null
            }
        }

        emit(owners)
    }
}
