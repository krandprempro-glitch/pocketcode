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
import com.termux.app.configuration.managers.ConfigNavigationManager
import com.termux.app.configuration.models.ConfigurationItem

class ConfigurationMainFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ConfigurationMainAdapter
    private lateinit var navigationManager: ConfigNavigationManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_configuration_main, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupNavigation()
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
        adapter.setItems(ConfigurationItem.DEFAULT_ITEMS.toList())
    }
    
    private fun setupNavigation() {
        // 使用childFragmentManager来管理子Fragment的导航
        // ViewPager2中的Fragment应该使用自己的FragmentManager来管理子Fragment
        navigationManager = ConfigNavigationManager(childFragmentManager, android.R.id.content)
    }
    
    private fun onConfigurationItemClicked(item: ConfigurationItem) {
        navigationManager.navigateByConfigType(item.type)
    }
    
    companion object {
        fun newInstance(): ConfigurationMainFragment {
            return ConfigurationMainFragment()
        }
    }
}