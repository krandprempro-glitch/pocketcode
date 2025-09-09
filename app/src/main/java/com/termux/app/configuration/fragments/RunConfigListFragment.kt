package com.termux.app.configuration.fragments

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.termux.R
import com.termux.app.configuration.adapters.RunConfigAdapter
import com.termux.app.configuration.activities.RunConfigDetailActivity
import com.termux.app.configuration.managers.RunConfigurationManager
import com.termux.app.configuration.models.RunConfiguration
import com.termux.app.configuration.utils.CommandBuilder
import com.termux.shared.logger.Logger
import com.termux.app.floating.services.RemoteCommandExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 运行配置列表页面
 * 显示所有运行配置，支持编辑、删除、复制命令和快速运行
 */
class RunConfigListFragment : Fragment() {
    
    companion object {
        private const val LOG_TAG = "RunConfigListFragment"
        
        fun newInstance(): RunConfigListFragment {
            return RunConfigListFragment()
        }
    }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RunConfigAdapter
    private lateinit var btnNewConfig: MaterialButton
    private lateinit var tvEmptyState: TextView
    
    private lateinit var configManager: RunConfigurationManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_run_config_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        loadRunConfigs()
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rv_run_configs)
        btnNewConfig = view.findViewById(R.id.btn_new_run_config)
        tvEmptyState = view.findViewById(R.id.tv_empty_state)
        
        configManager = RunConfigurationManager.getInstance(requireContext())
    }
    
    private fun setupRecyclerView() {
        adapter = RunConfigAdapter(requireContext())
        adapter.setOnConfigActionListener(object : RunConfigAdapter.OnConfigActionListener {
            override fun onEditConfig(config: RunConfiguration) {
                val intent = RunConfigDetailActivity.newIntent(requireContext(), config.id)
                startActivity(intent)
            }
            
            override fun onDeleteConfig(config: RunConfiguration) {
                showDeleteConfirmDialog(config)
            }
            
            override fun onCopyCommand(config: RunConfiguration) {
                // 改为停止：直接执行停止指令
                stopRemoteProcess(config)
            }
            
            override fun onQuickRun(config: RunConfiguration) {
                showQuickRunDialog(config)
            }
            
            override fun onDuplicateConfig(config: RunConfiguration) {
                duplicateConfig(config)
            }
        })
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    
    private fun setupClickListeners() {
        btnNewConfig.setOnClickListener {
            val intent = RunConfigDetailActivity.newIntent(requireContext(), null)
            startActivity(intent)
        }
    }
    
    private fun loadRunConfigs() {
        try {
            val configs = configManager.getAllConfigurations()
            adapter.setConfigurations(configs)
            
            // 显示/隐藏空状态
            val isEmpty = configs.isEmpty()
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            
            Logger.logInfo(LOG_TAG, "Loaded ${configs.size} run configurations")
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to load run configurations: ${e.message}")
            Toast.makeText(context, "加载运行配置失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 复制命令到剪贴板
     */
    private fun copyCommandToClipboard(config: RunConfiguration) {
        try {
            val command = CommandBuilder.buildCompleteCommand(config)
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("运行命令", command)
            clipboard.setPrimaryClip(clip)
            
            Toast.makeText(context, "运行命令已复制到剪贴板", Toast.LENGTH_SHORT).show()
            Logger.logInfo(LOG_TAG, "Command copied to clipboard: ${config.name}")
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to copy command: ${e.message}")
            Toast.makeText(context, "复制失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 执行停止指令（通过SSH远程执行）
     */
    private fun stopRemoteProcess(config: RunConfiguration) {
        try {
            val stopCmd = CommandBuilder.buildStopCommand(config)
            val executor = RemoteCommandExecutor.getInstance(requireContext())

            // 在后台线程执行停止命令
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val tempConfig = config.copy(
                        id = System.currentTimeMillis().toString(),
                        command = stopCmd.substringAfter("&& "),
                        runInBackground = false
                    )
                    val result = executor.executeConfiguration(tempConfig)
                    withContext(Dispatchers.Main) {
                        val msg = if (result.isSuccess()) "已发送停止信号" else (result.errorMessage.ifBlank { "停止失败" })
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "停止失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to build stop command: ${e.message}")
            Toast.makeText(context, "生成停止命令失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 显示快速运行对话框
     */
    private fun showQuickRunDialog(config: RunConfiguration) {
        try {
            val command = CommandBuilder.buildCompleteCommand(config)
            
            AlertDialog.Builder(requireContext())
                .setTitle("快速运行")
                .setMessage("确定要运行以下命令吗？\n\n配置: ${config.name}\n命令: $command")
                .setPositiveButton("运行") { _, _ ->
                    executeQuickRun(config, command)
                }
                .setNeutralButton("复制命令") { _, _ ->
                    copyCommandToClipboard(config)
                }
                .setNegativeButton("取消", null)
                .show()
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to show quick run dialog: ${e.message}")
            Toast.makeText(context, "生成命令失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 执行快速运行
     */
    private fun executeQuickRun(config: RunConfiguration, command: String) {
        try {
            // 更新最后使用时间
            config.lastUsedTime = System.currentTimeMillis()
            configManager.saveConfiguration(config)
            
            // TODO: 这里应该实现实际的远程命令执行逻辑
            // 可能需要：
            // 1. 获取对应的SSH配置
            // 2. 建立SSH连接
            // 3. 执行远程命令
            // 4. 显示执行结果
            
            Toast.makeText(context, "快速运行功能待实现", Toast.LENGTH_LONG).show()
            Logger.logInfo(LOG_TAG, "Quick run requested for: ${config.name}")
            
            // 刷新列表以更新最后使用时间
            loadRunConfigs()
            
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to execute quick run: ${e.message}")
            Toast.makeText(context, "执行失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 复制配置
     */
    private fun duplicateConfig(config: RunConfiguration) {
        try {
            val duplicatedConfig = config.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = "${config.name} - 副本",
                createdTime = System.currentTimeMillis(),
                lastUsedTime = 0L
            )
            
            val success = configManager.saveConfiguration(duplicatedConfig)
            if (success) {
                Toast.makeText(context, "配置已复制", Toast.LENGTH_SHORT).show()
                loadRunConfigs() // 刷新列表
                Logger.logInfo(LOG_TAG, "Config duplicated: ${config.name}")
            } else {
                Toast.makeText(context, "复制配置失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to duplicate config: ${e.message}")
            Toast.makeText(context, "复制失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(config: RunConfiguration) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除运行配置")
            .setMessage("确定要删除配置 \"${config.name}\" 吗？\n\n此操作不可撤销。")
            .setPositiveButton("删除") { _, _ ->
                deleteConfig(config)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 删除配置
     */
    private fun deleteConfig(config: RunConfiguration) {
        try {
            val success = configManager.deleteConfiguration(config.id)
            if (success) {
                Toast.makeText(context, "配置删除成功", Toast.LENGTH_SHORT).show()
                loadRunConfigs() // 刷新列表
                Logger.logInfo(LOG_TAG, "Config deleted: ${config.name}")
            } else {
                Toast.makeText(context, "配置删除失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to delete config: ${e.message}")
            Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 刷新配置列表
     */
    fun refreshConfigs() {
        loadRunConfigs()
    }
    
    override fun onResume() {
        super.onResume()
        // 从详情页面返回时刷新列表
        refreshConfigs()
    }
}
