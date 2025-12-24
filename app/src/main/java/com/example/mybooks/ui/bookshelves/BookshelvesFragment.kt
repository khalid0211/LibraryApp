package com.example.mybooks.ui.bookshelves

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mybooks.R
import com.example.mybooks.data.model.Bookshelf
import com.example.mybooks.data.repository.BookshelfRepositoryImpl
import com.example.mybooks.databinding.FragmentBookshelvesBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class BookshelvesFragment : Fragment() {

    private val binding: FragmentBookshelvesBinding by lazy { FragmentBookshelvesBinding.inflate(layoutInflater) }

    private lateinit var viewModel: BookshelvesViewModel
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var emptyView: View
//    private lateinit var fab: TextView
    private lateinit var adapter: BookshelvesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
        //return inflater.inflate(R.layout.fragment_bookshelves, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = BookshelvesViewModel(BookshelfRepositoryImpl())

        // Initialize views


        // Setup RecyclerView
        adapter = BookshelvesAdapter(
            onDeleteClick = { bookshelf ->
                showDeleteConfirmation(bookshelf)
            }
        )
        binding.bookshelvesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.bookshelvesRecyclerView.adapter = adapter

        // Setup observers
        setupObservers()

        // Load bookshelves
        viewModel.loadBookshelves()

        // Setup FAB
        binding.fabAddBookshelf.setOnClickListener {
            showAddBookshelfDialog()
        }
    }

    private fun setupObservers() {
        viewModel.bookshelves.observe(viewLifecycleOwner) { bookshelves ->
            adapter.submitList(bookshelves)
            if (bookshelves.isEmpty()) {
                binding.bookshelvesRecyclerView.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
            } else {
                binding.bookshelvesRecyclerView.visibility = View.VISIBLE
                binding.emptyView.visibility = View.GONE
                binding.progressBar.visibility= View.GONE
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

        val shelfIdInput = dialogView.findViewById<EditText>(R.id.shelf_id_input)
        val titleInput = dialogView.findViewById<EditText>(R.id.title_input)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.description_input)
        val cancelButton = dialogView.findViewById<TextView>(R.id.cancel_button)
        val saveButton = dialogView.findViewById<TextView>(R.id.save_button)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialogue)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            val shelfIdText = shelfIdInput.text.toString().trim()
            val title = titleInput.text.toString().trim()
            val description = descriptionInput.text.toString().trim()

            if (shelfIdText.isEmpty()) {
                Toast.makeText(requireContext(), "Shelf ID is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val shelfId = shelfIdText.toIntOrNull()
            if (shelfId == null || shelfId <= 0) {
                Toast.makeText(requireContext(), "Shelf ID must be a positive number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show()
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

    @SuppressLint("SetTextI18n")
    private fun showDeleteConfirmation(bookshelf: Bookshelf) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.delete_dialogue_layout, null)
        val tvDescription = dialogView.findViewById<TextView>(R.id.tvDescription)
        val cancelButton = dialogView.findViewById<TextView>(R.id.cancel_button)
        val deleteButton = dialogView.findViewById<TextView>(R.id.delete_btn)
        tvDescription.setText("Are you sure you want to delete \"${bookshelf.title}\"?")

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.rounded_dialogue)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        deleteButton.setOnClickListener {
            viewModel.deleteBookshelf(bookshelf.id)
            dialog.dismiss()
        }


        dialog.show()
    }
}
