package com.termux.app.floating.dialogs

import android.app.Dialog
import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.termux.R
import com.termux.app.floating.models.ExecutionResult

class ExecutionResultDialog(
    context: Context,
    private var executionResult: ExecutionResult
) : Dialog(context, R.style.Theme_FloatingDialog) {
    
    private lateinit var ivResultIcon: ImageView
    private lateinit var tvResultTitle: TextView
    private lateinit var tvResultMessage: TextView
    private lateinit var tvCommandOutput: TextView
    private lateinit var btnViewLog: MaterialButton
    private lateinit var btnReExecute: MaterialButton
    private lateinit var btnClose: MaterialButton
    private lateinit var progressBar: ProgressBar
    
    private var actionListener: OnResultActionListener? = null
    
    interface OnResultActionListener {
        fun onViewLogRequested(logFilePath: String)
        fun onReExecuteRequested()
    }
    
    init {
        initDialog()
    }
    
    private fun initDialog() {
        setContentView(R.layout.dialog_execution_result)
        
        window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
        }
        
        initViews()
        displayResult()
        setupListeners()
    }
    
    private fun initViews() {
        ivResultIcon = findViewById(R.id.iv_result_icon)
        tvResultTitle = findViewById(R.id.tv_result_title)
        tvResultMessage = findViewById(R.id.tv_result_message)
        tvCommandOutput = findViewById(R.id.tv_command_output)
        btnViewLog = findViewById(R.id.btn_view_log)
        btnReExecute = findViewById(R.id.btn_re_execute)
        btnClose = findViewById(R.id.btn_close)
        progressBar = findViewById(R.id.progress_bar)
    }
    
    private fun displayResult() {
        when (executionResult.status) {
            ExecutionResult.Status.EXECUTING -> showExecutingState()
            ExecutionResult.Status.SUCCESS -> showSuccessState()
            ExecutionResult.Status.ERROR -> showErrorState()
            ExecutionResult.Status.TIMEOUT -> showTimeoutState()
        }
    }
    
    private fun showExecutingState() {
        progressBar.visibility = View.VISIBLE
        ivResultIcon.visibility = View.GONE
        
        tvResultTitle.text = "正在执行命令"
        tvResultMessage.text = "请稍候，命令正在远程服务器上执行..."
        
        btnReExecute.isEnabled = false
        btnViewLog.isEnabled = false
        
        // 显示执行的命令
        tvCommandOutput.text = executionResult.executedCommand
    }
    
    private fun showSuccessState() {
        progressBar.visibility = View.GONE
        ivResultIcon.visibility = View.VISIBLE
        ivResultIcon.setImageResource(R.drawable.ic_check_circle)
        ivResultIcon.setColorFilter(ContextCompat.getColor(context, R.color.success_color))
        
        tvResultTitle.text = "执行成功"
        
        val message = buildString {
            append("✅ SSH连接成功\n")
            
            if (executionResult.killedPrevious) {
                append("✅ 已终止前一次进程\n")
            }
            
            append("✅ 命令执行成功")
            
            if (executionResult.processId > 0) {
                append("\n📝 进程ID: ${executionResult.processId}")
            }
            
            if (!TextUtils.isEmpty(executionResult.logFilePath)) {
                append("\n📄 日志文件: ${executionResult.logFilePath}")
            }
        }
        
        tvResultMessage.text = message
        
        // 显示命令输出（如果有）
        val output = executionResult.output
        if (!TextUtils.isEmpty(output)) {
            tvCommandOutput.text = output
            tvCommandOutput.visibility = View.VISIBLE
        }
        
        btnReExecute.isEnabled = true
        btnViewLog.isEnabled = !TextUtils.isEmpty(executionResult.logFilePath)
    }
    
    private fun showErrorState() {
        progressBar.visibility = View.GONE
        ivResultIcon.visibility = View.VISIBLE
        ivResultIcon.setImageResource(R.drawable.ic_error_circle)
        ivResultIcon.setColorFilter(ContextCompat.getColor(context, R.color.error_color))
        
        tvResultTitle.text = "执行失败"
        tvResultMessage.text = "❌ ${executionResult.errorMessage}"
        
        // 显示错误输出
        val errorOutput = executionResult.errorOutput
        if (!TextUtils.isEmpty(errorOutput)) {
            tvCommandOutput.text = errorOutput
            tvCommandOutput.visibility = View.VISIBLE
        }
        
        btnReExecute.isEnabled = true
        btnViewLog.isEnabled = false
    }
    
    private fun showTimeoutState() {
        progressBar.visibility = View.GONE
        ivResultIcon.visibility = View.VISIBLE
        ivResultIcon.setImageResource(R.drawable.ic_timeout_circle)
        ivResultIcon.setColorFilter(ContextCompat.getColor(context, R.color.warning_color))
        
        tvResultTitle.text = "执行超时"
        tvResultMessage.text = "⏰ 命令执行超时，可能仍在后台运行"
        
        btnReExecute.isEnabled = true
        btnViewLog.isEnabled = !TextUtils.isEmpty(executionResult.logFilePath)
    }
    
    private fun setupListeners() {
        btnClose.setOnClickListener { dismiss() }
        
        btnViewLog.setOnClickListener {
            actionListener?.onViewLogRequested(executionResult.logFilePath)
        }
        
        btnReExecute.setOnClickListener {
            actionListener?.onReExecuteRequested()
            dismiss()
        }
    }
    
    /**
     * 更新执行状态（用于实时更新）
     */
    fun updateResult(result: ExecutionResult) {
        this.executionResult = result
        displayResult()
    }
    
    fun setOnResultActionListener(listener: OnResultActionListener) {
        this.actionListener = listener
    }
}
