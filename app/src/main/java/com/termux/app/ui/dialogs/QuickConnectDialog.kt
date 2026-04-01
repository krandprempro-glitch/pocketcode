package com.termux.app.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.models.SSHConnectionConfig
import com.termux.app.models.SSHConfigManager
import com.termux.databinding.DialogQuickConnectBinding

class QuickConnectDialog(context: Context) : Dialog(context) {

    interface OnQuickConnectListener {
        fun onQuickConnect(config: SSHConnectionConfig)
        fun onManageConnections()
    }

    private var listener: OnQuickConnectListener? = null
    private lateinit var binding: DialogQuickConnectBinding
    private lateinit var adapter: QuickConnectAdapter

    fun setOnQuickConnectListener(listener: OnQuickConnectListener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogQuickConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.apply {
            setBackgroundDrawableResource(R.color.dialog_window_background_light)
            setDimAmount(0.3f)
            clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val metrics = context.resources.displayMetrics
            val width = (metrics.widthPixels * 0.9f).toInt()
            setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        setupRecyclerView()
        loadConfigs()

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnManageConnections.setOnClickListener {
            listener?.onManageConnections()
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        adapter = QuickConnectAdapter { config ->
            listener?.onQuickConnect(config)
            dismiss()
        }
        binding.quickConnectRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@QuickConnectDialog.adapter
        }
    }

    private fun loadConfigs() {
        val configs = SSHConfigManager.getInstance(context).allConfigs
        if (configs.isEmpty()) {
            binding.quickConnectRecyclerView.visibility = View.GONE
            binding.quickConnectEmptyText.visibility = View.VISIBLE
        } else {
            binding.quickConnectRecyclerView.visibility = View.VISIBLE
            binding.quickConnectEmptyText.visibility = View.GONE
            adapter.updateConfigs(configs)
        }
    }

    private class QuickConnectAdapter(
        private val onConfigClick: (SSHConnectionConfig) -> Unit
    ) : RecyclerView.Adapter<QuickConnectAdapter.ViewHolder>() {

        private val configs = mutableListOf<SSHConnectionConfig>()

        fun updateConfigs(newConfigs: List<SSHConnectionConfig>) {
            configs.clear()
            configs.addAll(newConfigs)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_quick_connect_config, parent, false)
            return ViewHolder(view, onConfigClick)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(configs[position])
        }

        override fun getItemCount() = configs.size

        class ViewHolder(
            view: View,
            private val onClick: (SSHConnectionConfig) -> Unit
        ) : RecyclerView.ViewHolder(view) {
            private val nameText: TextView = view.findViewById(R.id.config_name)
            private val infoText: TextView = view.findViewById(R.id.config_info)

            fun bind(config: SSHConnectionConfig) {
                nameText.text = config.displayName.ifEmpty { config.host }
                infoText.text = "${config.username}@${config.host}:${config.port}"
                itemView.setOnClickListener { onClick(config) }
            }
        }
    }
}
