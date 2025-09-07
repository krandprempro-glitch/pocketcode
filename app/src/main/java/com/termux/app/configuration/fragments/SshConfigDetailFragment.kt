package com.termux.app.configuration.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.termux.R
import com.termux.app.configuration.utils.ConfigurationConstants
import com.termux.app.models.SSHConfigManager
import com.termux.app.models.SSHConnectionConfig
import com.termux.app.ssh.SSHConnectionManager
import com.termux.shared.logger.Logger

/**
 * SSH配置详情页面
 * 用于新建或编辑SSH连接配置
 */
class SshConfigDetailFragment : Fragment() {
    
    companion object {
        private const val LOG_TAG = "SshConfigDetailFragment"
        
        fun newInstance(configName: String?): SshConfigDetailFragment {
            val fragment = SshConfigDetailFragment()
            val args = Bundle().apply {
                putString(ConfigurationConstants.BUNDLE_CONFIG_ID, configName)
                putBoolean(ConfigurationConstants.BUNDLE_IS_EDIT_MODE, configName != null)
            }
            fragment.arguments = args
            return fragment
        }
    }
    
    // UI组件
    private lateinit var etConfigName: TextInputEditText
    private lateinit var etHost: TextInputEditText
    private lateinit var etPort: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var swUseKeyAuth: SwitchMaterial
    private lateinit var etPrivateKeyPath: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnTest: MaterialButton
    private lateinit var btnCancel: MaterialButton
    
    // 数据相关
    private var currentConfig: SSHConnectionConfig? = null
    private var isEditMode: Boolean = false
    private var configName: String? = null
    private lateinit var sshConfigManager: SSHConfigManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        arguments?.let { args ->
            configName = args.getString(ConfigurationConstants.BUNDLE_CONFIG_ID)
            isEditMode = args.getBoolean(ConfigurationConstants.BUNDLE_IS_EDIT_MODE, false)
        }
        
        sshConfigManager = SSHConfigManager.getInstance(requireContext())
        
        if (isEditMode && configName != null) {
            loadExistingConfig()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ssh_config_detail, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupClickListeners()
        setupSwitchListeners()
        
        if (currentConfig != null) {
            populateFields()
        } else {
            setDefaultValues()
        }
    }
    
    private fun initViews(view: View) {
        etConfigName = view.findViewById(R.id.et_config_name)
        etHost = view.findViewById(R.id.et_host)
        etPort = view.findViewById(R.id.et_port)
        etUsername = view.findViewById(R.id.et_username)
        etPassword = view.findViewById(R.id.et_password)
        swUseKeyAuth = view.findViewById(R.id.sw_use_key_auth)
        etPrivateKeyPath = view.findViewById(R.id.et_private_key_path)
        btnSave = view.findViewById(R.id.btn_save)
        btnTest = view.findViewById(R.id.btn_test)
        btnCancel = view.findViewById(R.id.btn_cancel)
    }
    
    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            saveConfig()
        }
        
        btnTest.setOnClickListener {
            testConfig()
        }
        
        btnCancel.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }
    
    private fun setupSwitchListeners() {
        swUseKeyAuth.setOnCheckedChangeListener { _, isChecked ->
            // 密钥认证和密码认证互斥
            etPassword.isEnabled = !isChecked
            etPrivateKeyPath.isEnabled = isChecked
            
            if (isChecked) {
                etPassword.setText("")
            } else {
                etPrivateKeyPath.setText("")
            }
        }
    }
    
    private fun loadExistingConfig() {
        try {
            currentConfig = sshConfigManager.getConfigByName(configName!!)
            if (currentConfig == null) {
                Logger.logError(LOG_TAG, "Config not found: $configName")
                Toast.makeText(context, "配置不存在", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressed()
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to load config: ${e.message}")
            Toast.makeText(context, "加载配置失败", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }
    }
    
    private fun populateFields() {
        currentConfig?.let { config ->
            etConfigName.setText(config.name)
            etHost.setText(config.host)
            etPort.setText(config.port.toString())
            etUsername.setText(config.username)
            etPassword.setText(config.password)
            
            // 这里可以根据实际情况设置密钥认证相关字段
            // 当前SSHConnectionConfig模型没有密钥相关字段，可能需要扩展
            swUseKeyAuth.isChecked = false
            etPrivateKeyPath.setText("")
        }
    }
    
    private fun setDefaultValues() {
        etPort.setText("22")
        swUseKeyAuth.isChecked = false
        etPrivateKeyPath.isEnabled = false
    }
    
    private fun validateInput(): Boolean {
        // 验证配置名称
        val name = etConfigName.text.toString().trim()
        if (name.isEmpty()) {
            etConfigName.error = "请输入配置名称"
            etConfigName.requestFocus()
            return false
        }
        
        // 检查名称是否已存在（编辑模式下排除当前配置）
        val existingConfig = sshConfigManager.getConfigByName(name)
        if (existingConfig != null && (!isEditMode || name != configName)) {
            etConfigName.error = "配置名称已存在"
            etConfigName.requestFocus()
            return false
        }
        
        // 验证主机地址
        val host = etHost.text.toString().trim()
        if (host.isEmpty()) {
            etHost.error = "请输入主机地址"
            etHost.requestFocus()
            return false
        }
        
        // 验证端口号
        val portText = etPort.text.toString().trim()
        if (portText.isEmpty()) {
            etPort.error = "请输入端口号"
            etPort.requestFocus()
            return false
        }
        
        try {
            val port = portText.toInt()
            if (port <= 0 || port > 65535) {
                etPort.error = "端口号必须在1-65535之间"
                etPort.requestFocus()
                return false
            }
        } catch (e: NumberFormatException) {
            etPort.error = "端口号格式不正确"
            etPort.requestFocus()
            return false
        }
        
        // 验证用户名
        val username = etUsername.text.toString().trim()
        if (username.isEmpty()) {
            etUsername.error = "请输入用户名"
            etUsername.requestFocus()
            return false
        }
        
        // 验证认证信息
        if (swUseKeyAuth.isChecked) {
            val keyPath = etPrivateKeyPath.text.toString().trim()
            if (keyPath.isEmpty()) {
                etPrivateKeyPath.error = "请输入私钥路径"
                etPrivateKeyPath.requestFocus()
                return false
            }
        } else {
            val password = etPassword.text.toString().trim()
            if (password.isEmpty()) {
                etPassword.error = "请输入密码"
                etPassword.requestFocus()
                return false
            }
        }
        
        return true
    }
    
    private fun buildConfigFromInput(): SSHConnectionConfig {
        val config = SSHConnectionConfig()
        
        config.name = etConfigName.text.toString().trim()
        config.host = etHost.text.toString().trim()
        config.port = etPort.text.toString().trim().toInt()
        config.username = etUsername.text.toString().trim()
        
        if (swUseKeyAuth.isChecked) {
            // 当前模型不支持密钥认证，这里先留空密码
            // 可以考虑扩展SSHConnectionConfig模型添加密钥支持
            config.password = ""
        } else {
            config.password = etPassword.text.toString().trim()
        }
        
        return config
    }
    
    private fun saveConfig() {
        if (!validateInput()) {
            return
        }
        
        try {
            val config = buildConfigFromInput()
            val success = sshConfigManager.saveConfig(config)
            
            if (success) {
                val message = if (isEditMode) "配置更新成功" else "配置保存成功"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                Logger.logInfo(LOG_TAG, "SSH config saved: ${config.name}")
                requireActivity().onBackPressed()
            } else {
                Toast.makeText(context, "配置保存失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to save config: ${e.message}")
            Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun testConfig() {
        if (!validateInput()) {
            return
        }
        
        try {
            val config = buildConfigFromInput()
            val sshCommand = SSHConnectionManager.generateSSHCommand(config)
            
            if (sshCommand != null) {
                Toast.makeText(context, "配置有效！\n连接命令: $sshCommand", Toast.LENGTH_LONG).show()
                Logger.logInfo(LOG_TAG, "Config test successful: $sshCommand")
            } else {
                Toast.makeText(context, "配置测试失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to test config: ${e.message}")
            Toast.makeText(context, "测试失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
