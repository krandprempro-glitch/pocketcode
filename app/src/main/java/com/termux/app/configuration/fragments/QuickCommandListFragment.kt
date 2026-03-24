package com.termux.app.configuration.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.termux.R
import com.termux.app.MainTabActivity
import com.termux.app.configuration.adapters.QuickCommandAdapter
import com.termux.app.configuration.managers.QuickCommandManager
import com.termux.app.configuration.models.QuickCommand
import com.termux.shared.logger.Logger

/**
 * 常用指令列表页面
 * 显示所有常用指令，支持编辑、删除、搜索和发送到终端执行
 */
class QuickCommandListFragment : Fragment() {

    companion object {
        private const val LOG_TAG = "QuickCommandListFragment"

        fun newInstance(): QuickCommandListFragment {
            return QuickCommandListFragment()
        }
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: QuickCommandAdapter
    private lateinit var btnNewCommand: MaterialButton
    private lateinit var tvEmptyState: TextView
    private lateinit var searchView: SearchView

    private lateinit var commandManager: QuickCommandManager
    private var allCommands: List<QuickCommand> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quick_command_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupSearchView()
        setupClickListeners()
        loadCommands()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rv_quick_commands)
        btnNewCommand = view.findViewById(R.id.btn_new_quick_command)
        tvEmptyState = view.findViewById(R.id.tv_empty_state)
        searchView = view.findViewById(R.id.search_view)

        commandManager = QuickCommandManager.getInstance(requireContext())
    }

    private fun setupRecyclerView() {
        adapter = QuickCommandAdapter(requireContext())
        adapter.setOnCommandActionListener(object : QuickCommandAdapter.OnCommandActionListener {
            override fun onExecuteCommand(command: QuickCommand) {
                showExecuteConfirmDialog(command)
            }

            override fun onEditCommand(command: QuickCommand) {
                openEditFragment(command.id)
            }

            override fun onDeleteCommand(command: QuickCommand) {
                showDeleteConfirmDialog(command)
            }

            override fun onDuplicateCommand(command: QuickCommand) {
                duplicateCommand(command)
            }
        })

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterCommands(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterCommands(newText)
                return true
            }
        })
    }

    private fun setupClickListeners() {
        btnNewCommand.setOnClickListener {
            openEditFragment(null)
        }
    }

    private fun loadCommands() {
        try {
            allCommands = commandManager.getAllCommands()
            adapter.setCommands(allCommands)
            updateEmptyState()
            Logger.logInfo(LOG_TAG, "Loaded ${allCommands.size} quick commands")
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to load quick commands: ${e.message}")
            Toast.makeText(context, "加载常用指令失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filterCommands(query: String?) {
        val filtered = if (query.isNullOrBlank()) {
            allCommands
        } else {
            allCommands.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.command.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
        }
        adapter.setCommands(filtered)
        updateEmptyState(filtered.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean = allCommands.isEmpty()) {
        if (isEmpty && allCommands.isNotEmpty()) {
            // 搜索无结果
            tvEmptyState.text = "未找到匹配的指令"
            tvEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmptyState.text = "暂无常用指令\n\n点击上方「新建」按钮创建您的第一个常用指令"
            tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    /**
     * 显示执行确认对话框
     */
    private fun showExecuteConfirmDialog(command: QuickCommand) {
        AlertDialog.Builder(requireContext())
            .setTitle("执行指令")
            .setMessage("确定要执行以下指令吗？\n\n名称: ${command.name}\n命令: ${command.command}")
            .setPositiveButton("执行") { _, _ ->
                executeCommand(command)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 执行指令：发送到终端
     */
    private fun executeCommand(command: QuickCommand) {
        try {
            // 发送到终端
            (activity as? MainTabActivity)?.sendCommandToTerminal(command.command)

            // 更新使用统计
            commandManager.updateLastUsed(command.id)

            Toast.makeText(context, "指令已发送到终端", Toast.LENGTH_SHORT).show()
            Logger.logInfo(LOG_TAG, "Command executed: ${command.name}")

            // 刷新列表以更新使用次数和时间
            loadCommands()
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to execute command: ${e.message}")
            Toast.makeText(context, "执行失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(command: QuickCommand) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除常用指令")
            .setMessage("确定要删除指令 \"${command.name}\" 吗？\n\n此操作不可撤销。")
            .setPositiveButton("删除") { _, _ ->
                deleteCommand(command)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 删除指令
     */
    private fun deleteCommand(command: QuickCommand) {
        try {
            val success = commandManager.deleteCommand(command.id)
            if (success) {
                Toast.makeText(context, "指令删除成功", Toast.LENGTH_SHORT).show()
                loadCommands()
                Logger.logInfo(LOG_TAG, "Command deleted: ${command.name}")
            } else {
                Toast.makeText(context, "指令删除失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to delete command: ${e.message}")
            Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 复制指令
     */
    private fun duplicateCommand(command: QuickCommand) {
        try {
            val duplicatedCommand = command.copy(
                id = "",
                name = "${command.name} - 副本",
                createdTime = System.currentTimeMillis(),
                lastUsedTime = 0L,
                useCount = 0
            )

            val success = commandManager.saveCommand(duplicatedCommand)
            if (success) {
                Toast.makeText(context, "指令已复制", Toast.LENGTH_SHORT).show()
                loadCommands()
                Logger.logInfo(LOG_TAG, "Command duplicated: ${command.name}")
            } else {
                Toast.makeText(context, "复制指令失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to duplicate command: ${e.message}")
            Toast.makeText(context, "复制失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 打开编辑页面
     */
    private fun openEditFragment(commandId: String?) {
        val fragment = QuickCommandEditFragment.newInstance(commandId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    /**
     * 刷新指令列表
     */
    fun refreshCommands() {
        loadCommands()
    }

    override fun onResume() {
        super.onResume()
        // 从编辑页面返回时刷新列表
        refreshCommands()
    }
}
