package com.example.mybooks.ui.addbook

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mybooks.R
import com.example.mybooks.data.model.Book
import com.example.mybooks.data.model.Bookshelf
import com.example.mybooks.data.model.Owner
import com.example.mybooks.data.repository.BookRepositoryImpl
import com.example.mybooks.data.repository.BookshelfRepositoryImpl
import com.example.mybooks.data.repository.OwnerRepositoryImpl
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddBookFragment : Fragment() {

    private lateinit var viewModel: AddBookViewModel
    private lateinit var titleInput: EditText
    private lateinit var authorsInput: EditText
    private lateinit var publisherInput: EditText
    private lateinit var isbnInput: EditText
    private lateinit var editionInput: EditText
    private lateinit var pageCountInput: EditText
    private lateinit var publishDateInput: EditText
    private lateinit var ownerSpinner: Spinner
    private lateinit var bookshelfSpinner: Spinner
    private lateinit var saveButton: TextView
    private lateinit var cancelButton: TextView

    private var owners = listOf<Owner>()
    private var bookshelves = listOf<Bookshelf>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_book, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = AddBookViewModel(
            BookRepositoryImpl(),
            OwnerRepositoryImpl(),
            BookshelfRepositoryImpl()
        )

        // Initialize views
        titleInput = view.findViewById(R.id.title_input)
        authorsInput = view.findViewById(R.id.authors_input)
        publisherInput = view.findViewById(R.id.publisher_input)
        isbnInput = view.findViewById(R.id.isbn_input)
        editionInput = view.findViewById(R.id.edition_input)
        pageCountInput = view.findViewById(R.id.page_count_input)
        publishDateInput = view.findViewById(R.id.publish_date_input)
        ownerSpinner = view.findViewById(R.id.owner_spinner)
        bookshelfSpinner = view.findViewById(R.id.bookshelf_spinner)
        saveButton = view.findViewById(R.id.save_button)
        cancelButton = view.findViewById(R.id.cancel_button)

        // Setup observers
        setupObservers()

        // Load data
        viewModel.loadOwners()
        viewModel.loadBookshelves()

        // Setup date picker for publish date
        publishDateInput.setOnClickListener {
            showDatePicker()
        }

        // Setup button click listeners
        saveButton.setOnClickListener {
            saveBook()
        }

        cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupObservers() {
        viewModel.owners.observe(viewLifecycleOwner) { ownersList ->
            android.util.Log.d("AddBookFragment", "Owners loaded: ${ownersList.size} items")
            ownersList.forEach { owner ->
                android.util.Log.d("AddBookFragment", "Owner: id=${owner.id}, owner_id=${owner.owner_id}, name=${owner.name}")
            }
            owners = ownersList
            val ownerNames = ownersList.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                ownerNames
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            ownerSpinner.adapter = adapter
        }

        viewModel.bookshelves.observe(viewLifecycleOwner) { bookshelvesList ->
            android.util.Log.d("AddBookFragment", "Bookshelves loaded: ${bookshelvesList.size} items")
            bookshelvesList.forEach { bookshelf ->
                android.util.Log.d("AddBookFragment", "Bookshelf: id=${bookshelf.id}, shelf_id=${bookshelf.shelf_id}, title=${bookshelf.title}")
            }
            bookshelves = bookshelvesList
            val bookshelfTitles = bookshelvesList.map { it.title }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                bookshelfTitles
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            bookshelfSpinner.adapter = adapter
        }

        viewModel.addBookSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Book added successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    private fun saveBook() {
        val title = titleInput.text.toString().trim()
        val authorsText = authorsInput.text.toString().trim()
        val publisher = publisherInput.text.toString().trim()
        val isbn = isbnInput.text.toString().trim()
        val edition = editionInput.text.toString().trim()
        val pageCount = pageCountInput.text.toString().trim()
        val publishDate = publishDateInput.text.toString().trim()

        // Validation
        if (title.isEmpty()) {
            Toast.makeText(context, "Title is required", Toast.LENGTH_SHORT).show()
            return
        }

        if (isbn.isEmpty()) {
            Toast.makeText(context, "ISBN is required", Toast.LENGTH_SHORT).show()
            return
        }

        if (owners.isEmpty()) {
            Toast.makeText(
                context,
                "No owners found in database. Please add owners first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (ownerSpinner.selectedItemPosition == -1) {
            Toast.makeText(context, "Please select an owner", Toast.LENGTH_SHORT).show()
            return
        }

        if (bookshelves.isEmpty()) {
            Toast.makeText(
                context,
                "No bookshelves found in database. Please add bookshelves first.",
                Toast.LENGTH_LONG
            ).show()
            android.util.Log.e("AddBookFragment", "Bookshelves list is empty!")
            return
        }

        if (bookshelfSpinner.selectedItemPosition == -1) {
            Toast.makeText(context, "Please select a bookshelf", Toast.LENGTH_SHORT).show()
            return
        }


        // Parse authors (comma-separated)
        val authorsList = if (authorsText.isEmpty()) {
            emptyList()
        } else {
            authorsText.split(",").map { it.trim() }
        }

        // Get selected owner and bookshelf IDs
        val selectedOwner = owners[ownerSpinner.selectedItemPosition]
        val selectedBookshelf = bookshelves[bookshelfSpinner.selectedItemPosition]

        android.util.Log.d("AddBookFragment", "Selected owner: ${selectedOwner.owner_id}, name: ${selectedOwner.name}")
        android.util.Log.d("AddBookFragment", "Selected bookshelf: ${selectedBookshelf.shelf_id}, title: ${selectedBookshelf.title}")

        // Create Book object
        val book = Book(
            title = title,
            authors = authorsList,
            publisher = publisher,
            isbn = isbn,
            owner_id = selectedOwner.owner_id,
            bookshelf_id = selectedBookshelf.shelf_id,
            edition = edition,
            page_count = pageCount,
            publish_date = publishDate
        )

        android.util.Log.d("AddBookFragment", "Creating book with owner_id=${book.owner_id}, bookshelf_id=${book.bookshelf_id}")

        // Add book
        viewModel.addBook(book)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // Parse existing date if any
        val existingDate = publishDateInput.text.toString()
        if (existingDate.isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = dateFormat.parse(existingDate)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                // Use current date if parsing fails
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Format the date as yyyy-MM-dd
                val formattedDate = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    selectedYear,
                    selectedMonth + 1, // Month is 0-based
                    selectedDay
                )
                publishDateInput.setText(formattedDate)
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }
}
