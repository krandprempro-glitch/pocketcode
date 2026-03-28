package com.termux.app.managers;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.termux.app.models.ScriptItem;
import com.termux.shared.logger.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ScriptManager {
    private static final String TAG = "ScriptManager";
    private static final String SCRIPTS_DIR = "scripts";
    private static final String SCRIPTS_INDEX = "scripts.json";

    private static ScriptManager instance;
    private List<ScriptItem> cachedScripts;

    public static ScriptManager getInstance() {
        if (instance == null) {
            instance = new ScriptManager();
        }
        return instance;
    }

    public List<ScriptItem> getScripts(Context context) {
        if (cachedScripts != null) {
            return cachedScripts;
        }

        cachedScripts = new ArrayList<>();

        try {
            String indexContent = readAssetFile(context, SCRIPTS_DIR + "/" + SCRIPTS_INDEX);
            ScriptsIndex index = new Gson().fromJson(indexContent, ScriptsIndex.class);

            if (index != null && index.scripts != null) {
                for (ScriptEntry entry : index.scripts) {
                    try {
                        String content = readAssetFile(context, SCRIPTS_DIR + "/" + entry.file);
                        cachedScripts.add(new ScriptItem(entry.name, entry.description, entry.file, content));
                    } catch (Exception e) {
                        Logger.logWarn(TAG, "Failed to load script: " + entry.file);
                    }
                }
            }
        } catch (Exception e) {
            Logger.logError(TAG, "Failed to load scripts index: " + e.getMessage());
        }

        return cachedScripts;
    }

    private String readAssetFile(Context context, String path) throws IOException {
        try (Reader reader = new InputStreamReader(context.getAssets().open(path), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[1024];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        }
    }

    public void clearCache() {
        cachedScripts = null;
    }

    static class ScriptsIndex {
        @SerializedName("scripts")
        List<ScriptEntry> scripts;
    }

    static class ScriptEntry {
        String name;
        String description;
        String file;
    }
}
