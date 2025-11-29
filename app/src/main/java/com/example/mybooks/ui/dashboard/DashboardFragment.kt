package com.example.mybooks.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mybooks.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DashboardFragment : Fragment() {

    private val db: FirebaseFirestore by lazy { Firebase.firestore }

    private lateinit var totalBooksText: TextView
    private lateinit var booksOnLoanText: TextView
    private lateinit var availableBooksText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        totalBooksText = view.findViewById(R.id.total_books)
        booksOnLoanText = view.findViewById(R.id.books_on_loan)
        availableBooksText = view.findViewById(R.id.available_books)

        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Get total books
                val booksSnapshot = db.collection("books").get().await()
                val totalBooks = booksSnapshot.size()

                // Get books on loan
                val loanedBooks = booksSnapshot.documents.count {
                    it.getBoolean("is_lent") == true
                }

                val availableBooks = totalBooks - loanedBooks

                totalBooksText.text = totalBooks.toString()
                booksOnLoanText.text = loanedBooks.toString()
                availableBooksText.text = availableBooks.toString()

            } catch (e: Exception) {
                // Show 0 instead of error when collection doesn't exist yet
                totalBooksText.text = "0"
                booksOnLoanText.text = "0"
                availableBooksText.text = "0"
            }
        }
    }
}
