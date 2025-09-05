package com.termux.filebrowser.data.mapper

import com.termux.filebrowser.data.datasource.remote.RemoteFileInfo
import com.termux.filebrowser.domain.model.FileItem

class FileItemMapper {
    fun mapToFileItem(remoteFileInfo: RemoteFileInfo): FileItem {
        return FileItem(
            name = remoteFileInfo.name,
            path = remoteFileInfo.path,
            isDirectory = remoteFileInfo.isDirectory,
            size = remoteFileInfo.size,
            lastModified = remoteFileInfo.lastModified,
            permissions = remoteFileInfo.permissions,
            owner = remoteFileInfo.owner,
            group = remoteFileInfo.group
        )
    }
    
    fun mapToFileItemList(remoteFiles: List<RemoteFileInfo>): List<FileItem> {
        return remoteFiles.map { mapToFileItem(it) }
    }
}