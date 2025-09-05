package com.termux.filebrowser.data.datasource.local

data class BookmarkEntity(
    val id: String,
    val name: String,
    val path: String,
    val connectionId: String,
    val createdAt: Long
)

interface BookmarkDataSource {
    suspend fun saveBookmark(bookmark: BookmarkEntity)
    suspend fun deleteBookmark(bookmarkId: String): Boolean
    suspend fun getAllBookmarks(): List<BookmarkEntity>
    suspend fun getBookmarksForConnection(connectionId: String): List<BookmarkEntity>
    suspend fun updateBookmark(bookmark: BookmarkEntity): Boolean
    suspend fun isBookmarked(path: String, connectionId: String): Boolean
}