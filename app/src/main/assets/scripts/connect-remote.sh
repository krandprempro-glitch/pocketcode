#!/bin/bash
# 连接远程服务器示例脚本
echo "=== Remote Connection Script ==="
echo "Hostname: $(hostname)"
echo "User: $(whoami)"
echo "Date: $(date)"
echo "Uptime: $(uptime -p 2>/dev/null || uptime)"