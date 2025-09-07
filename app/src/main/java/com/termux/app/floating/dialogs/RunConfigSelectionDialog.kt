package com.termux.app.floating.dialogs

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.termux.R
import com.termux.app.configuration.managers.RunConfigurationManager
import com.termux.app.configuration.models.RunConfiguration

class RunConfigSelectionDialog(context: Context) : Dialog(context, R.style.Theme_FloatingDialog) {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RunConfigSelectionAdapter
    private lateinit var configManager: RunConfigurationManager
    private lateinit var tvEmptyState: TextView
    private lateinit var btnNewConfig: MaterialButton
    
    private var configSelectedListener: OnConfigSelectedListener? = null
    
    interface OnConfigSelectedListener {
        fun onConfigSelected(config: RunConfiguration)
        fun onNewConfigRequested()
    }
    
    init {
        initDialog()
    }
    
    private fun initDialog() {
        setContentView(R.layout.dialog_run_config_selection)
        
        // 设置对话框属性
        window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
            window.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
        }
        
        initViews()
        setupRecyclerView()
        loadConfigurations()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.rv_run_configs)
        tvEmptyState = findViewById(R.id.tv_empty_state)
        btnNewConfig = findViewById(R.id.btn_new_config)
        
        configManager = RunConfigurationManager.getInstance(context)
        
        btnNewConfig.setOnClickListener {
            configSelectedListener?.onNewConfigRequested()
            dismiss()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = RunConfigSelectionAdapter(context) { config ->
            configSelectedListener?.onConfigSelected(config)
            dismiss()
        }
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }
    
    private fun loadConfigurations() {
        val configs = configManager.getAllConfigurations()
        
        // 按最近使用时间排序
        val sortedConfigs = configs.sortedByDescending { it.lastUsedTime }
        
        adapter.setConfigurations(sortedConfigs)
        
        // 显示/隐藏空状态
        val isEmpty = configs.isEmpty()
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }
    
    fun setOnConfigSelectedListener(listener: OnConfigSelectedListener) {
        this.configSelectedListener = listener
    }
    
    // 简单的适配器实现
    private class RunConfigSelectionAdapter(
        private val context: Context,
        private val onConfigClick: (RunConfiguration) -> Unit
    ) : RecyclerView.Adapter<RunConfigSelectionAdapter.ViewHolder>() {
        
        private var configurations: List<RunConfiguration> = emptyList()
        
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tv_config_name)
            val tvDescription: TextView = itemView.findViewById(R.id.tv_config_description)
            val tvLastUsed: TextView = itemView.findViewById(R.id.tv_last_used)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(context)
                .inflate(R.layout.item_run_config_selection, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val config = configurations[position]
            holder.tvName.text = config.name
            holder.tvDescription.text = "${config.languageType.displayName} • ${config.projectPath}"
            
            // 格式化最后使用时间
            val lastUsedText = if (config.lastUsedTime > 0) {
                val diff = System.currentTimeMillis() - config.lastUsedTime
                when {
                    diff < 60 * 1000 -> "刚刚使用"
                    diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
                    diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
                    else -> "${diff / (24 * 60 * 60 * 1000)}天前"
                }
            } else {
                "从未使用"
            }
            holder.tvLastUsed.text = lastUsedText
            
            holder.itemView.setOnClickListener {
                onConfigClick(config)
            }
        }
        
        override fun getItemCount(): Int = configurations.size
        
        fun setConfigurations(configs: List<RunConfiguration>) {
            configurations = configs
            notifyDataSetChanged()
        }
    }
}
