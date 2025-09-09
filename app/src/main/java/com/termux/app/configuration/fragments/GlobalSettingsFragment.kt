package com.termux.app.configuration.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.floating.managers.FloatingWindowManager
import com.termux.app.floating.services.FloatingPermissionService
import com.termux.app.configuration.models.RunConfiguration
import com.termux.app.configuration.models.LanguageType
import com.termux.app.floating.services.RemoteCommandExecutor
import com.termux.app.models.SSHConfigManager
import com.termux.app.sftp.SFTPConnectionManager
import com.termux.shared.logger.Logger
import android.widget.Button
import android.widget.Toast
import kotlinx.coroutines.launch

/**
 * 全局设置Fragment
 * 包含悬浮窗开关等设置选项
 */
class GlobalSettingsFragment : Fragment() {
    
    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        
        fun newInstance(): GlobalSettingsFragment {
            return GlobalSettingsFragment()
        }
    }
    
    private lateinit var floatingManager: FloatingWindowManager
    private lateinit var permissionService: FloatingPermissionService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_global_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initManagers()
        initViews(view)
    }
    
    private fun initManagers() {
        floatingManager = FloatingWindowManager.getInstance(requireContext())
        permissionService = FloatingPermissionService(requireContext())
    }
    
    private fun initViews(view: View) {

        view.findViewById<Button>(R.id.btn_quick_test_pwd)?.setOnClickListener {
            runQuickTest("pwd")
        }
        view.findViewById<Button>(R.id.btn_quick_test_ls)?.setOnClickListener {
            runQuickTest("ls -la")
        }
    }


    private fun runQuickTest(command: String) {
        val context = requireContext()
        val sftpManager = SFTPConnectionManager.getInstance()
        val sshManager = SSHConfigManager.getInstance(context)

        // 选择 SSH 配置：优先复用已连接的 SFTP 配置，其次最后使用，再次第一个可用配置
        val chosenConfig = when {
            sftpManager.isConnected && sftpManager.currentConfig != null -> sftpManager.currentConfig
            sshManager.lastUsedConfig != null -> sshManager.lastUsedConfig
            else -> sshManager.allConfigs.firstOrNull()
        }

        if (chosenConfig == null) {
            Toast.makeText(context, "请先在Tab2添加或选择一个SSH连接", Toast.LENGTH_SHORT).show()
            return
        }

        // 项目目录：使用当前SFTP工作目录，若不可用则推断到用户家目录
        val projectPath = if (sftpManager.isConnected) {
            sftpManager.currentWorkingDirectory
        } else {
            "/home/${chosenConfig.username}"
        }

        val runConfig = RunConfiguration(
            id = System.currentTimeMillis().toString(),
            name = "QuickTest-$command",
            sshConfigId = chosenConfig.name, // 按名称索引
            projectPath = projectPath,
            languageType = LanguageType.CUSTOM,
            command = command,
            workingDir = ".",
            envVariables = "",
            runInBackground = false,
            logFileName = "app.log"
        )

        val executor = RemoteCommandExecutor.getInstance(context)

        // 异步执行并反馈结果
        lifecycleScope.launch {
            try {
                Toast.makeText(context, "正在执行: $command", Toast.LENGTH_SHORT).show()
                val result = executor.executeConfiguration(runConfig)
                val output = if (!result.output.isNullOrBlank()) result.output else (result.errorMessage ?: "无输出")
                AlertDialog.Builder(context)
                    .setTitle("快速测试: $command")
                    .setMessage(output)
                    .setPositiveButton("确定", null)
                    .show()
            } catch (e: Exception) {
                Logger.logError("GlobalSettings", "Quick test failed: ${e.message}")
                AlertDialog.Builder(context)
                    .setTitle("快速测试失败")
                    .setMessage(e.message ?: "Unknown error")
                    .setPositiveButton("确定", null)
                    .show()
            }
        }
    }
    
    private fun requestFloatingPermission() {
        floatingManager.requestFloatingPermission(
            requireActivity(),
            object : FloatingPermissionService.OnPermissionResultListener {
                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        floatingManager.enableFloating()
                    }
                }
            }
        )
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            val granted = permissionService.hasOverlayPermission()
            if (granted) {
                floatingManager.enableFloating()
            }
        }
    }
}
