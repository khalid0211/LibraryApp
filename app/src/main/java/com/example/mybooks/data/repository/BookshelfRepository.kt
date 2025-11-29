package com.example.mybooks.data.repository

import com.example.mybooks.data.model.Bookshelf
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

interface BookshelfRepository {
    fun getBookshelves(): Flow<List<Bookshelf>>
    suspend fun addBookshelf(bookshelf: Bookshelf): Result<Unit>
    suspend fun deleteBookshelf(bookshelfId: String): Result<Unit>
}

class BookshelfRepositoryImpl : BookshelfRepository {

    private val db: FirebaseFirestore = Firebase.firestore

    override fun getBookshelves(): Flow<List<Bookshelf>> = flow {
        val snapshot = db.collection("bookshelves").get().await()

        android.util.Log.d("BookshelfRepository", "Total documents in bookshelves collection: ${snapshot.documents.size}")

        // Manually parse bookshelves to handle any field mismatches
        val bookshelves = snapshot.documents.mapNotNull { doc ->
            try {
                android.util.Log.d("BookshelfRepository", "Processing document ID: ${doc.id}")
                android.util.Log.d("BookshelfRepository", "All fields in document: ${doc.data}")

                // Handle shelf_id as number (Int or Long from Firebase)
                val shelfIdRaw = doc.get("shelf_id")
                val shelfId = when (shelfIdRaw) {
                    is Number -> shelfIdRaw.toInt()
                    is String -> shelfIdRaw.toIntOrNull() ?: 0
                    else -> 0
                }

                val titleFromField = doc.getString("title")

                android.util.Log.d("BookshelfRepository", "  shelf_id field: $shelfIdRaw -> $shelfId")
                android.util.Log.d("BookshelfRepository", "  title field: $titleFromField")

                val title = titleFromField ?: "Unnamed Bookshelf"

                if (shelfId == 0) {
                    android.util.Log.w("BookshelfRepository", "Document ${doc.id} has invalid shelf_id, skipping")
                    return@mapNotNull null
                }

                if (title.isEmpty()) {
                    android.util.Log.w("BookshelfRepository", "Document ${doc.id} has empty title, skipping")
                    return@mapNotNull null
                }

                Bookshelf(
                    id = doc.id,
                    shelf_id = shelfId,
                    title = title
                )
            } catch (e: Exception) {
                android.util.Log.e("BookshelfRepository", "Error parsing bookshelf ${doc.id}: ${e.message}")
                android.util.Log.e("BookshelfRepository", "Document data: ${doc.data}")
                null
            }
        }

        android.util.Log.d("BookshelfRepository", "Successfully parsed ${bookshelves.size} bookshelves")
        bookshelves.forEach { bookshelf ->
            android.util.Log.d("BookshelfRepository", "  Bookshelf: id=${bookshelf.id}, shelf_id=${bookshelf.shelf_id}, title=${bookshelf.title}")
        }

        emit(bookshelves)
    }

    override suspend fun addBookshelf(bookshelf: Bookshelf): Result<Unit> {
        return try {
            val data = hashMapOf(
                "shelf_id" to bookshelf.shelf_id,
                "title" to bookshelf.title,
                "description" to bookshelf.description
            )
            db.collection("bookshelves").add(data).await()
            android.util.Log.d("BookshelfRepository", "Bookshelf added: ${bookshelf.shelf_id}")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("BookshelfRepository", "Error adding bookshelf", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteBookshelf(bookshelfId: String): Result<Unit> {
        return try {
            db.collection("bookshelves").document(bookshelfId).delete().await()
            android.util.Log.d("BookshelfRepository", "Bookshelf deleted: $bookshelfId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("BookshelfRepository", "Error deleting bookshelf", e)
            Result.failure(e)
        }
    }
}
