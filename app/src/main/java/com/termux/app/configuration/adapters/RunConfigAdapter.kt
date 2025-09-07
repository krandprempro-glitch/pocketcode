package com.termux.app.configuration.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.termux.R
import com.termux.app.configuration.models.LanguageType
import com.termux.app.configuration.models.RunConfiguration
import java.text.SimpleDateFormat
import java.util.*

/**
 * 运行配置列表适配器
 * 用于显示运行配置列表
 */
class RunConfigAdapter(private val context: Context) :
    RecyclerView.Adapter<RunConfigAdapter.RunConfigViewHolder>() {
    
    private var configurations: List<RunConfiguration> = emptyList()
    private var actionListener: OnConfigActionListener? = null
    private val dateFormatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    
    interface OnConfigActionListener {
        fun onEditConfig(config: RunConfiguration)
        fun onDeleteConfig(config: RunConfiguration)
        fun onCopyCommand(config: RunConfiguration)
        fun onQuickRun(config: RunConfiguration)
        fun onDuplicateConfig(config: RunConfiguration)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunConfigViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_run_config, parent, false)
        return RunConfigViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: RunConfigViewHolder, position: Int) {
        val config = configurations[position]
        holder.bind(config)
    }
    
    override fun getItemCount(): Int = configurations.size
    
    fun setConfigurations(configurations: List<RunConfiguration>) {
        this.configurations = configurations
        notifyDataSetChanged()
    }
    
    fun setOnConfigActionListener(listener: OnConfigActionListener) {
        this.actionListener = listener
    }
    
    inner class RunConfigViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_config)
        private val tvConfigName: TextView = itemView.findViewById(R.id.tv_config_name)
        private val tvLanguageType: TextView = itemView.findViewById(R.id.tv_language_type)
        private val tvCommand: TextView = itemView.findViewById(R.id.tv_command)
        private val tvProjectPath: TextView = itemView.findViewById(R.id.tv_project_path)
        private val tvLastUsed: TextView = itemView.findViewById(R.id.tv_last_used)
        private val ivLanguageIcon: ImageView = itemView.findViewById(R.id.iv_language_icon)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btn_delete)
        private val btnCopyCommand: MaterialButton = itemView.findViewById(R.id.btn_copy_command)
        private val btnQuickRun: MaterialButton = itemView.findViewById(R.id.btn_quick_run)
        private val btnDuplicate: MaterialButton = itemView.findViewById(R.id.btn_duplicate)
        
        fun bind(config: RunConfiguration) {
            // 设置配置信息
            tvConfigName.text = config.name
            tvLanguageType.text = getLanguageDisplayName(config.languageType)
            tvCommand.text = config.command
            tvProjectPath.text = config.projectPath ?: "未设置项目路径"
            
            // 设置最后使用时间
            tvLastUsed.text = formatLastUsedTime(config.lastUsedTime)
            
            // 设置语言图标
            ivLanguageIcon.setImageResource(getLanguageIcon(config.languageType))
            
            // 设置卡片颜色（根据语言类型）
            setCardColor(config.languageType)
            
            // 设置按钮点击事件
            btnEdit.setOnClickListener {
                actionListener?.onEditConfig(config)
            }
            
            btnDelete.setOnClickListener {
                actionListener?.onDeleteConfig(config)
            }
            
            btnCopyCommand.setOnClickListener {
                actionListener?.onCopyCommand(config)
            }
            
            btnQuickRun.setOnClickListener {
                actionListener?.onQuickRun(config)
            }
            
            btnDuplicate.setOnClickListener {
                actionListener?.onDuplicateConfig(config)
            }
            
            // 点击整个卡片进入编辑
            cardView.setOnClickListener {
                actionListener?.onEditConfig(config)
            }
            
            // 根据配置有效性设置状态
            val isValid = config.isValid()
            itemView.alpha = if (isValid) 1.0f else 0.7f
            btnQuickRun.isEnabled = isValid
            
            if (!isValid) {
                tvConfigName.setTextColor(ContextCompat.getColor(context, R.color.text_error))
            } else {
                tvConfigName.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
        }
        
        private fun getLanguageDisplayName(languageType: LanguageType): String {
            return languageType.displayName
        }
        
        private fun getLanguageIcon(languageType: LanguageType): Int {
            return when (languageType) {
                LanguageType.NODEJS -> R.drawable.ic_nodejs
                LanguageType.PYTHON -> R.drawable.ic_python
                LanguageType.JAVA -> R.drawable.ic_java
                LanguageType.GO -> R.drawable.ic_go
                LanguageType.PHP -> R.drawable.ic_php
                LanguageType.RUBY -> R.drawable.ic_ruby
                LanguageType.RUST -> R.drawable.ic_rust
                LanguageType.CPP -> R.drawable.ic_cpp
                LanguageType.CUSTOM -> R.drawable.ic_terminal
            }
        }
        
        private fun setCardColor(languageType: LanguageType) {
            val colorRes = when (languageType) {
                LanguageType.NODEJS -> R.color.language_nodejs
                LanguageType.PYTHON -> R.color.language_python
                LanguageType.JAVA -> R.color.language_java
                LanguageType.GO -> R.color.language_go
                LanguageType.PHP -> R.color.language_php
                LanguageType.RUBY -> R.color.language_ruby
                LanguageType.RUST -> R.color.language_rust
                LanguageType.CPP -> R.color.language_cpp
                LanguageType.CUSTOM -> R.color.language_custom
            }
            
            cardView.setCardBackgroundColor(ContextCompat.getColor(context, colorRes))
        }
        
        private fun formatLastUsedTime(timestamp: Long): String {
            if (timestamp == 0L) return "从未使用"
            
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60 * 1000 -> "刚刚使用"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
                diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
                else -> dateFormatter.format(Date(timestamp))
            }
        }
    }
}
