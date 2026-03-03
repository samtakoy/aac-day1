# Project Memory: aac-day1_kimi

## Project Type
Android AI Chat App — Clean Architecture, Dagger (NOT Hilt), Room, Kotlin Coroutines + Flow, Jetpack Compose.

## Key Features
- PLANNER chat group type with 3-layer memory architecture (STM/WM/LTM)
- Pattern-based pseudo tool-calling: SAVE_FACT, CREATE_STAGE, COMPLETE_STAGE
- Human-in-the-loop stage creation via StageCreationDialog

## Architecture Rules
- ViewModels use UseCases, not Repositories directly
- ConsoleFeatureDeps → ConsoleFeatureComponent → ConsoleViewModelImpl factories
- Three factories: Factory (LLM), AgentFactory (Agents), PlannerFactory (Planner)

## Day 11 Fixes Applied (2026-03-03)
See: plans/day11_fix_plan.md

**Bugs fixed:**
1. `GetLongTermMemoryByGroupUseCase` created — wraps `LongTermMemoryRepository.getFactsByGroupFlow()`
2. `ConsoleViewModelImpl` — LTM now reactively loaded via `flatMapLatest` in PlannerFactory
3. `refreshMemoryInspector()` called when messages/chat/LTM change (was never called before)
4. Memory Inspector `isExpanded` toggle fixed — was no-op, now fires `ToggleMemoryInspector` event
5. `OpenMemoryInspector` event triggers data refresh + expands inspector
6. Duplicate `confirmStageCreation()` / `declineStageCreation()` methods removed
7. Stage-чаты в ChatsScreen — добавлен `isStageChat` флаг, визуальная стилизация чипов (↳ prefix)

## Key Files
- `features/console/impl/ui/viewmodel/ConsoleViewModelImpl.kt` — main ViewModel
- `features/console/impl/ui/ConsoleScreen.kt` — chat UI screen
- `core/core_features/agent/domain/workers/PlannerWorker.kt` — orchestrates memory + LLM
- `core/core_features/chat/domain/usecase/GetLongTermMemoryByGroupUseCase.kt` — NEW
- `plans/day11_fix_plan.md` — detailed fix plan with all issues documented

## What's Still TODO
- Artifacts viewer UI (ArtifactsBottomSheet)
- Memory Inspector scrolling for large STM lists
