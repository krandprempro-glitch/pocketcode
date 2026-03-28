# Background Terminal Sessions Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Allow terminal sessions to continue running in the background when user presses back, maintaining SSH connections and enabling session recovery.

**Architecture:** TermuxService will remain running after Activity unbinds, sessions persist in `mShellManager.mTermuxSessions`, and session recovery happens via `terminalHandle` lookup.

**Tech Stack:** Android Service lifecycle, TermuxSession/TerminalSession, SharedPreferences for session metadata persistence.

---

## Task 1: Verify Session Persistence in TermuxService

**Files:**
- Modify: `app/src/main/java/com/termux/app/TermuxService.java:163-166`

**Step 1: Check current onStartCommand return value**

Read `TermuxService.java` lines 163-166 to verify current return value.

**Expected:**
```java
// If this service really do get killed, there is no point restarting it automatically - let the user do on next
// start of {@link Term):
return Service.START_NOT_STICKY;
```

**Step 2: Change to START_STICKY for service survival**

Edit line 165 to:
```java
return Service.START_STICKY;
```

**Step 3: Add debug logging before return**

Add logging to confirm the change:
```java
Logger.logDebug(LOG_TAG, "Returning START_STICKY for service survival");
return Service.START_STICKY;
```

**Step 4: Commit**

```bash
git add app/src/main/java/com/termux/app/TermuxService.java
git commit -m "feat(background-session): enable START_STICKY for service survival"
```

---

## Task 2: Ensure SessionManager Persists terminalHandle

**Files:**
- Modify: `app/src/main/java/com/termux/app/sessions/SessionManager.kt`

**Step 1: Verify terminalHandle persistence**

Check that `updateTerminalHandle()` calls `saveToStorage()` - it does at line 81.

**Step 2: Verify getSessionByHandle method exists**

If not present, add method to retrieve session by terminalHandle:
```kotlin
fun getSessionByHandle(handle: String): SessionInfo? {
    return sessions.find { it.terminalHandle == handle }
}
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/termux/app/sessions/SessionManager.kt
git commit -m "feat(background-session): add getSessionByHandle method"
```

---

## Task 3: Verify FullTerminalActivity Session Recovery

**Files:**
- Modify: `app/src/main/java/com/termux/app/terminal/FullTerminalActivity.java:226-246`

**Step 1: Review attachSession flow**

Read lines 226-246 to verify the session recovery logic:
```java
private void attachSession() {
    // Try to find existing session by handle
    if (mSessionHandle != null && mTermuxService != null) {
        TerminalSession session = findSessionByHandle(mSessionHandle);
        if (session != null) {
            // Found existing session - attach
            mTerminalSession = session;
            doAttachSession(session);
            return;
        }
    }
    // Not found - create new
    createNewSession();
}
```

**Step 2: Verify findSessionByHandle implementation**

Check method at lines 303-318 - it iterates through `mTermuxService.getTermuxSessions()` and matches `mHandle`.

**Step 3: Add debug logging when session not found by handle**

Modify the else branch in `attachSession()`:
```java
} else {
    Log.d(TAG, "Session not found by handle: " + mSessionHandle + ", will create new");
}
```

**Step 4: Commit**

```bash
git add app/src/main/java/com/termux/app/terminal/FullTerminalActivity.java
git commit -m "debug(background-session): add logging for session recovery"
```

---

## Task 4: Verify onDestroy Keeps Service Running

**Files:**
- Modify: `app/src/main/java/com/termux/app/terminal/FullTerminalActivity.java:369-378`

**Step 1: Review onDestroy implementation**

Current code:
```java
@Override
protected void onDestroy() {
    super.onDestroy();
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    if (mServiceBound) {
        unbindService(this);
        mServiceBound = false;
    }
}
```

**Step 2: Verify onUnbind returns false in TermuxService**

Check `TermuxService.java:192-201`:
```java
@Override
public boolean onUnbind(Intent intent) {
    Logger.logVerbose(LOG_TAG, "onUnbind");
    if (mTermuxTerminalSessionActivityClient != null)
        unsetTermuxTerminalSessionClient();
    return false;  // This prevents service destruction
}
```

**Step 3: Ensure onDestroy does NOT stop service**

Current implementation only unbinds, does not call `stopService()`. This is correct.

**Step 4: Commit**

```bash
git add app/src/main/java/com/termux/app/terminal/FullTerminalActivity.java
git commit -m "feat(background-session): ensure service continues after activity destroy"
```

---

## Task 5: Add SessionListFragment Handles to Intent

**Files:**
- Modify: `app/src/main/java/com/termux/app/sessions/SessionListFragment.kt`

**Step 1: Review openTerminalSession method**

Check that `terminalHandle` is passed in intent extras. Current code at lines 75-81 passes it:
```kotlin
session.terminalHandle?.let { putExtra(FullTerminalActivity.EXTRA_SESSION_HANDLE, it) }
```

**Step 2: Verify SessionInfo has terminalHandle**

The data class already has `terminalHandle: String? = null` field.

**Step 3: Commit**

```bash
git add app/src/main/java/com/termux/app/sessions/SessionListFragment.kt
git commit -m "feat(background-session): ensure terminalHandle passed to activity"
```

---

## Task 6: Build and Test

**Step 1: Run Gradle build**

```bash
./gradlew app:assembleDebug 2>&1 | tail -50
```

**Expected:** BUILD SUCCESSFUL

**Step 2: If build fails, fix errors and commit**

**Step 3: Mark tasks complete**

After successful build, mark all tasks completed.

---

## Task 7: Integration Testing

**Manual test steps:**

1. **Start app** → Tab1 session list
2. **Click session** → FullTerminalActivity opens, terminal displays
3. **Press back** → Return to session list
4. **Verify terminal still running** (if SSH, connection maintained)
5. **Click same session** → Should restore existing terminal view (not blank)
6. **Open multiple sessions** → Each persists independently in background

**Expected results:**
- Step 3: Terminal process continues running
- Step 5: Session restored with full terminal content
- Step 6: All sessions remain active

---

## Error Handling Cases

### Case 1: Service killed by system

- Service restarts via START_STICKY
- Sessions are lost (in-memory)
- FullTerminalActivity creates new session

### Case 2: Session handle not found

- Log warning: "Session not found by handle, creating new"
- Fall back to createNewSession()

### Case 3: SSH connection lost

- Terminal stays running but shows disconnect
- User may need to reconnect manually

---

## Success Criteria

1. TermuxService continues running after all activities unbind
2. Terminal sessions persist in mShellManager.mTermuxSessions
3. Session recovery via terminalHandle works correctly
4. Multiple sessions can run in background simultaneously
5. Build succeeds without errors
