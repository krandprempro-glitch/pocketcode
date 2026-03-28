#!/bin/bash
#
# SSH Keepalive 配置脚本
# 用于防止 SSH 连接后台时因空闲断开
#
# 使用方法:
#   ./ssh-keepalive-setup.sh          # 交互式配置
#   ./ssh-keepalive-setup.sh --apply  # 自动应用默认配置
#   ./ssh-keepalive-setup.sh --check # 检查当前配置
#   ./ssh-keepalive-setup.sh --undo  # 恢复默认配置

set -e

# 默认配置值
DEFAULT_CLIENT_ALIVE_INTERVAL=30
DEFAULT_CLIENT_ALIVE_COUNT_MAX=3
DEFAULT_TCP_KEEPALIVE=yes

# SSH 配置文件路径
SSH_CONFIG_FILE="/etc/ssh/sshd_config"
SSH_CONFIG_BACKUP="/etc/ssh/sshd_config.backup.$(date +%Y%m%d_%H%M%S)"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查是否以 root 权限运行
check_root() {
    if [[ $EUID -ne 0 ]]; then
        print_error "此脚本需要 root 权限运行"
        print_info "请使用: sudo $0 $@"
        exit 1
    fi
}

# 检查配置文件中是否存在指定配置项
check_config_exists() {
    local config_name="$1"
    grep -q "^[[:space:]]*${config_name}[[:space:]]" "$SSH_CONFIG_FILE" 2>/dev/null
}

# 获取配置项的值
get_config_value() {
    local config_name="$1"
    grep "^[[:space:]]*${config_name}[[:space:]]" "$SSH_CONFIG_FILE" | awk '{print $2}' | head -1
}

# 检查当前配置
check_config() {
    print_info "检查当前 SSH Keepalive 配置..."
    echo ""

    local client_alive_interval
    local client_alive_count_max
    local tcp_keepalive

    if check_config_exists "ClientAliveInterval"; then
        client_alive_interval=$(get_config_value "ClientAliveInterval")
        echo -e "ClientAliveInterval: ${GREEN}${client_alive_interval}${NC} 秒"
    else
        echo -e "ClientAliveInterval: ${YELLOW}未设置 (默认关闭)${NC}"
    fi

    if check_config_exists "ClientAliveCountMax"; then
        client_alive_count_max=$(get_config_value "ClientAliveCountMax")
        echo -e "ClientAliveCountMax: ${GREEN}${client_alive_count_max}${NC} 次"
    else
        echo -e "ClientAliveCountMax: ${YELLOW}未设置 (默认3次)${NC}"
    fi

    if check_config_exists "TCPKeepAlive"; then
        tcp_keepalive=$(get_config_value "TCPKeepAlive")
        echo -e "TCPKeepAlive: ${GREEN}${tcp_keepalive}${NC}"
    else
        echo -e "TCPKeepAlive: ${YELLOW}未设置 (默认yes)${NC}"
    fi

    echo ""
    print_info "配置说明:"
    echo "  - ClientAliveInterval: 服务端发送心跳的间隔(秒)"
    echo "  - ClientAliveCountMax: 多少次心跳无响应后断开连接"
    echo "  - TCPKeepAlive: 底层TCP keepalive开关"
    echo ""
    print_info "推荐配置:"
    echo "  ClientAliveInterval 30"
    echo "  ClientAliveCountMax 3"
    echo "  TCPKeepAlive yes"
    echo ""
    print_info "计算: ${DEFAULT_CLIENT_ALIVE_INTERVAL}秒 x ${DEFAULT_CLIENT_ALIVE_COUNT_MAX}次 = ${DEFAULT_CLIENT_ALIVE_INTERVAL * DEFAULT_CLIENT_ALIVE_COUNT_MAX}秒无响应后断开"
}

# 应用配置
apply_config() {
    check_root

    print_info "应用 SSH Keepalive 配置..."

    # 备份原配置
    if [[ -f "$SSH_CONFIG_FILE" ]]; then
        cp "$SSH_CONFIG_FILE" "$SSH_CONFIG_BACKUP"
        print_info "已备份原配置到: $SSH_CONFIG_BACKUP"
    fi

    # 使用 sed 更新或添加配置
    local config_updated=false

    # 处理 ClientAliveInterval
    if check_config_exists "ClientAliveInterval"; then
        sed -i "s/^[[:space:]]*ClientAliveInterval[[:space:]].*/ClientAliveInterval $DEFAULT_CLIENT_ALIVE_INTERVAL/" "$SSH_CONFIG_FILE"
        print_info "已更新 ClientAliveInterval: $DEFAULT_CLIENT_ALIVE_INTERVAL"
    else
        echo "ClientAliveInterval $DEFAULT_CLIENT_ALIVE_INTERVAL" >> "$SSH_CONFIG_FILE"
        print_info "已添加 ClientAliveInterval: $DEFAULT_CLIENT_ALIVE_INTERVAL"
    fi
    config_updated=true

    # 处理 ClientAliveCountMax
    if check_config_exists "ClientAliveCountMax"; then
        sed -i "s/^[[:space:]]*ClientAliveCountMax[[:space:]].*/ClientAliveCountMax $DEFAULT_CLIENT_ALIVE_COUNT_MAX/" "$SSH_CONFIG_FILE"
        print_info "已更新 ClientAliveCountMax: $DEFAULT_CLIENT_ALIVE_COUNT_MAX"
    else
        echo "ClientAliveCountMax $DEFAULT_CLIENT_ALIVE_COUNT_MAX" >> "$SSH_CONFIG_FILE"
        print_info "已添加 ClientAliveCountMax: $DEFAULT_CLIENT_ALIVE_COUNT_MAX"
    fi

    # 处理 TCPKeepAlive
    if check_config_exists "TCPKeepAlive"; then
        sed -i "s/^[[:space:]]*TCPKeepAlive[[:space:]].*/TCPKeepAlive $DEFAULT_TCP_KEEPALIVE/" "$SSH_CONFIG_FILE"
        print_info "已更新 TCPKeepAlive: $DEFAULT_TCP_KEEPALIVE"
    else
        echo "TCPKeepAlive $DEFAULT_TCP_KEEPALIVE" >> "$SSH_CONFIG_FILE"
        print_info "已添加 TCPKeepAlive: $DEFAULT_TCP_KEEPALIVE"
    fi

    echo ""
    print_info "验证配置语法..."
    if sshd -t 2>/dev/null; then
        print_info "配置语法正确"
    else
        print_error "配置语法错误，恢复备份..."
        if [[ -f "$SSH_CONFIG_BACKUP" ]]; then
            cp "$SSH_CONFIG_BACKUP" "$SSH_CONFIG_FILE"
            print_info "已恢复原配置"
        fi
        exit 1
    fi

    echo ""
    print_warn "需要重启 SSH 服务以使配置生效"
    print_info "运行以下命令重启 SSH 服务:"
    echo ""
    echo "  # 对于 systemd (Ubuntu/Debian/CentOS 7+)"
    echo "  sudo systemctl restart sshd"
    echo ""
    echo "  # 对于 sysvinit (CentOS 6)"
    echo "  sudo service sshd restart"
    echo ""

    # 询问是否立即重启
    read -p "是否立即重启 SSH 服务? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_info "正在重启 SSH 服务..."
        if command -v systemctl &> /dev/null; then
            systemctl restart sshd 2>/dev/null || systemctl restart ssh 2>/dev/null
        elif command -v service &> /dev/null; then
            service sshd restart 2>/dev/null || service ssh restart 2>/dev/null
        fi
        if [[ $? -eq 0 ]]; then
            print_info "SSH 服务已重启"
        else
            print_error "重启 SSH 服务失败，请手动重启"
        fi
    else
        print_info "请手动重启 SSH 服务"
    fi
}

# 恢复默认配置
undo_config() {
    check_root

    print_info "恢复 SSH Keepalive 默认配置..."

    local backup_file=$(ls -t /etc/ssh/sshd_config.backup.* 2>/dev/null | head -1)

    if [[ -z "$backup_file" ]]; then
        print_warn "未找到备份文件，尝试手动移除配置..."
        sed -i '/^ClientAliveInterval/d' "$SSH_CONFIG_FILE" 2>/dev/null
        sed -i '/^ClientAliveCountMax/d' "$SSH_CONFIG_FILE" 2>/dev/null
        sed -i '/^TCPKeepAlive/d' "$SSH_CONFIG_FILE" 2>/dev/null
    else
        cp "$backup_file" "$SSH_CONFIG_FILE"
        print_info "已从 $backup_file 恢复配置"
    fi

    print_info "验证配置语法..."
    if sshd -t 2>/dev/null; then
        print_info "配置语法正确"
    else
        print_error "配置语法错误"
        exit 1
    fi

    print_warn "需要重启 SSH 服务以使配置生效"
}

# 交互式配置
interactive_config() {
    echo "=========================================="
    echo "      SSH Keepalive 交互式配置"
    echo "=========================================="
    echo ""

    check_root

    echo "当前配置:"
    echo ""
    check_config
    echo ""

    echo "----------------------------------------"
    echo "请输入配置值 (直接回车使用默认值):"
    echo ""

    read -p "ClientAliveInterval [${DEFAULT_CLIENT_ALIVE_INTERVAL}]: " client_alive_interval
    client_alive_interval=${client_alive_interval:-$DEFAULT_CLIENT_ALIVE_INTERVAL}

    read -p "ClientAliveCountMax [${DEFAULT_CLIENT_ALIVE_COUNT_MAX}]: " client_alive_count_max
    client_alive_count_max=${client_alive_count_max:-$DEFAULT_CLIENT_ALIVE_COUNT_MAX}

    read -p "TCPKeepAlive [${DEFAULT_TCP_KEEPALIVE}]: " tcp_keepalive
    tcp_keepalive=${tcp_keepalive:-$DEFAULT_TCP_KEEPALIVE}

    echo ""
    print_info "确认配置:"
    echo "  ClientAliveInterval: $client_alive_interval"
    echo "  ClientAliveCountMax: $client_alive_count_max"
    echo "  TCPKeepAlive: $tcp_keepalive"
    echo ""

    read -p "是否应用此配置? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        # 临时修改默认值以应用
        DEFAULT_CLIENT_ALIVE_INTERVAL=$client_alive_interval
        DEFAULT_CLIENT_ALIVE_COUNT_MAX=$client_alive_count_max
        DEFAULT_TCP_KEEPALIVE=$tcp_keepalive
        apply_config
    else
        print_info "已取消"
    fi
}

# 显示使用帮助
show_help() {
    echo "SSH Keepalive 配置脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  --apply     应用默认配置"
    echo "  --check     检查当前配置"
    echo "  --undo      恢复默认配置"
    echo "  --help      显示此帮助信息"
    echo ""
    echo "无参数时进入交互式配置模式"
    echo ""
    echo "配置说明:"
    echo "  ClientAliveInterval  - 服务端发送心跳的间隔(秒)"
    echo "  ClientAliveCountMax   - 多少次心跳无响应后断开"
    echo "  TCPKeepAlive          - 底层TCP keepalive开关"
    echo ""
    echo "推荐配置适用于:"
    echo "  - 移动设备SSH连接"
    echo "  - 长时间后台运行的命令 (如 Claude Code)"
    echo "  - 防止NAT超时断开"
}

# 主程序
main() {
    case "${1:-}" in
        --apply)
            apply_config
            ;;
        --check)
            check_config
            ;;
        --undo)
            undo_config
            ;;
        --help|-h)
            show_help
            ;;
        "")
            interactive_config
            ;;
        *)
            print_error "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
