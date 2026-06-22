---
name: kotlin-multiplatform-audit
description: >
  KMP project audit skill for reviewing an existing Kotlin Multiplatform codebase.
  Use this skill to inspect architecture, module boundaries, state handling, repository
  and network layering, Compose patterns, expect/actual usage, shared resources,
  design system usage, test coverage, platform readiness, and the skills repo itself.
  Produces findings, risk levels, and a fix sequence instead of implementation code.
  Pair with kotlin-multiplatform-expert to route any follow-up work to the right
  domain skills.
license: Apache-2.0
metadata:
  author: kmm-agent-skills
  last-updated: '2026-06-13'
  keywords:
    - KMP audit
    - project audit
    - architecture review
    - boundary review
    - architecture drift
    - clean architecture audit
    - module audit
    - state audit
    - repository audit
    - Compose audit
    - expect actual audit
    - KMP review
    - project health check
    - readiness review
    - freshness audit
    - deprecation audit
    - script audit
    - skills repo audit
    - issue draft
    - question draft
---

## When to Use This Skill

Use this skill when you need to:
- Review an existing KMP repo for architecture drift or missing boundaries
- Check whether a feature or module is in the right place
- Validate MVI, repository, Compose, and `expect/actual` choices
- Produce a fix order before making code changes
- Compare the project against this collection's recommended KMP patterns
- Audit the skills repo for missing references, examples, scripts, rules, and freshness
- Turn confirmed findings into GitHub issue drafts or question drafts when the user
  wants work items instead of just findings

**Trigger keywords:** audit repo, review architecture, project health, boundary check,
module review, KMP audit, clean architecture review, readiness review, architecture drift,
what is wrong with this project, inspect this repo, audit skills repo, script hygiene,
freshness check, deprecation risk, references audit.

**Freshness rule:** the audit checklist references Compose, MVI, network, and database patterns —
recheck the `kotlin-multiplatform-expert` skill map and this collection's PLAN.md before auditing
against a new version baseline.

---

## Recommendation First

Default to **running the bundled scripts first, then reviewing findings manually against the
checklist in this skill**.

Why:
- `audit_project.py` catches mechanical smells (effect replay, state-copy races, UI/data leaks)
  faster than manual review
- scripts produce evidence-backed findings that are easier to convert to issue drafts
- the manual checklist catches architectural problems the scripts cannot detect

Do not skip the scripts and go straight to manual review — you will miss mechanical issues
that automation finds reliably.

---

## Audit Flow

1. Read the project docs first: `AGENTS.md`, `README.md`, architecture notes, and any
   module-specific guidance.
2. Inspect the module graph and dependency direction.
3. Check data flow boundaries: UI, domain, data, network, database, platform code.
4. Check Compose patterns: MVI, state hoisting, slots, state containers, design system.
5. Check multiplatform choices: `expect/actual`, shared resources, platform targets.
6. Report findings with severity, evidence, and the recommended fix order.

This skill does **not** implement fixes by default. It is the review surface that tells
the user and the other skills what to do next.

---

## What to Inspect

### 1) Module boundaries
- UI must not import `:data`
- Domain must not know about DTOs or SQLDelight entities
- Repository interfaces should live in `:api`, implementations in `:data`
- Shared UI primitives should live in the design system, not feature modules

### 2) State and MVI
- Screen state should be immutable
- One-shot effects should not be replayed
- Prefer `Screen` / `Content` split for testability
- Check for the wrong state container in ephemeral UI state

### 3) Data layer
- DTOs and entities stay inside `:data`
- NetworkResult should not leak into UI state
- Repositories should own mapping and fetch strategy
- Offline support should be explicit, not accidental

### 4) Multiplatform code
- Prefer shared code in `commonMain`
- Use `expect/actual` only when platform behavior is genuinely different
- Check platform target coverage against the product goal

### 5) Design system
- Verify tokens, palette rules, and typography are consistent
- Check whether components use the right pattern for the repo's chosen UI system
- Flag hardcoded colors, sizes, and text styles

### 6) Skills repo hygiene
- Ensure every skill has `name`, `description`, and `metadata.last-updated`
- Ensure trigger guidance is explicit enough to fire in practice
- Prefer references for fast-moving topics and keep examples only when they clarify
- Check that scripts are executable, deterministic, and covered by tests when practical
- Flag skills that depend on fast-moving libraries without a freshness note or docs link
- Flag scripts that encode assumptions about deprecated or unstable APIs
- Ensure new-project scaffold guidance names the `Kotlin/kmp-wizard` `all-targets`
  branch when the goal is Android, iOS, Web, Desktop, and Server
- Ensure KMM projects route plugin and dependency versions through `build-logic/`
  convention plugins and `gradle/libs.versions.toml` instead of scattering versions
  across module build files

---

## Output Format

When auditing, return:
- `Findings` first, ordered by severity
- `Evidence` for each finding, with file paths when available
- `Recommended fix order`
- `Skills to use next`
- `Optional issue drafts` when the user wants findings turned into GitHub-ready work items

Keep implementation advice short and actionable. If a finding maps cleanly to an existing skill,
name that skill so the follow-up path is obvious.

## From Finding to Issue

If the user wants repo work items, convert each confirmed finding into one of two things:
- a **GitHub issue draft** when the problem is actionable and should be tracked
- a **question draft** when the finding needs product or architecture confirmation first

Ask before creating any issue draft. Do not auto-file issues from an audit without
explicit confirmation from the user.

Every draft should include:
- a title following the format `[category] short problem description` — see categories below
- the evidence that triggered it (file path, line, or script output)
- the recommended fix or follow-up skill
- an attribution footer such as `Suggested by kotlin-multiplatform-audit`

### Issue Title Format

Use `[category] short problem description`. Keep titles under 72 characters.
The description names the symptom, not the fix.

| Category | Use for |
|---|---|
| `[arch]` | Layer boundary violations, wrong module placement |
| `[mvi]` | Effect replay, state copy race, wrong state container |
| `[presenter]` | ViewModel has Compose import, wrong scope, missing test |
| `[data]` | Pass-through repository, DTO escaping layer, no cache |
| `[ui]` | Stateless composable violates, missing Preview, design drift |
| `[di]` | Koin module scope wrong, missing factory/viewModel registration |
| `[build]` | Convention plugin misconfiguration, version drift |
| `[test]` | Missing test coverage, mock instead of fake, wrong scope |

**Examples:**
```
[arch] DTO from :data escapes to :feature:todo:ui
[mvi] Effect replayed on recomposition in TodoListScreen
[presenter] ViewModel imports Compose in :feature:todo:presenter
[data] Repository is pass-through — no local cache
[ui] AddTodoContent missing Preview for error state
[di] TodoListViewModel registered as factory instead of viewModel
```

## Common Anti-Patterns

- reporting findings before reading `AGENTS.md` and `README.md` — misses project-specific constraints
- producing implementation code during an audit instead of findings + fix order — audit and implement are separate steps
- auto-filing issues without user confirmation — always ask before creating GitHub issue drafts
- mapping every finding to the same skill — route each finding to the most specific applicable skill
- flagging style preferences as architecture violations — only flag boundary or correctness problems

An audit should produce findings that are actionable. If a finding doesn't map to a specific skill or fix, reclassify it as a question draft.

---

## Bundled Script

- `scripts/audit_project.py` — runs a lightweight scan for a few common KMP architecture
  smells such as effect replay bugs, state copy races, and obvious UI/data boundary leaks.
- `scripts/audit_skills_repo.py` — checks the skills repo for metadata, freshness, scripts,
  and documentation gaps.
- `scripts/draft_issue.py` — renders a GitHub-ready issue or question draft with an
  attribution footer.

---

## Related Skills

- `kotlin-multiplatform-expert` — use before running the audit; the expert skill identifies which domain skills apply and what build order to follow
- `kotlin-multiplatform-clean-architecture` — defines the 6-layer boundary rules the audit script enforces
- `kotlin-multiplatform-mvi` — most `state copy race` and `sharedflow replay effect` findings require this skill to fix correctly
- `kotlin-multiplatform-roborazzi` — replacement for `manual screen capture` findings
- `kotlin-multiplatform-design-system` — replacement for `magic color literal` and `hardcoded spacing` findings

---

## Output Style

When asked to audit a project or the skills repo, respond in this order:
1. run the bundled scripts and report any automated findings
2. work through the manual checklist sections (module boundaries, state, data layer, etc.)
3. findings ordered by severity (critical → high → medium → low)
4. evidence for each finding (file paths, grep output, or line references)
5. recommended fix order
6. skills to use next

Ask before converting findings to issue drafts. Keep implementation advice minimal — this skill routes work, it doesn't implement it.

---

## Changelog

| Date | Change |
|---|---|
| 2026-06-21 | GitHub issue title format defined: `[category] short description`. Category table added with 8 categories (`[arch]`, `[mvi]`, `[presenter]`, `[data]`, `[ui]`, `[di]`, `[build]`, `[test]`). |
| 2026-06-18 | Initial release — architecture audit checklist, `audit_project.py`, `audit_skills_repo.py`, `draft_issue.py`. |
