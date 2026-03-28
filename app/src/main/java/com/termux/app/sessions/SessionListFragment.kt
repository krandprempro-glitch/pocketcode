package com.termux.app.sessions

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.termux.R
import com.termux.databinding.FragmentSessionListBinding
import com.termux.app.managers.ScriptManager
import com.termux.app.models.ScriptItem
import com.termux.app.sftp.SFTPConnectionManager
import com.termux.app.terminal.FullTerminalActivity
import com.termux.app.ui.SSHConfigDialog
import com.termux.shared.logger.Logger

class SessionListFragment : Fragment() {

    private var _binding: FragmentSessionListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SessionManager.init(requireContext())

        setupToolbar()
        setupRecyclerView()
        setupFab()
        updateEmptyState()
    }

    private fun setupToolbar() {
        binding.btnSettings.setOnClickListener { view ->
            val popup = PopupMenu(requireContext(), view)
            popup.menuInflater.inflate(R.menu.menu_session_list, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_init_ssh -> {
                        openInitTerminal()
                        true
                    }
                    R.id.action_ssh_config -> {
                        showSshConfigDialog()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        binding.btnScripts.setOnClickListener {
            showScriptSelectionDialog()
        }
    }

    private fun showScriptSelectionDialog() {
        val scripts = ScriptManager.getInstance().getScripts(requireContext())
        if (scripts.isEmpty()) {
            Toast.makeText(requireContext(), "没有可用的脚本", Toast.LENGTH_SHORT).show()
            return
        }

        val names = scripts.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("选择脚本")
            .setItems(names) { _, which ->
                val script = scripts[which]
                executeScriptRemotely(script)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun executeScriptRemotely(script: ScriptItem) {
        val sftpManager = SFTPConnectionManager.getInstance()
        if (!sftpManager.isConnected) {
            Toast.makeText(requireContext(), "请先建立SSH连接", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(), "正在执行脚本: ${script.name}", Toast.LENGTH_SHORT).show()

        // Escape script content for bash -c
        val escapedContent = script.content
            .replace("\\", "\\\\")
            .replace("'", "'\\''")

        val command = "bash -c '$escapedContent'"

        sftpManager.executeCommand(command)
            .subscribe(
                { result ->
                    Logger.logDebug("ScriptManager", "Script output: $result")
                    Toast.makeText(requireContext(), "脚本执行完成", Toast.LENGTH_SHORT).show()
                },
                { error ->
                    Logger.logError("ScriptManager", "Script error: ${error.message}")
                    Toast.makeText(requireContext(), "脚本执行失败: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
    }

    private fun openInitTerminal() {
        // 创建会话，这样会在列表中持续保留状态
        val session = SessionManager.createSession(
            name = "SSH 初始化",
            sshConfigName = null,
            path = ""
        )
        val intent = Intent(requireContext(), FullTerminalActivity::class.java).apply {
            putExtra(FullTerminalActivity.EXTRA_SESSION_ID, session.id)
            putExtra(FullTerminalActivity.EXTRA_SESSION_NAME, session.name)
            putExtra(FullTerminalActivity.EXTRA_INITIAL_COMMAND, "pkg install openssh sshpass -y")
        }
        startActivity(intent)
    }

    private fun showSshConfigDialog() {
        val dialog = SSHConfigDialog(requireContext())
        dialog.setOnSSHConfigListener(object : SSHConfigDialog.OnSSHConfigListener {
            override fun onSSHConnect(config: com.termux.app.models.SSHConnectionConfig?) {
                // 连接并打开终端
            }
            override fun onSSHConfigSaved(config: com.termux.app.models.SSHConnectionConfig?) {}
            override fun onSSHConfigDeleted(configName: String?) {}
            override fun onDialogClosed() {}
        })
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        refreshSessions()
    }

    private fun setupRecyclerView() {
        adapter = SessionAdapter(
            onSessionClick = { session ->
                openTerminalSession(session)
            },
            onSessionLongClick = { session ->
                showSessionOptions(session)
                true
            }
        )

        binding.sessionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@SessionListFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabNewSession.setOnClickListener {
            if (!SessionManager.isSshInitialized()) {
                showSshInitRequiredDialog()
            } else {
                showNewSessionDialog()
            }
        }
    }

    private fun showSshInitRequiredDialog() {
        AlertDialog.Builder(requireContext(), R.style.AppTheme_Dialog_Dark)
            .setTitle("创建终端")
            .setMessage("首次使用需要先初始化 SSH 环境，是否立即初始化？")
            .setPositiveButton("确认初始化") { _, _ ->
                SessionManager.setSshInitialized(true)
                openInitTerminal()
            }
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .show()
    }

    private fun refreshSessions() {
        adapter.submitList(SessionManager.getAllSessions())
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val isEmpty = SessionManager.getAllSessions().isEmpty()
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.sessionsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showNewSessionDialog() {
        val dialog = NewSessionDialog(requireContext()) { name, sshConfig, path ->
            val session = SessionManager.createSession(name, sshConfig, path)
            openTerminalSession(session)
        }
        dialog.show()
    }

    private fun openTerminalSession(session: SessionInfo) {
        val intent = Intent(requireContext(), FullTerminalActivity::class.java).apply {
            putExtra(FullTerminalActivity.EXTRA_SESSION_ID, session.id)
            putExtra(FullTerminalActivity.EXTRA_SESSION_NAME, session.name)
            putExtra(FullTerminalActivity.EXTRA_SSH_CONFIG, session.sshConfigName)
            putExtra(FullTerminalActivity.EXTRA_INITIAL_PATH, session.currentPath)
            session.terminalHandle?.let { putExtra(FullTerminalActivity.EXTRA_SESSION_HANDLE, it) }
        }
        startActivity(intent)
    }

    private fun showSessionOptions(session: SessionInfo) {
        AlertDialog.Builder(requireContext())
            .setTitle(session.name)
            .setItems(arrayOf("关闭会话")) { _, _ ->
                SessionManager.closeSession(session.id)
                refreshSessions()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}