package com.termux.app.configuration.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.configuration.models.ConfigurationItem

class ConfigurationMainAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_NORMAL = 0
        private const val TYPE_VERSION = 1
    }

    private var items: List<ConfigurationItem> = emptyList()
    private var onItemClickListener: ((ConfigurationItem) -> Unit)? = null

    fun setItems(items: List<ConfigurationItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (ConfigurationItem) -> Unit) {
        this.onItemClickListener = listener
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].type == ConfigurationItem.ConfigurationType.ABOUT) {
            TYPE_VERSION
        } else {
            TYPE_NORMAL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_VERSION) {
            val view = LayoutInflater.from(context).inflate(
                R.layout.item_configuration_version,
                parent,
                false
            )
            VersionViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(
                R.layout.item_configuration_main,
                parent,
                false
            )
            ConfigViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ConfigViewHolder -> holder.bind(item)
            is VersionViewHolder -> holder.bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ConfigViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(items[position])
                }
            }
        }

        fun bind(item: ConfigurationItem) {
            ivIcon.setImageResource(item.iconRes)
            tvTitle.text = item.title
            tvDescription.text = item.description
        }
    }

    inner class VersionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val tvVersion: TextView = itemView.findViewById(R.id.tv_version)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)

        fun bind(item: ConfigurationItem) {
            ivIcon.setImageResource(item.iconRes)
            tvVersion.text = item.title
            tvDescription.text = item.description
        }
    }
}
