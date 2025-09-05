package com.termux.filebrowser.presentation.state

import com.termux.filebrowser.domain.model.FileItem

sealed class RemoteFileBrowserUiEffect {
    object CloseDrawer : RemoteFileBrowserUiEffect()
    data class ShowToast(val message: String) : RemoteFileBrowserUiEffect()
    data class ShowErrorDialog(val error: UiError) : RemoteFileBrowserUiEffect()
    data class NavigateToFileViewer(val file: FileItem) : RemoteFileBrowserUiEffect()
    data class StartFileDownload(val file: FileItem, val localPath: String) : RemoteFileBrowserUiEffect()
}