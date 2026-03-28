package com.termux.app.models;

public class ScriptItem {
    private final String name;
    private final String description;
    private final String fileName;
    private final String content;

    public ScriptItem(String name, String description, String fileName, String content) {
        this.name = name;
        this.description = description;
        this.fileName = fileName;
        this.content = content;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getFileName() { return fileName; }
    public String getContent() { return content; }
}
