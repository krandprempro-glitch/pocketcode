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
echo "共 $(ls "$COMMANDS_DIR"/*.md 2>/dev/null | wc -l) 个指令"
```
