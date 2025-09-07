package com.termux.app.configuration.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.termux.R
import com.termux.app.configuration.fragments.SshConfigDetailFragment

/**
 * SSH配置详情Activity
 * 独立Activity，包含工具栏和返回按钮
 */
class SshConfigDetailActivity : AppCompatActivity() {
    
    companion object {
        private const val EXTRA_CONFIG_NAME = "config_name"
        
        fun newIntent(context: Context, configName: String? = null): Intent {
            return Intent(context, SshConfigDetailActivity::class.java).apply {
                putExtra(EXTRA_CONFIG_NAME, configName)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration_detail)
        
        setupToolbar()
        setupFragment()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        val configName = intent.getStringExtra(EXTRA_CONFIG_NAME)
        val title = if (configName != null) "编辑SSH配置" else "新建SSH配置"
        
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setTitle(title)
        }
    }
    
    private fun setupFragment() {
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            val configName = intent.getStringExtra(EXTRA_CONFIG_NAME)
            val fragment = SshConfigDetailFragment.newInstance(configName)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}