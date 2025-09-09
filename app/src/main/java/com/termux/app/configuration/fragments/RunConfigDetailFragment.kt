package com.termux.app.configuration.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.termux.R
import com.termux.app.configuration.adapters.PathBookmarkSpinnerAdapter
import com.termux.app.configuration.managers.CommandTemplateManager
import com.termux.app.configuration.managers.RunConfigurationManager
import com.termux.app.configuration.models.LanguageType
import com.termux.app.configuration.models.RunConfiguration
import com.termux.app.configuration.utils.CommandBuilder
import com.termux.app.configuration.utils.ConfigurationConstants
import com.termux.app.configuration.utils.PathBookmarkHelper
import com.termux.app.models.DirectoryBookmark
import com.termux.app.models.SSHConfigManager
import com.termux.app.models.SSHConnectionConfig
import com.termux.shared.logger.Logger
import java.util.*

/**
 * 运行配置详情页面
 * 用于新建或编辑运行配置
 */
class RunConfigDetailFragment : Fragment() {
    
    companion object {
        private const val LOG_TAG = "RunConfigDetailFragment"
        
        fun newInstance(configId: String?): RunConfigDetailFragment {
            val fragment = RunConfigDetailFragment()
            val args = Bundle().apply {
                putString(ConfigurationConstants.BUNDLE_CONFIG_ID, configId)
                putBoolean(ConfigurationConstants.BUNDLE_IS_EDIT_MODE, configId != null)
            }
            fragment.arguments = args
            return fragment
        }
    }
    
    // UI组件
    private lateinit var etConfigName: TextInputEditText
    private lateinit var etCommand: TextInputEditText
    private lateinit var etWorkingDir: TextInputEditText
    private lateinit var etEnvVariables: TextInputEditText
    private lateinit var etPort: TextInputEditText
    private lateinit var etLogFileName: TextInputEditText
    private lateinit var spSSHConfig: Spinner
    private lateinit var spLanguageType: Spinner
    private lateinit var spProjectPath: Spinner
    private lateinit var swRunInBackground: SwitchMaterial
    private lateinit var tvCommandPreview: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnPreview: MaterialButton
    
    // 数据和管理器
    private var currentConfig: RunConfiguration? = null
    private lateinit var configManager: RunConfigurationManager
    private lateinit var sshConfigManager: SSHConfigManager
    private lateinit var templateManager: CommandTemplateManager
    private var isEditMode: Boolean = false
    private var configId: String? = null
    
    // 适配器
    private lateinit var sshAdapter: ArrayAdapter<SSHConnectionConfig>
    private lateinit var languageAdapter: ArrayAdapter<LanguageType>
    private lateinit var pathAdapter: PathBookmarkSpinnerAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        arguments?.let { args ->
            configId = args.getString(ConfigurationConstants.BUNDLE_CONFIG_ID)
            isEditMode = args.getBoolean(ConfigurationConstants.BUNDLE_IS_EDIT_MODE, false)
        }
        
        // 初始化管理器
        configManager = RunConfigurationManager.getInstance(requireContext())
        sshConfigManager = SSHConfigManager.getInstance(requireContext())
        templateManager = CommandTemplateManager.getInstance(requireContext())
        
        if (isEditMode && configId != null) {
            loadExistingConfig()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_run_config_detail, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupSpinners()
        setupClickListeners()
        setupTextWatchers()
        
        if (currentConfig != null) {
            populateFieldsFromConfig()
        } else {
            setDefaultValues()
        }
        
        updateCommandPreview()
    }
    
    private fun initViews(view: View) {
        etConfigName = view.findViewById(R.id.et_config_name)
        etCommand = view.findViewById(R.id.et_command)
        etWorkingDir = view.findViewById(R.id.et_working_dir)
        etEnvVariables = view.findViewById(R.id.et_env_variables)
        etPort = view.findViewById(R.id.et_port)
        etLogFileName = view.findViewById(R.id.et_log_file_name)
        spSSHConfig = view.findViewById(R.id.sp_ssh_config)
        spLanguageType = view.findViewById(R.id.sp_language_type)
        spProjectPath = view.findViewById(R.id.sp_project_path)
        swRunInBackground = view.findViewById(R.id.sw_run_in_background)
        tvCommandPreview = view.findViewById(R.id.tv_command_preview)
        btnSave = view.findViewById(R.id.btn_save)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnPreview = view.findViewById(R.id.btn_preview)
    }
    
    private fun setupSpinners() {
        setupSSHConfigSpinner()
        setupLanguageTypeSpinner()
        setupProjectPathSpinner()
    }
    
    private fun setupSSHConfigSpinner() {
        val sshConfigs = sshConfigManager.allConfigs
        sshAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sshConfigs)
        sshAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spSSHConfig.adapter = sshAdapter
        
        spSSHConfig.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedSSH = sshAdapter.getItem(position)
                selectedSSH?.let {
                    updateProjectPathOptions(it.name)
                    updateCommandPreview()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupLanguageTypeSpinner() {
        val languageTypes = LanguageType.values().toList()
        languageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languageTypes)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spLanguageType.adapter = languageAdapter
        
        spLanguageType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = languageAdapter.getItem(position)
                selectedType?.let {
                    updateCommandSuggestions(it)
                    updateCommandPreview()
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupProjectPathSpinner() {
        pathAdapter = PathBookmarkSpinnerAdapter(requireContext())
        spProjectPath.adapter = pathAdapter

        spProjectPath.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCommandPreview()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    /**
     * 根据选择的SSH配置更新项目路径选项
     */
    private fun updateProjectPathOptions(sshConfigName: String) {
        try {
            val bookmarks = PathBookmarkHelper.getBookmarksBySSHConfig(requireContext(), sshConfigName)
            pathAdapter.setBookmarks(bookmarks)
            
            if (bookmarks.isEmpty()) {
                Toast.makeText(
                    context,
                    "该SSH连接暂无收藏路径，请先在文件浏览器中添加收藏",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                // 如果是编辑模式且已有项目路径，尝试恢复选择
                currentConfig?.let { cfg ->
                    if (!cfg.projectPath.isNullOrBlank()) {
                        val index = bookmarks.indexOfFirst { it.fullPath == cfg.projectPath }
                        if (index >= 0) {
                            spProjectPath.setSelection(index)
                        }
                    }
                }
            }
            // 刷新命令预览
            updateCommandPreview()
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to update project path options: ${e.message}")
        }
    }
    
    /**
     * 根据选择的语言类型更新命令建议
     */
    private fun updateCommandSuggestions(languageType: LanguageType) {
        try {
            val commonCommands = templateManager.getCommonCommands(languageType)
            
            if (commonCommands.isNotEmpty() && etCommand.text.toString().trim().isEmpty()) {
                etCommand.setText(commonCommands[0])
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to update command suggestions: ${e.message}")
        }
    }
    
    private fun setupTextWatchers() {
        val previewUpdater = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCommandPreview()
            }
        }
        
        etCommand.addTextChangedListener(previewUpdater)
        etWorkingDir.addTextChangedListener(previewUpdater)
        etEnvVariables.addTextChangedListener(previewUpdater)
        etPort.addTextChangedListener(previewUpdater)
        etLogFileName.addTextChangedListener(previewUpdater)
    }
    
    private fun setupClickListeners() {
        btnSave.setOnClickListener { saveConfiguration() }
        btnCancel.setOnClickListener { requireActivity().onBackPressed() }
        btnPreview.setOnClickListener { showCommandPreviewDialog() }
        
        swRunInBackground.setOnCheckedChangeListener { _, isChecked ->
            etLogFileName.isEnabled = isChecked
            updateCommandPreview()
        }
    }
    
    private fun updateCommandPreview() {
        try {
            val tempConfig = buildConfigurationFromInput()
            if (tempConfig.isValid()) {
                val command = CommandBuilder.buildCompleteCommand(tempConfig)
                tvCommandPreview.text = command
                tvCommandPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_success))
            } else {
                tvCommandPreview.text = "请完善配置信息"
                tvCommandPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_warning))
            }
        } catch (e: Exception) {
            tvCommandPreview.text = "配置信息有误: ${e.message}"
            tvCommandPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_error))
        }
    }
    
    private fun showCommandPreviewDialog() {
        val command = tvCommandPreview.text.toString()
        
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("命令预览")
            .setMessage(command)
            .setPositiveButton("复制") { _, _ ->
                copyToClipboard(command)
            }
            .setNegativeButton("关闭", null)
            .show()
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) 
                as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("运行命令", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "命令已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }
    
    private fun validateInput(): Boolean {
        // 验证配置名称
        val name = etConfigName.text.toString().trim()
        if (name.isEmpty()) {
            etConfigName.error = "请输入配置名称"
            etConfigName.requestFocus()
            return false
        }
        
        // 检查名称是否已存在
        val existingConfig = configManager.getConfiguration(name)
        if (existingConfig != null && (!isEditMode || existingConfig.id != configId)) {
            etConfigName.error = "配置名称已存在"
            etConfigName.requestFocus()
            return false
        }
        
        // 验证SSH配置
        if (spSSHConfig.selectedItem == null) {
            Toast.makeText(context, "请选择SSH配置", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // 验证项目路径
        if (spProjectPath.selectedItem == null) {
            Toast.makeText(context, "请选择项目路径", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // 验证命令
        val command = etCommand.text.toString().trim()
        if (command.isEmpty()) {
            etCommand.error = "请输入运行命令"
            etCommand.requestFocus()
            return false
        }
        
        return true
    }
    
    private fun buildConfigurationFromInput(): RunConfiguration {
        val config = RunConfiguration()
        
        if (isEditMode && configId != null) {
            config.id = configId!!
        } else {
            config.id = UUID.randomUUID().toString()
            config.createdTime = System.currentTimeMillis()
        }
        
        config.name = etConfigName.text.toString().trim()
        
        val selectedSSH = spSSHConfig.selectedItem as? SSHConnectionConfig
        config.sshConfigId = selectedSSH?.name ?: ""
        
        val selectedPath = spProjectPath.selectedItem as? DirectoryBookmark
        config.projectPath = selectedPath?.fullPath ?: ""
        
        val selectedType = spLanguageType.selectedItem as? LanguageType
        config.languageType = selectedType ?: LanguageType.CUSTOM
        
        config.command = etCommand.text.toString().trim()
        config.workingDir = etWorkingDir.text.toString().trim().ifEmpty { "." }
        config.envVariables = etEnvVariables.text.toString().trim()
        // 端口（可选）
        val portText = etPort.text?.toString()?.trim().orEmpty()
        config.port = portText.toIntOrNull()?.coerceIn(1, 65535) ?: 0
        config.runInBackground = swRunInBackground.isChecked
        config.logFileName = etLogFileName.text.toString().trim().ifEmpty { 
            ConfigurationConstants.DEFAULT_LOG_FILE 
        }
        
        return config
    }
    
    private fun saveConfiguration() {
        if (!validateInput()) {
            return
        }
        
        try {
            val config = buildConfigurationFromInput()
            val success = configManager.saveConfiguration(config)
            
            if (success) {
                val message = if (isEditMode) "配置更新成功" else "配置保存成功"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                Logger.logInfo(LOG_TAG, "Run configuration saved: ${config.name}")
                requireActivity().onBackPressed()
            } else {
                Toast.makeText(context, "配置保存失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to save configuration: ${e.message}")
            Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadExistingConfig() {
        try {
            currentConfig = configManager.getConfiguration(configId!!)
            if (currentConfig == null) {
                Logger.logError(LOG_TAG, "Configuration not found: $configId")
                Toast.makeText(context, "配置不存在", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressed()
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to load configuration: ${e.message}")
            Toast.makeText(context, "加载配置失败", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }
    }
    
    private fun populateFieldsFromConfig() {
        currentConfig?.let { config ->
            etConfigName.setText(config.name)
            etCommand.setText(config.command)
            etWorkingDir.setText(config.workingDir)
            etEnvVariables.setText(config.envVariables)
            etLogFileName.setText(config.logFileName)
            swRunInBackground.isChecked = config.runInBackground
            if (config.port > 0) {
                etPort.setText(config.port.toString())
            } else {
                etPort.setText("")
            }
            
            // 设置Spinner选中项
            setSpinnerSelection(spLanguageType, config.languageType)
            // 恢复 SSH 与 项目路径选择
            try {
                // SSH 根据名称匹配
                val sshIndex = sshAdapter.let { adapter ->
                    (0 until adapter.count).firstOrNull { i ->
                        adapter.getItem(i)?.name == config.sshConfigId
                    } ?: -1
                }
                if (sshIndex >= 0) {
                    spSSHConfig.setSelection(sshIndex)
                }

                // 项目路径将在 updateProjectPathOptions 中按当前配置恢复
                // 如果当前 SSH 下没有对应书签，保持默认选择
            } catch (e: Exception) {
                Logger.logError(LOG_TAG, "Failed to restore spinner selections: ${e.message}")
            }
        }
    }
    
    private fun setDefaultValues() {
        etWorkingDir.setText(".")
        etLogFileName.setText(ConfigurationConstants.DEFAULT_LOG_FILE)
        swRunInBackground.isChecked = true
        spLanguageType.setSelection(0) // 默认选择第一个语言类型
    }
    
    private fun <T> setSpinnerSelection(spinner: Spinner, item: T) {
        val adapter = spinner.adapter as? ArrayAdapter<T>
        adapter?.let {
            val position = it.getPosition(item)
            if (position >= 0) {
                spinner.setSelection(position)
            }
        }
    }
}
