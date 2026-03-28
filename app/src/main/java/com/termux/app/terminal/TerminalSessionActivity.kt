package com.termux.app.terminal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.termux.app.TermuxService
import com.termux.app.sessions.SessionManager
import com.termux.app.sessions.SessionStatus
import com.termux.databinding.ActivityTerminalSessionBinding
import com.termux.shared.logger.Logger
import com.termux.shared.termux.terminal.TermuxTerminalViewClientBase
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalViewClient

class TerminalSessionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_SESSION_NAME = "session_name"
        const val EXTRA_SSH_CONFIG = "ssh_config"
        const val EXTRA_INITIAL_PATH = "initial_path"
        private const val LOG_TAG = "TerminalSessionActivity"
    }

    private lateinit var binding: ActivityTerminalSessionBinding
    private var sessionId: String = ""
    private var sessionName: String = ""

    // TermuxService connection
    private var termuxService: TermuxService? = null
    private var serviceBound = false

    // Terminal session
    private var terminalSession: TerminalSession? = null

    // TerminalView client - using base class for default implementations
    private val terminalViewClient = object : TermuxTerminalViewClientBase() {
        override fun isTerminalViewSelected(): Boolean = true
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TermuxService.LocalBinder
            termuxService = binder.service
            serviceBound = true
            onServiceConnected()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            termuxService = null
            serviceBound = false
        }
    }

    private var pendingTerminalSession: TerminalSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide the app's default action bar
        supportActionBar?.hide()

        // Keep screen on while terminal is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: run {
            finish()
            return
        }
        sessionName = intent.getStringExtra(EXTRA_SESSION_NAME) ?: "终端"

        setupToolbar()
        setupInputView()
        setupTerminalView()

        // Bind to TermuxService
        bindToTermuxService()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = sessionName
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
    }

    private fun setupTerminalView() {
        binding.terminalView.apply {
            setTerminalViewClient(terminalViewClient)
            isFocusable = true
            isFocusableInTouchMode = true
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

        // Handle keyboard action
        binding.terminalCommandInput.setOnEditorActionListener { _, _, _ ->
            val command = binding.terminalCommandInput.text?.toString() ?: ""
            if (command.isNotEmpty()) {
                sendCommand(command)
                binding.terminalCommandInput.text?.clear()
            }
            true
        }
    }

    private fun bindToTermuxService() {
        val intent = Intent(this, TermuxService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun onServiceConnected() {
        if (terminalSession == null) {
            createTerminalSession()
        } else {
            // Attach existing session
            attachTerminalSession(terminalSession!!)
        }
    }

    private fun attachTerminalSession(session: TerminalSession) {
        // Use post() to ensure view is laid out before attaching
        binding.terminalView.post {
            try {
                binding.terminalView.attachSession(session)
                binding.terminalView.onScreenUpdated()
                Logger.logDebug(LOG_TAG, "Terminal session attached")
            } catch (e: Exception) {
                Logger.logError(LOG_TAG, "Failed to attach session: ${e.message}")
                // Retry after a short delay
                binding.terminalView.postDelayed({
                    try {
                        binding.terminalView.attachSession(session)
                        binding.terminalView.onScreenUpdated()
                    } catch (e2: Exception) {
                        Logger.logError(LOG_TAG, "Retry failed: ${e2.message}")
                    }
                }, 100)
            }
        }
    }

    private fun createTerminalSession() {
        val service = termuxService ?: return

        binding.loadingIndicator.visibility = android.view.View.VISIBLE

        val initialPath = intent.getStringExtra(EXTRA_INITIAL_PATH) ?: ""

        // Create terminal session through TermuxService
        val newTermuxSession = service.createTermuxSession(
            null, // executablePath - use default shell
            null, // arguments
            null, // stdin
            initialPath.ifEmpty { null }, // workingDirectory
            false, // isFailSafe
            sessionName // sessionName
        )

        if (newTermuxSession != null) {
            terminalSession = newTermuxSession.terminalSession

            // Attach to terminal view using post() to ensure view is ready
            attachTerminalSession(terminalSession!!)

            // Update session manager
            SessionManager.updateLastCommand(sessionId, "会话已启动")
            SessionManager.updateSessionStatus(sessionId, SessionStatus.RUNNING, null)

            Logger.logDebug(LOG_TAG, "Terminal session created: ${terminalSession?.mHandle}")
        } else {
            Toast.makeText(this, "创建会话失败", Toast.LENGTH_SHORT).show()
            Logger.logError(LOG_TAG, "Failed to create terminal session")
        }

        binding.loadingIndicator.visibility = android.view.View.GONE
    }

    private fun sendCommand(command: String) {
        val session = terminalSession ?: return

        // Append newline if not present
        val fullCommand = if (command.endsWith("\n")) command else "$command\n"

        // Write to terminal using UTF-8 encoding
        try {
            session.write(fullCommand.toByteArray(Charsets.UTF_8), 0, fullCommand.length)
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to write command: ${e.message}")
        }

        // Update session manager
        SessionManager.updateLastCommand(sessionId, command)

        // Detect special commands and update status
        when {
            command.contains("claude-code") || command.contains("claude") -> {
                SessionManager.updateSessionStatus(sessionId, SessionStatus.CLAUDE_THINKING, "启动中")
            }
            command.startsWith("/") -> {
                SessionManager.updateSessionStatus(sessionId, SessionStatus.CLAUDE_THINKING, "思考中")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (serviceBound && terminalSession != null) {
            binding.terminalView.onScreenUpdated()
        }
        SessionManager.updateSessionStatus(sessionId, SessionStatus.RUNNING, null)
    }

    override fun onPause() {
        super.onPause()
        SessionManager.updateSessionStatus(sessionId, SessionStatus.IDLE, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Clear terminal session reference
        terminalSession = null

        // Unbind from service
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }
}
