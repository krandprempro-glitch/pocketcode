package com.termux.app.configuration.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.termux.R
import com.termux.app.configuration.fragments.SshConfigListFragment

/**
 * SSH配置列表Activity
 * 独立Activity，包含工具栏和返回按钮
 */
class SshConfigListActivity : AppCompatActivity() {
    
    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SshConfigListActivity::class.java)
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
        
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "SSH配置"
        }
    }
    
    private fun setupFragment() {
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            val fragment = SshConfigListFragment.newInstance()
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