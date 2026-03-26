package com.termux.app.models;

/**
 * Git Commit 数据模型
 */
public class GitCommit {
    private String hash;       // 7位hash
    private String fullHash;  // 完整40位hash
    private String message;   // 提交消息
    private String author;     // 作者
    private long timestamp;   // 时间戳(秒)

    public GitCommit() {}

    public GitCommit(String hash, String fullHash, String message, String author, long timestamp) {
        this.hash = hash;
        this.fullHash = fullHash;
        this.message = message;
        this.author = author;
        this.timestamp = timestamp;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getFullHash() {
        return fullHash;
    }

    public void setFullHash(String fullHash) {
        this.fullHash = fullHash;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}