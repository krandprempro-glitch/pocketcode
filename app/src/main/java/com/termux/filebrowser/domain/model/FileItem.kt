package com.termux.filebrowser.domain.model

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val permissions: String = "",
    val owner: String = "",
    val group: String = ""
) {
    val displaySize: String get() = when (isDirectory) {
        true -> ""
        false -> size.formatFileSize()
    }
    
    val displayDate: String get() = lastModified.formatDate()
}

fun Long.formatFileSize(): String = when {
    this < 1024L -> "$this B"
    this < 1024L * 1024 -> "${this / 1024} KB"
    this < 1024L * 1024 * 1024 -> "${this / (1024 * 1024)} MB"
    else -> "${this / (1024 * 1024 * 1024)} GB"
}

fun Long.formatDate(): String {
    val date = java.util.Date(this)
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return formatter.format(date)
}