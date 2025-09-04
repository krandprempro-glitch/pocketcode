package com.termux.app.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.termux.shared.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * SSH配置管理器
 * 负责SSH连接配置的持久化存储和管理
 */
public class SSHConfigManager {
    private static final String PREF_NAME = "termux_ssh_configs";
    private static final String KEY_SSH_CONFIGS = "ssh_configs";
    private static final String KEY_LAST_USED_CONFIG = "last_used_config";
    private static final String LOG_TAG = "SSHConfigManager";
    
    private final SharedPreferences mPreferences;
    private static SSHConfigManager instance;

    private SSHConfigManager(Context context) {
        mPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 获取单例实例
     */
    public static synchronized SSHConfigManager getInstance(Context context) {
        if (instance == null) {
            instance = new SSHConfigManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 保存SSH配置
     */
    public boolean saveConfig(SSHConnectionConfig config) {
        if (config == null || !config.isValid()) {
            Logger.logError(LOG_TAG, "Invalid SSH config provided");
            return false;
        }

        try {
            List<SSHConnectionConfig> configs = getAllConfigs();
            
            // 检查是否已存在同名配置
            for (int i = 0; i < configs.size(); i++) {
                if (configs.get(i).getName().equals(config.getName())) {
                    configs.set(i, config); // 更新现有配置
                    return saveAllConfigs(configs);
                }
            }
            
            // 添加新配置
            configs.add(config);
            return saveAllConfigs(configs);
            
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to save SSH config", e);
            return false;
        }
    }

    /**
     * 获取所有SSH配置
     */
    public List<SSHConnectionConfig> getAllConfigs() {
        List<SSHConnectionConfig> configs = new ArrayList<>();
        
        try {
            String configsJson = mPreferences.getString(KEY_SSH_CONFIGS, null);
            if (TextUtils.isEmpty(configsJson)) {
                return configs;
            }

            JSONArray jsonArray = new JSONArray(configsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject configJson = jsonArray.getJSONObject(i);
                SSHConnectionConfig config = SSHConnectionConfig.fromJson(configJson.toString());
                if (config != null && config.isValid()) {
                    configs.add(config);
                }
            }
        } catch (JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to parse SSH configs", e);
        }
        
        return configs;
    }

    /**
     * 根据名称获取SSH配置
     */
    public SSHConnectionConfig getConfigByName(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }

        List<SSHConnectionConfig> configs = getAllConfigs();
        for (SSHConnectionConfig config : configs) {
            if (name.equals(config.getName())) {
                return config;
            }
        }
        return null;
    }

    /**
     * 删除SSH配置
     */
    public boolean deleteConfig(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }

        try {
            List<SSHConnectionConfig> configs = getAllConfigs();
            configs.removeIf(config -> name.equals(config.getName()));
            return saveAllConfigs(configs);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to delete SSH config", e);
            return false;
        }
    }

    /**
     * 保存最后使用的配置名称
     */
    public void setLastUsedConfig(String configName) {
        mPreferences.edit().putString(KEY_LAST_USED_CONFIG, configName).apply();
    }

    /**
     * 获取最后使用的配置
     */
    public SSHConnectionConfig getLastUsedConfig() {
        String lastUsedName = mPreferences.getString(KEY_LAST_USED_CONFIG, null);
        if (TextUtils.isEmpty(lastUsedName)) {
            return null;
        }
        return getConfigByName(lastUsedName);
    }

    /**
     * 保存所有配置到SharedPreferences
     */
    private boolean saveAllConfigs(List<SSHConnectionConfig> configs) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (SSHConnectionConfig config : configs) {
                String configJson = config.toJson();
                if (!TextUtils.isEmpty(configJson)) {
                    jsonArray.put(new JSONObject(configJson));
                }
            }
            
            return mPreferences.edit()
                    .putString(KEY_SSH_CONFIGS, jsonArray.toString())
                    .commit();
                    
        } catch (JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to save SSH configs", e);
            return false;
        }
    }

    /**
     * 清空所有配置
     */
    public boolean clearAllConfigs() {
        return mPreferences.edit()
                .remove(KEY_SSH_CONFIGS)
                .remove(KEY_LAST_USED_CONFIG)
                .commit();
    }

    /**
     * 检查是否存在配置
     */
    public boolean hasConfigs() {
        return !getAllConfigs().isEmpty();
    }

    /**
     * 获取配置数量
     */
    public int getConfigCount() {
        return getAllConfigs().size();
    }
}