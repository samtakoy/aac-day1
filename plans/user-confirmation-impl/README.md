# User Confirmation Implementation Plan (Variant B)

## Overview
This directory contains a staged, sequential implementation plan for user confirmation of dangerous tools, based on the session-driven architecture (Variant B).

## Locked Decisions
1. `runId` generation is provided to `AIAgent` via lambda/provider dependency.
2. Resume is an explicit contract (not implicit restart of `execute`).
3. One unified confirmation event type only.
4. Rollout scope is all worker flows that use tool-calling.
5. Concurrency policy is minimal and strict (idempotent decision handling, stale decision rejection, one pending confirmation per `runId`).
6. `ExecutionSessionRepository` is in-memory for now; code should include TODO comments for Room migration.

## Stages
1. `stage-1-session-contracts.md`
2. `stage-2-confirmation-toolcall-manager.md`
3. `stage-3-session-manager-orchestrator.md`
4. `stage-4-agent-worker-ui.md`
5. `stage-5-di-tests.md`

## Flow
Stage 1 > Stage 2 > Stage 3 > Stage 4 > Stage 5

Each stage is self-contained and should be completed in order.

## Progress Checklist
- [x] Stage 1: Session and Result Contracts
- [x] Stage 2: Confirmation Policy and ToolCallManager
- [x] Stage 3: Session Manager and Orchestrator Refactor
- [x] Stage 4: Agent + Worker + UI Resume Flow
- [ ] Stage 5: DI Wiring, Clean-up, and Tests

