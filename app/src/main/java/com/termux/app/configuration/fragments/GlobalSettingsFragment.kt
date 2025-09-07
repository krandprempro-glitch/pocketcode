package com.termux.app.configuration.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.termux.R
import com.termux.app.floating.managers.FloatingWindowManager
import com.termux.app.floating.services.FloatingPermissionService

/**
 * 全局设置Fragment
 * 包含悬浮窗开关等设置选项
 */
class GlobalSettingsFragment : Fragment() {
    
    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        
        fun newInstance(): GlobalSettingsFragment {
            return GlobalSettingsFragment()
        }
    }
    
    private lateinit var floatingManager: FloatingWindowManager
    private lateinit var permissionService: FloatingPermissionService
    private lateinit var floatingToggle: Switch
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_global_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initManagers()
        initViews(view)
        setupFloatingToggle()
    }
    
    private fun initManagers() {
        floatingManager = FloatingWindowManager.getInstance(requireContext())
        permissionService = FloatingPermissionService(requireContext())
    }
    
    private fun initViews(view: View) {
        floatingToggle = view.findViewById(R.id.switch_floating_window)
    }
    
    private fun setupFloatingToggle() {
        // 设置初始状态
        floatingToggle.isChecked = floatingManager.isFloatingEnabled()
        
        // 设置开关监听器
        floatingToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableFloating()
            } else {
                disableFloating()
            }
        }
    }
    
    private fun enableFloating() {
        if (permissionService.hasOverlayPermission()) {
            floatingManager.enableFloating()
        } else {
            // 需要权限，先取消选中
            floatingToggle.isChecked = false
            // 请求权限
            requestFloatingPermission()
        }
    }
    
    private fun disableFloating() {
        floatingManager.disableFloating()
    }
    
    private fun requestFloatingPermission() {
        floatingManager.requestFloatingPermission(
            requireActivity(),
            object : FloatingPermissionService.OnPermissionResultListener {
                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        floatingToggle.isChecked = true
                        floatingManager.enableFloating()
                    }
                }
            }
        )
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            val granted = permissionService.hasOverlayPermission()
            if (granted) {
                floatingToggle.isChecked = true
                floatingManager.enableFloating()
            }
        }
    }
}