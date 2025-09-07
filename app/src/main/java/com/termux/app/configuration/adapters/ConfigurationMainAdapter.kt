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
) : RecyclerView.Adapter<ConfigurationMainAdapter.ConfigurationViewHolder>() {
    
    private var items: List<ConfigurationItem> = emptyList()
    private var onItemClickListener: ((ConfigurationItem) -> Unit)? = null
    
    fun setItems(items: List<ConfigurationItem>) {
        this.items = items
        notifyDataSetChanged()
    }
    
    fun setOnItemClickListener(listener: (ConfigurationItem) -> Unit) {
        this.onItemClickListener = listener
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConfigurationViewHolder {
        val view = LayoutInflater.from(context).inflate(
            R.layout.item_configuration_main, 
            parent, 
            false
        )
        return ConfigurationViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ConfigurationViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }
    
    override fun getItemCount(): Int = items.size
    
    inner class ConfigurationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
            
            // 设置启用状态
            itemView.isEnabled = item.enabled
            itemView.alpha = if (item.enabled) 1.0f else 0.5f
        }
    }
}