package com.termux.app.sessions

data class SessionInfo(
    val id: String,
    val name: String,
    val sshConfigName: String?,
    val currentPath: String,
    val lastCommand: String,
    val userPrompt: String?,
    val status: SessionStatus,
    val statusDetail: String?,
    val startTime: Long,
    val lastActiveTime: Long
)

enum class SessionStatus {
    IDLE,
    RUNNING,
    CLAUDE_THINKING,
    CLAUDE_WORKING,
    WAITING_INPUT,
    ERROR
}