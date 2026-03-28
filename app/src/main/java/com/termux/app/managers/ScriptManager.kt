package com.termux.app.managers

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.termux.app.models.ScriptItem
import com.termux.shared.logger.Logger
import java.io.IOException

class ScriptManager private constructor() {

    companion object {
        private const val LOG_TAG = "ScriptManager"
        private const val SCRIPTS_DIR = "scripts"
        private const val SCRIPTS_INDEX = "scripts.json"

        @Volatile
        private var instance: ScriptManager? = null

        @JvmStatic
        fun getInstance(): ScriptManager {
            return instance ?: synchronized(this) {
                instance ?: ScriptManager().also { instance = it }
            }
        }
    }

    private var cachedScripts: List<ScriptItem>? = null

    data class ScriptsIndex(
        val scripts: List<ScriptEntry>
    )

    data class ScriptEntry(
        val name: String,
        val description: String,
        val file: String
    )

    fun loadScripts(context: Context): List<ScriptItem> {
        cachedScripts?.let { return it }

        val scripts = mutableListOf<ScriptItem>()

        try {
            // Read scripts.json index
            val indexContent = context.assets.open("$SCRIPTS_DIR/$SCRIPTS_INDEX").bufferedReader().use { it.readText() }
            val index = Gson().fromJson(indexContent, ScriptsIndex::class.java)

            // Load each script
            for (entry in index.scripts) {
                try {
                    val content = context.assets.open("$SCRIPTS_DIR/${entry.file}").bufferedReader().use { it.readText() }
                    scripts.add(ScriptItem(
                        name = entry.name,
                        description = entry.description,
                        fileName = entry.file,
                        content = content
                    ))
                } catch (e: IOException) {
                    Logger.logWarn(LOG_TAG, "Failed to load script: ${entry.file}")
                }
            }
        } catch (e: IOException) {
            Logger.logError(LOG_TAG, "Failed to load scripts index: ${e.message}")
        }

        cachedScripts = scripts
        return scripts
    }

    fun getScripts(context: Context): List<ScriptItem> {
        return cachedScripts ?: loadScripts(context)
    }

    fun getScriptContent(context: Context, name: String): String? {
        return getScripts(context).find { it.name == name }?.content
    }

    fun clearCache() {
        cachedScripts = null
    }
}