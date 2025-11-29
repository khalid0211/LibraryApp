package com.example.mybooks.data.api

import com.example.mybooks.data.model.api.BookSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksService {
    @GET("v1/volumes")
    suspend fun searchBooks(@Query("q") query: String): BookSearchResponse
}
