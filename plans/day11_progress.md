# Day 11 Implementation Progress

Started: 2026-03-02

## Phase 1: Database Foundation ✅ COMPLETED

### Step 1.1: Add PLANNER ChatType ✅
- [x] Modified `core/core_features/chat/domain/model/ChatType.kt`
- [x] Added PLANNER("planner", "Project Planner") enum value

### Step 1.2: Extend ChatEntity ✅
- [x] Added parent_id column (nullable, foreign key to self)
- [x] Added working_summary column (nullable)
- [x] Added is_planner_main column (Boolean, default false)
- [x] Modified `core/core_features/chat/data/local/model/ChatEntity.kt`
- [x] Added foreign key constraint for parent_id
- [x] Added index on parent_id

### Step 1.3: Create LongTermMemoryEntity ✅
- [x] Created `core/core_features/chat/data/local/model/LongTermMemoryEntity.kt`
- [x] Fields: memoryKey (PK), category, fact, updated_at
- [x] Uses memoryKey for UPSERT operations

### Step 1.4: Create ProjectArtifactEntity ✅
- [x] Created `core/core_features/chat/data/local/model/ProjectArtifactEntity.kt`
- [x] Fields: id (auto), chat_id (FK), stage_title, content, created_at
- [x] Foreign key constraint on chat_id

### Step 1.5: Update ChatDatabase ✅
- [x] Added LongTermMemoryEntity and ProjectArtifactEntity to @Database
- [x] Incremented version to 5
- [x] Added longTermMemoryDao() accessor
- [x] Added artifactDao() accessor
- [x] Modified `core/core_features/chat/data/local/ChatDatabase.kt`

### Step 1.6: Create DAOs ✅
- [x] Created `core/core_features/chat/data/local/dao/LongTermMemoryDao.kt`
  - upsert() with REPLACE strategy
  - getAll() and getAllOnce()
  - getByKey(), deleteByKey(), clearAll()
- [x] Created `core/core_features/chat/data/local/dao/ArtifactDao.kt`
  - insert()
  - getByChatId(), getByParentChatId()
  - deleteByChatId()

### Step 1.7: Extend ChatDao ✅
- [x] Added getSubChats(parentId): Flow
- [x] Added getSubChatsOnce(parentId): List
- [x] Added updateWorkingSummary(chatId, summary)
- [x] Added updateIsPlannerMain(chatId, isMain)
- [x] Added getMainPlannerChat(groupId)

---

## Phase 2: Domain Layer ✅ COMPLETED

### Step 2.1: Create Domain Models ✅
- [x] Created `core/core_features/chat/domain/model/LongTermMemory.kt`
  - data class with memoryKey, category, fact, updatedAt
- [x] Created `core/core_features/chat/domain/model/Artifact.kt`
  - data class with id, chatId, stageTitle, content, createdAt

### Step 2.2: Create LongTermMemoryRepository ✅
- [x] Created interface `core/core_features/chat/domain/repository/LongTermMemoryRepository.kt`
  - upsertFact(), getAllFacts(), getAllFactsFlow(), getFactByKey(), deleteFact(), clearAllFacts()
- [x] Created implementation `core/core_features/chat/data/repository/LongTermMemoryRepositoryImpl.kt`
  - Uses LongTermMemoryDao with entity-to-domain mapping

### Step 2.3: Create ArtifactRepository ✅
- [x] Created interface `core/core_features/chat/domain/repository/ArtifactRepository.kt`
  - saveArtifact(), getArtifactsForChat(), getArtifactsForParent(), deleteArtifactsForChat()
- [x] Created implementation `core/core_features/chat/data/repository/ArtifactRepositoryImpl.kt`
  - Uses ArtifactDao with entity-to-domain mapping

### Step 2.4: Extend ChatRepository ✅
- [x] Added to `core/core_features/chat/domain/ChatRepository.kt`:
  - createSubChat(), getSubChats(), updateWorkingSummary(), markAsPlannerMain(), getMainPlannerChat()
- [x] Implemented in `core/core_features/chat/data/ChatRepositoryImpl.kt`
  - All methods with proper mutex locking and settings creation

---

## Phase 3: PlannerWorker ✅ COMPLETED

### Step 3.1: Create PromptBuilder ✅
- [x] Created `core/core_features/agent/domain/prompt/PlannerPromptBuilder.kt`
  - buildMainPlannerPrompt(ltmFacts, projectContext): Comprehensive system prompt for main planner
  - buildStagePrompt(stageTitle, parentSummary, ltmFacts): System prompt for stage chats
  - injectLTMIntoPrompt(): Helper for LTM injection with placeholder support

### Step 3.2: Create Tool Response Parser ✅
- [x] Created `core/core_features/agent/domain/tools/ToolResponseParser.kt`
  - SAVE_FACT[key:category:fact] pattern
  - CREATE_STAGE[title:context] pattern
  - COMPLETE_STAGE[outcome] pattern
  - ParsedResponse data class with cleaned response and command lists
  - hasToolPatterns() helper for quick checking

### Step 3.3: Create PlannerWorker ✅
- [x] Created `core/core_features/agent/domain/workers/PlannerWorker.kt`
  - Implements AWorker interface
  - Injects: AIAgentFactory, LTM repo, Chat repo, Artifact repo, ChatTools
  - Full workflow:
    1. Retrieve LTM facts from repository
    2. Detect chat type (main vs stage) via parentId check
    3. Get working memory (parent context for stage chats)
    4. Build system prompt with PlannerPromptBuilder
    5. Get conversation history
    6. Create AIAgent with planner config
    7. Call LLM and handle result
    8. Parse response with ToolResponseParser
    9. Auto-save facts via SAVE_FACT commands
    10. Emit StageCreationSuggested for CREATE_STAGE (human-in-the-loop)
    11. Save artifacts and update parent for COMPLETE_STAGE
    12. Return cleaned response

### Step 3.4: Extend WorkerEvent ✅
- [x] Added to `core/core_features/agent/domain/workers/base/WorkerEvent.kt`:
  - `StageCreationSuggested(stageTitle, workingSummary)` - For UI to show confirmation
  - `StageCompleted(chatId, artifactContent)` - When stage is marked done
  - `FactSaved(memoryKey, category, fact)` - When fact is saved to LTM

---

## Phase 4: DI Module Updates ✅ COMPLETED

### Step 4.1: Update ChatCoreFeatureModule ✅
- [x] Added bindings for LongTermMemoryRepository and ArtifactRepository
- [x] Added DAO providers for LongTermMemoryDao and ArtifactDao
- [x] Updated imports for all new classes

### Step 4.2: Update AgentCoreFeatureModule ✅
- [x] Added providePlannerWorker() with all dependencies
- [x] Updated imports for AIAgentFactory, repositories, and PlannerWorker

---

## Summary: Core Infrastructure Complete ✅

### What Was Implemented

**Database Layer (Phase 1):**
- `ChatType.PLANNER` - New chat group type
- `ChatEntity` extended with parent_id, working_summary, is_planner_main
- `LongTermMemoryEntity` - Persistent user facts with UPSERT support
- `ProjectArtifactEntity` - Stage completion artifacts
- `ChatDatabase` version 5 with all new entities and DAOs

**Domain Layer (Phase 2):**
- Domain models: `LongTermMemory`, `Artifact`
- Repository interfaces and implementations for LTM and Artifacts
- Extended `ChatRepository` with sub-chat methods

**Worker Layer (Phase 3):**
- `ToolResponseParser` - Pattern-based tool calling (SAVE_FACT, CREATE_STAGE, COMPLETE_STAGE)
- `PlannerPromptBuilder` - System prompts with memory injection
- `PlannerWorker` - Full implementation with:
  - Three-layer memory architecture
  - LTM retrieval and injection into prompts
  - Parent context for stage chats
  - Auto-save facts to LTM
  - Human-in-the-loop stage creation (emits event, doesn't auto-create)
  - Artifact saving for completed stages
- Extended `WorkerEvent` with StageCreationSuggested, StageCompleted, FactSaved

**DI Layer (Phase 4):**
- `ChatCoreFeatureModule` updated with new repositories and DAOs
- `AgentCoreFeatureModule` updated with PlannerWorker provider

### Next Steps (UI & Integration)

**Remaining Work:**
1. Create UI components:
   - `StageCreationSuggestion` - Card with confirm/dismiss buttons
   - `ArtifactsBottomSheet` - View completed stage results
   
2. Update screens:
   - `ConsoleScreen` - Show suggestion buttons and "View Artifacts" button
   - `ChatsScreen` - Show stage indicators (child chats)
   - `GroupChoiceScreen` - Add PLANNER group creation option
   
3. Update ViewModels:
   - `ConsoleViewModel` - Use PlannerWorker for PLANNER groups, handle StageCreationSuggested
   - `ChatsViewModel` - Handle stage chat creation on user confirmation

4. Testing:
   - LTM persistence across sessions
   - Stage creation flow with confirmation
   - Context inheritance from parent to stage
   - Artifact collection
   - Regression testing for regular groups

### Key Design Decisions

1. **Human-in-the-loop**: Stage creation requires explicit user confirmation (CREATE_STAGE emits event, doesn't create chat automatically)

2. **Pattern-based tool calling**: Uses regex patterns instead of native LLM function calling for broader compatibility

3. **Three-layer memory**:
   - Short-term: Messages table (conversation history)
   - Working: ChatEntity.working_summary (project context, inherited from parent)
   - Long-term: LongTermMemoryEntity (persistent user profile)

4. **UPSERT for LTM**: Memory uses memoryKey for update-or-insert, preventing duplicates

**Status: Ready for UI/Integration phase** 🚀

---

## Phase 3: PlannerWorker

### Step 3.1: Create PlannerAgentConfig
- [ ] Create `core/core_features/agent/domain/model/PlannerAgentConfig.kt`
  - Main planner system prompt template
  - Stage chat system prompt template
  - Tool usage instructions

### Step 3.2: Create PromptBuilder
- [ ] Create `core/core_features/agent/domain/prompt/PlannerPromptBuilder.kt`
  - buildMainPlannerPrompt(ltmFacts, projectContext): String
  - buildStagePrompt(stageTitle, parentSummary, ltmFacts): String
  - injectLTMIntoPrompt(basePrompt: String, facts: List<LongTermMemory>): String

### Step 3.3: Create Tool Response Parser
- [ ] Create `core/core_features/agent/domain/tools/ToolResponseParser.kt`
  - Pattern matching for SAVE_FACT[key:category:fact]
  - Pattern matching for CREATE_STAGE[title:context]
  - Pattern matching for COMPLETE_STAGE[outcome]
  - cleanResponse(text): String (removes tool markers)

### Step 3.4: Create PlannerWorker
- [ ] Create `core/core_features/agent/domain/workers/PlannerWorker.kt`
  - Implements AWorker
  - Constructor inject: AIAgentFactory, LTM repo, Chat repo, Artifact repo, ChatTools
  - doWork() implementation:
    1. Detect chat type (main planner vs stage)
    2. Retrieve LTM facts
    3. For stage: retrieve parent context
    4. Build system prompt with memory injection
    5. Call LLM via AIAgent
    6. Parse response for tool patterns
    7. Handle SAVE_FACT: upsert to LTM
    8. Handle CREATE_STAGE: emit StageCreationSuggested event (NOT auto-create)
    9. Handle COMPLETE_STAGE: save artifact, update parent
    10. Return result with cleaned response

### Step 3.5: Extend WorkerEvent
- [ ] Add `StageCreationSuggested(val stageTitle: String, val workingSummary: String)` to WorkerEvent
- [ ] Add `StageCompleted(val chatId: Long, val artifact: String)` to WorkerEvent
- [ ] Add `FactSaved(val memoryKey: String)` to WorkerEvent

---

## Phase 4: UI Layer

### Step 4.1: Create StageCreationSuggestion Component
- [ ] Create `features/console/impl/ui/components/StageCreationSuggestion.kt`
  - Shows card with stage title and summary
  - "Create Stage" button (confirms creation)
  - "Dismiss" button (ignores suggestion)

### Step 4.2: Create ArtifactsBottomSheet
- [ ] Create `features/console/impl/ui/components/ArtifactsBottomSheet.kt`
  - Displays list of stage artifacts
  - Shows stage title and content preview
  - Expandable to view full content

### Step 4.3: Update ConsoleScreen
- [ ] Add support for displaying `StageCreationSuggestion`
- [ ] Handle suggestion confirmation (call ViewModel to create chat)
- [ ] Add "View Artifacts" button for planner main chat
- [ ] Show stage indicator in message list (if in stage chat)

### Step 4.4: Update ChatsScreen
- [ ] Add visual indicator for stage chats (child chats)
- [ ] Show parent chat reference in stage chat items
- [ ] Different styling for planner main vs stage chats

### Step 4.5: Update GroupChoiceScreen
- [ ] Add option to create PLANNER type group
- [ ] Visual distinction (different color/icon) for planner groups

---

## Phase 5: Integration

### Step 5.1: Update DI Modules
- [ ] Update `core/core_features/chat/di/ChatCoreFeatureModule.kt`
  - Provide LongTermMemoryRepository
  - Provide ArtifactRepository
- [ ] Update `core/core_features/agent/di/AgentCoreFeatureModule.kt`
  - Provide PlannerWorker
  - Provide PlannerPromptBuilder

### Step 5.2: Update ConsoleViewModel
- [ ] Detect PLANNER group type
- [ ] Use PlannerWorker for PLANNER groups (instead of TalkWorker)
- [ ] Handle `StageCreationSuggested` event - show UI suggestion
- [ ] Handle suggestion confirmation - call `createSubChat`
- [ ] Handle "View Artifacts" action - load and display artifacts

### Step 5.3: Update MainActivity Navigation
- [ ] No changes needed if reusing ConsoleScreen
- [ ] Or add dedicated Planner route if separate screen preferred

### Step 5.4: Update ChatsFeature Routing
- [ ] In `features/chats/impl/ChatsFeatureEntryImpl.kt`
  - Check group type when creating new chat
  - If PLANNER group and no main chat exists, create as main planner
  - Otherwise create as regular chat

---

## Phase 6: Testing

### Step 6.1: Test LTM Persistence
- [ ] Verify facts are saved with UPSERT
- [ ] Verify facts persist across app restarts
- [ ] Verify updatedAt changes on update

### Step 6.2: Test Stage Creation Flow
- [ ] Verify LLM suggests stage creation via pattern
- [ ] Verify UI shows suggestion with confirm/dismiss
- [ ] Verify chat created only after confirmation
- [ ] Verify parent_id is set correctly

### Step 6.3: Test Context Inheritance
- [ ] Verify stage chat receives parent working_summary
- [ ] Verify stage chat has access to LTM
- [ ] Verify main planner sees stage artifacts

### Step 6.4: Test Artifact Collection
- [ ] Verify COMPLETE_STAGE saves artifact
- [ ] Verify artifacts appear in "View Artifacts" list
- [ ] Verify parent summary is updated on completion

### Step 6.5: Regression Testing
- [ ] Verify SIMPLE_HISTORY groups still work
- [ ] Verify AGENT_COMMANDS groups still work
- [ ] Verify no crashes in existing features

---

**Last completed step:** Phase 1.7 - Extended ChatDao with sub-chat methods

**Next step:** Phase 2.1 - Create domain models for LongTermMemory and Artifact

**Notes:**
- Human-in-the-loop requirement: Stage creation only after explicit user confirmation
- No DB migration needed (destructive migration acceptable for development)
- Pattern-based tool calling instead of native function calling
