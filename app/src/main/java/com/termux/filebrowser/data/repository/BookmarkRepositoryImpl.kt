package com.termux.filebrowser.data.repository

import com.termux.filebrowser.data.datasource.local.BookmarkDataSource
import com.termux.filebrowser.data.mapper.BookmarkMapper
import com.termux.filebrowser.domain.model.Bookmark
import com.termux.filebrowser.domain.repository.BookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class BookmarkRepositoryImpl(
    private val bookmarkDataSource: BookmarkDataSource,
    private val bookmarkMapper: BookmarkMapper
) : BookmarkRepository {

    override suspend fun addBookmark(bookmark: Bookmark): Boolean = withContext(Dispatchers.IO) {
        val bookmarkData = bookmarkMapper.mapToEntity(bookmark)
        bookmarkDataSource.saveBookmark(bookmarkData)
        true
    }

    override suspend fun removeBookmark(bookmarkId: String): Boolean = withContext(Dispatchers.IO) {
        bookmarkDataSource.deleteBookmark(bookmarkId)
    }

    override suspend fun getBookmarks(): List<Bookmark> = withContext(Dispatchers.IO) {
        val bookmarksData = bookmarkDataSource.getAllBookmarks()
        bookmarksData.map { bookmarkMapper.mapToBookmark(it) }
    }

    override suspend fun getBookmarksForConnection(connectionId: String): List<Bookmark> = withContext(Dispatchers.IO) {
        val bookmarksData = bookmarkDataSource.getBookmarksForConnection(connectionId)
        bookmarksData.map { bookmarkMapper.mapToBookmark(it) }
    }

    override suspend fun isBookmarked(path: String, connectionId: String): Boolean = withContext(Dispatchers.IO) {
        bookmarkDataSource.isBookmarked(path, connectionId)
    }

    suspend fun toggleBookmark(path: String, name: String, connectionId: String): Boolean = withContext(Dispatchers.IO) {
        val isCurrentlyBookmarked = bookmarkDataSource.isBookmarked(path, connectionId)
        
        if (isCurrentlyBookmarked) {
            // Find and remove existing bookmark
            val allBookmarks = bookmarkDataSource.getBookmarksForConnection(connectionId)
            val existingBookmark = allBookmarks.find { it.path == path }
            existingBookmark?.let {
                bookmarkDataSource.deleteBookmark(it.id)
            }
            false
        } else {
            // Add new bookmark
            val newBookmark = Bookmark(
                id = generateBookmarkId(),
                name = name,
                path = path,
                connectionId = connectionId,
                createdAt = System.currentTimeMillis()
            )
            val bookmarkData = bookmarkMapper.mapToEntity(newBookmark)
            bookmarkDataSource.saveBookmark(bookmarkData)
            true
        }
    }

    suspend fun getBookmarkByPath(path: String, connectionId: String): Bookmark? = withContext(Dispatchers.IO) {
        val allBookmarks = bookmarkDataSource.getBookmarksForConnection(connectionId)
        val bookmarkData = allBookmarks.find { it.path == path }
        bookmarkData?.let { bookmarkMapper.mapToBookmark(it) }
    }

    override suspend fun updateBookmark(bookmark: Bookmark): Boolean = withContext(Dispatchers.IO) {
        val bookmarkData = bookmarkMapper.mapToEntity(bookmark)
        bookmarkDataSource.updateBookmark(bookmarkData)
    }

    suspend fun clearAllBookmarks() = withContext(Dispatchers.IO) {
        val allBookmarks = bookmarkDataSource.getAllBookmarks()
        allBookmarks.forEach { bookmark ->
            bookmarkDataSource.deleteBookmark(bookmark.id)
        }
    }

    private fun generateBookmarkId(): String {
        return "bookmark_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}