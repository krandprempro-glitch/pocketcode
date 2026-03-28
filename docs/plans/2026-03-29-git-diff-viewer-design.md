# Git Diff Viewer Enhancement Design

**Date**: 2026-03-29
**Status**: Approved

## Summary

Two improvements to the Tab3 Git History feature:
1. File list in commit accordion shows only filename (not full path)
2. GitFileDetailActivity Diff tab upgraded to GitLab-style colored diff view

## Change 1: Filename-Only in File List

**Current**: `item_changed_file.xml` shows the full file path (e.g., `src/app/java/com/example/MainActivity.java`)
**Target**: Show only the filename (e.g., `MainActivity.java`)

**Implementation**:
- In `GitCommitAdapter.bind()`, extract filename from path: `file.getPath().substring(file.getPath().lastIndexOf('/') + 1)`
- Full path remains available via `file.getPath()` for click actions (passed to `GitFileDetailActivity`)

## Change 2: GitLab-Style Diff Viewer

### Current State
- `GitFileDetailActivity` runs `git show <hash> -- <path>` and dumps raw output into a plain `TextView`
- No coloring, no line numbers, no hunk headers

### Target State
- RecyclerView-based diff renderer with colored lines
- GitLab/GitHub code review style

### New Components

#### 1. DiffLine Model (`com.termux.app.models.DiffLine`)
```
- type: enum (ADD, REMOVE, CONTEXT, HUNK_HEADER, FILE_HEADER, NO_NEWLINE)
- oldLineNumber: int (-1 if N/A)
- newLineNumber: int (-1 if N/A)
- content: String (the text content without +/- prefix)
```

#### 2. DiffParser Utility (`com.termux.app.utils.DiffParser`)
- Input: raw `git diff` output string
- Output: `List<DiffLine>`
- Parsing logic:
  - Lines starting with `diff --git`: FILE_HEADER
  - Lines starting with `@@`: HUNK_HEADER, extract line numbers via regex `@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@`
  - Lines starting with `+` (not `+++`): ADD, increment new line counter
  - Lines starting with `-` (not `---`): REMOVE, increment old line counter
  - Lines starting with `\ No newline`: NO_NEWLINE
  - Everything else: CONTEXT, increment both counters
  - Skip `index`, `---`, `+++`, `new file`, `deleted file` lines
- Line number tracking: maintain separate old/new counters, advancing based on line type

#### 3. DiffLineAdapter (`com.termux.app.adapters.DiffLineAdapter`)
- RecyclerView adapter rendering `DiffLine` items
- Layout: horizontal row with:
  - Old line number column (fixed width, right-aligned, gray)
  - New line number column (fixed width, right-aligned, gray)
  - Content area (monospace, colored background)
- Color scheme:
  - ADD: green background (`#e6ffed`), green left border, `+` prefix in text
  - REMOVE: red background (`#ffeef0`), red left border, `-` prefix in text
  - CONTEXT: white/transparent background
  - HUNK_HEADER: light blue background (`#f1f8ff`), bold
  - FILE_HEADER: gray background, bold
  - NO_NEWLINE: italic, gray text

#### 4. Layout: `item_diff_line.xml`
```
LinearLayout (horizontal, minHeight=24dp)
├── TextView (old_line_num, 48dp wide, right-aligned, gray text, 10sp)
├── TextView (new_line_num, 48dp wide, right-aligned, gray text, 10sp)
└── TextView (content, weight=1, monospace, 12sp, singleLine=false)
```

#### 5. Updated `activity_git_file_detail.xml`
- Replace `ScrollView > TextView` (diff_content) with `RecyclerView` (diff_recycler_view)
- Keep `ScrollView > TextView` (file_content) for the "File" tab unchanged
- Keep ProgressBar and error view

### Git Command Change
- Current: `git -C "<dir>" show <hash> -- "<path>"`
- New: `git -C "<dir>" diff <hash>^ <hash> -- "<path>"` (shows proper diff with parent)
- Fallback for initial commits (no parent): `git -C "<dir>" show <hash> -- "<path>"` with diff format flag
- Better approach: Use `git -C "<dir>" show --format="" <hash> -- "<path>"` which gives diff output directly

### Color Resources
Add to `colors.xml`:
- `diff_add_background` = `#e6ffed`
- `diff_add_text` = `#22863a`
- `diff_remove_background` = `#ffeef0`
- `diff_remove_text` = `#cb2431`
- `diff_hunk_background` = `#f1f8ff`
- `diff_line_number` = `#959da5`
- `diff_file_header_background` = `#f6f8fa`

## Files to Create
1. `app/src/main/java/com/termux/app/models/DiffLine.java`
2. `app/src/main/java/com/termux/app/utils/DiffParser.java`
3. `app/src/main/java/com/termux/app/adapters/DiffLineAdapter.java`
4. `app/src/main/res/layout/item_diff_line.xml`

## Files to Modify
1. `app/src/main/java/com/termux/app/activities/GitFileDetailActivity.java` — replace TextView with RecyclerView
2. `app/src/main/res/layout/activity_git_file_detail.xml` — swap diff TextView for RecyclerView
3. `app/src/main/java/com/termux/app/adapters/GitCommitAdapter.java` — show filename only
4. `app/src/main/res/values/colors.xml` — add diff color resources

## Out of Scope
- Syntax highlighting (language-aware coloring)
- Inline diff in commit accordion
- Side-by-side diff view
- File content tab enhancement
