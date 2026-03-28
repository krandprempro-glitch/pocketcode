package com.termux.app.floating.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.termux.R
import com.termux.app.models.SSHConfigManager
import com.termux.app.models.SSHConnectionConfig

class SshConnectionDialog(context: Context) : Dialog(context, R.style.Theme_FloatingDialog) {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SshConnectionAdapter
    private lateinit var sshConfigManager: SSHConfigManager
    private lateinit var tvEmptyState: TextView
    private lateinit var btnManageConfigs: MaterialButton
    
    private var actionListener: OnConnectionActionListener? = null
    
    interface OnConnectionActionListener {
        fun onConnectRequested(config: SSHConnectionConfig)
        fun onManageConfigsRequested()
    }
    
    init {
        initDialog()
    }
    
    private fun initDialog() {
        setContentView(R.layout.dialog_ssh_connection)
        
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
        recyclerView = findViewById(R.id.rv_ssh_configs)
        tvEmptyState = findViewById(R.id.tv_empty_state)
        btnManageConfigs = findViewById(R.id.btn_manage_configs)
        
        sshConfigManager = SSHConfigManager.getInstance(context)
        
        btnManageConfigs.setOnClickListener {
            actionListener?.onManageConfigsRequested()
            dismiss()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = SshConnectionAdapter(context) { config ->
            actionListener?.onConnectRequested(config)
            dismiss()
        }
        
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }
    
    private fun loadConfigurations() {
        val configs = sshConfigManager.getAllConfigs()
        
        // 按名称排序（因为没有lastUsedTime属性）
        val sortedConfigs = configs.sortedBy { it.name ?: it.host }
        
        adapter.setConfigurations(sortedConfigs)
        
        // 显示/隐藏空状态
        val isEmpty = configs.isEmpty()
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }
    
    fun setOnConnectionActionListener(listener: OnConnectionActionListener) {
        this.actionListener = listener
    }
    
    // SSH连接适配器
    private class SshConnectionAdapter(
        private val context: Context,
        private val onConfigClick: (SSHConnectionConfig) -> Unit
    ) : RecyclerView.Adapter<SshConnectionAdapter.ViewHolder>() {
        
        private var configurations: List<SSHConnectionConfig> = emptyList()
        
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.ssh_config_name)
            val tvConnection: TextView = itemView.findViewById(R.id.ssh_user_host)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(context)
                .inflate(R.layout.item_ssh_connection, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val config = configurations[position]
            holder.tvName.text = config.name
            holder.tvConnection.text = "${config.username}@${config.host}:${config.port}"
            
            holder.itemView.setOnClickListener {
                onConfigClick(config)
            }
        }
        
        override fun getItemCount(): Int = configurations.size
        
        fun setConfigurations(configs: List<SSHConnectionConfig>) {
            configurations = configs
            notifyDataSetChanged()
        }
    }
}
