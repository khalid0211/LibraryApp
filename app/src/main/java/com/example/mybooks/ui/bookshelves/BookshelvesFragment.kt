package com.example.mybooks.ui.bookshelves

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybooks.R
import com.example.mybooks.data.model.Bookshelf
import com.example.mybooks.data.repository.BookshelfRepositoryImpl
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class BookshelvesFragment : Fragment() {

    private lateinit var viewModel: BookshelvesViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var fab: FloatingActionButton
    private lateinit var adapter: BookshelvesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bookshelves, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = BookshelvesViewModel(BookshelfRepositoryImpl())

        // Initialize views
        recyclerView = view.findViewById(R.id.bookshelves_recycler_view)
        emptyView = view.findViewById(R.id.empty_view)
        fab = view.findViewById(R.id.fab_add_bookshelf)

        // Setup RecyclerView
        adapter = BookshelvesAdapter(
            onDeleteClick = { bookshelf ->
                showDeleteConfirmation(bookshelf)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Setup observers
        setupObservers()

        // Load bookshelves
        viewModel.loadBookshelves()

        // Setup FAB
        fab.setOnClickListener {
            showAddBookshelfDialog()
        }
    }

    private fun setupObservers() {
        viewModel.bookshelves.observe(viewLifecycleOwner) { bookshelves ->
            adapter.submitList(bookshelves)
            if (bookshelves.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            }
        }

        viewModel.addBookshelfSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Bookshelf added successfully!", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.deleteBookshelfSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Bookshelf deleted successfully!", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    private fun showAddBookshelfDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_bookshelf, null)

        val shelfIdInput = dialogView.findViewById<TextInputEditText>(R.id.shelf_id_input)
        val titleInput = dialogView.findViewById<TextInputEditText>(R.id.title_input)
        val descriptionInput = dialogView.findViewById<TextInputEditText>(R.id.description_input)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)
        val saveButton = dialogView.findViewById<Button>(R.id.save_button)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            val shelfIdText = shelfIdInput.text.toString().trim()
            val title = titleInput.text.toString().trim()
            val description = descriptionInput.text.toString().trim()

            if (shelfIdText.isEmpty()) {
                shelfIdInput.error = "Shelf ID is required"
                return@setOnClickListener
            }

            val shelfId = shelfIdText.toIntOrNull()
            if (shelfId == null || shelfId <= 0) {
                shelfIdInput.error = "Shelf ID must be a positive number"
                return@setOnClickListener
            }

            if (title.isEmpty()) {
                titleInput.error = "Title is required"
                return@setOnClickListener
            }

            val bookshelf = Bookshelf(
                shelf_id = shelfId,
                title = title,
                description = description
            )

            viewModel.addBookshelf(bookshelf)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(bookshelf: Bookshelf) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Bookshelf")
            .setMessage("Are you sure you want to delete \"${bookshelf.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteBookshelf(bookshelf.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
