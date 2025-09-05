package com.termux.filebrowser.data.mapper

import com.termux.filebrowser.data.datasource.local.BookmarkEntity
import com.termux.filebrowser.domain.model.Bookmark

class BookmarkMapper {
    fun mapToBookmark(entity: BookmarkEntity): Bookmark {
        return Bookmark(
            id = entity.id,
            name = entity.name,
            path = entity.path,
            connectionId = entity.connectionId,
            createdAt = entity.createdAt
        )
    }
    
    fun mapToEntity(bookmark: Bookmark): BookmarkEntity {
        return BookmarkEntity(
            id = bookmark.id,
            name = bookmark.name,
            path = bookmark.path,
            connectionId = bookmark.connectionId,
            createdAt = bookmark.createdAt
        )
    }
    
    fun mapToBookmarkList(entities: List<BookmarkEntity>): List<Bookmark> {
        return entities.map { mapToBookmark(it) }
    }
}