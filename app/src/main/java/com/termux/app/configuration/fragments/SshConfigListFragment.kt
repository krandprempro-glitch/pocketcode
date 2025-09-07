package com.termux.app.configuration.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.termux.R
import com.termux.app.configuration.adapters.SshConfigAdapter
import com.termux.app.configuration.managers.ConfigNavigationManager
import com.termux.app.models.SSHConfigManager
import com.termux.app.models.SSHConnectionConfig
import com.termux.app.ssh.SSHConnectionManager
import com.termux.shared.logger.Logger

/**
 * SSH配置列表页面
 * 显示所有SSH连接配置，支持编辑、删除和测试连接
 */
class SshConfigListFragment : Fragment() {
    
    companion object {
        private const val LOG_TAG = "SshConfigListFragment"
        
        fun newInstance(): SshConfigListFragment {
            return SshConfigListFragment()
        }
    }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SshConfigAdapter
    private lateinit var btnNewConfig: MaterialButton
    
    private lateinit var sshConfigManager: SSHConfigManager
    private lateinit var navigationManager: ConfigNavigationManager
    
    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ssh_config_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        loadSSHConfigs()
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rv_ssh_configs)
        btnNewConfig = view.findViewById(R.id.btn_new_ssh_config)
        
        sshConfigManager = SSHConfigManager.getInstance(requireContext())
        navigationManager = ConfigNavigationManager.getInstance(requireActivity())
    }
    
    private fun setupRecyclerView() {
        adapter = SshConfigAdapter(requireContext())
        adapter.setOnConfigActionListener(object : SshConfigAdapter.OnConfigActionListener {
            override fun onEditConfig(config: SSHConnectionConfig) {
                navigationManager.navigateToSSHConfigDetail(config.name)
            }
            
            override fun onDeleteConfig(config: SSHConnectionConfig) {
                showDeleteConfirmDialog(config)
            }
            
            override fun onTestConnection(config: SSHConnectionConfig) {
                testSSHConnection(config)
            }
            
            override fun onUseConfig(config: SSHConnectionConfig) {
                useSSHConfig(config)
            }
        })
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }
    
    private fun setupClickListeners() {
        btnNewConfig.setOnClickListener {
            navigationManager.navigateToSSHConfigDetail(null)
        }
    }
    
    private fun loadSSHConfigs() {
        try {
            val configs = sshConfigManager.getAllConfigs()
            adapter.setConfigs(configs)
            
            Logger.logInfo(LOG_TAG, "Loaded ${configs.size} SSH configurations")
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to load SSH configurations: ${e.message}")
            Toast.makeText(context, "加载SSH配置失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 测试SSH连接
     */
    private fun testSSHConnection(config: SSHConnectionConfig) {
        try {
            val sshCommand = SSHConnectionManager.generateSSHCommand(config)
            if (sshCommand != null) {
                Toast.makeText(context, "连接命令: $sshCommand", Toast.LENGTH_LONG).show()
                Logger.logInfo(LOG_TAG, "Generated SSH command for ${config.name}: $sshCommand")
            } else {
                Toast.makeText(context, "生成连接命令失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to test SSH connection: ${e.message}")
            Toast.makeText(context, "测试连接失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 使用SSH配置进行连接
     */
    private fun useSSHConfig(config: SSHConnectionConfig) {
        try {
            sshConfigManager.setLastUsedConfig(config.name)
            val sshCommand = SSHConnectionManager.generateSSHCommand(config)
            
            if (sshCommand != null) {
                Toast.makeText(context, "已设置为默认连接，命令已生成", Toast.LENGTH_SHORT).show()
                Logger.logInfo(LOG_TAG, "Set ${config.name} as default SSH config")
                
                // 可以在这里添加自动切换到终端并执行命令的逻辑
                // navigationManager.navigateToTerminalWithCommand(sshCommand)
            } else {
                Toast.makeText(context, "生成连接命令失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to use SSH config: ${e.message}")
            Toast.makeText(context, "使用配置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(config: SSHConnectionConfig) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除SSH配置")
            .setMessage("确定要删除配置 \"${config.name}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteConfig(config)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 删除SSH配置
     */
    private fun deleteConfig(config: SSHConnectionConfig) {
        try {
            val success = sshConfigManager.deleteConfig(config.name)
            if (success) {
                Toast.makeText(context, "配置删除成功", Toast.LENGTH_SHORT).show()
                loadSSHConfigs() // 刷新列表
                Logger.logInfo(LOG_TAG, "Deleted SSH config: ${config.name}")
            } else {
                Toast.makeText(context, "配置删除失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to delete SSH config: ${e.message}")
            Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 刷新配置列表
     */
    fun refreshConfigs() {
        loadSSHConfigs()
    }
    
    override fun onResume() {
        super.onResume()
        // 从详情页面返回时刷新列表
        refreshConfigs()
    }
}
