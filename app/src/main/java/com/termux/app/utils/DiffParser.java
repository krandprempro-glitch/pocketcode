package com.termux.app.utils;

import com.termux.app.models.DiffLine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw git diff output into structured DiffLine objects.
 * Input format: output of "git show --format="" <hash> -- <path>"
 */
public class DiffParser {

    private static final Pattern HUNK_PATTERN = Pattern.compile("^@@ -(\\d+)(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@");

    public static List<DiffLine> parse(String rawDiff) {
        List<DiffLine> lines = new ArrayList<>();
        if (rawDiff == null || rawDiff.isEmpty()) return lines;

        String[] rawLines = rawDiff.split("\n");
        int oldLine = 0;
        int newLine = 0;

        for (String raw : rawLines) {
            if (raw.startsWith("diff --git ")) {
                lines.add(new DiffLine(DiffLine.Type.FILE_HEADER, -1, -1, raw));
            } else if (raw.startsWith("@@")) {
                Matcher m = HUNK_PATTERN.matcher(raw);
                if (m.find()) {
                    oldLine = Integer.parseInt(m.group(1));
                    newLine = Integer.parseInt(m.group(2));
                }
                lines.add(new DiffLine(DiffLine.Type.HUNK_HEADER, -1, -1, raw));
            } else if (raw.startsWith("---") || raw.startsWith("+++")) {
                // Skip file path header lines
            } else if (raw.startsWith("index ") || raw.startsWith("new file") || raw.startsWith("deleted file") || raw.startsWith("Binary files")) {
                // Skip metadata lines
            } else if (raw.startsWith("+")) {
                lines.add(new DiffLine(DiffLine.Type.ADD, -1, newLine, raw.substring(1)));
                newLine++;
            } else if (raw.startsWith("-")) {
                lines.add(new DiffLine(DiffLine.Type.REMOVE, oldLine, -1, raw.substring(1)));
                oldLine++;
            } else if (raw.startsWith("\\ No newline")) {
                lines.add(new DiffLine(DiffLine.Type.NO_NEWLINE, -1, -1, raw));
            } else {
                // Context line — may have a leading space
                String content = raw.startsWith(" ") ? raw.substring(1) : raw;
                lines.add(new DiffLine(DiffLine.Type.CONTEXT, oldLine, newLine, content));
                oldLine++;
                newLine++;
            }
        }
        return lines;
    }
}
