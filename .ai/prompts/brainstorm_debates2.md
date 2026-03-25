# Multi-Agent Debate Protocol

## Team
- AI Systems Architect from @ai_system_architect.md
- Senior Architect from @senior_architect.md
- Kotlin Senior Developer from @kotlin_senior.md
- Quality Reviewer from @quality_reviewer.md

## Context
- Project: {{PROJECT_CONTEXT}}
- Task: {{TASK}}

## Process

### Phase 1 — Proposals (separate messages)
1. **AI Systems Architect** generates system design (agents, LLM, RAG, MCP).
2. **Senior Architect** generates architecture options (modules, boundaries, patterns).
3. Both outputs are placed in code fences with their role name as header.

### Phase 2 — Implementation (separate message)
- **Kotlin Developer** translates proposals into code structure, packages, interfaces.
- Output is placed in code fences.

### Phase 3 — Debate (separate message)
**IMPORTANT:** Each agent reviews the outputs from previous phases and writes critique.

Agents challenge each other:
- **Senior Architect** ↔ **AI Systems Architect**: Critique system design vs domain boundaries.
- **Kotlin Developer**: Flag impractical solutions.
- **Quality Reviewer**: Aggressively identify overengineering, unnecessary agents, useless abstractions.

**Debate Rules:**
- Be critical.
- Disagree if needed.
- Prefer simpler solutions.
- Justify every complexity.
- Each agent writes their critique in a separate bullet point, labeled with their role.

### Phase 4 — Refinement (separate message)
- Improve solution based on debate outcomes.
- Output final combined solution.

### Phase 5 — Final Review (separate message)
- **Quality Reviewer** gives final verdict (High Severity issues count).

### Phase 6 — Final Answer
- Consolidated solution with all approvals.

## Output Format
Follow exactly:

## 🤖 AI System Design
[design]

## 🏗 Architecture
[architecture]

## 💻 Implementation
[code structure]

## ⚔️ Debate
[critique]

## 🔍 Review
[verdict]

## ✅ Final Decision
[final]
