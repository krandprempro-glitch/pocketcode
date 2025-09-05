package com.termux.filebrowser.domain.usecase.bookmark

import com.termux.filebrowser.domain.repository.BookmarkRepository

class RemoveBookmarkUseCase(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(bookmarkId: String): Boolean {
        return bookmarkRepository.removeBookmark(bookmarkId)
    }
}