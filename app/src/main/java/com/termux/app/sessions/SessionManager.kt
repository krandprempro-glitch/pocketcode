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
        sessions.removeAll { it.id == sessionId }
        saveToStorage()
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

    fun getSession(sessionId: String): SessionInfo? {
        return sessions.find { it.id == sessionId }
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