package com.termux.app.sessions

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.termux.app.databinding.FragmentSessionListBinding
import com.termux.app.terminal.TerminalSessionActivity

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

        setupRecyclerView()
        setupFab()
        updateEmptyState()
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
            showNewSessionDialog()
        }
    }

    private fun refreshSessions() {
        adapter.submitList(SessionManager.allSessions)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val isEmpty = SessionManager.allSessions.isEmpty()
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
        val intent = Intent(requireContext(), TerminalSessionActivity::class.java).apply {
            putExtra(TerminalSessionActivity.EXTRA_SESSION_ID, session.id)
            putExtra(TerminalSessionActivity.EXTRA_SESSION_NAME, session.name)
            putExtra(TerminalSessionActivity.EXTRA_SSH_CONFIG, session.sshConfigName)
            putExtra(TerminalSessionActivity.EXTRA_INITIAL_PATH, session.currentPath)
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