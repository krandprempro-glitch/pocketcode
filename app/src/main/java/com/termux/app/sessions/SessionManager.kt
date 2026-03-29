package com.termux.app.sessions

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SessionManager {
    private const val PREFS_NAME = "termux_sessions"
    private const val KEY_SESSIONS = "sessions"

    private lateinit var prefs: SharedPreferences
    private val sessions = mutableListOf<SessionInfo>()
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromStorage()
    }

    fun createSession(name: String, sshConfigName: String?, path: String): SessionInfo {
        val session = SessionInfo(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            sshConfigName = sshConfigName,
            currentPath = path,
            lastCommand = "",
            userPrompt = null,
            status = SessionStatus.IDLE,
            statusDetail = null,
            startTime = System.currentTimeMillis(),
            lastActiveTime = System.currentTimeMillis()
        )
        sessions.add(0, session)
        saveToStorage()
        return session
    }

    fun closeSession(sessionId: String) {
        // 先找到 session，获取 terminalHandle 用于关闭终端
        val session = sessions.find { it.id == sessionId }
        val handle = session?.terminalHandle

        // 从列表移除
        sessions.removeAll { it.id == sessionId }
        saveToStorage()

        // 如果有 terminalHandle，尝试关闭终端进程
        if (handle != null) {
            Thread {
                try {
                    // 尝试用 ps 找到相关进程并 kill
                    val pid = findPidByHandle(handle)
                    if (pid > 0) {
                        // 先尝试正常退出
                        Runtime.getRuntime().exec(arrayOf("kill", "-HUP", pid.toString())).waitFor()
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }.start()
        }
    }

    private fun findPidByHandle(handle: String): Int {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "ps -ef | grep '$handle' | grep -v grep | awk '{print \$2}' | head -1"))
            val reader = process.inputStream.bufferedReader()
            val line = reader.readLine()?.trim()
            reader.close()
            line?.toIntOrNull() ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    fun updateSessionStatus(sessionId: String, status: SessionStatus, detail: String?) {
        sessions.indexOfFirst { it.id == sessionId }.takeIf { it >= 0 }?.let { index ->
            sessions[index] = sessions[index].copy(
                status = status,
                statusDetail = detail,
                lastActiveTime = System.currentTimeMillis()
            )
            saveToStorage()
        }
    }

    fun updateSessionPrompt(sessionId: String, prompt: String) {
        sessions.indexOfFirst { it.id == sessionId }.takeIf { it >= 0 }?.let { index ->
            sessions[index] = sessions[index].copy(
                userPrompt = prompt,
                lastActiveTime = System.currentTimeMillis()
            )
            saveToStorage()
        }
    }

    fun updateLastCommand(sessionId: String, command: String) {
        sessions.indexOfFirst { it.id == sessionId }.takeIf { it >= 0 }?.let { index ->
            sessions[index] = sessions[index].copy(
                lastCommand = command,
                lastActiveTime = System.currentTimeMillis()
            )
            saveToStorage()
        }
    }

    fun updateTerminalHandle(sessionId: String, handle: String) {
        sessions.indexOfFirst { it.id == sessionId }.takeIf { it >= 0 }?.let { index ->
            sessions[index] = sessions[index].copy(
                terminalHandle = handle,
                lastActiveTime = System.currentTimeMillis()
            )
            saveToStorage()
        }
    }

    fun renameSession(sessionId: String, newName: String) {
        sessions.indexOfFirst { it.id == sessionId }.takeIf { it >= 0 }?.let { index ->
            sessions[index] = sessions[index].copy(
                name = newName,
                lastActiveTime = System.currentTimeMillis()
            )
            saveToStorage()
        }
    }

    fun getSession(sessionId: String): SessionInfo? {
        return sessions.find { it.id == sessionId }
    }

    fun getSessionByHandle(handle: String): SessionInfo? {
        return sessions.find { it.terminalHandle == handle }
    }

    fun getAllSessions(): List<SessionInfo> = sessions.toList()

    private fun saveToStorage() {
        val json = gson.toJson(sessions)
        prefs.edit().putString(KEY_SESSIONS, json).apply()
    }

    private fun loadFromStorage() {
        val json = prefs.getString(KEY_SESSIONS, null)
        if (json != null) {
            val type = object : TypeToken<List<SessionInfo>>() {}.type
            sessions.clear()
            sessions.addAll(gson.fromJson(json, type))
        }
    }
}