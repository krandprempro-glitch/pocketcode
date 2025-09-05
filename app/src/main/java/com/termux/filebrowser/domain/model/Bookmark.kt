package com.termux.filebrowser.domain.model

data class Bookmark(
    val id: String,
    val name: String,
    val path: String,
    val connectionId: String,
    val createdAt: Long = System.currentTimeMillis()
)