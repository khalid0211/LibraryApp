package com.example.mybooks.ui.bookdetails

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.mybooks.data.model.Book
import com.example.mybooks.databinding.FragmentBookDetailsBinding
import com.google.gson.Gson


class BookDetailsFragment : Fragment() {


    private val binding: FragmentBookDetailsBinding by lazy {
        FragmentBookDetailsBinding.inflate(layoutInflater)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bookJson = arguments?.getString("data")

        if (bookJson != null) {
            val book = Gson().fromJson(bookJson, Book::class.java)

            if (book.preview_url.isNullOrEmpty()){
               // Glide.with(requireActivity()).load("").into(binding.bookImage)
            }else{
                Glide.with(requireActivity()).load(book.preview_url).into(binding.bookImage)
            }
            if (book.title.isEmpty()){
                binding.bookTitle.text = "N/A"
            }else{
                binding.bookTitle.text = book.title
            }
            if (book.authors.isEmpty()){
                binding.bookAuthor.text = "N/A"
            }else{
                binding.bookAuthor.text = book.authors.joinToString(", ")
            }
            if (book.publish_date.isNullOrEmpty()){
                binding.bookPublishedYear.text = "N/A"
                }
            else{
                binding.bookPublishedYear.text = book.publish_date
            }
            if (book.isbn.isEmpty()){
                binding.bookDetails.text = "N/A"
                }
            else{
                binding.bookDetails.text = book.isbn
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

}