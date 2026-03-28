#!/bin/bash
# 系统检查脚本
echo "=== System Information ==="
echo "Hostname: $(hostname)"
echo "Kernel: $(uname -r)"
echo "OS: $(uname -o)"
echo ""
echo "=== Resource Usage ==="
echo "Memory:"
free -h 2>/dev/null || cat /proc/meminfo | head -3
echo ""
echo "Disk:"
df -h / 2>/dev/null | tail -1
echo ""
echo "=== Network ==="
ip -4 addr show 2>/dev/null | grep inet || hostname -I