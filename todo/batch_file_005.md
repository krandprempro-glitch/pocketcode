# Batch 005: 代码语法高亮和样式完善

## 目标
为代码查看器添加基础的语法高亮功能，支持常见编程语言(Java、JavaScript、Python、HTML、CSS等)，完善VSCode Light主题样式，优化代码显示性能和用户体验。

## 任务列表

### 1. 创建语法高亮器基类
**文件**: `app/src/main/java/com/termux/app/syntax/SyntaxHighlighter.java`

```java
package com.termux.app.syntax;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import androidx.core.content.ContextCompat;
import com.termux.R;
import com.termux.shared.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基础语法高亮器
 * 支持常见编程语言的语法高亮
 */
public abstract class SyntaxHighlighter {
    private static final String LOG_TAG = "SyntaxHighlighter";
    
    protected Context context;
    protected int keywordColor;
    protected int stringColor;
    protected int commentColor;
    protected int numberColor;
    protected int defaultColor;
    
    // 高亮规则
    protected List<HighlightRule> rules;
    
    public static class HighlightRule {
        public Pattern pattern;
        public int color;
        public int style;
        public String name;
        
        public HighlightRule(String regex, int color, int style, String name) {
            this.pattern = Pattern.compile(regex, Pattern.MULTILINE);
            this.color = color;
            this.style = style;
            this.name = name;
        }
        
        public HighlightRule(String regex, int color, String name) {
            this(regex, color, Typeface.NORMAL, name);
        }
    }
    
    public SyntaxHighlighter(Context context) {
        this.context = context;
        initColors();
        initRules();
    }
    
    private void initColors() {
        keywordColor = ContextCompat.getColor(context, R.color.code_keyword);
        stringColor = ContextCompat.getColor(context, R.color.code_string);
        commentColor = ContextCompat.getColor(context, R.color.code_comment);
        numberColor = ContextCompat.getColor(context, R.color.code_number);
        defaultColor = ContextCompat.getColor(context, R.color.code_text_default);
    }
    
    /**
     * 子类实现具体的规则初始化
     */
    protected abstract void initRules();
    
    /**
     * 对代码进行语法高亮
     */
    public SpannableStringBuilder highlight(String code) {
        if (code == null || code.isEmpty()) {
            return new SpannableStringBuilder("");
        }
        
        SpannableStringBuilder spannable = new SpannableStringBuilder(code);
        
        // 应用所有高亮规则
        for (HighlightRule rule : rules) {
            try {
                applyRule(spannable, rule);
            } catch (Exception e) {
                Logger.logError(LOG_TAG, "Error applying rule " + rule.name + ": " + e.getMessage());
            }
        }
        
        return spannable;
    }
    
    /**
     * 应用单个高亮规则
     */
    private void applyRule(SpannableStringBuilder spannable, HighlightRule rule) {
        Matcher matcher = rule.pattern.matcher(spannable);
        
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            
            // 应用颜色
            spannable.setSpan(
                new ForegroundColorSpan(rule.color),
                start, end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            
            // 应用样式 (粗体、斜体等)
            if (rule.style != Typeface.NORMAL) {
                spannable.setSpan(
                    new StyleSpan(rule.style),
                    start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
    }
    
    /**
     * 创建语法高亮器工厂方法
     */
    public static SyntaxHighlighter createHighlighter(Context context, String fileName) {
        String extension = getFileExtension(fileName.toLowerCase());
        
        switch (extension) {
            case "java":
            case "kt":
                return new JavaSyntaxHighlighter(context);
            case "js":
            case "ts":
                return new JavaScriptSyntaxHighlighter(context);
            case "py":
                return new PythonSyntaxHighlighter(context);
            case "html":
            case "htm":
                return new HtmlSyntaxHighlighter(context);
            case "css":
                return new CssSyntaxHighlighter(context);
            case "json":
                return new JsonSyntaxHighlighter(context);
            case "xml":
                return new XmlSyntaxHighlighter(context);
            case "sql":
                return new SqlSyntaxHighlighter(context);
            case "sh":
            case "bash":
                return new ShellSyntaxHighlighter(context);
            case "md":
                return new MarkdownSyntaxHighlighter(context);
            default:
                return new GenericSyntaxHighlighter(context);
        }
    }
    
    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }
}
```

### 2. 创建Java语法高亮器
**文件**: `app/src/main/java/com/termux/app/syntax/JavaSyntaxHighlighter.java`

```java
package com.termux.app.syntax;

import android.content.Context;
import android.graphics.Typeface;

import java.util.ArrayList;

/**
 * Java语法高亮器
 */
public class JavaSyntaxHighlighter extends SyntaxHighlighter {
    
    public JavaSyntaxHighlighter(Context context) {
        super(context);
    }
    
    @Override
    protected void initRules() {
        rules = new ArrayList<>();
        
        // 单行注释
        rules.add(new HighlightRule("//.*$", commentColor, "single_line_comment"));
        
        // 多行注释
        rules.add(new HighlightRule("/\\*[\\s\\S]*?\\*/", commentColor, "multi_line_comment"));
        
        // 文档注释
        rules.add(new HighlightRule("/\\*\\*[\\s\\S]*?\\*/", commentColor, "doc_comment"));
        
        // 字符串 (双引号)
        rules.add(new HighlightRule("\"(?:[^\"\\\\]|\\\\.)*\"", stringColor, "string"));
        
        // 字符 (单引号)
        rules.add(new HighlightRule("'(?:[^'\\\\]|\\\\.)*'", stringColor, "character"));
        
        // 数字 (整数和浮点数)
        rules.add(new HighlightRule("\\b\\d+\\.?\\d*[fFdDlL]?\\b", numberColor, "number"));
        
        // 十六进制数字
        rules.add(new HighlightRule("\\b0[xX][0-9a-fA-F]+[lL]?\\b", numberColor, "hex_number"));
        
        // Java关键字
        String javaKeywords = "\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|null|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false)\\b";
        rules.add(new HighlightRule(javaKeywords, keywordColor, Typeface.BOLD, "keywords"));
        
        // 注解
        rules.add(new HighlightRule("@\\w+", keywordColor, "annotation"));
        
        // 类名 (首字母大写的标识符)
        rules.add(new HighlightRule("\\b[A-Z][a-zA-Z0-9_]*\\b", keywordColor, "class_name"));
    }
}
```

### 3. 创建JavaScript语法高亮器
**文件**: `app/src/main/java/com/termux/app/syntax/JavaScriptSyntaxHighlighter.java`

```java
package com.termux.app.syntax;

import android.content.Context;
import android.graphics.Typeface;

import java.util.ArrayList;

/**
 * JavaScript/TypeScript语法高亮器
 */
public class JavaScriptSyntaxHighlighter extends SyntaxHighlighter {
    
    public JavaScriptSyntaxHighlighter(Context context) {
        super(context);
    }
    
    @Override
    protected void initRules() {
        rules = new ArrayList<>();
        
        // 单行注释
        rules.add(new HighlightRule("//.*$", commentColor, "single_line_comment"));
        
        // 多行注释
        rules.add(new HighlightRule("/\\*[\\s\\S]*?\\*/", commentColor, "multi_line_comment"));
        
        // 字符串 (双引号、单引号、模板字符串)
        rules.add(new HighlightRule("\"(?:[^\"\\\\]|\\\\.)*\"", stringColor, "double_quote_string"));
        rules.add(new HighlightRule("'(?:[^'\\\\]|\\\\.)*'", stringColor, "single_quote_string"));
        rules.add(new HighlightRule("`(?:[^`\\\\]|\\\\.)*`", stringColor, "template_string"));
        
        // 正则表达式
        rules.add(new HighlightRule("/(?:[^/\\\\\\n]|\\\\.)+/[gimuy]*", stringColor, "regex"));
        
        // 数字
        rules.add(new HighlightRule("\\b\\d+\\.?\\d*\\b", numberColor, "number"));
        
        // JavaScript关键字
        String jsKeywords = "\\b(async|await|break|case|catch|class|const|continue|debugger|default|delete|do|else|enum|export|extends|false|finally|for|function|if|import|in|instanceof|let|new|null|of|return|super|switch|this|throw|true|try|typeof|undefined|var|void|while|with|yield)\\b";
        rules.add(new HighlightRule(jsKeywords, keywordColor, Typeface.BOLD, "keywords"));
        
        // 内置对象
        String builtinObjects = "\\b(Array|Boolean|Date|Error|Function|JSON|Math|Number|Object|RegExp|String|Promise|Map|Set|WeakMap|WeakSet|Symbol|console|window|document)\\b";
        rules.add(new HighlightRule(builtinObjects, keywordColor, "builtin_objects"));
    }
}
```

### 4. 创建Python语法高亮器
**文件**: `app/src/main/java/com/termux/app/syntax/PythonSyntaxHighlighter.java`

```java
package com.termux.app.syntax;

import android.content.Context;
import android.graphics.Typeface;

import java.util.ArrayList;

/**
 * Python语法高亮器
 */
public class PythonSyntaxHighlighter extends SyntaxHighlighter {
    
    public PythonSyntaxHighlighter(Context context) {
        super(context);
    }
    
    @Override
    protected void initRules() {
        rules = new ArrayList<>();
        
        // 注释
        rules.add(new HighlightRule("#.*$", commentColor, "comment"));
        
        // 文档字符串 (三引号)
        rules.add(new HighlightRule("\"\"\"[\\s\\S]*?\"\"\"", commentColor, "docstring_double"));
        rules.add(new HighlightRule("'''[\\s\\S]*?'''", commentColor, "docstring_single"));
        
        // 字符串
        rules.add(new HighlightRule("\"(?:[^\"\\\\]|\\\\.)*\"", stringColor, "double_quote_string"));
        rules.add(new HighlightRule("'(?:[^'\\\\]|\\\\.)*'", stringColor, "single_quote_string"));
        
        // 原始字符串
        rules.add(new HighlightRule("r\"(?:[^\"\\\\]|\\\\.)*\"", stringColor, "raw_string_double"));
        rules.add(new HighlightRule("r'(?:[^'\\\\]|\\\\.)*'", stringColor, "raw_string_single"));
        
        // f-strings
        rules.add(new HighlightRule("f\"(?:[^\"\\\\]|\\\\.)*\"", stringColor, "f_string_double"));
        rules.add(new HighlightRule("f'(?:[^'\\\\]|\\\\.)*'", stringColor, "f_string_single"));
        
        // 数字
        rules.add(new HighlightRule("\\b\\d+\\.?\\d*\\b", numberColor, "number"));
        
        // Python关键字
        String pythonKeywords = "\\b(False|None|True|and|as|assert|async|await|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield)\\b";
        rules.add(new HighlightRule(pythonKeywords, keywordColor, Typeface.BOLD, "keywords"));
        
        // 内置函数
        String builtinFunctions = "\\b(abs|all|any|ascii|bin|bool|breakpoint|bytearray|bytes|callable|chr|classmethod|compile|complex|delattr|dict|dir|divmod|enumerate|eval|exec|filter|float|format|frozenset|getattr|globals|hasattr|hash|help|hex|id|input|int|isinstance|issubclass|iter|len|list|locals|map|max|memoryview|min|next|object|oct|open|ord|pow|print|property|range|repr|reversed|round|set|setattr|slice|sorted|staticmethod|str|sum|super|tuple|type|vars|zip)\\b";
        rules.add(new HighlightRule(builtinFunctions, keywordColor, "builtin_functions"));
        
        // 装饰器
        rules.add(new HighlightRule("@\\w+", keywordColor, "decorator"));
    }
}
```

### 5. 创建通用语法高亮器
**文件**: `app/src/main/java/com/termux/app/syntax/GenericSyntaxHighlighter.java`

```java
package com.termux.app.syntax;

import android.content.Context;

import java.util.ArrayList;

/**
 * 通用语法高亮器
 * 用于不支持特定语法的文件类型
 */
public class GenericSyntaxHighlighter extends SyntaxHighlighter {
    
    public GenericSyntaxHighlighter(Context context) {
        super(context);
    }
    
    @Override
    protected void initRules() {
        rules = new ArrayList<>();
        
        // 基础的通用规则
        
        // 单行注释 (多种格式)
        rules.add(new HighlightRule("//.*$", commentColor, "cpp_comment"));
        rules.add(new HighlightRule("#.*$", commentColor, "hash_comment"));
        rules.add(new HighlightRule(";.*$", commentColor, "semicolon_comment"));
        
        // 多行注释
        rules.add(new HighlightRule("/\\*[\\s\\S]*?\\*/", commentColor, "c_style_comment"));
        
        // 字符串 (双引号和单引号)
        rules.add(new HighlightRule("\"(?:[^\"\\\\]|\\\\.)*\"", stringColor, "double_quote_string"));
        rules.add(new HighlightRule("'(?:[^'\\\\]|\\\\.)*'", stringColor, "single_quote_string"));
        
        // 数字
        rules.add(new HighlightRule("\\b\\d+\\.?\\d*\\b", numberColor, "number"));
        
        // 通用关键字 (常见的编程语言关键字)
        String commonKeywords = "\\b(true|false|null|undefined|void|int|char|float|double|string|bool|boolean|if|else|for|while|do|switch|case|default|break|continue|return|function|def|class|struct|enum|public|private|protected|static|const|var|let)\\b";
        rules.add(new HighlightRule(commonKeywords, keywordColor, "common_keywords"));
    }
}
```

### 6. 创建其他常用语言高亮器
**文件**: `app/src/main/java/com/termux/app/syntax/JsonSyntaxHighlighter.java`

```java
package com.termux.app.syntax;

import android.content.Context;
import android.graphics.Typeface;

import java.util.ArrayList;

/**
 * JSON语法高亮器
 */
public class JsonSyntaxHighlighter extends SyntaxHighlighter {
    
    public JsonSyntaxHighlighter(Context context) {
        super(context);
    }
    
    @Override
    protected void initRules() {
        rules = new ArrayList<>();
        
        // 字符串键和值
        rules.add(new HighlightRule("\"(?:[^\"\\\\]|\\\\.)*\"", stringColor, "string"));
        
        // 数字
        rules.add(new HighlightRule("-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?", numberColor, "number"));
        
        // 布尔值和null
        rules.add(new HighlightRule("\\b(true|false|null)\\b", keywordColor, Typeface.BOLD, "literal"));
        
        // 对象键 (引号中的字符串后跟冒号)
        rules.add(new HighlightRule("\"(?:[^\"\\\\]|\\\\.)*\"\\s*:", keywordColor, "object_key"));
    }
}
```

**文件**: `app/src/main/java/com/termux/app/syntax/HtmlSyntaxHighlighter.java`

```java
package com.termux.app.syntax;

import android.content.Context;

import java.util.ArrayList;

/**
 * HTML语法高亮器
 */
public class HtmlSyntaxHighlighter extends SyntaxHighlighter {
    
    public HtmlSyntaxHighlighter(Context context) {
        super(context);
    }
    
    @Override
    protected void initRules() {
        rules = new ArrayList<>();
        
        // HTML注释
        rules.add(new HighlightRule("<!--[\\s\\S]*?-->", commentColor, "html_comment"));
        
        // HTML标签
        rules.add(new HighlightRule("</?\\b\\w+\\b[^>]*>", keywordColor, "html_tag"));
        
        // 属性名
        rules.add(new HighlightRule("\\b\\w+(?=\\s*=)", numberColor, "attribute_name"));
        
        // 属性值 (引号内)
        rules.add(new HighlightRule("\"[^\"]*\"", stringColor, "attribute_value_double"));
        rules.add(new HighlightRule("'[^']*'", stringColor, "attribute_value_single"));
        
        // DOCTYPE
        rules.add(new HighlightRule("<!DOCTYPE[^>]*>", keywordColor, "doctype"));
    }
}
```

**文件**: `app/src/main/java/com/termux/app/syntax/CssSyntaxHighlighter.java`

```java
package com.termux.app.syntax;

import android.content.Context;

import java.util.ArrayList;

/**
 * CSS语法高亮器
 */
public class CssSyntaxHighlighter extends SyntaxHighlighter {
    
    public CssSyntaxHighlighter(Context context) {
        super(context);
    }
    
    @Override
    protected void initRules() {
        rules = new ArrayList<>();
        
        // CSS注释
        rules.add(new HighlightRule("/\\*[\\s\\S]*?\\*/", commentColor, "css_comment"));
        
        // CSS选择器
        rules.add(new HighlightRule("[.#]?[a-zA-Z][a-zA-Z0-9_-]*(?=\\s*\\{)", keywordColor, "css_selector"));
        
        // CSS属性名
        rules.add(new HighlightRule("\\b[a-zA-Z-]+(?=\\s*:)", numberColor, "css_property"));
        
        // CSS属性值
        rules.add(new HighlightRule(":\\s*[^;\\}]+", stringColor, "css_value"));
        
        // 颜色值
        rules.add(new HighlightRule("#[0-9a-fA-F]{3,6}\\b", numberColor, "css_color"));
        
        // CSS单位
        rules.add(new HighlightRule("\\d+(?:px|em|rem|%|vh|vw|pt|pc|in|mm|cm)\\b", numberColor, "css_unit"));
    }
}
```

### 7. 更新CodeViewer以支持语法高亮
在 `CodeViewer.java` 中添加语法高亮支持:

```java
// 添加新的成员变量
private SyntaxHighlighter syntaxHighlighter;
private boolean syntaxHighlightEnabled = true;

/**
 * 加载代码内容 (更新现有方法)
 */
public void loadCode(String content, String fileName) {
    this.fileName = fileName;
    
    // 创建相应的语法高亮器
    syntaxHighlighter = SyntaxHighlighter.createHighlighter(getContext(), fileName);
    
    if (content == null || content.isEmpty()) {
        showEmptyContent();
        return;
    }
    
    Logger.logInfo(LOG_TAG, "Loading code with syntax highlighting for: " + fileName);
    
    isLoading = true;
    showLoadingState();
    
    // 在后台线程处理文本分割和语法高亮
    executor.execute(() -> {
        try {
            List<String> lines = splitContentIntoLines(content);
            
            // 回到主线程更新UI
            post(() -> {
                codeLines = lines;
                displayCodeLinesWithHighlighting();
                isLoading = false;
                
                if (listener != null) {
                    listener.onCodeLoaded(lines.size());
                }
            });
            
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error processing code content: " + e.getMessage());
            post(() -> {
                showErrorState("处理文件内容时出错: " + e.getMessage());
                isLoading = false;
                
                if (listener != null) {
                    listener.onCodeLoadError(e.getMessage());
                }
            });
        }
    });
}

/**
 * 显示带语法高亮的代码行
 */
private void displayCodeLinesWithHighlighting() {
    Logger.logInfo(LOG_TAG, "Displaying code with syntax highlighting: " + codeLines.size() + " lines");
    
    // 清空现有内容
    lineNumbersContainer.removeAllViews();
    codeLinesContainer.removeAllViews();
    
    // 分批显示，避免一次性创建太多View导致卡顿
    int linesToShow = Math.min(codeLines.size(), MAX_LINES_INITIAL);
    
    for (int i = 0; i < linesToShow; i++) {
        addCodeLineWithHighlighting(i + 1, codeLines.get(i));
    }
    
    if (codeLines.size() > MAX_LINES_INITIAL) {
        Logger.logInfo(LOG_TAG, "Large file detected, showing first " + linesToShow + " lines with syntax highlighting");
    }
}

/**
 * 添加带语法高亮的单行代码
 */
private void addCodeLineWithHighlighting(int lineNumber, String lineContent) {
    Context context = getContext();
    
    // 创建行号TextView (保持不变)
    TextView lineNumberView = createLineNumberView(context, lineNumber);
    lineNumbersContainer.addView(lineNumberView);
    
    // 创建带语法高亮的代码行TextView
    TextView codeLineView = createHighlightedCodeLineView(context, lineContent, lineNumber);
    codeLinesContainer.addView(codeLineView);
}

/**
 * 创建带语法高亮的代码行视图
 */
private TextView createHighlightedCodeLineView(Context context, String lineContent, int lineNumber) {
    TextView codeLineView = new TextView(context);
    
    // 处理空行
    String displayText = TextUtils.isEmpty(lineContent) ? " " : lineContent;
    
    // 应用语法高亮
    if (syntaxHighlightEnabled && syntaxHighlighter != null) {
        try {
            SpannableStringBuilder highlightedText = syntaxHighlighter.highlight(displayText);
            codeLineView.setText(highlightedText);
        } catch (Exception e) {
            Logger.logError(LOG_TAG, "Error applying syntax highlighting to line " + lineNumber + ": " + e.getMessage());
            // 如果高亮失败，显示原始文本
            codeLineView.setText(displayText);
        }
    } else {
        codeLineView.setText(displayText);
    }
    
    // 设置样式 (保持不变)
    codeLineView.setTextSize(codeTextSize);
    if (!syntaxHighlightEnabled) {
        codeLineView.setTextColor(ContextCompat.getColor(context, R.color.code_text_default));
    }
    codeLineView.setTypeface(codeTypeface);
    codeLineView.setPadding(0, 2, 16, 2);
    
    // 设置行高
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        lineHeight
    );
    codeLineView.setLayoutParams(params);
    
    // 设置点击事件 (保持不变)
    codeLineView.setOnClickListener(v -> {
        if (listener != null) {
            listener.onLineClicked(lineNumber, lineContent);
        }
    });
    
    codeLineView.setBackgroundResource(android.R.drawable.list_selector_background);
    
    return codeLineView;
}

/**
 * 启用/禁用语法高亮
 */
public void setSyntaxHighlightEnabled(boolean enabled) {
    if (this.syntaxHighlightEnabled != enabled) {
        this.syntaxHighlightEnabled = enabled;
        
        // 如果当前有代码显示，重新渲染
        if (!codeLines.isEmpty()) {
            displayCodeLinesWithHighlighting();
        }
    }
}

/**
 * 检查语法高亮是否启用
 */
public boolean isSyntaxHighlightEnabled() {
    return syntaxHighlightEnabled;
}

/**
 * 获取当前语法高亮器类型
 */
public String getSyntaxHighlighterType() {
    if (syntaxHighlighter != null) {
        return syntaxHighlighter.getClass().getSimpleName();
    }
    return "None";
}
```

### 8. 添加其他必要的语法高亮器

**文件**: `app/src/main/java/com/termux/app/syntax/XmlSyntaxHighlighter.java`

```java
package com.termux.app.syntax;

import android.content.Context;

import java.util.ArrayList;

public class XmlSyntaxHighlighter extends SyntaxHighlighter {
    public XmlSyntaxHighlighter(Context context) { super(context); }
    @Override
    protected void initRules() {
        rules = new ArrayList<>();
        rules.add(new HighlightRule("<!--[\\s\\S]*?-->", commentColor, "xml_comment"));
        rules.add(new HighlightRule("<\\?[\\s\\S]*?\\?>", keywordColor, "xml_declaration"));
        rules.add(new HighlightRule("</?\\b\\w+\\b[^>]*>", keywordColor, "xml_tag"));
        rules.add(new HighlightRule("\\b\\w+(?=\\s*=)", numberColor, "attribute_name"));
        rules.add(new HighlightRule("\"[^\"]*\"", stringColor, "attribute_value"));
    }
}
```

**文件**: `app/src/main/java/com/termux/app/syntax/SqlSyntaxHighlighter.java`

```java
package com.termux.app.syntax;

import android.content.Context;
import android.graphics.Typeface;

import java.util.ArrayList;

public class SqlSyntaxHighlighter extends SyntaxHighlighter {
    public SqlSyntaxHighlighter(Context context) { super(context); }
    @Override
    protected void initRules() {
        rules = new ArrayList<>();
        rules.add(new HighlightRule("--.*$", commentColor, "sql_comment"));
        rules.add(new HighlightRule("/\\*[\\s\\S]*?\\*/", commentColor, "sql_multiline_comment"));
        rules.add(new HighlightRule("'(?:[^'\\\\]|\\\\.)*'", stringColor, "sql_string"));
        String sqlKeywords = "\\b(?i)(SELECT|FROM|WHERE|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER|TABLE|INDEX|DATABASE|SCHEMA|PRIMARY|KEY|FOREIGN|REFERENCES|NOT|NULL|AUTO_INCREMENT|DEFAULT|UNIQUE|CHECK|CONSTRAINT)\\b";
        rules.add(new HighlightRule(sqlKeywords, keywordColor, Typeface.BOLD, "sql_keywords"));
    }
}
```

**文件**: `app/src/main/java/com/termux/app/syntax/ShellSyntaxHighlighter.java`

```java
package com.termux.app.syntax;

import android.content.Context;

import java.util.ArrayList;

public class ShellSyntaxHighlighter extends SyntaxHighlighter {
    public ShellSyntaxHighlighter(Context context) { super(context); }
    @Override
    protected void initRules() {
        rules = new ArrayList<>();
        rules.add(new HighlightRule("#.*$", commentColor, "shell_comment"));
        rules.add(new HighlightRule("\"(?:[^\"\\\\]|\\\\.)*\"", stringColor, "double_quote_string"));
        rules.add(new HighlightRule("'[^']*'", stringColor, "single_quote_string"));
        String shellKeywords = "\\b(if|then|else|elif|fi|for|while|do|done|case|esac|function|return|exit|break|continue|local|export|unset|cd|echo|printf)\\b";
        rules.add(new HighlightRule(shellKeywords, keywordColor, "shell_keywords"));
        rules.add(new HighlightRule("\\$\\w+", numberColor, "shell_variable"));
    }
}
```

**文件**: `app/src/main/java/com/termux/app/syntax/MarkdownSyntaxHighlighter.java`

```java
package com.termux.app.syntax;

import android.content.Context;
import android.graphics.Typeface;

import java.util.ArrayList;

public class MarkdownSyntaxHighlighter extends SyntaxHighlighter {
    public MarkdownSyntaxHighlighter(Context context) { super(context); }
    @Override
    protected void initRules() {
        rules = new ArrayList<>();
        rules.add(new HighlightRule("^#{1,6}\\s.*$", keywordColor, Typeface.BOLD, "md_heading"));
        rules.add(new HighlightRule("\\*\\*[^*]+\\*\\*", keywordColor, Typeface.BOLD, "md_bold"));
        rules.add(new HighlightRule("\\*[^*]+\\*", keywordColor, Typeface.ITALIC, "md_italic"));
        rules.add(new HighlightRule("`[^`]+`", stringColor, "md_code"));
        rules.add(new HighlightRule("```[\\s\\S]*?```", stringColor, "md_code_block"));
        rules.add(new HighlightRule("\\[[^\\]]+\\]\\([^)]+\\)", numberColor, "md_link"));
    }
}
```

### 9. 添加必要的import语句
在相关Java文件中添加:

```java
// CodeViewer.java
import android.text.SpannableStringBuilder;
import com.termux.app.syntax.SyntaxHighlighter;

// RemoteFileBrowserFragment.java 无需额外import
```

## 验证要点
1. 语法高亮正确应用到各种编程语言
2. 关键字、字符串、注释等元素颜色正确
3. 高亮性能良好，不影响滚动流畅度
4. 大文件的语法高亮不会导致卡顿
5. 语法高亮可以正确启用/禁用
6. 不支持的文件类型使用通用高亮器
7. 高亮过程中的异常得到正确处理
8. VSCode Light主题风格保持一致

## 预估工作量
- 语法高亮器基类和具体实现: ~5万token
- CodeViewer集成语法高亮: ~2万token
- 性能优化和异常处理: ~2万token
- 总计: ~9万token

## 下一步
完成此批次后，代码语法高亮功能完整，可以继续进行 Batch 006 的文件标签页和最终集成。