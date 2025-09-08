package com.termux.app.models;

import android.text.TextUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * SSH连接配置数据模型
 */
public class SSHConnectionConfig {
    private String id;          // 配置唯一标识符
    private String name;        // 配置名称
    private String host;        // 主机IP地址
    private int port;           // 端口号，默认22
    private String username;    // 用户名
    private String password;    // 密码
    private String privateKeyPath; // 私钥文件路径
    private Integer connectionTimeout; // 连接超时时间(秒)

    public SSHConnectionConfig() {
        this.port = 22; // 默认SSH端口
    }

    public SSHConnectionConfig(String name, String host, int port, String username, String password) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPrivateKeyPath() { return privateKeyPath; }
    public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }

    public Integer getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(Integer connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        if (!TextUtils.isEmpty(name)) {
            return name;
        }
        return username + "@" + host + ":" + port;
    }

    /**
     * 验证配置是否有效
     */
    public boolean isValid() {
        return !TextUtils.isEmpty(host) && 
               !TextUtils.isEmpty(username) && 
               port > 0 && port <= 65535;
    }

    /**
     * 生成SSH连接命令
     */
    public String generateSSHCommand() {
        if (!isValid()) {
            return null;
        }
        
        StringBuilder command = new StringBuilder();
        command.append("ssh ");
        
        // 添加端口参数（如果不是默认端口）
        if (port != 22) {
            command.append("-p ").append(port).append(" ");
        }
        
        // 添加用户名和主机
        command.append(username).append("@").append(host);
        
        return command.toString();
    }

    /**
     * 转换为JSON字符串用于存储
     */
    public String toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("name", name);
            json.put("host", host);
            json.put("port", port);
            json.put("username", username);
            json.put("password", password);
            json.put("privateKeyPath", privateKeyPath);
            json.put("connectionTimeout", connectionTimeout);
            return json.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * 从JSON字符串创建对象
     */
    public static SSHConnectionConfig fromJson(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            SSHConnectionConfig config = new SSHConnectionConfig();
            config.setId(json.optString("id"));
            config.setName(json.optString("name"));
            config.setHost(json.optString("host"));
            config.setPort(json.optInt("port", 22));
            config.setUsername(json.optString("username"));
            config.setPassword(json.optString("password"));
            config.setPrivateKeyPath(json.optString("privateKeyPath"));
            if (json.has("connectionTimeout") && !json.isNull("connectionTimeout")) {
                config.setConnectionTimeout(json.optInt("connectionTimeout"));
            }
            return config;
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "SSHConnectionConfig{" +
                "name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                '}';
    }
}