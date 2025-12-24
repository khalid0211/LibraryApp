package com.example.mybooks.ui.books

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybooks.MainActivity
import com.example.mybooks.R
import com.example.mybooks.data.model.Book
import com.example.mybooks.data.model.Owner
import com.example.mybooks.data.model.Bookshelf
import com.example.mybooks.data.repository.BookRepositoryImpl
import com.example.mybooks.databinding.FragmentBooksBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class BooksFragment : Fragment() {


    private val binding: FragmentBooksBinding by lazy { FragmentBooksBinding.inflate(layoutInflater) }


    private val db: FirebaseFirestore by lazy { Firebase.firestore }

    private lateinit var adapter: BooksAdapter
    private var allBooks = listOf<Book>()
    private var ownersMap = mapOf<String, String>()
    private var bookshelvesMap = mapOf<Int, String>()
    private var mainActivity: MainActivity?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BooksAdapter { book ->
           // showBookDetailsDialog(book)
            val gson = Gson()
            val bookJson = gson.toJson(book)
            val bundle= Bundle()
            bundle.putString("data", bookJson)

            findNavController().navigate(R.id.navigation_book_details, bundle)
        }
        binding.booksRecyclerView.layoutManager = GridLayoutManager(context, 3)
        binding.booksRecyclerView.adapter = adapter

        binding.addBookButton.setOnClickListener {
            findNavController().navigate(R.id.action_books_to_add_book)
        }

        setupSearchFilter()
        loadBooks()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = activity as? MainActivity
    }

    override fun onResume() {
        super.onResume()
        loadBooks()
    }

    private fun setupSearchFilter() {
        if (mainActivity!=null){
            mainActivity!!.onSearch { query ->
                filterBooks(query)
            }
        }
    }

    private fun filterBooks(query: String) {
        val filteredBooks = if (query.isEmpty()) {
            allBooks
        } else {
            allBooks.filter { book ->
                book.title.contains(query, ignoreCase = true) ||
                book.authors.any { it.contains(query, ignoreCase = true) } ||
                book.isbn.contains(query, ignoreCase = true) ||
                book.tracking_number.contains(query, ignoreCase = true)
            }
        }

        updateBooksList(filteredBooks)
    }

    private fun loadBooks() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Load all collections in parallel
                val booksSnapshot = db.collection("books").get().await()
                val ownersSnapshot = db.collection("owners").get().await()
                val bookshelvesSnapshot = db.collection("bookshelves").get().await()

                // Build lookup maps using the correct linking fields
                ownersMap = ownersSnapshot.documents.associate { doc ->
                    (doc.getString("owner_id") ?: "") to (doc.getString("name") ?: "Unknown Owner")
                }
                bookshelvesMap = bookshelvesSnapshot.documents.mapNotNull { doc ->
                    val shelfId = doc.get("shelf_id")
                    val shelfIdInt = when (shelfId) {
                        is Number -> shelfId.toInt()
                        is String -> shelfId.toIntOrNull()
                        else -> null
                    }
                    val title = doc.getString("title") ?: "Unknown Bookshelf"
                    shelfIdInt?.let { it to title }
                }.toMap()

                // Parse books manually to handle any field mismatches
                val books = booksSnapshot.documents.mapNotNull { doc ->
                    try {
                        // Handle authors field - it can be either a String or List<String>
                        val authorsField = doc.get("authors")
                        val authorsList = when (authorsField) {
                            is String -> listOf(authorsField)
                            is List<*> -> authorsField.filterIsInstance<String>()
                            else -> emptyList()
                        }

                        // Handle bookshelf_id as number
                        val bookshelfIdRaw = doc.get("bookshelf_id")
                        val bookshelfId = when (bookshelfIdRaw) {
                            is Number -> bookshelfIdRaw.toInt()
                            is String -> bookshelfIdRaw.toIntOrNull() ?: 0
                            else -> 0
                        }

                        Book(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            authors = authorsList,
                            publisher = doc.getString("publisher") ?: "",
                            isbn = doc.getString("isbn") ?: "",
                            bookshelf_id = bookshelfId,
                            owner_id = doc.getString("owner_id") ?: "",
                            tracking_number = doc.getString("tracking_number") ?: "",
                            is_lent = doc.getBoolean("is_lent") ?: false,
                            lent_to = doc.getString("lent_to"),
                            lent_by = doc.getString("lent_by"),
                            lent_date = doc.getString("lent_date"),
                            due_date = doc.getTimestamp("due_date"),
                            preview_url = doc.getString("preview_url"),
                            edition = doc.getString("edition"),
                            page_count = doc.getString("page_count"),
                            publish_date = doc.getString("publish_date"),
                            created_at = doc.getString("created_at")
                        )
                    } catch (e: Exception) {
                        // Skip books that can't be parsed
                        android.util.Log.e("BooksFragment", "Error parsing book ${doc.id}: ${e.message}")
                        null
                    }
                }

                allBooks = books
                updateBooksList(books)

            } catch (e: Exception) {
                Toast.makeText(context, "Error loading books: ${e.message}", Toast.LENGTH_LONG).show()
                android.util.Log.e("BooksFragment", "Error loading books", e)
            }
        }
    }

    private fun updateBooksList(books: List<Book>) {
        if (books.isEmpty()) {
            binding.booksRecyclerView.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
            binding.emptyState.text = if (allBooks.isEmpty()) {
                "No books yet.\nAdd your first book!"
            } else {
                "No books match your search."
            }
        } else {
            binding. booksRecyclerView.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
            binding.progressBar.visibility = View.GONE
            adapter.submitList(books)
        }
    }

    private fun showBookDetailsDialog(book: Book) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_book_details, null)

        val titleText: TextView = dialogView.findViewById(R.id.dialog_book_title)
        val trackingText: TextView = dialogView.findViewById(R.id.dialog_book_tracking)
        val ownerText: TextView = dialogView.findViewById(R.id.dialog_book_owner)
        val bookshelfText: TextView = dialogView.findViewById(R.id.dialog_book_bookshelf)
        val authorsText: TextView = dialogView.findViewById(R.id.dialog_book_authors)
        val publisherText: TextView = dialogView.findViewById(R.id.dialog_book_publisher)
        val isbnText: TextView = dialogView.findViewById(R.id.dialog_book_isbn)
        val editionText: TextView = dialogView.findViewById(R.id.dialog_book_edition)
        val publishDateText: TextView = dialogView.findViewById(R.id.dialog_book_publish_date)
        val pageCountText: TextView = dialogView.findViewById(R.id.dialog_book_page_count)
        val statusText: TextView = dialogView.findViewById(R.id.dialog_book_status)
        val loanInfoContainer: View = dialogView.findViewById(R.id.loan_info_container)
        val lentToText: TextView = dialogView.findViewById(R.id.dialog_book_lent_to)
        val lentByText: TextView = dialogView.findViewById(R.id.dialog_book_lent_by)
        val lentDateText: TextView = dialogView.findViewById(R.id.dialog_book_lent_date)
        val dueDateText: TextView = dialogView.findViewById(R.id.dialog_book_due_date)
        val changeBookshelfButton: Button = dialogView.findViewById(R.id.change_bookshelf_button)

        titleText.text = book.title
        trackingText.text = book.tracking_number
        ownerText.text = ownersMap[book.owner_id] ?: "Unknown Owner"
        bookshelfText.text = bookshelvesMap[book.bookshelf_id] ?: "Unknown Bookshelf"
        authorsText.text = book.authors.joinToString(", ").ifEmpty { "Unknown" }
        publisherText.text = book.publisher.ifEmpty { "Unknown" }
        isbnText.text = book.isbn.ifEmpty { "N/A" }
        editionText.text = book.edition?.ifEmpty { "N/A" } ?: "N/A"
        publishDateText.text = book.publish_date?.let { formatDate(it) } ?: "N/A"
        pageCountText.text = book.page_count?.ifEmpty { "N/A" } ?: "N/A"

        if (book.is_lent) {
            statusText.text = "On Loan"
            statusText.setTextColor(0xFFFF9800.toInt())
            loanInfoContainer.visibility = View.VISIBLE
            lentToText.text = book.lent_to ?: "Unknown"
            lentByText.text = book.lent_by ?: "Unknown"
            lentDateText.text = book.lent_date?.let { formatDate(it) } ?: "N/A"

            val dueDate = book.due_date?.toDate()
            dueDateText.text = if (dueDate != null) {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(dueDate)
            } else {
                "N/A"
            }
        } else {
            statusText.text = "Available"
            statusText.setTextColor(0xFF4CAF50.toInt())
            loanInfoContainer.visibility = View.GONE
        }

        val detailsDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
            .create()

        changeBookshelfButton.setOnClickListener {
            detailsDialog.dismiss()
            showChangeBookshelfDialog(book)
        }

        detailsDialog.show()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            if (date != null) outputFormat.format(date) else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun showChangeBookshelfDialog(book: Book) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_change_bookshelf, null)

        val bookTitleText: TextView = dialogView.findViewById(R.id.book_title_text)
        val bookshelfSpinner: Spinner = dialogView.findViewById(R.id.bookshelf_spinner)
        val cancelButton: Button = dialogView.findViewById(R.id.cancel_button)
        val saveButton: Button = dialogView.findViewById(R.id.save_button)

        bookTitleText.text = book.title

        // Load bookshelves into spinner
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val bookshelvesSnapshot = db.collection("bookshelves").get().await()
                val bookshelvesList = bookshelvesSnapshot.documents.mapNotNull { doc ->
                    val shelfIdRaw = doc.get("shelf_id")
                    val shelfId = when (shelfIdRaw) {
                        is Number -> shelfIdRaw.toInt()
                        is String -> shelfIdRaw.toIntOrNull()
                        else -> null
                    }
                    val title = doc.getString("title") ?: "Unknown Bookshelf"
                    shelfId?.let { Bookshelf(id = doc.id, shelf_id = it, title = title) }
                }

                if (bookshelvesList.isEmpty()) {
                    Toast.makeText(context, "No bookshelves found. Please add bookshelves first.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val bookshelfTitles = bookshelvesList.map { it.title }
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    bookshelfTitles
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                bookshelfSpinner.adapter = adapter

                // Select current bookshelf
                val currentIndex = bookshelvesList.indexOfFirst { it.shelf_id == book.bookshelf_id }
                if (currentIndex >= 0) {
                    bookshelfSpinner.setSelection(currentIndex)
                }

                val dialog = AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create()

                cancelButton.setOnClickListener {
                    dialog.dismiss()
                }

                saveButton.setOnClickListener {
                    val selectedBookshelf = bookshelvesList[bookshelfSpinner.selectedItemPosition]

                    if (selectedBookshelf.shelf_id == book.bookshelf_id) {
                        Toast.makeText(context, "Book is already on this bookshelf", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        return@setOnClickListener
                    }

                    viewLifecycleOwner.lifecycleScope.launch {
                        val repository = BookRepositoryImpl()
                        val result = repository.updateBookshelf(book.id, selectedBookshelf.shelf_id)

                        if (result.isSuccess) {
                            Toast.makeText(context, "Bookshelf changed successfully!", Toast.LENGTH_SHORT).show()
                            loadBooks() // Reload to show updated data
                            dialog.dismiss()
                        } else {
                            Toast.makeText(context, "Failed to change bookshelf: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                dialog.show()

            } catch (e: Exception) {
                Toast.makeText(context, "Error loading bookshelves: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}


