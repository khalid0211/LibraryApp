package com.example.mybooks.ui.books

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mybooks.R
import com.example.mybooks.data.model.Book

class BooksAdapter(
    private val onBookClick: (Book) -> Unit
) : RecyclerView.Adapter<BooksAdapter.BookViewHolder>() {

    private var books = listOf<Book>()

    fun submitList(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view, onBookClick)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount() = books.size

    class BookViewHolder(
        itemView: View,
        private val onBookClick: (Book) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.book_title)
        private val authorText: TextView = itemView.findViewById(R.id.tvAuthor)
        private val statusText: TextView = itemView.findViewById(R.id.book_status)
        private val bookImage: ImageView = itemView.findViewById(R.id.bookImage)

        fun bind(book: Book) {
            titleText.text = book.title
            authorText.text = "Authors: ${book.authors.joinToString(", ").ifEmpty { "Unknown" }}"

            if (book.preview_url.isNullOrEmpty()){
             //   Glide.with(itemView.context).load("").into(bookImage)
            }else{
                Glide.with(itemView.context).load(book.preview_url).into(bookImage)
            }
            if (book.is_lent) {
                statusText.text = "On Loan"
                statusText.setTextColor(0xFFFF9800.toInt())
            } else {
                statusText.text = "Available"
                statusText.setTextColor(0xFF4CAF50.toInt())
            }

            itemView.setOnClickListener {
                onBookClick(book)
            }
        }
    }
}