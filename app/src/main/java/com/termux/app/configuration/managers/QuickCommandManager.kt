package com.termux.app.configuration.managers

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.termux.app.configuration.models.QuickCommand
import com.termux.app.configuration.utils.ConfigurationConstants
import com.termux.shared.logger.Logger

/**
 * 快速命令管理类
 * 使用单例模式管理快速命令的增删改查操作
 */
class QuickCommandManager private constructor(context: Context) {

    private val context: Context = context.applicationContext
    private val preferences: SharedPreferences = context.getSharedPreferences(
        ConfigurationConstants.PREFS_QUICK_COMMANDS,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        @Volatile
        private var INSTANCE: QuickCommandManager? = null

        /**
         * 获取单例实例
         */
        @JvmStatic
        fun getInstance(context: Context): QuickCommandManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: QuickCommandManager(context).also { INSTANCE = it }
            }
        }
    }

    /**
     * 保存命令
     * @param config 快速命令对象
     * @return 保存是否成功
     */
    fun saveCommand(config: QuickCommand): Boolean {
        return try {
            // 如果ID为空，生成新ID
            if (config.id.isBlank()) {
                config.id = config.generateUniqueId()
            }

            val json = gson.toJson(config)
            preferences.edit()
                .putString("${ConfigurationConstants.KEY_QUICK_CMD_PREFIX}${config.id}", json)
                .apply()
            true
        } catch (e: Exception) {
            Logger.logError("QuickCommandManager", "保存命令失败: ${e.message}")
            false
        }
    }

    /**
     * 获取单个命令
     * @param id 命令ID
     * @return 快速命令对象，如果不存在则返回null
     */
    fun getCommand(id: String): QuickCommand? {
        return try {
            val json = preferences.getString("${ConfigurationConstants.KEY_QUICK_CMD_PREFIX}$id", null)
            json?.let { gson.fromJson(it, QuickCommand::class.java) }
        } catch (e: Exception) {
            Logger.logError("QuickCommandManager", "获取命令失败: ${e.message}")
            null
        }
    }

    /**
     * 获取所有命令
     * @return 按最后使用时间倒序排列的命令列表
     */
    fun getAllCommands(): List<QuickCommand> {
        val commands = mutableListOf<QuickCommand>()
        try {
            val allEntries = preferences.all
            for ((key, value) in allEntries) {
                if (key.startsWith(ConfigurationConstants.KEY_QUICK_CMD_PREFIX) && value is String) {
                    val command = gson.fromJson(value, QuickCommand::class.java)
                    command?.let { commands.add(it) }
                }
            }
            // 按最后使用时间倒序排序
            commands.sortByDescending { it.lastUsedTime }
        } catch (e: Exception) {
            Logger.logError("QuickCommandManager", "获取所有命令失败: ${e.message}")
        }
        return commands
    }

    /**
     * 删除命令
     * @param id 命令ID
     * @return 删除是否成功
     */
    fun deleteCommand(id: String): Boolean {
        return try {
            preferences.edit()
                .remove("${ConfigurationConstants.KEY_QUICK_CMD_PREFIX}$id")
                .apply()
            true
        } catch (e: Exception) {
            Logger.logError("QuickCommandManager", "删除命令失败: ${e.message}")
            false
        }
    }

    /**
     * 更新最后使用时间和使用次数
     * @param id 命令ID
     */
    fun updateLastUsed(id: String) {
        val command = getCommand(id)
        command?.let {
            it.updateLastUsed()
            saveCommand(it)
        }
    }

    /**
     * 检查名称是否存在
     * @param name 命令名称
     * @param excludeId 排除的ID（用于编辑时排除自身）
     * @return 名称是否存在
     */
    fun isNameExists(name: String, excludeId: String? = null): Boolean {
        return getAllCommands().any {
            it.name == name && it.id != excludeId
        }
    }

    /**
     * 按分类获取命令
     * @param category 分类名称
     * @return 该分类下的所有命令
     */
    fun getCommandsByCategory(category: String): List<QuickCommand> {
        return getAllCommands().filter { it.category == category }
    }

    /**
     * 获取所有分类
     * @return 分类名称列表
     */
    fun getAllCategories(): List<String> {
        return getAllCommands()
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
    }

    /**
     * 获取最近使用的命令
     * @param limit 限制数量
     * @return 最近使用的命令列表
     */
    fun getRecentCommands(limit: Int = 10): List<QuickCommand> {
        return getAllCommands().take(limit)
    }

    /**
     * 获取最常用的命令
     * @param limit 限制数量
     * @return 使用次数最多的命令列表
     */
    fun getMostUsedCommands(limit: Int = 10): List<QuickCommand> {
        return getAllCommands()
            .sortedByDescending { it.useCount }
            .take(limit)
    }

    /**
     * 获取命令数量
     * @return 命令总数
     */
    fun getCommandCount(): Int {
        return getAllCommands().size
    }

    /**
     * 清除所有命令
     * @return 清除是否成功
     */
    fun clearAllCommands(): Boolean {
        return try {
            val editor = preferences.edit()
            val allEntries = preferences.all
            for (key in allEntries.keys) {
                if (key.startsWith(ConfigurationConstants.KEY_QUICK_CMD_PREFIX)) {
                    editor.remove(key)
                }
            }
            editor.apply()
            true
        } catch (e: Exception) {
            Logger.logError("QuickCommandManager", "清除所有命令失败: ${e.message}")
            false
        }
    }
}
