package com.termux.filebrowser.di

import com.termux.filebrowser.data.datasource.local.BookmarkDataSource
import com.termux.filebrowser.data.datasource.local.WorkspaceDataSource
import com.termux.filebrowser.data.datasource.remote.SftpDataSource
import com.termux.filebrowser.data.mapper.BookmarkMapper
import com.termux.filebrowser.data.mapper.FileItemMapper
import com.termux.filebrowser.data.mapper.WorkspaceMapper
import com.termux.filebrowser.data.repository.SftpRepositoryImpl
import com.termux.filebrowser.domain.repository.BookmarkRepository
import com.termux.filebrowser.domain.repository.SftpRepository
import com.termux.filebrowser.domain.repository.WorkspaceRepository
import com.termux.filebrowser.domain.usecase.bookmark.AddBookmarkUseCase
import com.termux.filebrowser.domain.usecase.bookmark.GetBookmarksUseCase
import com.termux.filebrowser.domain.usecase.bookmark.RemoveBookmarkUseCase
import com.termux.filebrowser.domain.usecase.bookmark.ToggleBookmarkUseCase
import com.termux.filebrowser.domain.usecase.connection.CheckConnectionUseCase
import com.termux.filebrowser.domain.usecase.connection.ConnectToServerUseCase
import com.termux.filebrowser.domain.usecase.connection.DisconnectUseCase
import com.termux.filebrowser.domain.usecase.file.DownloadFileUseCase
import com.termux.filebrowser.domain.usecase.file.GetFileDetailsUseCase
import com.termux.filebrowser.domain.usecase.file.ListFilesUseCase
import com.termux.filebrowser.domain.usecase.file.NavigateToDirectoryUseCase
import com.termux.filebrowser.presentation.viewmodel.RemoteFileBrowserViewModel

object FileBrowserModule {
    
    // Data Sources
    fun provideSftpDataSource(): SftpDataSource {
        // 这里需要具体实现，暂时返回接口
        // return SftpDataSourceImpl()
        throw NotImplementedError("SftpDataSource implementation needed")
    }
    
    fun provideWorkspaceDataSource(): WorkspaceDataSource {
        // 这里需要具体实现，暂时返回接口
        // return WorkspaceDataSourceImpl()
        throw NotImplementedError("WorkspaceDataSource implementation needed")
    }
    
    fun provideBookmarkDataSource(): BookmarkDataSource {
        // 这里需要具体实现，暂时返回接口
        // return BookmarkDataSourceImpl()
        throw NotImplementedError("BookmarkDataSource implementation needed")
    }
    
    // Mappers
    fun provideFileItemMapper(): FileItemMapper = FileItemMapper()
    
    fun provideWorkspaceMapper(): WorkspaceMapper = WorkspaceMapper()
    
    fun provideBookmarkMapper(): BookmarkMapper = BookmarkMapper()
    
    // Repositories
    fun provideSftpRepository(
        sftpDataSource: SftpDataSource = provideSftpDataSource(),
        mapper: FileItemMapper = provideFileItemMapper()
    ): SftpRepository = SftpRepositoryImpl(sftpDataSource, mapper)
    
    fun provideWorkspaceRepository(
        workspaceDataSource: WorkspaceDataSource = provideWorkspaceDataSource(),
        mapper: WorkspaceMapper = provideWorkspaceMapper()
    ): WorkspaceRepository {
        // return WorkspaceRepositoryImpl(workspaceDataSource, mapper)
        throw NotImplementedError("WorkspaceRepository implementation needed")
    }
    
    fun provideBookmarkRepository(
        bookmarkDataSource: BookmarkDataSource = provideBookmarkDataSource(),
        mapper: BookmarkMapper = provideBookmarkMapper()
    ): BookmarkRepository {
        // return BookmarkRepositoryImpl(bookmarkDataSource, mapper)
        throw NotImplementedError("BookmarkRepository implementation needed")
    }
    
    // Use Cases
    fun provideConnectToServerUseCase(
        sftpRepository: SftpRepository = provideSftpRepository(),
        workspaceRepository: WorkspaceRepository = provideWorkspaceRepository()
    ): ConnectToServerUseCase = ConnectToServerUseCase(sftpRepository, workspaceRepository)
    
    fun provideDisconnectUseCase(
        sftpRepository: SftpRepository = provideSftpRepository()
    ): DisconnectUseCase = DisconnectUseCase(sftpRepository)
    
    fun provideCheckConnectionUseCase(
        sftpRepository: SftpRepository = provideSftpRepository()
    ): CheckConnectionUseCase = CheckConnectionUseCase(sftpRepository)
    
    fun provideListFilesUseCase(
        sftpRepository: SftpRepository = provideSftpRepository()
    ): ListFilesUseCase = ListFilesUseCase(sftpRepository)
    
    fun provideNavigateToDirectoryUseCase(
        listFilesUseCase: ListFilesUseCase = provideListFilesUseCase(),
        workspaceRepository: WorkspaceRepository = provideWorkspaceRepository()
    ): NavigateToDirectoryUseCase = NavigateToDirectoryUseCase(listFilesUseCase, workspaceRepository)
    
    fun provideDownloadFileUseCase(
        sftpRepository: SftpRepository = provideSftpRepository()
    ): DownloadFileUseCase = DownloadFileUseCase(sftpRepository)
    
    fun provideGetFileDetailsUseCase(
        sftpRepository: SftpRepository = provideSftpRepository()
    ): GetFileDetailsUseCase = GetFileDetailsUseCase(sftpRepository)
    
    fun provideAddBookmarkUseCase(
        bookmarkRepository: BookmarkRepository = provideBookmarkRepository()
    ): AddBookmarkUseCase = AddBookmarkUseCase(bookmarkRepository)
    
    fun provideRemoveBookmarkUseCase(
        bookmarkRepository: BookmarkRepository = provideBookmarkRepository()
    ): RemoveBookmarkUseCase = RemoveBookmarkUseCase(bookmarkRepository)
    
    fun provideGetBookmarksUseCase(
        bookmarkRepository: BookmarkRepository = provideBookmarkRepository()
    ): GetBookmarksUseCase = GetBookmarksUseCase(bookmarkRepository)
    
    fun provideToggleBookmarkUseCase(
        bookmarkRepository: BookmarkRepository = provideBookmarkRepository(),
        addBookmarkUseCase: AddBookmarkUseCase = provideAddBookmarkUseCase()
    ): ToggleBookmarkUseCase = ToggleBookmarkUseCase(bookmarkRepository, addBookmarkUseCase)
    
    // ViewModel
    fun provideRemoteFileBrowserViewModel(): RemoteFileBrowserViewModel {
        return RemoteFileBrowserViewModel(
            connectToServerUseCase = provideConnectToServerUseCase(),
            disconnectUseCase = provideDisconnectUseCase(),
            checkConnectionUseCase = provideCheckConnectionUseCase(),
            navigateToDirectoryUseCase = provideNavigateToDirectoryUseCase(),
            downloadFileUseCase = provideDownloadFileUseCase(),
            addBookmarkUseCase = provideAddBookmarkUseCase(),
            removeBookmarkUseCase = provideRemoveBookmarkUseCase(),
            getBookmarksUseCase = provideGetBookmarksUseCase()
        )
    }
}