package com.termux.app.configuration.models

data class RunConfiguration(
    var id: String = "",
    var name: String = "",
    var sshConfigId: String = "",
    var projectPath: String = "",
    var languageType: LanguageType = LanguageType.CUSTOM,
    var command: String = "",
    var workingDir: String = ".",
    var envVariables: String = "",
    var port: Int = 0,
    var runInBackground: Boolean = true,
    var logFileName: String = "app.log",
    var createdTime: Long = System.currentTimeMillis(),
    var lastUsedTime: Long = System.currentTimeMillis()
) {
    
    fun isValid(): Boolean {
        return name.isNotBlank() && 
               sshConfigId.isNotBlank() && 
               projectPath.isNotBlank() && 
               command.isNotBlank()
    }
    
    fun generateUniqueId(): String {
        return "${System.currentTimeMillis()}_${name.hashCode()}"
    }
    
    fun updateLastUsed() {
        lastUsedTime = System.currentTimeMillis()
    }
    
    companion object {
        fun createDefault(): RunConfiguration {
            return RunConfiguration(
                id = System.currentTimeMillis().toString(),
                workingDir = ".",
                logFileName = "app.log",
                runInBackground = true
            )
        }
    }
}
