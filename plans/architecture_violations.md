# Architecture Violations Documentation

## Rule: UI Layer Must Not Access Repositories Directly

**Violation**: ViewModels must NOT inject or use Repository interfaces directly.

### Reason
This violates Clean Architecture's dependency rule:
- **UI Layer** (ViewModels) → depends on → **Domain Layer** (UseCases, Domain Models)
- **Domain Layer** → depends on → **Data Layer** (Repositories, Entities)

### Correct Approach
Use UseCases to encapsulate business logic:

```kotlin
// ❌ WRONG - ViewModel injecting Repository
class GroupChoiceViewModelImpl(
    private val chatRepository: ChatRepository  // FORBIDDEN!
)

// ✅ CORRECT - ViewModel using UseCase
class GroupChoiceViewModelImpl(
    private val createChatGroupUseCase: CreateChatGroupUseCase,
    private val createChatUseCase: CreateChatUseCase
)
```

### How to Fix This Violation

1. **Create new UseCase** in `core_features/chat/domain/usecase/`
2. **Add UseCase to Feature Module** DI
3. **Inject UseCase in ViewModel** instead of Repository

## History of Violations

### 2026-03-02: GroupChoiceViewModelImpl
- **Issue**: Tried to inject `ChatRepository` directly into ViewModel
- **Reason**: Needed to create PLANNER group with main chat in single transaction
- **Fix**: Create `CreatePlannerGroupWithMainChatUseCase` instead
