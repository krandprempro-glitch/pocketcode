package com.termux.filebrowser.domain.repository

import com.termux.filebrowser.domain.model.Bookmark

interface BookmarkRepository {
    suspend fun addBookmark(bookmark: Bookmark): Boolean
    suspend fun removeBookmark(bookmarkId: String): Boolean
    suspend fun getBookmarks(): List<Bookmark>
    suspend fun getBookmarksForConnection(connectionId: String): List<Bookmark>
    suspend fun updateBookmark(bookmark: Bookmark): Boolean
    suspend fun isBookmarked(path: String, connectionId: String): Boolean
}