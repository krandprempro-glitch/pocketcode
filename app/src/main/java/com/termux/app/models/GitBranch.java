package com.termux.app.models;

/**
 * Git 分支数据模型
 */
public class GitBranch {
    private String name;
    private boolean isCurrent;

    public GitBranch() {}

    public GitBranch(String name, boolean isCurrent) {
        this.name = name;
        this.isCurrent = isCurrent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public void setCurrent(boolean current) {
        isCurrent = current;
    }
}
