package com.termux.app.configuration.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.termux.R
import com.termux.app.configuration.managers.QuickCommandManager
import com.termux.app.configuration.models.QuickCommand
import com.termux.app.configuration.utils.ConfigurationConstants
import com.termux.shared.logger.Logger

/**
 * 常用指令编辑页面
 * 用于新建或编辑常用指令
 */
class QuickCommandEditFragment : Fragment() {

    companion object {
        private const val LOG_TAG = "QuickCommandEditFragment"

        fun newInstance(commandId: String?): QuickCommandEditFragment {
            val fragment = QuickCommandEditFragment()
            val args = Bundle().apply {
                putString(ConfigurationConstants.BUNDLE_CONFIG_ID, commandId)
                putBoolean(ConfigurationConstants.BUNDLE_IS_EDIT_MODE, commandId != null)
            }
            fragment.arguments = args
            return fragment
        }
    }

    // UI组件
    private lateinit var tilName: TextInputLayout
    private lateinit var tilCommand: TextInputLayout
    private lateinit var tilDescription: TextInputLayout
    private lateinit var tilCategory: TextInputLayout
    private lateinit var etName: TextInputEditText
    private lateinit var etCommand: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etCategory: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton

    // 数据和管理器
    private var currentCommand: QuickCommand? = null
    private lateinit var commandManager: QuickCommandManager
    private var isEditMode: Boolean = false
    private var commandId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { args ->
            commandId = args.getString(ConfigurationConstants.BUNDLE_CONFIG_ID)
            isEditMode = args.getBoolean(ConfigurationConstants.BUNDLE_IS_EDIT_MODE, false)
        }

        // 初始化管理器
        commandManager = QuickCommandManager.getInstance(requireContext())

        if (isEditMode && commandId != null) {
            loadExistingCommand()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_quick_command_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        setupTextWatchers()

        if (currentCommand != null) {
            populateFieldsFromCommand()
        }
    }

    private fun initViews(view: View) {
        tilName = view.findViewById(R.id.til_name)
        tilCommand = view.findViewById(R.id.til_command)
        tilDescription = view.findViewById(R.id.til_description)
        tilCategory = view.findViewById(R.id.til_category)
        etName = view.findViewById(R.id.et_name)
        etCommand = view.findViewById(R.id.et_command)
        etDescription = view.findViewById(R.id.et_description)
        etCategory = view.findViewById(R.id.et_category)
        btnSave = view.findViewById(R.id.btn_save)
        btnCancel = view.findViewById(R.id.btn_cancel)
    }

    private fun setupTextWatchers() {
        // 清除错误提示当用户开始输入
        etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilName.error = null
            }
        })

        etCommand.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                tilCommand.error = null
            }
        })
    }

    private fun setupClickListeners() {
        btnSave.setOnClickListener { saveCommand() }
        btnCancel.setOnClickListener { requireActivity().onBackPressed() }
    }

    private fun validateInput(): Boolean {
        var isValid = true

        // 验证名称
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            tilName.error = "请输入指令名称"
            etName.requestFocus()
            isValid = false
        } else {
            // 检查名称是否已存在（编辑时排除自身）
            if (commandManager.isNameExists(name, if (isEditMode) commandId else null)) {
                tilName.error = "指令名称已存在"
                etName.requestFocus()
                isValid = false
            }
        }

        // 验证命令
        val command = etCommand.text.toString().trim()
        if (command.isEmpty()) {
            tilCommand.error = "请输入命令内容"
            if (isValid) etCommand.requestFocus()
            isValid = false
        }

        return isValid
    }

    private fun buildCommandFromInput(): QuickCommand {
        val command = if (isEditMode && commandId != null) {
            currentCommand?.copy() ?: QuickCommand()
        } else {
            QuickCommand()
        }

        command.name = etName.text.toString().trim()
        command.command = etCommand.text.toString().trim()
        command.description = etDescription.text.toString().trim()
        command.category = etCategory.text.toString().trim()

        // 如果是新建模式，生成新ID
        if (!isEditMode) {
            command.id = command.generateUniqueId()
            command.createdTime = System.currentTimeMillis()
        }

        return command
    }

    private fun saveCommand() {
        if (!validateInput()) {
            return
        }

        try {
            val command = buildCommandFromInput()
            val success = commandManager.saveCommand(command)

            if (success) {
                val message = if (isEditMode) "指令更新成功" else "指令保存成功"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                Logger.logInfo(LOG_TAG, "Quick command saved: ${command.name}")
                requireActivity().onBackPressed()
            } else {
                Toast.makeText(context, "指令保存失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to save command: ${e.message}")
            Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadExistingCommand() {
        try {
            currentCommand = commandManager.getCommand(commandId!!)
            if (currentCommand == null) {
                Logger.logError(LOG_TAG, "Command not found: $commandId")
                Toast.makeText(context, "指令不存在", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressed()
            }
        } catch (e: Exception) {
            Logger.logError(LOG_TAG, "Failed to load command: ${e.message}")
            Toast.makeText(context, "加载指令失败", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        }
    }

    private fun populateFieldsFromCommand() {
        currentCommand?.let { cmd ->
            etName.setText(cmd.name)
            etCommand.setText(cmd.command)
            etDescription.setText(cmd.description)
            etCategory.setText(cmd.category)
        }
    }
}
