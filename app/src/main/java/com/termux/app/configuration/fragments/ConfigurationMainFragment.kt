package com.termux.app.configuration.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.configuration.adapters.ConfigurationMainAdapter
import com.termux.app.configuration.activities.SshConfigListActivity
import com.termux.app.configuration.activities.RunConfigListActivity
import com.termux.app.configuration.activities.QuickCommandListActivity
import com.termux.app.configuration.activities.GlobalSettingsActivity
import com.termux.app.configuration.models.ConfigurationItem
import com.termux.shared.android.PackageUtils

class ConfigurationMainFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ConfigurationMainAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_configuration_main_simple, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.rv_configuration_items)
    }
    
    private fun setupRecyclerView() {
        adapter = ConfigurationMainAdapter(requireContext())
        adapter.setOnItemClickListener { item ->
            onConfigurationItemClicked(item)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        
        // 设置配置项数据
        val items = ConfigurationItem.DEFAULT_ITEMS.map { item ->
            if (item.type == ConfigurationItem.ConfigurationType.ABOUT) {
                val versionName = PackageUtils.getVersionNameForPackage(requireContext()) ?: "unknown"
                item.copy(title = "v$versionName")
            } else {
                item
            }
        }
        adapter.setItems(items)
    }
    
    private fun onConfigurationItemClicked(item: ConfigurationItem) {
        when (item.type) {
            ConfigurationItem.ConfigurationType.SSH_CONFIG -> {
                val intent = SshConfigListActivity.newIntent(requireContext())
                startActivity(intent)
            }
            ConfigurationItem.ConfigurationType.RUN_CONFIG -> {
                val intent = RunConfigListActivity.newIntent(requireContext())
                startActivity(intent)
            }
            ConfigurationItem.ConfigurationType.QUICK_COMMANDS -> {
                val intent = QuickCommandListActivity.newIntent(requireContext())
                startActivity(intent)
            }
            ConfigurationItem.ConfigurationType.GLOBAL_SETTINGS -> {
                val intent = GlobalSettingsActivity.newIntent(requireContext())
                startActivity(intent)
            }
            else -> {}
        }
    }
    
    companion object {
        fun newInstance(): ConfigurationMainFragment {
            return ConfigurationMainFragment()
        }
    }
}