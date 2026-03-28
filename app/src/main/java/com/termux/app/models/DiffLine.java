package com.termux.app.models;

public class DiffLine {

    public enum Type {
        ADD,        // line starting with '+'
        REMOVE,     // line starting with '-'
        CONTEXT,    // unchanged line
        HUNK_HEADER,// @@ -x,y +a,b @@
        FILE_HEADER,// diff --git a/... b/...
        NO_NEWLINE  // \ No newline at end of file
    }

    private final Type type;
    private final int oldLineNumber;  // -1 if N/A
    private final int newLineNumber;  // -1 if N/A
    private final String content;     // text content without +/- prefix

    public DiffLine(Type type, int oldLineNumber, int newLineNumber, String content) {
        this.type = type;
        this.oldLineNumber = oldLineNumber;
        this.newLineNumber = newLineNumber;
        this.content = content;
    }

    public Type getType() { return type; }
    public int getOldLineNumber() { return oldLineNumber; }
    public int getNewLineNumber() { return newLineNumber; }
    public String getContent() { return content; }
}
