package com.termux.filebrowser.domain.model

data class Workspace(
    val id: String,
    val connectionId: String,
    val name: String,
    val currentPath: String = "/",
    val navigationHistory: List<String> = emptyList(),
    val expandedDirectories: Set<String> = emptySet(),
    val lastAccessed: Long = System.currentTimeMillis()
)

data class NavigationResult(
    val path: String,
    val files: List<FileItem>,
    val canGoBack: Boolean
)