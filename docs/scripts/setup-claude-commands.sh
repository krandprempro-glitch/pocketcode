#!/bin/bash
#
# Claude Code Skills 初始化脚本
# 在远程服务器上创建 ~/.claude/commands/ 目录和 cmd-index skill
#
# 使用方法:
#   ./setup-claude-commands.sh              # 交互式
#   ./setup-claude-commands.sh --install    # 直接安装
#   ./setup-claude-commands.sh --update      # 更新 cmd-index skill

set -e

COMMANDS_DIR="$HOME/.claude/commands"
SKILL_FILE="$COMMANDS_DIR/cmd-index.md"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# cmd-index skill 内容
create_cmd_index_skill() {
    cat > "$SKILL_FILE" << 'SKELEOF'
# cmd-index

生成 Claude Code 自定义指令索引文件，供 Termux App 读取显示快捷指令。

## 命令格式

```
/cmd-index [目录路径]
```

- 不带参数：扫描 `~/.claude/commands/` 目录下所有 `.md` 文件
- 带参数：扫描指定目录

## 输出格式

在 `~/.claude/commands/commands.md` 生成索引文件，格式如下：

```
## /user:review
代码审查 - Review code changes

## /user:deploy
部署应用 - Deploy to production
```

每行格式：`## /user:文件名\n描述（第一行内容）\n`

## 使用场景

在 Termux App 的悬浮菜单连接 SSH 后，App 会自动读取 `~/.claude/commands/commands.md` 来显示自定义快捷指令列表。

## 示例

```
/cmd-index
# 生成 ~/.claude/commands/commands.md

/cmd-index /path/to/my-commands
# 生成自定义路径下的索引文件
```

## 实现脚本

```bash
#!/bin/bash
COMMANDS_DIR="${1:-$HOME/.claude/commands}"
OUTPUT_FILE="$COMMANDS_DIR/commands.md"

if [ ! -d "$COMMANDS_DIR" ]; then
    echo "目录不存在: $COMMANDS_DIR"
    exit 1
fi

{
    echo "# Claude Code Commands Index"
    echo ""
    echo "自动生成时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    echo "格式说明：每行格式为 /user:文件名"
    echo ""
} > "$OUTPUT_FILE"

for f in "$COMMANDS_DIR"/*.md; do
    [ -f "$f" ] || continue
    filename=$(basename "$f" .md)
    # Skip only the output file itself (commands.md) to avoid recursion
    if [ "$filename" = "commands" ]; then
        continue
    fi
    first_line=$(head -1 "$f" 2>/dev/null | sed 's/^#* *//' | tr -d '\n')
    if [ -z "$first_line" ]; then
        first_line="$filename"
    fi
    echo "## /user:$filename" >> "$OUTPUT_FILE"
    echo "$first_line" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
done

echo "已生成索引文件: $OUTPUT_FILE"
echo "共 $(ls "$COMMANDS_DIR"/*.md 2>/dev/null | grep -v 'commands.md$' | wc -l) 个指令"
```
SKELEOF
}

install_skill() {
    print_info "安装 Claude Code Skills..."

    # 创建目录
    if [ ! -d "$COMMANDS_DIR" ]; then
        mkdir -p "$COMMANDS_DIR"
        print_info "已创建目录: $COMMANDS_DIR"
    else
        print_info "目录已存在: $COMMANDS_DIR"
    fi

    # 创建 cmd-index skill
    create_cmd_index_skill
    print_info "已创建 cmd-index skill: $SKILL_FILE"

    # 生成初始索引
    print_info "生成初始索引..."
    bash "$SKILL_FILE" 2>/dev/null || true

    print_info ""
    print_info "安装完成！"
    print_info "在 Claude Code 中执行 /cmd-index 即可生成自定义指令索引"
}

update_skill() {
    print_info "更新 cmd-index skill..."
    create_cmd_index_skill
    print_info "已更新: $SKILL_FILE"
}

show_help() {
    echo "Claude Code Skills 初始化脚本"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  --install   安装 cmd-index skill"
    echo "  --update    更新 cmd-index skill"
    echo "  --help      显示帮助"
    echo ""
    echo "无参数时进入交互式安装"
}

main() {
    case "${1:-}" in
        --install|-i)
            install_skill
            ;;
        --update|-u)
            update_skill
            ;;
        --help|-h)
            show_help
            ;;
        "")
            install_skill
            ;;
        *)
            print_error "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
