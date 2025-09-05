package com.termux.filebrowser.domain.usecase.bookmark

import com.termux.filebrowser.domain.repository.BookmarkRepository

class ToggleBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository,
    private val addBookmarkUseCase: AddBookmarkUseCase
) {
    suspend operator fun invoke(path: String, name: String, connectionId: String): Boolean {
        return if (bookmarkRepository.isBookmarked(path, connectionId)) {
            // 如果已存在书签，则移除
            val bookmarks = bookmarkRepository.getBookmarksForConnection(connectionId)
            val bookmark = bookmarks.find { it.path == path }
            bookmark?.let { 
                bookmarkRepository.removeBookmark(it.id) 
            } ?: false
        } else {
            // 如果不存在书签，则添加
            addBookmarkUseCase(path, name, connectionId)
        }
    }
}