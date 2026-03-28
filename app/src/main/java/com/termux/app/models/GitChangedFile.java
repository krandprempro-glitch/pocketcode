package com.termux.app.models;

/**
 * Git changed file data model
 */
public class GitChangedFile {
    public static final String STATUS_ADDED = "A";
    public static final String STATUS_MODIFIED = "M";
    public static final String STATUS_DELETED = "D";
    public static final String STATUS_RENAMED = "R";

    private String path;
    private String status; // "A"=added, "M"=modified, "D"=deleted, "R"=renamed

    public GitChangedFile() {}

    public GitChangedFile(String path, String status) {
        this.path = path;
        this.status = status;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}