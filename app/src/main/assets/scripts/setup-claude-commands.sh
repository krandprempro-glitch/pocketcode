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

生成 Claude Code 可调用项索引文件（自定义指令 + Skills + 插件命令），供 Termux App 读取显示快捷指令。

## 命令格式

```
/cmd-index [目录路径]
```

- 不带参数：输出到 `~/.claude/commands/commands.md`
- 带参数：输出到指定目录下的 `commands.md`

## 扫描范围

默认会扫描以下可调用来源：

1. 本地自定义 commands：`~/.claude/commands/*.md`
2. 本地自定义 skills：`~/.claude/skills/*/SKILL.md`
3. 插件 commands：
   - `~/.claude/plugins/cache/*/*/*/commands/*.md`
   - `~/.claude/plugins/marketplaces/*/.claude/commands/*.md`
4. 插件 skills：
   - `~/.claude/plugins/cache/*/*/*/skills/*/SKILL.md`
   - `~/.claude/plugins/marketplaces/*/.claude/skills/*/SKILL.md`

## 输出格式

在 `~/.claude/commands/commands.md` 生成索引文件，示例：

```
## /review
代码审查 - Review code changes

## /superpowers:brainstorming
You MUST use this before any creative work...
```

每项格式：`## /命令名\n描述\n`

## 使用场景

在 Termux App 的悬浮菜单连接 SSH 后，App 会自动读取 `~/.claude/commands/commands.md` 来显示自定义快捷指令列表。

## 示例

```
/cmd-index
# 生成 ~/.claude/commands/commands.md

/cmd-index /path/to/output-dir
# 生成 /path/to/output-dir/commands.md
```

## 实现脚本

```bash
#!/bin/bash
set -euo pipefail

OUTPUT_DIR="${1:-$HOME/.claude/commands}"
OUTPUT_FILE="$OUTPUT_DIR/commands.md"

if [ ! -d "$OUTPUT_DIR" ]; then
    echo "目录不存在: $OUTPUT_DIR"
    exit 1
fi

TMP_FILE="$(mktemp)"
SORTED_TMP="$(mktemp)"
trap 'rm -f "$TMP_FILE" "$SORTED_TMP"' EXIT

add_entry() {
    local name="$1"
    local desc="${2:-}"

    [ -n "$name" ] || return 0

    name="$(printf '%s' "$name" | tr -d '\r\n' | sed 's/^ *//;s/ *$//')"
    desc="$(printf '%s' "$desc" | tr -d '\r\n' | sed 's/^ *//;s/ *$//')"

    [ -n "$name" ] || return 0
    [ -n "$desc" ] || desc="$name"

    printf '%s\t%s\n' "$name" "$desc" >> "$TMP_FILE"
}

extract_first_line_desc() {
    local file="$1"
    local line
    line="$(head -1 "$file" 2>/dev/null || true)"
    line="$(printf '%s' "$line" | sed 's/^#* *//')"
    printf '%s' "$line"
}

extract_frontmatter_field() {
    local file="$1"
    local key="$2"
    awk -v k="$key" '
        NR==1 && $0!="---" { exit }
        NR==1 && $0=="---" { in_fm=1; next }
        in_fm && $0=="---" { exit }
        in_fm {
            if ($0 ~ "^" k ":[[:space:]]*") {
                sub("^" k ":[[:space:]]*", "", $0)
                gsub(/^"|"$/, "", $0)
                gsub(/^'\''|'\''$/, "", $0)
                print $0
                exit
            }
        }
    ' "$file"
}

# 从 plugin.json 读取插件名
get_plugin_name() {
    local plugin_dir="$1"
    local pjson="$plugin_dir/.claude-plugin/plugin.json"
    if [ -f "$pjson" ]; then
        jq -r '.name // empty' "$pjson" 2>/dev/null
    fi
}

# 从 plugin.json 读取暴露的 skills 列表（相对路径）
# 返回空则表示暴露所有
get_plugin_skills_list() {
    local plugin_dir="$1"
    local pjson="$plugin_dir/.claude-plugin/plugin.json"
    if [ -f "$pjson" ]; then
        jq -r '.skills[]? // empty' "$pjson" 2>/dev/null
    fi
}

# 检查 SKILL.md 路径是否在 plugin.json 的 skills 白名单中
is_skill_allowed() {
    local plugin_dir="$1"
    local skill_file="$2"
    local allowed_skills
    allowed_skills="$(get_plugin_skills_list "$plugin_dir")"

    # 没有白名单 = 全部允许
    [ -z "$allowed_skills" ] && return 0

    # 将 skill_file 转为相对路径
    local rel="${skill_file#"$plugin_dir/"}"

    # 检查是否在白名单中
    while IFS= read -r allowed; do
        [ -z "$allowed" ] && continue
        # 白名单项可能是 ./path 格式
        local clean="${allowed#./}"
        # 匹配：白名单路径是 skill 路径的前缀
        case "$rel" in
            "$clean"*) return 0 ;;
        esac
    done <<< "$allowed_skills"

    return 1
}

# 遍历一个插件目录，索引其 commands 和 skills
index_plugin() {
    local plugin_dir="$1"
    local plugin_name
    plugin_name="$(get_plugin_name "$plugin_dir")"
    [ -n "$plugin_name" ] || return 0

    # commands
    while IFS= read -r f; do
        [ -f "$f" ] || continue
        local base="$(basename "$f" .md)"
        [ "$base" = "commands" ] && continue
        local desc="$(extract_frontmatter_field "$f" "description")"
        [ -n "$desc" ] || desc="$(extract_first_line_desc "$f")"
        [ -n "$desc" ] || desc="$base"
        add_entry "$plugin_name:$base" "$desc"
    done < <(find "$plugin_dir" -path "*/commands/*.md" 2>/dev/null)

    # skills
    while IFS= read -r f; do
        [ -f "$f" ] || continue
        is_skill_allowed "$plugin_dir" "$f" || continue
        local skill_name="$(extract_frontmatter_field "$f" "name")"
        local skill_desc="$(extract_frontmatter_field "$f" "description")"
        [ -n "$skill_name" ] || skill_name="$(basename "$(dirname "$f")")"
        add_entry "$plugin_name:$skill_name" "$skill_desc"
    done < <(find "$plugin_dir" -name "SKILL.md" -path "*/skills/*" 2>/dev/null)
}

# 1) 本地 commands
for f in "$HOME/.claude/commands"/*.md; do
    [ -f "$f" ] || continue
    base="$(basename "$f" .md)"
    [ "$base" = "commands" ] && continue
    desc="$(extract_first_line_desc "$f")"
    [ -n "$desc" ] || desc="$base"
    add_entry "$base" "$desc"
done

# 2) 本地 skills
for f in "$HOME/.claude/skills"/*/SKILL.md; do
    [ -f "$f" ] || continue
    skill_name="$(extract_frontmatter_field "$f" "name")"
    skill_desc="$(extract_frontmatter_field "$f" "description")"
    [ -n "$skill_name" ] || skill_name="$(basename "$(dirname "$f")")"
    add_entry "$skill_name" "$skill_desc"
done

# 3) 插件 cache - 找到所有含 .claude-plugin/plugin.json 的目录
while IFS= read -r pjson; do
    [ -f "$pjson" ] || continue
    plugin_dir="$(dirname "$(dirname "$pjson")")"
    index_plugin "$plugin_dir"
done < <(find "$HOME/.claude/plugins/cache" -name "plugin.json" -path "*/.claude-plugin/plugin.json" 2>/dev/null)

# 4) marketplace - 只扫描顶层（已安装的 marketplace 级插件）
# 注意：marketplaces 下有 plugins/ 子目录包含未启用的插件，只取顶层
while IFS= read -r pjson; do
    [ -f "$pjson" ] || continue
    # 只取 marketplaces/{name}/.claude-plugin/plugin.json（跳过子目录里的）
    plugin_dir="$(dirname "$(dirname "$pjson")")"
    rel="${plugin_dir#"$HOME/.claude/plugins/marketplaces/"}"
    # 如果路径包含 /，说明是子插件（如 claude-plugins-official/plugins/xxx），跳过
    case "$rel" in
        */*) continue ;;
    esac
    index_plugin "$plugin_dir"
done < <(find "$HOME/.claude/plugins/marketplaces" -name "plugin.json" -path "*/.claude-plugin/plugin.json" 2>/dev/null)

# 去重（同名保留首条）
awk -F'\t' '!seen[$1]++' "$TMP_FILE" > "$SORTED_TMP"

{
    echo "# Claude Code Commands Index"
    echo ""
    echo "自动生成时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    echo "来源：local commands + local skills + plugin commands + plugin skills"
    echo ""
} > "$OUTPUT_FILE"

count=0
while IFS=$'\t' read -r name desc; do
    [ -n "$name" ] || continue
    echo "## /$name" >> "$OUTPUT_FILE"
    echo "$desc" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    count=$((count + 1))
done < "$SORTED_TMP"

echo "已生成索引文件: $OUTPUT_FILE"
echo "共 $count 个可调用项"
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
