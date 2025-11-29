package com.example.mybooks.ui.bookshelves

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mybooks.data.model.Bookshelf
import com.example.mybooks.data.repository.BookshelfRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class BookshelvesViewModel(
    private val bookshelfRepository: BookshelfRepository
) : ViewModel() {

    private val _bookshelves = MutableLiveData<List<Bookshelf>>()
    val bookshelves: LiveData<List<Bookshelf>> = _bookshelves

    private val _addBookshelfSuccess = MutableLiveData<Boolean>()
    val addBookshelfSuccess: LiveData<Boolean> = _addBookshelfSuccess

    private val _deleteBookshelfSuccess = MutableLiveData<Boolean>()
    val deleteBookshelfSuccess: LiveData<Boolean> = _deleteBookshelfSuccess

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadBookshelves() {
        viewModelScope.launch {
            android.util.Log.d("BookshelvesViewModel", "Loading bookshelves...")
            bookshelfRepository.getBookshelves()
                .catch { e ->
                    android.util.Log.e("BookshelvesViewModel", "Error loading bookshelves", e)
                    _error.postValue("Failed to load bookshelves: ${e.message}")
                }
                .collect { bookshelves ->
                    android.util.Log.d("BookshelvesViewModel", "Bookshelves loaded: ${bookshelves.size} items")
                    _bookshelves.postValue(bookshelves)
                }
        }
    }

    fun addBookshelf(bookshelf: Bookshelf) {
        viewModelScope.launch {
            val result = bookshelfRepository.addBookshelf(bookshelf)
            if (result.isFailure) {
                _error.postValue("Failed to add bookshelf: ${result.exceptionOrNull()?.message}")
                _addBookshelfSuccess.postValue(false)
            } else {
                _addBookshelfSuccess.postValue(true)
                loadBookshelves() // Reload the list
            }
        }
    }

    fun deleteBookshelf(bookshelfId: String) {
        viewModelScope.launch {
            val result = bookshelfRepository.deleteBookshelf(bookshelfId)
            if (result.isFailure) {
                _error.postValue("Failed to delete bookshelf: ${result.exceptionOrNull()?.message}")
                _deleteBookshelfSuccess.postValue(false)
            } else {
                _deleteBookshelfSuccess.postValue(true)
                loadBookshelves() // Reload the list
            }
        }
    }
}
