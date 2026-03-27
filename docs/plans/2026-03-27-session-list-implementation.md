# Terminal Session List Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Transform Tab1 from single terminal view to session list (WeChat-style) with independent terminal activities.

**Architecture:** SessionManager singleton manages all sessions with SharedPreferences persistence. Each session launches independent TerminalSessionActivity. SessionListFragment shows all sessions with real-time status updates on resume.

**Tech Stack:** Kotlin, AndroidX RecyclerView, MaterialCardView, SharedPreferences, LocalBroadcastManager

---

## Task 1: Create SessionInfo Data Model

**Files:**
- Create: `app/src/main/java/com/termux/app/sessions/SessionInfo.kt`

**Step 1: Write the data model**

```kotlin
package com.termux.app.sessions

data class SessionInfo(
    val id: String,
    val name: String,
    val sshConfigName: String?,
    val currentPath: String,
    val lastCommand: String,
    val userPrompt: String?,
    val status: SessionStatus,
    val statusDetail: String?,
    val startTime: Long,
    val lastActiveTime: Long
)

enum class SessionStatus {
    IDLE,
    RUNNING,
    CLAUDE_THINKING,
    CLAUDE_WORKING,
    WAITING_INPUT,
    ERROR
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/termux/app/sessions/SessionInfo.kt
git commit -m "feat: add SessionInfo data model and SessionStatus enum"
```

---

## Task 2: Create SessionManager Singleton

**Files:**
- Create: `app/src/main/java/com/termux/app/sessions/SessionManager.kt`

**Step 1: Write SessionManager**

```kotlin
package com.termux.app.sessions

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SessionManager {
    private const val PREFS_NAME = "termux_sessions"
    private const val KEY_SESSIONS = "sessions"

    private lateinit var prefs: SharedPreferences
    private val sessions = mutableListOf<SessionInfo>()
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromStorage()
    }

    fun createSession(name: String, sshConfigName: String?, path: String): SessionInfo {
        val session = SessionInfo(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            sshConfigName = sshConfigName,
            currentPath = path,
            lastCommand = "",
            userPrompt = null,
            status = SessionStatus.IDLE,
            statusDetail = null,
            startTime = System.currentTimeMillis(),
            lastActiveTime = System.currentTimeMillis()
        )
        sessions.add(0, session)
        saveToStorage()
        return session
    }

    fun closeSession(sessionId: String) {
        sessions.removeAll { it.id == sessionId }
        saveToStorage()
    }

    fun updateSessionStatus(sessionId: String, status: SessionStatus, detail: String?) {
        sessions.indexOfFirst { it.id == sessionId }.takeIf { it >= 0 }?.let { index ->
            sessions[index] = sessions[index].copy(
                status = status,
                statusDetail = detail,
                lastActiveTime = System.currentTimeMillis()
            )
            saveToStorage()
        }
    }

    fun updateSessionPrompt(sessionId: String, prompt: String) {
        sessions.indexOfFirst { it.id == sessionId }.takeIf { it >= 0 }?.let { index ->
            sessions[index] = sessions[index].copy(
                userPrompt = prompt,
                lastActiveTime = System.currentTimeMillis()
            )
            saveToStorage()
        }
    }

    fun updateLastCommand(sessionId: String, command: String) {
        sessions.indexOfFirst { it.id == sessionId }.takeIf { it >= 0 }?.let { index ->
            sessions[index] = sessions[index].copy(
                lastCommand = command,
                lastActiveTime = System.currentTimeMillis()
            )
            saveToStorage()
        }
    }

    fun getSession(sessionId: String): SessionInfo? {
        return sessions.find { it.id == sessionId }
    }

    fun getAllSessions(): List<SessionInfo> = sessions.toList()

    private fun saveToStorage() {
        val json = gson.toJson(sessions)
        prefs.edit().putString(KEY_SESSIONS, json).apply()
    }

    private fun loadFromStorage() {
        val json = prefs.getString(KEY_SESSIONS, null)
        if (json != null) {
            val type = object : TypeToken<List<SessionInfo>>() {}.type
            sessions.clear()
            sessions.addAll(gson.fromJson(json, type))
        }
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/termux/app/sessions/SessionManager.kt
git commit -m "feat: add SessionManager singleton for session lifecycle management"
```

---

## Task 3: Create Session Item Layout

**Files:**
- Create: `app/src/main/res/layout/item_session.xml`

**Step 1: Write the layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/session_card"
    android:layout_width="match_parent"
    android:layout_height="72dp"
    android:layout_marginHorizontal="8dp"
    android:layout_marginVertical="4dp"
    android:clickable="true"
    android:focusable="true"
    app:cardBackgroundColor="@color/surface"
    app:cardCornerRadius="12dp"
    app:cardElevation="0dp"
    app:rippleColor="@color/primary_20">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:padding="12dp">

        <!-- Status Icon -->
        <TextView
            android:id="@+id/session_status_icon"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="@drawable/bg_session_icon"
            android:gravity="center"
            android:text="○"
            android:textColor="@color/status_idle"
            android:textSize="20sp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            tools:text="◐" />

        <!-- Session Name -->
        <TextView
            android:id="@+id/session_name"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:layout_marginEnd="8dp"
            android:ellipsize="end"
            android:maxLines="1"
            android:textColor="@color/text_primary"
            android:textSize="16sp"
            android:textStyle="bold"
            app:layout_constraintEnd_toStartOf="@id/session_time"
            app:layout_constraintStart_toEndOf="@id/session_status_icon"
            app:layout_constraintTop_toTopOf="parent"
            tools:text="~/project" />

        <!-- Last Command / User Prompt -->
        <TextView
            android:id="@+id/session_last_command"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="12dp"
            android:layout_marginTop="2dp"
            android:layout_marginEnd="8dp"
            android:ellipsize="end"
            android:maxLines="1"
            android:textColor="@color/text_secondary"
            android:textSize="14sp"
            app:layout_constraintEnd_toStartOf="@id/session_status_text"
            app:layout_constraintStart_toEndOf="@id/session_status_icon"
            app:layout_constraintTop_toBottomOf="@id/session_name"
            tools:text="$ claude-code" />

        <!-- Time -->
        <TextView
            android:id="@+id/session_time"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textColor="@color/text_hint"
            android:textSize="12sp"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            tools:text="10:30" />

        <!-- Status Text -->
        <TextView
            android:id="@+id/session_status_text"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textColor="@color/text_hint"
            android:textSize="12sp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            tools:text="空闲" />

    </androidx.constraintlayout.widget.ConstraintLayout>

</com.google.android.material.card.MaterialCardView>
```

**Step 2: Create supporting drawable**

Create: `app/src/main/res/drawable/bg_session_icon.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/surface_elevated" />
    <corners android:radius="8dp" />
</shape>
```

**Step 3: Add colors**

Modify: `app/src/main/res/values/colors.xml` (add if not exists)

```xml
<color name="background">#020617</color>
<color name="surface">#0f172a</color>
<color name="surface_elevated">#1e293b</color>
<color name="text_primary">#f8fafc</color>
<color name="text_secondary">#94a3b8</color>
<color name="text_hint">#64748b</color>
<color name="primary">#3b82f6</color>
<color name="primary_20">#333b82f6</color>
<color name="status_idle">#64748b</color>
<color name="status_running">#3b82f6</color>
<color name="status_thinking">#f59e0b</color>
<color name="status_working">#8b5cf6</color>
<color name="status_waiting">#22c55e</color>
<color name="status_error">#ef4444</color>
```

**Step 4: Commit**

```bash
git add app/src/main/res/layout/item_session.xml \
        app/src/main/res/drawable/bg_session_icon.xml \
        app/src/main/res/values/colors.xml
git commit -m "feat: add session item layout and supporting resources"
```

---

## Task 4: Create Session List Fragment Layout

**Files:**
- Create: `app/src/main/res/layout/fragment_session_list.xml`

**Step 1: Write the layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background">

    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@color/background">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="56dp"
            app:title="终端会话"
            app:titleTextColor="@color/text_primary" />

    </com.google.android.material.appbar.AppBarLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/sessions_recycler_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:clipToPadding="false"
        android:paddingTop="8dp"
        android:paddingBottom="88dp"
        app:layout_behavior="@string/appbar_scrolling_view_behavior" />

    <!-- Empty State -->
    <LinearLayout
        android:id="@+id/empty_state"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:gravity="center"
        android:orientation="vertical"
        android:visibility="gone">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="暂无会话"
            android:textColor="@color/text_secondary"
            android:textSize="16sp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="点击右下角 + 新建会话"
            android:textColor="@color/text_hint"
            android:textSize="14sp" />

    </LinearLayout>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/fab_new_session"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom|end"
        android:layout_margin="16dp"
        android:contentDescription="新建会话"
        android:src="@drawable/ic_add"
        app:backgroundTint="@color/primary"
        app:tint="@color/white" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

**Step 2: Commit**

```bash
git add app/src/main/res/layout/fragment_session_list.xml
git commit -m "feat: add session list fragment layout"
```

---

## Task 5: Create SessionAdapter

**Files:**
- Create: `app/src/main/java/com/termux/app/sessions/SessionAdapter.kt`

**Step 1: Write the adapter**

```kotlin
package com.termux.app.sessions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.termux.app.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionAdapter(
    private val onSessionClick: (SessionInfo) -> Unit,
    private val onSessionLongClick: (SessionInfo) -> Boolean
) : ListAdapter<SessionInfo, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(
        private val binding: ItemSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: SessionInfo) {
            binding.apply {
                sessionName.text = session.name
                sessionLastCommand.text = session.userPrompt ?: session.lastCommand
                sessionTime.text = formatTime(session.lastActiveTime)

                // Status icon and text
                val (icon, color, statusText) = getStatusDisplay(session.status)
                sessionStatusIcon.text = icon
                sessionStatusIcon.setTextColor(color)
                sessionStatusText.text = statusText
                sessionStatusText.setTextColor(color)

                root.setOnClickListener { onSessionClick(session) }
                root.setOnLongClickListener { onSessionLongClick(session) }
            }
        }

        private fun getStatusDisplay(status: SessionStatus): Triple<String, Int, String> {
            val context = binding.root.context
            return when (status) {
                SessionStatus.IDLE -> Triple("○", getColor(context, "status_idle"), "空闲")
                SessionStatus.RUNNING -> Triple("●", getColor(context, "status_running"), "运行中")
                SessionStatus.CLAUDE_THINKING -> Triple("◐", getColor(context, "status_thinking"), "思考中")
                SessionStatus.CLAUDE_WORKING -> Triple("◑", getColor(context, "status_working"), "输出中")
                SessionStatus.WAITING_INPUT -> Triple("◕", getColor(context, "status_waiting"), "等待输入")
                SessionStatus.ERROR -> Triple("⚠", getColor(context, "status_error"), "错误")
            }
        }

        private fun getColor(context: android.content.Context, name: String): Int {
            return context.getColor(context.resources.getIdentifier(name, "color", context.packageName))
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            return when {
                diff < 60_000 -> "刚刚"
                diff < 3600_000 -> "${diff / 60_000}分钟前"
                diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                else -> SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<SessionInfo>() {
        override fun areItemsTheSame(oldItem: SessionInfo, newItem: SessionInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SessionInfo, newItem: SessionInfo): Boolean {
            return oldItem == newItem
        }
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/termux/app/sessions/SessionAdapter.kt
git commit -m "feat: add SessionAdapter with status display"
```

---

## Task 6: Create NewSessionDialog

**Files:**
- Create: `app/src/main/java/com/termux/app/sessions/NewSessionDialog.kt`
- Create: `app/src/main/res/layout/dialog_new_session.xml`

**Step 1: Write the dialog layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="新建会话"
        android:textColor="@color/text_primary"
        android:textSize="20sp"
        android:textStyle="bold" />

    <!-- SSH Connection Dropdown -->
    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/ssh_dropdown_layout"
        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:hint="SSH连接 (可选)">

        <AutoCompleteTextView
            android:id="@+id/ssh_dropdown"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="none"
            android:textColor="@color/text_primary" />

    </com.google.android.material.textfield.TextInputLayout>

    <!-- Bookmark Path Dropdown -->
    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/path_dropdown_layout"
        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox.ExposedDropdownMenu"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:hint="收藏路径">

        <AutoCompleteTextView
            android:id="@+id/path_dropdown"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="none"
            android:textColor="@color/text_primary" />

    </com.google.android.material.textfield.TextInputLayout>

    <!-- Session Name -->
    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/session_name_layout"
        style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:hint="会话名称 (可选)">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/session_name_input"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="text"
            android:textColor="@color/text_primary" />

    </com.google.android.material.textfield.TextInputLayout>

    <!-- Buttons -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="24dp"
        android:gravity="end"
        android:orientation="horizontal">

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_cancel"
            style="@style/Widget.MaterialComponents.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="取消" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_create"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:text="创建" />

    </LinearLayout>

</LinearLayout>
```

**Step 2: Write the dialog class**

```kotlin
package com.termux.app.sessions

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import com.termux.app.R
import com.termux.app.databinding.DialogNewSessionBinding
import com.termux.app.models.SSHConfigManager
import com.termux.app.managers.ProjectWorkspaceManager
import com.termux.app.models.DirectoryBookmark

class NewSessionDialog(
    context: Context,
    private val onSessionCreate: (name: String, sshConfig: String?, path: String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogNewSessionBinding
    private var selectedSshConfig: String? = null
    private var selectedPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogNewSessionBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        window?.setBackgroundDrawableResource(android.R.color.transparent)

        setupSshDropdown()
        setupPathDropdown()
        setupButtons()
    }

    private fun setupSshDropdown() {
        val sshConfigs = try {
            SSHConfigManager.getInstance(context).allConfigs.map { it.name }
        } catch (e: Exception) {
            emptyList()
        }

        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, sshConfigs)
        binding.sshDropdown.setAdapter(adapter)
        binding.sshDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedSshConfig = sshConfigs[position]
        }
    }

    private fun setupPathDropdown() {
        val paths = try {
            ProjectWorkspaceManager.getInstance(context)
                .allBookmarks
                .map { it.displayName to it.fullPath }
        } catch (e: Exception) {
            emptyList()
        }

        val displayNames = paths.map { it.first }
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, displayNames)
        binding.pathDropdown.setAdapter(adapter)
        binding.pathDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedPath = paths[position].second
        }

        // Default to first path if available
        if (paths.isNotEmpty()) {
            selectedPath = paths[0].second
            binding.pathDropdown.setText(paths[0].first, false)
        }
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnCreate.setOnClickListener {
            val name = binding.sessionNameInput.text?.toString()?.takeIf { it.isNotBlank() }
                ?: selectedPath.substringAfterLast("/").ifEmpty { "终端" }

            onSessionCreate(name, selectedSshConfig, selectedPath)
            dismiss()
        }
    }
}
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/termux/app/sessions/NewSessionDialog.kt \
        app/src/main/res/layout/dialog_new_session.xml
git commit -m "feat: add NewSessionDialog with SSH and path selection"
```

---

## Task 7: Create SessionListFragment

**Files:**
- Create: `app/src/main/java/com/termux/app/sessions/SessionListFragment.kt`

**Step 1: Write the fragment**

```kotlin
package com.termux.app.sessions

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.termux.app.databinding.FragmentSessionListBinding
import com.termux.app.terminal.TerminalSessionActivity

class SessionListFragment : Fragment() {

    private var _binding: FragmentSessionListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SessionManager.init(requireContext())

        setupRecyclerView()
        setupFab()
        updateEmptyState()
    }

    override fun onResume() {
        super.onResume()
        refreshSessions()
    }

    private fun setupRecyclerView() {
        adapter = SessionAdapter(
            onSessionClick = { session ->
                openTerminalSession(session)
            },
            onSessionLongClick = { session ->
                showSessionOptions(session)
                true
            }
        )

        binding.sessionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@SessionListFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabNewSession.setOnClickListener {
            showNewSessionDialog()
        }
    }

    private fun refreshSessions() {
        adapter.submitList(SessionManager.allSessions)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val isEmpty = SessionManager.allSessions.isEmpty()
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.sessionsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showNewSessionDialog() {
        val dialog = NewSessionDialog(requireContext()) { name, sshConfig, path ->
            val session = SessionManager.createSession(name, sshConfig, path)
            openTerminalSession(session)
        }
        dialog.show()
    }

    private fun openTerminalSession(session: SessionInfo) {
        val intent = Intent(requireContext(), TerminalSessionActivity::class.java).apply {
            putExtra(TerminalSessionActivity.EXTRA_SESSION_ID, session.id)
            putExtra(TerminalSessionActivity.EXTRA_SESSION_NAME, session.name)
            putExtra(TerminalSessionActivity.EXTRA_SSH_CONFIG, session.sshConfigName)
            putExtra(TerminalSessionActivity.EXTRA_INITIAL_PATH, session.currentPath)
        }
        startActivity(intent)
    }

    private fun showSessionOptions(session: SessionInfo) {
        AlertDialog.Builder(requireContext())
            .setTitle(session.name)
            .setItems(arrayOf("关闭会话")) { _, _ ->
                SessionManager.closeSession(session.id)
                refreshSessions()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/termux/app/sessions/SessionListFragment.kt
git commit -m "feat: add SessionListFragment with RecyclerView and FAB"
```

---

## Task 8: Create TerminalSessionActivity

**Files:**
- Create: `app/src/main/java/com/termux/app/terminal/TerminalSessionActivity.kt`
- Create: `app/src/main/res/layout/activity_terminal_session.xml`

**Step 1: Write the activity layout**

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

    <!-- Toolbar -->
    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:background="@color/background"
        app:navigationIcon="@drawable/ic_arrow_back"
        app:navigationIconTint="@color/text_primary"
        app:titleTextColor="@color/text_primary" />

    <!-- Terminal View -->
    <com.termux.view.TerminalView
        android:id="@+id/terminal_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_above="@+id/terminal_toolbar_view_pager"
        android:layout_below="@+id/toolbar"
        android:background="#000000" />

    <!-- Terminal Toolbar -->
    <androidx.viewpager.widget.ViewPager
        android:id="@+id/terminal_toolbar_view_pager"
        android:layout_width="match_parent"
        android:layout_height="75dp"
        android:background="@color/background"
        android:layout_above="@+id/terminal_input_container" />

    <!-- Terminal Input -->
    <LinearLayout
        android:id="@+id/terminal_input_container"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true"
        android:background="@color/surface"
        android:orientation="horizontal"
        android:padding="8dp">

        <EditText
            android:id="@+id/terminal_command_input"
            android:layout_width="0dp"
            android:layout_height="40dp"
            android:layout_weight="1"
            android:background="@drawable/terminal_input_background"
            android:hint="输入命令..."
            android:imeOptions="actionSend"
            android:inputType="text"
            android:paddingHorizontal="12dp"
            android:singleLine="true"
            android:textColor="@color/white"
            android:textColorHint="@color/text_hint"
            android:textSize="14sp" />

        <ImageButton
            android:id="@+id/claude_code_menu_button"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:layout_marginStart="4dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="Claude Code 菜单"
            android:src="@drawable/ic_claude_code"
            android:padding="8dp" />

        <ImageButton
            android:id="@+id/terminal_send_button"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:layout_marginStart="4dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="发送命令"
            android:src="@drawable/ic_send"
            android:padding="8dp" />

    </LinearLayout>

</RelativeLayout>
```

**Step 2: Write the activity class (simplified based on existing TermuxFragment)**

```kotlin
package com.termux.app.terminal

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.termux.app.sessions.SessionManager
import com.termux.app.sessions.SessionStatus
import com.termux.app.databinding.ActivityTerminalSessionBinding
import com.termux.app.terminal.io.TerminalToolbarViewPager
import com.termux.shared.logger.Logger

class TerminalSessionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_SESSION_NAME = "session_name"
        const val EXTRA_SSH_CONFIG = "ssh_config"
        const val EXTRA_INITIAL_PATH = "initial_path"
    }

    private lateinit var binding: ActivityTerminalSessionBinding
    private var sessionId: String = ""
    private var sessionName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: run {
            finish()
            return
        }
        sessionName = intent.getStringExtra(EXTRA_SESSION_NAME) ?: "终端"

        setupToolbar()
        setupTerminalView()
        setupInputView()

        // Update session status to running
        SessionManager.updateSessionStatus(sessionId, SessionStatus.RUNNING, null)
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = sessionName
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
    }

    private fun setupTerminalView() {
        // Reuse terminal setup logic from TermuxFragment
        // This is a simplified version - actual implementation will use TermuxService
        binding.terminalView.apply {
            // Terminal view setup similar to TermuxFragment
        }
    }

    private fun setupInputView() {
        binding.terminalSendButton.setOnClickListener {
            val command = binding.terminalCommandInput.text?.toString() ?: ""
            if (command.isNotEmpty()) {
                sendCommand(command)
                binding.terminalCommandInput.text?.clear()
            }
        }
    }

    private fun sendCommand(command: String) {
        // Send command to terminal session
        // Update session manager with last command
        SessionManager.updateLastCommand(sessionId, command)

        // Detect Claude Code commands and update status
        when {
            command.contains("claude-code") || command.contains("claude") -> {
                SessionManager.updateSessionStatus(sessionId, SessionStatus.CLAUDE_THINKING, "启动中")
            }
            command.startsWith("/") -> {
                // AI command
                SessionManager.updateSessionStatus(sessionId, SessionStatus.CLAUDE_THINKING, "思考中")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SessionManager.updateSessionStatus(sessionId, SessionStatus.RUNNING, null)
    }

    override fun onPause() {
        super.onPause()
        SessionManager.updateSessionStatus(sessionId, SessionStatus.IDLE, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Session continues running even if activity destroyed
        // User can return to it from session list
    }
}
```

**Step 3: Add drawable resources**

Create: `app/src/main/res/drawable/ic_arrow_back.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z"/>
</vector>
```

Create: `app/src/main/res/drawable/terminal_input_background.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/surface_elevated" />
    <corners android:radius="8dp" />
</shape>
```

**Step 4: Register Activity in manifest**

Modify: `app/src/main/AndroidManifest.xml` (add activity declaration)

```xml
<activity
    android:name=".terminal.TerminalSessionActivity"
    android:exported="false"
    android:launchMode="singleTop" />
```

**Step 5: Commit**

```bash
git add app/src/main/java/com/termux/app/terminal/TerminalSessionActivity.kt \
        app/src/main/res/layout/activity_terminal_session.xml \
        app/src/main/res/drawable/ic_arrow_back.xml \
        app/src/main/res/drawable/terminal_input_background.xml
git commit -m "feat: add TerminalSessionActivity for full-screen terminal"
```

---

## Task 9: Update MainTabActivity to Use SessionListFragment

**Files:**
- Modify: `app/src/main/java/com/termux/app/MainTabActivity.kt`

**Step 1: Update TabPagerAdapter**

Change Tab 1 from `TermuxFragment()` to `SessionListFragment()`:

```kotlin
// In TabPagerAdapter.createFragment()
when (position) {
    0 -> SessionListFragment() // Changed from TermuxFragment()
    1 -> RemoteFileBrowserFragment()
    2 -> GitHistoryFragment()
    3 -> ConfigurationMainFragment()
}
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/termux/app/MainTabActivity.kt
git commit -m "refactor: use SessionListFragment for Tab1"
```

---

## Task 10: Build and Test

**Step 1: Build debug APK**

```bash
cd /home/swenze/StudioProjects/termux-ai-dev
./gradlew app:assembleDebug 2>&1
```

**Step 2: Fix any compilation errors**

Common issues:
- Missing imports
- Resource not found
- Activity not registered

**Step 3: Verify features**

1. Open app → Tab1 shows session list (empty state)
2. Tap FAB → New session dialog appears
3. Select SSH config and path → Create session
4. Session card appears in list
5. Tap session card → TerminalActivity opens
6. Return to list → Session status and command updated

---

## Summary

| Task | Description |
|------|-------------|
| 1 | SessionInfo data model |
| 2 | SessionManager singleton |
| 3 | Session item layout |
| 4 | Session list fragment layout |
| 5 | SessionAdapter |
| 6 | NewSessionDialog |
| 7 | SessionListFragment |
| 8 | TerminalSessionActivity |
| 9 | Update MainTabActivity |
| 10 | Build and test |

---

**Plan complete.** Two execution options:

**1. Subagent-Driven (this session)** - I dispatch fresh subagent per task, review between tasks, fast iteration

**2. Parallel Session (separate)** - Open new session with executing-plans, batch execution with checkpoints

**Which approach?**