package com.termux.app.configuration.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.termux.R
import com.termux.app.configuration.fragments.RunConfigDetailFragment

/**
 * 运行配置详情Activity
 * 独立Activity，包含工具栏和返回按钮
 */
class RunConfigDetailActivity : AppCompatActivity() {
    
    companion object {
        private const val EXTRA_CONFIG_ID = "config_id"
        
        fun newIntent(context: Context, configId: String? = null): Intent {
            return Intent(context, RunConfigDetailActivity::class.java).apply {
                putExtra(EXTRA_CONFIG_ID, configId)
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
        
        val configId = intent.getStringExtra(EXTRA_CONFIG_ID)
        val title = if (configId != null) "编辑运行配置" else "新建运行配置"
        
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setTitle(title)
        }
    }
    
    private fun setupFragment() {
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            val configId = intent.getStringExtra(EXTRA_CONFIG_ID)
            val fragment = RunConfigDetailFragment.newInstance(configId)
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