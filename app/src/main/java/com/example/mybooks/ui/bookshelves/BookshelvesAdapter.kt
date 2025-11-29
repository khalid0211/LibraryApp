package com.example.mybooks.ui.bookshelves

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mybooks.R
import com.example.mybooks.data.model.Bookshelf

class BookshelvesAdapter(
    private val onDeleteClick: (Bookshelf) -> Unit
) : ListAdapter<Bookshelf, BookshelvesAdapter.BookshelfViewHolder>(BookshelfDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookshelfViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookshelf, parent, false)
        return BookshelfViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookshelfViewHolder, position: Int) {
        holder.bind(getItem(position), onDeleteClick)
    }

    class BookshelfViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.bookshelf_title)
        private val descriptionText: TextView = itemView.findViewById(R.id.bookshelf_description)
        private val idText: TextView = itemView.findViewById(R.id.bookshelf_id)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button)

        fun bind(bookshelf: Bookshelf, onDeleteClick: (Bookshelf) -> Unit) {
            titleText.text = bookshelf.title

            // Show or hide description based on whether it's empty
            if (bookshelf.description.isNotEmpty()) {
                descriptionText.text = bookshelf.description
                descriptionText.visibility = View.VISIBLE
            } else {
                descriptionText.visibility = View.GONE
            }

            idText.text = "ID: ${bookshelf.shelf_id}"
            deleteButton.setOnClickListener {
                onDeleteClick(bookshelf)
            }
        }
    }

    private class BookshelfDiffCallback : DiffUtil.ItemCallback<Bookshelf>() {
        override fun areItemsTheSame(oldItem: Bookshelf, newItem: Bookshelf): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Bookshelf, newItem: Bookshelf): Boolean {
            return oldItem == newItem
        }
    }
}
