# Tab3 Git历史增强与文件Diff查看 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为Tab3实现可分页滚动的Git提交历史、提交间分割线、单展开文件列表，以及点击文件进入新页面默认查看Diff并可切换完整文件内容（复用Tab2查看能力）。

**Architecture:** 在现有Tab3提交数据链路上增加分页状态与触底加载机制，保持现有Git命令执行方式不变。提交列表项扩展为单展开手风琴结构，展开后动态加载该commit变更文件。新增Git文件详情页承接文件点击，默认渲染diff，同时通过已有文件查看链路显示完整文件快照。

**Tech Stack:** Android(Java), RecyclerView, Material Components, Activity导航, 现有Termux会话与Git命令执行能力

---

### Task 1: 定位Tab3与Tab2复用入口（只读梳理）

**Files:**
- Modify: `app/src/main/java/com/termux/app/fragments/TermuxFragment.java`
- Modify: `app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java`
- Modify: `app/src/main/java/com/termux/app/terminal/FullTerminalActivity.java`
- Modify: `app/src/main/res/layout/fragment_git_history.xml`

**Step 1: Write the failing test**

手工失败用例：
- 进入Tab3只能看到有限条commit，无法持续加载。
- 点击commit无法展开文件列表。
- 点击文件不能进入diff页面。

**Step 2: Run test to verify it fails**

Run: 手工在调试包中进入Tab3验证
Expected: 以上3个场景均失败

**Step 3: Write minimal implementation**

本任务只做定位，记录并确认：
- Git历史加载方法位置
- 列表Adapter与点击事件入口
- Tab2文件查看入口方法与参数

**Step 4: Run test to verify it passes**

Run: 代码阅读校验
Expected: 后续任务具备精确修改点（文件与函数明确）

**Step 5: Commit**

```bash
git add app/src/main/java/com/termux/app/fragments/TermuxFragment.java app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java app/src/main/java/com/termux/app/terminal/FullTerminalActivity.java app/src/main/res/layout/fragment_git_history.xml
git commit -m "chore: map tab3 git history flow and tab2 viewer integration points"
```

---

### Task 2: 实现Tab3分页加载（解决"数量有限"）

**Files:**
- Modify: `app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java`
- Modify: `app/src/main/res/layout/fragment_git_history.xml`

**Step 1: Write the failing test**

新增手工用例：仓库commit数量>100时，Tab3应分批加载并可持续下拉获取更多。

**Step 2: Run test to verify it fails**

Run: 打开Tab3并滚动到底部
Expected: 失败，当前不会自动加载下一批

**Step 3: Write minimal implementation**

在 `GitHistoryFragment` 增加分页字段：
```java
private static final int PAGE_SIZE = 30;
private int loadedCount = 0;
private boolean isLoading = false;
private boolean hasMore = true;
```

改造加载命令（示意）：
```java
String cmd = "git log --pretty=format:'%H|%an|%ad|%s' --date=iso -n " + PAGE_SIZE + " --skip " + loadedCount;
```

RecyclerView触底自动 `loadMore()`，并在成功后append数据：
```java
loadedCount += newItems.size();
if (newItems.size() < PAGE_SIZE) hasMore = false;
```

**Step 4: Run test to verify it passes**

Run: 手工连续下拉到底
Expected: 自动继续加载直到无更多commit

**Step 5: Commit**

```bash
git add app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java app/src/main/res/layout/fragment_git_history.xml
git commit -m "feat(tab3): add paginated git history loading with infinite scroll"
```

---

### Task 3: 提交项分割线 + 单展开手风琴

**Files:**
- Modify: `app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java`
- Modify: `app/src/main/java/com/termux/app/fragments/GitHistoryAdapter.java`
- Modify: `app/src/main/res/layout/item_git_history.xml`
- Modify: `app/src/main/res/layout/fragment_git_history.xml`

**Step 1: Write the failing test**

手工用例：
- 每条commit之间应有分割线。
- 点击某条commit展开文件列表。
- 同时只能展开一条（单展开）。

**Step 2: Run test to verify it fails**

Run: 进入Tab3点击多条记录
Expected: 当前不满足分割线与单展开规则

**Step 3: Write minimal implementation**

Adapter增加展开状态：
```java
private String expandedCommitHash = null;
```

点击逻辑：
```java
if (hash.equals(expandedCommitHash)) expandedCommitHash = null;
else expandedCommitHash = hash;
```

只刷新旧展开项和新展开项。
分割线优先使用 `RecyclerView.ItemDecoration`，避免侵入item布局。

展开时加载文件列表（示意）：
```bash
git show --name-only --pretty=format: <hash>
```

**Step 4: Run test to verify it passes**

Run: 逐条点击commit
Expected: 单展开、切换稳定、分割线可见

**Step 5: Commit**

```bash
git add app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java app/src/main/java/com/termux/app/fragments/GitHistoryAdapter.java app/src/main/res/layout/item_git_history.xml app/src/main/res/layout/fragment_git_history.xml
git commit -m "feat(tab3): add commit divider and single-expand changed-files accordion"
```

---

### Task 4: 点击文件跳转新页面，默认Diff

**Files:**
- Create: `app/src/main/java/com/termux/app/activities/GitFileDetailActivity.java`
- Create: `app/src/main/res/layout/activity_git_file_detail.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java`
- Modify: `app/src/main/java/com/termux/app/fragments/GitHistoryAdapter.java`

**Step 1: Write the failing test**

手工用例：点击展开区文件项后应跳转新页，默认显示该文件在该commit下的diff内容。

**Step 2: Run test to verify it fails**

Run: 点击commit中的文件
Expected: 当前不跳转或不显示diff

**Step 3: Write minimal implementation**

创建 `GitFileDetailActivity`，接收参数：
```java
public static final String EXTRA_COMMIT_HASH = "extra_commit_hash";
public static final String EXTRA_FILE_PATH = "extra_file_path";
```

默认加载diff（示意）：
```bash
git show <commitHash> -- <filePath>
```

页面上提供两个Tab/Button：`Diff`（默认）与 `File`。

**Step 4: Run test to verify it passes**

Run: 点击文件项进入新页
Expected: 首屏显示Diff，页面可正常返回

**Step 5: Commit**

```bash
git add app/src/main/java/com/termux/app/activities/GitFileDetailActivity.java app/src/main/res/layout/activity_git_file_detail.xml app/src/main/AndroidManifest.xml app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java app/src/main/java/com/termux/app/fragments/GitHistoryAdapter.java
git commit -m "feat(tab3): open file detail activity with diff-first mode"
```

---

### Task 5: 复用Tab2文件查看能力（File模式）

**Files:**
- Modify: `app/src/main/java/com/termux/app/activities/GitFileDetailActivity.java`
- Modify: `app/src/main/java/com/termux/app/fragments/TermuxFragment.java`
- Modify: `app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java`

**Step 1: Write the failing test**

手工用例：在详情页切换到 `File` 模式应显示该commit版本完整文件内容（不是当前工作区内容）。

**Step 2: Run test to verify it fails**

Run: Diff页切换到File
Expected: 当前无正确内容或未复用已有查看能力

**Step 3: Write minimal implementation**

复用Tab2读取渲染链路，commit快照读取命令：
```bash
git show <commitHash>:<filePath>
```

将结果交给Tab2已有文件内容显示组件/方法（抽最小公共方法，不复制整套逻辑）。

**Step 4: Run test to verify it passes**

Run: Diff↔File切换
Expected: File模式内容与 `git show <hash>:<path>`一致

**Step 5: Commit**

```bash
git add app/src/main/java/com/termux/app/activities/GitFileDetailActivity.java app/src/main/java/com/termux/app/fragments/TermuxFragment.java app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java
git commit -m "feat(tab3): reuse tab2 file viewer flow for commit snapshot content"
```

---

### Task 6: UI/UX收尾（滚动、状态、空态/错误态）

**Files:**
- Modify: `app/src/main/res/layout/fragment_git_history.xml`
- Modify: `app/src/main/res/layout/activity_git_file_detail.xml`
- Modify: `app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java`
- Modify: `app/src/main/java/com/termux/app/activities/GitFileDetailActivity.java`

**Step 1: Write the failing test**

手工用例：
- 列表不被父容器截断（可滚过一屏）。
- 分页中有加载反馈。
- diff/file加载失败有提示。

**Step 2: Run test to verify it fails**

Run: 快速滚动与弱网场景
Expected: 当前反馈不完整或滚动受限

**Step 3: Write minimal implementation**

- 修正列表容器高度/约束避免"一屏限制"
- 增加底部loading视图与"无更多数据"状态
- 增加空态与错误提示（Toast/inline text）

**Step 4: Run test to verify it passes**

Run: 手工全链路验证
Expected: 滚动、加载、失败反馈都可用

**Step 5: Commit**

```bash
git add app/src/main/res/layout/fragment_git_history.xml app/src/main/res/layout/activity_git_file_detail.xml app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java app/src/main/java/com/termux/app/activities/GitFileDetailActivity.java
git commit -m "fix(tab3): improve scroll constraints and loading/error UX states"
```

---

### Task 7: 构建与回归验证

**Files:**
- Test: `app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java`
- Test: `app/src/main/java/com/termux/app/activities/GitFileDetailActivity.java`

**Step 1: Write the failing test**

回归清单：
- Tab3分页持续加载
- 单展开手风琴
- 文件跳转Diff默认
- File模式正确快照
- Tab2原功能无回归

**Step 2: Run test to verify it fails**

Run: 逐项手测
Expected: 修复前至少一项失败

**Step 3: Write minimal implementation**

修复发现的问题（仅最小改动）。

**Step 4: Run test to verify it passes**

Run:
```bash
./gradlew app:assembleDebug
```
Expected: BUILD SUCCESSFUL

并完成手工回归全部通过。

**Step 5: Commit**

```bash
git add app/src/main/java/com/termux/app/fragments/GitHistoryFragment.java app/src/main/java/com/termux/app/activities/GitFileDetailActivity.java app/src/main/res/layout/fragment_git_history.xml app/src/main/res/layout/activity_git_file_detail.xml
git commit -m "test: verify tab3 git pagination accordion and file diff/detail flow"
```

---

## Verification Matrix (End-to-End)

1. 准备含 100+ commits 的仓库目录作为会话 cwd
2. 打开 Tab3，确认初始只加载 30 条
3. 连续滑到底，确认自动追加到 60/90...
4. 点任意 commit 展开文件，确认其他 commit 自动收起
5. 点展开文件进入详情页，默认停留 `Diff`
6. 切到 `File`，内容与 `git show <hash>:<path>` 一致
7. 快速前后切换不同文件，确认无错位/无崩溃
8. 返回 Tab3 保持列表位置与展开状态
