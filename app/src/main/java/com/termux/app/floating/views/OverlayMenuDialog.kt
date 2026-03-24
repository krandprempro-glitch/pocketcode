package com.termux.app.floating.views

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.models.SSHConfigManager
import com.termux.app.models.SSHConnectionConfig

class OverlayMenuDialog(
    context: Context
) : Dialog(context, R.style.Theme_FloatingDialog) {

    interface OnMenuSelectListener {
        fun onSSHConnection()
        fun onSSHConfigSelected(config: SSHConnectionConfig)
        fun onRunCommand()
        fun onQuickSettings()
    }

    private var listener: OnMenuSelectListener? = null
    private lateinit var sshConfigManager: SSHConfigManager

    // SSH折叠相关视图
    private lateinit var containerSSHList: LinearLayout
    private lateinit var ivSSHExpand: ImageView
    private lateinit var rvSSHConfigs: RecyclerView
    private lateinit var tvSSHEmpty: TextView
    private lateinit var tvManageSSH: TextView
    private var isSSHExpanded = false
    private var sshAdapter: SSHConfigAdapter? = null

    fun setOnMenuSelectListener(l: OnMenuSelectListener) {
        listener = l
    }

    override fun show() {
        // Configure as overlay window before showing
        window?.let { w ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                w.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                @Suppress("DEPRECATION")
                w.setType(WindowManager.LayoutParams.TYPE_PHONE)
            }
            val metrics = context.resources.displayMetrics
            val desiredWidth = (metrics.widthPixels * 0.8f).toInt()
            w.setLayout(desiredWidth, WindowManager.LayoutParams.WRAP_CONTENT)
            w.setGravity(Gravity.CENTER)
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            w.attributes = w.attributes.apply { dimAmount = 0.35f }
        }

        super.show()
        loadSSHConfigs()
    }

    init {
        setContentView(R.layout.overlay_floating_menu)

        sshConfigManager = SSHConfigManager.getInstance(context)
        initViews()
    }

    private fun initViews() {
        // SSH折叠相关视图
        containerSSHList = findViewById(R.id.container_ssh_list)
        ivSSHExpand = findViewById(R.id.iv_ssh_expand)
        rvSSHConfigs = findViewById(R.id.rv_ssh_configs)
        tvSSHEmpty = findViewById(R.id.tv_ssh_empty)
        tvManageSSH = findViewById(R.id.tv_manage_ssh)

        // SSH行点击 - 展开/折叠
        findViewById<View>(R.id.row_ssh)?.setOnClickListener {
            toggleSSHExpand()
        }

        // 运行命令
        findViewById<View>(R.id.row_run)?.setOnClickListener {
            listener?.onRunCommand()
            dismiss()
        }

        // 快捷设置
        findViewById<View>(R.id.row_settings)?.setOnClickListener {
            listener?.onQuickSettings()
            dismiss()
        }

        // 关闭按钮
        findViewById<View>(R.id.btn_close)?.setOnClickListener { dismiss() }

        // 管理SSH连接
        tvManageSSH.setOnClickListener {
            listener?.onSSHConnection()
            dismiss()
        }

        // 设置SSH配置列表
        setupSSHRecyclerView()
    }

    private fun setupSSHRecyclerView() {
        rvSSHConfigs.layoutManager = LinearLayoutManager(context)
        sshAdapter = SSHConfigAdapter { config ->
            listener?.onSSHConfigSelected(config)
            dismiss()
        }
        rvSSHConfigs.adapter = sshAdapter
    }

    private fun toggleSSHExpand() {
        isSSHExpanded = !isSSHExpanded

        // 展开/折叠动画
        if (isSSHExpanded) {
            containerSSHList.visibility = View.VISIBLE
            ivSSHExpand.animate().rotation(180f).setDuration(200).start()
            loadSSHConfigs()
        } else {
            containerSSHList.visibility = View.GONE
            ivSSHExpand.animate().rotation(0f).setDuration(200).start()
        }
    }

    private fun loadSSHConfigs() {
        val configs = sshConfigManager.getAllConfigs()
            .sortedBy { it.name ?: it.host }

        if (configs.isEmpty()) {
            rvSSHConfigs.visibility = View.GONE
            tvSSHEmpty.visibility = View.VISIBLE
        } else {
            rvSSHConfigs.visibility = View.VISIBLE
            tvSSHEmpty.visibility = View.GONE
            sshAdapter?.setConfigs(configs)
        }
    }

    /**
     * SSH配置列表适配器
     */
    private class SSHConfigAdapter(
        private val onConfigClick: (SSHConnectionConfig) -> Unit
    ) : RecyclerView.Adapter<SSHConfigAdapter.ViewHolder>() {

        private var configs: List<SSHConnectionConfig> = emptyList()

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tv_ssh_name)
            val tvInfo: TextView = itemView.findViewById(R.id.tv_ssh_info)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ssh_config_menu, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val config = configs[position]
            holder.tvName.text = config.name ?: config.host
            holder.tvInfo.text = "${config.username}@${config.host}:${config.port}"
            holder.itemView.setOnClickListener {
                onConfigClick(config)
            }
        }

        override fun getItemCount(): Int = configs.size

        fun setConfigs(newConfigs: List<SSHConnectionConfig>) {
            configs = newConfigs
            notifyDataSetChanged()
        }
    }
}
