package com.example.mybooks.data.repository

import com.example.mybooks.data.model.Loan
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

interface LoanRepository {
    suspend fun lendBook(bookId: String, borrowerEmail: String, dueDate: Timestamp): Result<Unit>
}

class LoanRepositoryImpl : LoanRepository {

    private val db: FirebaseFirestore = Firebase.firestore

    override suspend fun lendBook(bookId: String, borrowerEmail: String, dueDate: Timestamp): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val bookRef = db.collection("books").document(bookId)
                val loanRef = db.collection("book_loans").document()

                // 1. Update the book document
                transaction.update(
                    bookRef,
                    "is_lent", true,
                    "lent_to_email", borrowerEmail,
                    "due_date", dueDate
                )

                // 2. Create a new loan document
                val loan = Loan(
                    book_id = bookId,
                    borrowed_by_email = borrowerEmail,
                    due_date = dueDate,
                    loan_date = Timestamp.now()
                )
                transaction.set(loanRef, loan)

                // Transaction will be committed automatically
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}