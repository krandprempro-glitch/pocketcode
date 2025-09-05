package com.termux.filebrowser.domain.model

import com.termux.app.models.SSHConnectionConfig

data class Connection(
    val id: String,
    val config: SSHConnectionConfig,
    val isConnected: Boolean = false,
    val lastConnected: Long = 0L,
    val serverInfo: String = ""
)

data class ConnectionResult(
    val isConnected: Boolean,
    val serverInfo: String,
    val workspace: Workspace
)