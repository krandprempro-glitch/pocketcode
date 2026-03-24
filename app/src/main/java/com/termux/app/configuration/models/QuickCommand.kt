package com.termux.app.configuration.models

/**
 * 快速命令数据模型
 * 用于存储用户常用的命令快捷方式
 */
data class QuickCommand(
    var id: String = "",
    var name: String = "",           // 命令名称（如：启动MySQL）
    var command: String = "",        // 实际命令文本
    var description: String = "",    // 描述（可选）
    var category: String = "",       // 分类（可选）
    var createdTime: Long = System.currentTimeMillis(),
    var lastUsedTime: Long = 0L,
    var useCount: Int = 0            // 使用次数
) {

    /**
     * 验证命令是否有效
     */
    fun isValid(): Boolean {
        return name.isNotBlank() && command.isNotBlank()
    }

    /**
     * 生成唯一ID
     */
    fun generateUniqueId(): String {
        return "${System.currentTimeMillis()}_${name.hashCode()}"
    }

    /**
     * 更新最后使用时间
     */
    fun updateLastUsed() {
        lastUsedTime = System.currentTimeMillis()
        useCount++
    }

    companion object {
        /**
         * 创建默认快速命令
         */
        fun createDefault(): QuickCommand {
            return QuickCommand(
                id = System.currentTimeMillis().toString(),
                createdTime = System.currentTimeMillis(),
                lastUsedTime = 0L,
                useCount = 0
            )
        }
    }
}
