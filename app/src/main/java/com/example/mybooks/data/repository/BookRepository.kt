package com.example.mybooks.data.repository

import com.example.mybooks.data.model.Book
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

interface BookRepository {
    suspend fun addBook(book: Book): Result<Unit>
    fun getBooks(): Flow<List<Book>>
    fun searchBooks(query: String): Flow<List<Book>>
    suspend fun updateBookshelf(bookId: String, newBookshelfId: Int): Result<Unit>
}

class BookRepositoryImpl : BookRepository {

    private val db: FirebaseFirestore = Firebase.firestore

    override suspend fun addBook(book: Book): Result<Unit> {
        return try {
            val trackingNumber = generateTrackingNumber()
            val bookWithTracking = book.copy(tracking_number = trackingNumber)
            db.collection("books").add(bookWithTracking).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getBooks(): Flow<List<Book>> = flow {
        val snapshot = db.collection("books").get().await()
        emit(snapshot.toObjects(Book::class.java))
    }

    override fun searchBooks(query: String): Flow<List<Book>> = flow {
        val titleQuery = db.collection("books")
            .whereGreaterThanOrEqualTo("title", query)
            .whereLessThanOrEqualTo("title", query + '\uf8ff')
            .get()

        val authorQuery = db.collection("books")
            .whereArrayContains("authors", query)
            .get()

        val titleResults = titleQuery.await()
        val authorResults = authorQuery.await()

        val combinedResults = titleResults.toObjects(Book::class.java) +
                authorResults.toObjects(Book::class.java)

        emit(combinedResults.distinctBy { it.id })
    }

    override suspend fun updateBookshelf(bookId: String, newBookshelfId: Int): Result<Unit> {
        return try {
            db.collection("books")
                .document(bookId)
                .update("bookshelf_id", newBookshelfId)
                .await()
            android.util.Log.d("BookRepository", "Updated book $bookId to bookshelf $newBookshelfId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("BookRepository", "Error updating book bookshelf", e)
            Result.failure(e)
        }
    }

    private suspend fun generateTrackingNumber(): String {
        // Get the last tracking number from all books
        val lastBook = db.collection("books")
            .orderBy("tracking_number", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        if (lastBook.isEmpty) {
            // No books exist, start with A0001
            return "A0001"
        }

        val lastTrackingNumber = lastBook.documents[0].getString("tracking_number") ?: "A0000"

        // Extract letter and number parts (e.g., "A0025" -> letter='A', number=25)
        val letter = lastTrackingNumber.first()
        val number = lastTrackingNumber.substring(1).toIntOrNull() ?: 0

        return if (number >= 9999) {
            // Roll over to next letter (A9999 -> B0001, B9999 -> C0001, etc.)
            val nextLetter = (letter + 1)
            "${nextLetter}0001"
        } else {
            // Increment the number (A0025 -> A0026)
            "$letter${String.format("%04d", number + 1)}"
        }
    }
}