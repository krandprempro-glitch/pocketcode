package com.termux.filebrowser.highlight

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import java.util.regex.Pattern

/**
 * Lightweight, best-effort syntax highlighter for TextView.
 * Designed for read-only previews without external dependencies.
 */
object SimpleSyntaxHighlighter {

    private const val DARK_KEYWORD = 0xFF569CD6.toInt()
    private const val DARK_STRING = 0xFFCE9178.toInt()
    private const val DARK_NUMBER = 0xFFB5CEA8.toInt()
    private const val DARK_COMMENT = 0xFF6A9955.toInt()
    private const val DARK_TAG = 0xFF569CD6.toInt()
    private const val DARK_ATTR = 0xFF92C5F8.toInt()
    private const val DARK_PROP = 0xFF9CDCFE.toInt()

    fun build(text: String, fileName: String): CharSequence {
        val sb = SpannableStringBuilder(text)
        val name = fileName.lowercase()

        when {
            name.endsWith(".vue") -> applyVue(sb)
            name.endsWith(".html") || name.endsWith(".htm") -> applyHtml(sb)
            name.endsWith(".js") || name.endsWith(".jsx") || name.endsWith(".mjs") || name.endsWith(".cjs") -> applyJs(sb)
            name.endsWith(".ts") || name.endsWith(".tsx") -> { applyJs(sb) /* coarse */ }
            name.endsWith(".css") || name.endsWith(".scss") || name.endsWith(".sass") || name.endsWith(".less") -> applyCss(sb)
            name.endsWith(".json") -> applyJson(sb)
            name.endsWith(".xml") || name.endsWith(".svg") -> applyXml(sb)
            name.endsWith(".md") || name.endsWith(".markdown") -> applyMarkdown(sb)
            name.endsWith(".kt") || name.endsWith(".kts") -> applyKotlin(sb)
            name.endsWith(".java") -> applyJavaLike(sb)
            name.endsWith(".py") -> applyPython(sb)
            else -> applyGeneric(sb)
        }
        return sb
    }

    private fun applyGeneric(sb: SpannableStringBuilder) {
        applyCommonStringsNumbersComments(sb)
    }

    private fun applyJs(sb: SpannableStringBuilder) {
        applyCommonStringsNumbersComments(sb)
        val keywords = "break|case|catch|class|const|continue|debugger|default|delete|do|else|export|extends|finally|for|function|if|import|in|instanceof|let|new|return|super|switch|this|throw|try|typeof|var|void|while|with|yield|async|await"
        applyWordSet(sb, keywords, DARK_KEYWORD)
        // template literal
        applyRegex(sb, Pattern.compile("`[^`]*`"), DARK_STRING)
    }

    private fun applyKotlin(sb: SpannableStringBuilder) {
        applyCommonStringsNumbersComments(sb)
        val keywords = "as|break|class|continue|do|else|false|for|fun|if|in|interface|is|null|object|package|return|super|this|throw|true|try|typealias|typeof|val|var|when|while|by|catch|constructor|delegate|dynamic|field|file|finally|get|import|init|param|property|receiver|set|setparam|where|actual|abstract|annotation|companion|const|crossinline|data|enum|expect|final|infix|inline|inner|internal|lateinit|noinline|open|operator|out|override|private|protected|public|reified|sealed|suspend|tailrec|vararg"
        applyWordSet(sb, keywords, DARK_KEYWORD)
    }

    private fun applyJavaLike(sb: SpannableStringBuilder) {
        applyCommonStringsNumbersComments(sb)
        val keywords = "abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while"
        applyWordSet(sb, keywords, DARK_KEYWORD)
    }

    private fun applyPython(sb: SpannableStringBuilder) {
        applyRegex(sb, Pattern.compile("#.*$", Pattern.MULTILINE), DARK_COMMENT, italic = true)
        // Triple-quoted strings
        applyRegex(sb, Pattern.compile("\"\"\"[\\s\\S]*?\"\"\""), DARK_STRING)
        applyRegex(sb, Pattern.compile("'''[\\s\\S]*?'''"), DARK_STRING)
        // Normal strings
        applyRegex(sb, Pattern.compile("\"[^\"\n]*\""), DARK_STRING)
        applyRegex(sb, Pattern.compile("'[^'\n]*'"), DARK_STRING)
        // Numbers
        applyRegex(sb, Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b"), DARK_NUMBER)
        // Keywords
        val keywords = "and|as|assert|break|class|continue|def|del|elif|else|except|False|finally|for|from|global|if|import|in|is|lambda|None|nonlocal|not|or|pass|raise|return|True|try|while|with|yield"
        applyWordSet(sb, keywords, DARK_KEYWORD)
    }

    private fun applyCss(sb: SpannableStringBuilder) {
        applyRegex(sb, Pattern.compile("/\\*[\\s\\S]*?\\*/"), DARK_COMMENT, italic = true)
        applyRegex(sb, Pattern.compile("\\b[0-9]+(?:\\.[0-9]+)?(?:px|em|rem|%|vh|vw|pt)?\\b"), DARK_NUMBER)
        applyRegex(sb, Pattern.compile("#[0-9a-fA-F]{3,6}\\b"), DARK_STRING)
        // property: value;
        applyRegex(sb, Pattern.compile("\\b[a-zA-Z-]+(?=\\s*:)"), DARK_PROP)
        applyRegex(sb, Pattern.compile("\"[^\"\n]*\""), DARK_STRING)
        applyRegex(sb, Pattern.compile("'[^'\n]*'"), DARK_STRING)
    }

    private fun applyHtml(sb: SpannableStringBuilder) {
        applyRegex(sb, Pattern.compile("<!--[\\s\\S]*?-->"), DARK_COMMENT, italic = true)
        // Tags and attributes (coarse)
        applyRegex(sb, Pattern.compile("<[^>]+>"), DARK_TAG)
        applyRegex(sb, Pattern.compile("\\b[a-zA-Z_:][-a-zA-Z0-9_.:]*\\s*="), DARK_ATTR)
        applyRegex(sb, Pattern.compile("\"[^\"\n]*\""), DARK_STRING)
        applyRegex(sb, Pattern.compile("'[^'\n]*'"), DARK_STRING)
    }

    private fun applyXml(sb: SpannableStringBuilder) {
        applyRegex(sb, Pattern.compile("<!--[\\s\\S]*?-->"), DARK_COMMENT, italic = true)
        applyRegex(sb, Pattern.compile("<[^>]+>"), DARK_TAG)
        applyRegex(sb, Pattern.compile("\\b[a-zA-Z_:][-a-zA-Z0-9_.:]*\\s*="), DARK_ATTR)
        applyRegex(sb, Pattern.compile("\"[^\"\n]*\""), DARK_STRING)
        applyRegex(sb, Pattern.compile("'[^'\n]*'"), DARK_STRING)
        applyRegex(sb, Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b"), DARK_NUMBER)
    }

    private fun applyJson(sb: SpannableStringBuilder) {
        // keys and strings
        applyRegex(sb, Pattern.compile("\"[^\"]+\"(?=\\s*:)"), DARK_ATTR)
        applyRegex(sb, Pattern.compile("\"[^\"\\n]*\""), DARK_STRING)
        applyRegex(sb, Pattern.compile("\\b(?:true|false|null)\\b"), DARK_KEYWORD)
        applyRegex(sb, Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b"), DARK_NUMBER)
    }

    private fun applyMarkdown(sb: SpannableStringBuilder) {
        applyRegex(sb, Pattern.compile("^#{1,6} .*", Pattern.MULTILINE), DARK_KEYWORD)
        applyRegex(sb, Pattern.compile("```[\\s\\S]*?```"), DARK_TAG)
        applyRegex(sb, Pattern.compile("`[^`]*`"), DARK_TAG)
        applyRegex(sb, Pattern.compile("\\*\\*[^*]+\\*\\*"), DARK_PROP)
        applyRegex(sb, Pattern.compile("\\*[^*]+\\*"), DARK_ATTR)
        applyRegex(sb, Pattern.compile("!\\[[^]]*]\\([^)]*\\)"), DARK_STRING)
        applyRegex(sb, Pattern.compile("\\[[^]]*]\\([^)]*\\)"), DARK_STRING)
    }

    private fun applyVue(sb: SpannableStringBuilder) {
        // Treat as HTML base + extra attributes + embedded JS/CSS coarse handling
        applyHtml(sb)
        applyRegex(sb, Pattern.compile("\\b(v-[a-zA-Z-]+|:[a-zA-Z-]+|@[a-zA-Z-]+)\\b"), DARK_ATTR)
        // Interpolations {{ ... }}
        applyRegex(sb, Pattern.compile("\\{\\{[\\s\\S]*?\\}\\}"), DARK_TAG)
    }

    private fun applyCommonStringsNumbersComments(sb: SpannableStringBuilder) {
        applyRegex(sb, Pattern.compile("//.*$", Pattern.MULTILINE), DARK_COMMENT, italic = true)
        applyRegex(sb, Pattern.compile("/\\*[\\s\\S]*?\\*/"), DARK_COMMENT, italic = true)
        applyRegex(sb, Pattern.compile("\"[^\"\n]*\""), DARK_STRING)
        applyRegex(sb, Pattern.compile("'[^'\n]*'"), DARK_STRING)
        applyRegex(sb, Pattern.compile("\\b\\d+(?:\\.\\d+)?\\b"), DARK_NUMBER)
    }

    private fun applyWordSet(sb: SpannableStringBuilder, words: String, color: Int) {
        val pattern = Pattern.compile("\\b($words)\\b")
        applyRegex(sb, pattern, color)
    }

    private fun applyRegex(
        sb: SpannableStringBuilder,
        pattern: Pattern,
        color: Int,
        italic: Boolean = false
    ) {
        val matcher = pattern.matcher(sb)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            sb.setSpan(ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (italic) sb.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}

