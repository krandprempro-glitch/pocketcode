package com.termux.app.terminal

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.termux.app.sessions.SessionManager
import com.termux.app.sessions.SessionStatus
import com.termux.databinding.ActivityTerminalSessionBinding

class TerminalSessionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_SESSION_NAME = "session_name"
        const val EXTRA_SSH_CONFIG = "ssh_config"
        const val EXTRA_INITIAL_PATH = "initial_path"
    }

    private lateinit var binding: ActivityTerminalSessionBinding
    private var sessionId: String = ""
    private var sessionName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: run {
            finish()
            return
        }
        sessionName = intent.getStringExtra(EXTRA_SESSION_NAME) ?: "终端"

        setupToolbar()
        setupInputView()

        // Update session status to running
        SessionManager.updateSessionStatus(sessionId, SessionStatus.RUNNING, null)
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = sessionName
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
    }

    private fun setupInputView() {
        binding.terminalSendButton.setOnClickListener {
            val command = binding.terminalCommandInput.text?.toString() ?: ""
            if (command.isNotEmpty()) {
                sendCommand(command)
                binding.terminalCommandInput.text?.clear()
            }
        }
    }

    private fun sendCommand(command: String) {
        // Send command to terminal session
        // Update session manager with last command
        SessionManager.updateLastCommand(sessionId, command)

        // Detect Claude Code commands and update status
        when {
            command.contains("claude-code") || command.contains("claude") -> {
                SessionManager.updateSessionStatus(sessionId, SessionStatus.CLAUDE_THINKING, "启动中")
            }
            command.startsWith("/") -> {
                // AI command
                SessionManager.updateSessionStatus(sessionId, SessionStatus.CLAUDE_THINKING, "思考中")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SessionManager.updateSessionStatus(sessionId, SessionStatus.RUNNING, null)
    }

    override fun onPause() {
        super.onPause()
        SessionManager.updateSessionStatus(sessionId, SessionStatus.IDLE, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Session continues running even if activity destroyed
        // User can return to it from session list
    }
}