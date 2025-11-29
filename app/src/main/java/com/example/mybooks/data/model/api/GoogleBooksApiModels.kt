package com.example.mybooks.data.model.api

import com.google.gson.annotations.SerializedName

data class BookSearchResponse(
    val items: List<Volume>? = null
)

data class Volume(
    val volumeInfo: VolumeInfo?
)

data class VolumeInfo(
    val title: String?,
    val authors: List<String>?,
    val publisher: String?,
    val industryIdentifiers: List<IndustryIdentifier>?,
    val description: String?,
    @SerializedName("imageLinks") val imageLinks: ImageLinks?
)

data class IndustryIdentifier(
    val type: String?,
    val identifier: String?
)

data class ImageLinks(
    @SerializedName("thumbnail") val thumbnailUrl: String?
)
