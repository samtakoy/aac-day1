# Plan: Migrate from ToolProvider to ToolRegistry (Clean Solution)

## Current State Analysis

### Backward Compatibility Crutches Added

1. **`ToolProvider.kt`** (typealias file)
   - Contains: `typealias ToolProvider = ToolRegistry`
   - Purpose: Allows old code using `ToolProvider` to still compile
   - **Problem**: This file should be deleted after migration

2. **`McpToolProvider.kt`** (class name)
   - Contains: `class McpToolProvider : ToolRegistry`
   - **Problem**: Class name still contains "Provider", should be `McpToolRegistry`

3. **Comments mentioning "ToolProvider"**
   - `ToolCallOrchestrator.kt` line 14: comment references `ToolProvider`
   - `ToolCallContext.kt` line 12: comment references `ToolProvider.getTools()`

---

## Migration Tasks

### Task 1: Rename McpToolProvider → McpToolRegistry

**Problem**: Class name still uses old naming convention.

**Changes:**
```kotlin
// Before
class McpToolProvider @Inject constructor(...)

// After
class McpToolRegistry @Inject constructor(...)
```

**Files to modify:**
- `McpToolProvider.kt` → rename file to `McpToolRegistry.kt`
- Update class declaration inside file

---

### Task 2: Update DI Module References

**Problem**: AgentCoreFeatureModule binds the old class name.

**Changes:**
```kotlin
// Before
import ...McpToolProvider
@Binds
fun bindsToolRegistry(impl: McpToolProvider): ToolRegistry

// After
import ...McpToolRegistry
@Binds
fun bindsToolRegistry(impl: McpToolRegistry): ToolRegistry
```

**Files to modify:**
- `AgentCoreFeatureModule.kt` — update import and bind statement

---

### Task 3: Update Comments Referencing ToolProvider

**Problem**: Documentation comments still reference old interface name.

**Changes:**
```kotlin
// ToolCallOrchestrator.kt line 14
// Before:
// - Выполнение инструментов через ToolProvider

// After:
// - Выполнение инструментов через ToolRegistry
```

```kotlin
// ToolCallContext.kt line 12
// Before:
// This is populated by ToolProvider.getTools() and used to route

// After:
// This is populated by ToolRegistry.getTools() and used to route
```

**Files to modify:**
- `ToolCallOrchestrator.kt` — update comment
- `ToolCallContext.kt` — update comment

---

### Task 4: Delete ToolProvider.kt

**Problem**: This file only exists for backward compatibility.

**Action:**
```bash
# Delete the file
rm app/src/main/java/com/example/day/core/core_features/agent/domain/tools/ToolProvider.kt
```

**Note:** This can only be done after all usages are migrated.

---

## Verification Steps

After migration, verify:

1. **No imports of ToolProvider** (except in deprecated locations if any)
   ```bash
   grep -r "import.*ToolProvider" app/src/main/java
   # Should return no results
   ```

2. **No references to `McpToolProvider`** (only `McpToolRegistry`)
   ```bash
   grep -r "McpToolProvider" app/src/main/java
   # Should return no results
   ```

3. **Build succeeds**
   ```bash
   ./gradlew assembleDebug
   # Should compile without errors
   ```

---

## Execution Order

1. **Task 3** (comments) — Safe, no code changes
2. **Task 1** (rename class) — Requires file rename
3. **Task 2** (update DI) — After class is renamed
4. **Task 4** (delete typealias) — After all usages migrated

---

## Files to Modify/Delete

| Task | File | Change Type |
|------|------|-------------|
| 1 | `McpToolProvider.kt` → `McpToolRegistry.kt` | Rename |
| 1 | `McpToolRegistry.kt` | Update class name |
| 2 | `AgentCoreFeatureModule.kt` | Update import and bind |
| 3 | `ToolCallOrchestrator.kt` | Update comment |
| 3 | `ToolCallContext.kt` | Update comment |
| 4 | `ToolProvider.kt` | **DELETE** |

---

## Expected Result

After migration:

```
✅ No "Provider" naming in tool-related classes
✅ No typealias files for backward compatibility
✅ Clean code with consistent Koog-aligned naming
✅ All comments updated to reference new names
```

### Final Architecture

```
ToolRegistry (interface) ← defined directly
     ↑
     │
McpToolRegistry (implementation) ← renamed from McpToolProvider

ToolProvider.kt (typealias) ← DELETED
```

---

## Motivation

1. **Consistency with Koog**: The naming should match JetBrains Koog framework
2. **Clean Code**: No unnecessary typealias files
3. **Maintainability**: Future developers won't wonder "why is there a typealias?"
4. **Documentation**: Comments accurately reflect actual code structure
