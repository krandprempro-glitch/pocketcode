package com.termux.app.configuration.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.termux.R
import com.termux.app.clipboard.ClipboardSyncManager

/**
 * 全局设置Fragment
 * 包含剪贴板同步等设置选项
 */
class GlobalSettingsFragment : Fragment() {

    companion object {
        private const val PREF_CLIPBOARD_SYNC_MASTER = "clipboard_sync_master"
        private const val PREF_CLIPBOARD_SYNC_SERVER_TO_PHONE = "clipboard_sync_server_to_phone"
        private const val PREF_CLIPBOARD_SYNC_PHONE_TO_SERVER = "clipboard_sync_phone_to_server"

        fun newInstance(): GlobalSettingsFragment {
            return GlobalSettingsFragment()
        }
    }

    private lateinit var switchMaster: Switch
    private lateinit var switchServerToPhone: Switch
    private lateinit var switchPhoneToServer: Switch
    private lateinit var cardServerToPhone: CardView
    private lateinit var cardPhoneToServer: CardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_global_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchMaster = view.findViewById(R.id.switch_clipboard_master)
        switchServerToPhone = view.findViewById(R.id.switch_server_to_phone)
        switchPhoneToServer = view.findViewById(R.id.switch_phone_to_server)
        cardServerToPhone = view.findViewById(R.id.card_server_to_phone)
        cardPhoneToServer = view.findViewById(R.id.card_phone_to_server)

        loadPreferences()
        setupListeners()
        applyMasterToggleState()
    }

    private fun loadPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val masterEnabled = prefs.getBoolean(PREF_CLIPBOARD_SYNC_MASTER, false)
        val serverToPhone = prefs.getBoolean(PREF_CLIPBOARD_SYNC_SERVER_TO_PHONE, true)
        val phoneToServer = prefs.getBoolean(PREF_CLIPBOARD_SYNC_PHONE_TO_SERVER, false)

        switchMaster.isChecked = masterEnabled
        switchServerToPhone.isChecked = if (masterEnabled) serverToPhone else false
        switchPhoneToServer.isChecked = if (masterEnabled) phoneToServer else false
    }

    private fun setupListeners() {
        switchMaster.setOnCheckedChangeListener { _, isChecked ->
            savePreference(PREF_CLIPBOARD_SYNC_MASTER, isChecked)
            if (isChecked) {
                // Restore saved sub-toggle states
                val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                switchServerToPhone.isChecked = prefs.getBoolean(PREF_CLIPBOARD_SYNC_SERVER_TO_PHONE, true)
                switchPhoneToServer.isChecked = prefs.getBoolean(PREF_CLIPBOARD_SYNC_PHONE_TO_SERVER, false)
            } else {
                // Force sub-toggles off
                switchServerToPhone.isChecked = false
                switchPhoneToServer.isChecked = false
            }
            applyMasterToggleState()
            notifySyncManager()
        }

        switchServerToPhone.setOnCheckedChangeListener { _, isChecked ->
            savePreference(PREF_CLIPBOARD_SYNC_SERVER_TO_PHONE, isChecked)
            notifySyncManager()
        }

        switchPhoneToServer.setOnCheckedChangeListener { _, isChecked ->
            savePreference(PREF_CLIPBOARD_SYNC_PHONE_TO_SERVER, isChecked)
            notifySyncManager()
        }
    }

    private fun applyMasterToggleState() {
        val enabled = switchMaster.isChecked
        switchServerToPhone.isEnabled = enabled
        switchPhoneToServer.isEnabled = enabled
        cardServerToPhone.alpha = if (enabled) 1.0f else 0.4f
        cardPhoneToServer.alpha = if (enabled) 1.0f else 0.4f
    }

    private fun savePreference(key: String, value: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit()
            .putBoolean(key, value)
            .apply()
    }

    private fun notifySyncManager() {
        ClipboardSyncManager.getInstance().updateSettings(
            switchMaster.isChecked,
            switchServerToPhone.isChecked,
            switchPhoneToServer.isChecked
        )
    }
}
