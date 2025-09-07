package com.termux.app.configuration.managers

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.termux.app.configuration.models.ConfigurationItem

class ConfigNavigationManager(
    private val fragmentManager: FragmentManager,
    private val containerId: Int
) {
    
    companion object {
        const val TAG_SSH_CONFIG_LIST = "ssh_config_list"
        const val TAG_SSH_CONFIG_DETAIL = "ssh_config_detail"
        const val TAG_RUN_CONFIG_LIST = "run_config_list"
        const val TAG_RUN_CONFIG_DETAIL = "run_config_detail"
        const val TAG_GLOBAL_SETTINGS = "global_settings"
        const val TAG_ABOUT = "about"
    }
    
    /**
     * 导航到SSH配置列表
     */
    fun navigateToSSHConfigList() {
        // TODO: 在Phase 2实现
        // val fragment = SshConfigListFragment()
        // replaceFragment(fragment, TAG_SSH_CONFIG_LIST, true)
        showPlaceholder("SSH配置列表")
    }
    
    /**
     * 导航到SSH配置详情
     */
    fun navigateToSSHConfigDetail(configId: String?) {
        // TODO: 在Phase 2实现
        // val fragment = SshConfigDetailFragment.newInstance(configId)
        // replaceFragment(fragment, TAG_SSH_CONFIG_DETAIL, true)
        showPlaceholder("SSH配置详情: $configId")
    }
    
    /**
     * 导航到运行配置列表
     */
    fun navigateToRunConfigList() {
        // TODO: 在Phase 2实现
        // val fragment = RunConfigListFragment()
        // replaceFragment(fragment, TAG_RUN_CONFIG_LIST, true)
        showPlaceholder("运行配置列表")
    }
    
    /**
     * 导航到运行配置详情
     */
    fun navigateToRunConfigDetail(configId: String?) {
        // TODO: 在Phase 2实现
        // val fragment = RunConfigDetailFragment.newInstance(configId)
        // replaceFragment(fragment, TAG_RUN_CONFIG_DETAIL, true)
        showPlaceholder("运行配置详情: $configId")
    }
    
    /**
     * 导航到全局设置
     */
    fun navigateToGlobalSettings() {
        // TODO: 在Phase 2实现
        // val fragment = GlobalSettingsFragment()
        // replaceFragment(fragment, TAG_GLOBAL_SETTINGS, true)
        showPlaceholder("全局设置")
    }
    
    /**
     * 显示关于页面
     */
    fun navigateToAbout() {
        // TODO: 在Phase 2实现
        // val fragment = AboutFragment()
        // replaceFragment(fragment, TAG_ABOUT, true)
        showPlaceholder("关于页面")
    }
    
    /**
     * 根据配置项类型导航
     */
    fun navigateByConfigType(type: ConfigurationItem.ConfigurationType) {
        when (type) {
            ConfigurationItem.ConfigurationType.SSH_CONFIG -> navigateToSSHConfigList()
            ConfigurationItem.ConfigurationType.RUN_CONFIG -> navigateToRunConfigList()
            ConfigurationItem.ConfigurationType.GLOBAL_SETTINGS -> navigateToGlobalSettings()
            ConfigurationItem.ConfigurationType.ABOUT -> navigateToAbout()
        }
    }
    
    /**
     * 返回上级页面
     */
    fun navigateBack(): Boolean {
        return if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStack()
            true
        } else {
            false
        }
    }
    
    /**
     * 返回主页
     */
    fun navigateToMain() {
        // 清空返回栈，回到主页
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
    
    /**
     * 替换Fragment
     */
    private fun replaceFragment(fragment: Fragment, tag: String, addToBackStack: Boolean) {
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(containerId, fragment, tag)
        
        if (addToBackStack) {
            transaction.addToBackStack(tag)
        }
        
        transaction.commit()
    }
    
    /**
     * 添加Fragment
     */
    private fun addFragment(fragment: Fragment, tag: String) {
        val transaction = fragmentManager.beginTransaction()
        transaction.add(containerId, fragment, tag)
        transaction.addToBackStack(tag)
        transaction.commit()
    }
    
    /**
     * Phase 1阶段的占位显示
     * 在Phase 2会移除此方法并实现真正的Fragment导航
     */
    private fun showPlaceholder(title: String) {
        // 创建一个简单的占位Fragment来显示功能
        val fragment = PlaceholderFragment.newInstance(title)
        replaceFragment(fragment, "placeholder", true)
    }
    
    /**
     * 检查Fragment是否存在于返回栈中
     */
    fun isFragmentInBackStack(tag: String): Boolean {
        for (i in 0 until fragmentManager.backStackEntryCount) {
            if (fragmentManager.getBackStackEntryAt(i).name == tag) {
                return true
            }
        }
        return false
    }
    
    /**
     * 获取当前Fragment的标签
     */
    fun getCurrentFragmentTag(): String? {
        val fragment = fragmentManager.findFragmentById(containerId)
        return fragment?.tag
    }
}

/**
 * 占位Fragment - Phase 1使用
 * Phase 2实现具体功能后将被移除
 */
class PlaceholderFragment : Fragment() {
    
    companion object {
        private const val ARG_TITLE = "title"
        
        fun newInstance(title: String): PlaceholderFragment {
            val fragment = PlaceholderFragment()
            val args = android.os.Bundle()
            args.putString(ARG_TITLE, title)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        val title = arguments?.getString(ARG_TITLE) ?: "未知页面"
        
        val view = inflater.inflate(android.R.layout.simple_list_item_1, container, false)
        val textView = view.findViewById<android.widget.TextView>(android.R.id.text1)
        textView.text = "$title\n(Phase 2将实现具体功能)"
        textView.gravity = android.view.Gravity.CENTER
        return view
    }
}