package com.termux.filebrowser.domain.usecase.bookmark

import com.termux.filebrowser.domain.model.Bookmark
import com.termux.filebrowser.domain.repository.BookmarkRepository
import java.util.UUID

class AddBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(path: String, name: String, connectionId: String): Boolean {
        // 检查是否已经存在相同的书签
        if (bookmarkRepository.isBookmarked(path, connectionId)) {
            return false // 已存在，不重复添加
        }
        
        val bookmark = Bookmark(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { path.split("/").lastOrNull() ?: path },
            path = path,
            connectionId = connectionId,
            createdAt = System.currentTimeMillis()
        )
        
        return bookmarkRepository.addBookmark(bookmark)
    }
}