package com.example.mybooks.ui.addbook

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybooks.data.api.GoogleBooksService
import com.example.mybooks.data.model.Book
import com.example.mybooks.data.model.Bookshelf
import com.example.mybooks.data.model.Owner
import com.example.mybooks.data.model.api.Volume
import com.example.mybooks.data.repository.BookRepository
import com.example.mybooks.data.repository.BookshelfRepository
import com.example.mybooks.data.repository.OwnerRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AddBookViewModel(
    private val bookRepository: BookRepository,
    private val ownerRepository: OwnerRepository,
    private val bookshelfRepository: BookshelfRepository
) : ViewModel() {

    private val _searchResults = MutableLiveData<List<Book>>()
    val searchResults: LiveData<List<Book>> = _searchResults

    private val _owners = MutableLiveData<List<Owner>>()
    val owners: LiveData<List<Owner>> = _owners

    private val _bookshelves = MutableLiveData<List<Bookshelf>>()
    val bookshelves: LiveData<List<Bookshelf>> = _bookshelves

    private val _addBookSuccess = MutableLiveData<Boolean>()
    val addBookSuccess: LiveData<Boolean> = _addBookSuccess

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val googleBooksService: GoogleBooksService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/books/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleBooksService::class.java)
    }

    fun searchBookByTitle(title: String) {
        viewModelScope.launch {
            try {
                val response = googleBooksService.searchBooks("intitle:$title")
                val books = response.items?.mapNotNull { volume ->
                    mapVolumeToBook(volume)
                } ?: emptyList()
                _searchResults.postValue(books)
            } catch (e: Exception) {
                _error.postValue("Failed to search for books: ${e.message}")
            }
        }
    }

    private fun mapVolumeToBook(volume: Volume): Book? {
        val volumeInfo = volume.volumeInfo ?: return null
        val isbn13 = volumeInfo.industryIdentifiers?.find { it.type == "ISBN_13" }?.identifier
        val isbn = isbn13 ?: volumeInfo.industryIdentifiers?.find { it.type == "ISBN_10" }?.identifier

        if (volumeInfo.title == null || isbn == null) {
            return null
        }

        return Book(
            title = volumeInfo.title,
            authors = volumeInfo.authors ?: emptyList(),
            publisher = volumeInfo.publisher ?: "",
            isbn = isbn,
            preview_url = volumeInfo.imageLinks?.thumbnailUrl?.replace("http://", "https://")
        )
    }

    fun loadOwners() {
        viewModelScope.launch {
            android.util.Log.d("AddBookViewModel", "Starting to load owners...")
            ownerRepository.getOwners()
                .catch { e ->
                    android.util.Log.e("AddBookViewModel", "Error loading owners", e)
                    _error.postValue("Failed to load owners: ${e.message}")
                }
                .collect { owners ->
                    android.util.Log.d("AddBookViewModel", "Owners loaded successfully: ${owners.size} items")
                    _owners.postValue(owners)
                }
        }
    }

    fun loadBookshelves() {
        viewModelScope.launch {
            android.util.Log.d("AddBookViewModel", "Starting to load bookshelves...")
            bookshelfRepository.getBookshelves()
                .catch { e ->
                    android.util.Log.e("AddBookViewModel", "Error loading bookshelves", e)
                    _error.postValue("Failed to load bookshelves: ${e.message}")
                }
                .collect { bookshelves ->
                    android.util.Log.d("AddBookViewModel", "Bookshelves loaded successfully: ${bookshelves.size} items")
                    _bookshelves.postValue(bookshelves)
                }
        }
    }

    fun addBook(book: Book) {
        viewModelScope.launch {
            val result = bookRepository.addBook(book)
            if (result.isFailure) {
                _error.postValue("Failed to add book: ${result.exceptionOrNull()?.message}")
                _addBookSuccess.postValue(false)
            } else {
                _addBookSuccess.postValue(true)
            }
        }
    }
}
