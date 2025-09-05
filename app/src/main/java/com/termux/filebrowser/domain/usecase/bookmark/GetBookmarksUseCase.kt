package com.termux.filebrowser.domain.usecase.bookmark

import com.termux.filebrowser.domain.model.Bookmark
import com.termux.filebrowser.domain.repository.BookmarkRepository

class GetBookmarksUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(connectionId: String? = null): List<Bookmark> {
        return if (connectionId != null) {
            bookmarkRepository.getBookmarksForConnection(connectionId)
        } else {
            bookmarkRepository.getBookmarks()
        }
    }
}