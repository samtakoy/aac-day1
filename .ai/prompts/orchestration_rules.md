# Multi-Agent Orchestration Rules

## Context Management
1. Each agent reads ALL previous agent outputs
2. Reference specific points: "Building on @senior_architect.md..."
3. If contradicting → explain why

## Conflict Resolution
1. Quality Reviewer has veto on overengineering
2. AI System Architect has veto on AI feasibility
3. Senior Architect has final say on domain boundaries

## Output Standardization
All agents must include:
- [ ] Confidence level (High/Medium/Low)
- [ ] Assumptions made
- [ ] Open questions

## Token Optimization
- Be concise, not verbose
- Use tables for comparisons
- Skip obvious explanations

## Exit Criteria
- ✅ 0 High Severity issues
- ✅ All roles: "Status: Approved"
- ✅ Max 3 iterations (then document trade-offs)	