package com.termux.app.configuration.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.termux.R
import com.termux.app.models.SSHConnectionConfig

/**
 * SSH配置列表适配器
 * 用于显示SSH连接配置列表
 */
class SshConfigAdapter(private val context: Context) : 
    RecyclerView.Adapter<SshConfigAdapter.SshConfigViewHolder>() {
    
    private var configs: List<SSHConnectionConfig> = emptyList()
    private var actionListener: OnConfigActionListener? = null
    
    interface OnConfigActionListener {
        fun onEditConfig(config: SSHConnectionConfig)
        fun onDeleteConfig(config: SSHConnectionConfig)
        fun onTestConnection(config: SSHConnectionConfig)
        fun onUseConfig(config: SSHConnectionConfig)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SshConfigViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_ssh_config, parent, false)
        return SshConfigViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SshConfigViewHolder, position: Int) {
        val config = configs[position]
        holder.bind(config)
    }
    
    override fun getItemCount(): Int = configs.size
    
    fun setConfigs(configs: List<SSHConnectionConfig>) {
        this.configs = configs
        notifyDataSetChanged()
    }
    
    fun setOnConfigActionListener(listener: OnConfigActionListener) {
        this.actionListener = listener
    }
    
    inner class SshConfigViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvConfigName: TextView = itemView.findViewById(R.id.tv_config_name)
        private val tvHostInfo: TextView = itemView.findViewById(R.id.tv_host_info)
        private val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        private val ivConnectionStatus: ImageView = itemView.findViewById(R.id.iv_connection_status)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btn_delete)
        private val btnTest: MaterialButton = itemView.findViewById(R.id.btn_test)
        private val btnUse: MaterialButton = itemView.findViewById(R.id.btn_use)
        
        fun bind(config: SSHConnectionConfig) {
            // 设置配置信息
            tvConfigName.text = if (config.name.isNotEmpty()) {
                config.name
            } else {
                "未命名配置"
            }
            
            tvHostInfo.text = "${config.host}:${config.port}"
            tvUsername.text = "用户: ${config.username}"
            
            // 设置连接状态图标（这里可以根据实际连接状态设置不同图标）
            ivConnectionStatus.setImageResource(
                if (config.isValid) R.drawable.ic_check_circle else R.drawable.ic_error
            )
            
            // 设置按钮点击事件
            btnEdit.setOnClickListener {
                actionListener?.onEditConfig(config)
            }
            
            btnDelete.setOnClickListener {
                actionListener?.onDeleteConfig(config)
            }
            
            btnTest.setOnClickListener {
                actionListener?.onTestConnection(config)
            }
            
            btnUse.setOnClickListener {
                actionListener?.onUseConfig(config)
            }
            
            // 点击整个item也可以进入编辑
            itemView.setOnClickListener {
                actionListener?.onEditConfig(config)
            }
            
            // 根据配置有效性启用/禁用按钮
            val isValid = config.isValid
            btnTest.isEnabled = isValid
            btnUse.isEnabled = isValid
            
            // 设置item背景色提示配置状态
            itemView.alpha = if (isValid) 1.0f else 0.7f
        }
    }
}
