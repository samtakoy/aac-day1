# Day 11 - PLANNER Memory Model Fixes

## Problem Analysis

### Issue 1: LTM Shared Across Groups (Critical)
**Root Cause**: `LongTermMemoryEntity` has NO `groupId` field - it's completely global.

When user creates a new PLANNER group, they see facts from ALL other groups because:
- `LongTermMemoryRepositoryImpl.getAllFacts()` returns ALL facts without filtering
- `Chat` has `chatGroupId` but `LongTermMemoryEntity` doesn't

### Issue 2: Pattern Visibility (#SAFE_FACT)
**Analysis**: Code uses `SAVE_FACT[key:category:fact]` pattern (not `#SAFE_FACT`).

The `ToolResponseParser.cleanedResponse` SHOULD remove patterns, but:
- Could fail if LLM responds incorrectly
- Need to verify cleaning is working

### Issue 3: No Memory Inspector UI
No UI component to view stored facts (LTM).

---

## Implementation Plan

### Phase 1: Fix LTM Group Isolation

#### 1.1 Add groupId to LongTermMemoryEntity
```kotlin
// File: app/src/main/java/com/example/day/core/core_features/chat/data/local/model/LongTermMemoryEntity.kt
@Entity(tableName = "long_term_memory")
data class LongTermMemoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "memory_key")
    val memoryKey: String,
    
    @ColumnInfo(name = "group_id")  // NEW
    val groupId: Long,               // NEW
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "fact")
    val fact: String,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
```

#### 1.2 Update LongTermMemoryDao
- Add queries with groupId filter
- Update UPSERT to include groupId

#### 1.3 Update LongTermMemory domain model
```kotlin
// File: app/src/main/java/com/example/day/core/core_features/chat/domain/model/LongTermMemory.kt
data class LongTermMemory(
    val memoryKey: String,
    val groupId: Long,  // NEW
    val category: String,
    val fact: String,
    val updatedAt: Long
)
```

#### 1.4 Update LongTermMemoryRepository
- Add groupId parameter to methods
- `suspend fun upsertFact(groupId: Long, memoryKey: String, category: String, fact: String)`
- `suspend fun getFactsByGroup(groupId: Long): List<LongTermMemory>`

#### 1.5 Update LongTermMemoryRepositoryImpl
- Implement groupId filtering

#### 1.6 Update PlannerWorker
- Pass `chat.chatGroup.id` when saving/loading LTM

---

### Phase 2: Fix Pattern Visibility

#### 2.1 Verify ToolResponseParser cleaning
- Check that `cleanedResponse` is being used correctly
- Add fallback regex cleaning if needed

#### 2.2 Add extra safety in PlannerWorker
- Double-clean response before saving to DB

---

### Phase 3: Create Memory Inspector UI

#### 3.1 Create MemoryInspectorViewModel
- Load LTM facts for current group
- Load Working Memory (chat.workingSummary)
- Show Short-term (recent messages)

#### 3.2 Create MemoryInspectorScreen (Compose)
- Three sections: LTM, Working, Short-term
- Display as list of facts

#### 3.3 Add button to ConsoleScreen
- Menu or FAB to open Memory Inspector

---

### Phase 4: PLANNER Group Creation

#### 4.1 Update GroupChoiceViewModel
- Add PLANNER type to group creation options
- Create ChatGroup with type = PLANNER

#### 4.2 Create initial main chat
- When PLANNER group created, create main chat
- Set `isPlannerMain = true`

---

### Phase 5: Stage Creation Flow

#### 5.1 Handle StageCreationSuggested event
- In PlannerTalkDelegate, emit UI event

#### 5.2 Update ConsoleViewModel
- Listen for StageCreationSuggested
- Show confirmation dialog

#### 5.3 Implement stage creation
- Call ChatRepository.createSubChat()
- Navigate to new stage chat

---

## Files to Modify

| File | Change |
|------|--------|
| `LongTermMemoryEntity.kt` | Add groupId field |
| `LongTermMemory.kt` | Add groupId field |
| `LongTermMemoryDao.kt` | Add groupId queries |
| `LongTermMemoryRepository.kt` | Add groupId params |
| `LongTermMemoryRepositoryImpl.kt` | Implement groupId filtering |
| `PlannerWorker.kt` | Pass groupId to repository |
| `PlannerTalkDelegate.kt` | Pass groupId from chat |
| `ChatMapper.kt` | Map groupId |
| `ChatEntity.kt` | Already has chatGroupId |
| GroupChoiceViewModel | Add PLANNER creation |
| ConsoleScreen | Add Memory Inspector button |
| New: MemoryInspectorScreen | Show memory contents |

---

## Architecture Decision: Global vs Group-Specific LTM

Currently planning: **Group-Specific LTM** (each PLANNER group has its own memory)

**Alternative**: Could make it hybrid:
- Global memory (user profile: name, skills)
- Group-specific memory (project details)

For now, implement group-specific to fix the immediate issue.
