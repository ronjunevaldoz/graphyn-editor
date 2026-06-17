# Agent Rules

This file stays thin on purpose.

Core rules:
- Prefer the repo docs over memory when making structural decisions.
- Keep the library core UI-agnostic.
- Keep editor-only concerns out of the core model.
- Add new guidance by reference, not by growing this file into a handbook.

Reference docs:
- [Project Structure](./architecture/README.md)
- [Plan Phases](./plans/README.md)
- [Type Model](./architecture/types.md)
- [Skills Prospect](./skills/README.md)

Enabled KMP Agent Skills:
- [Skill Orchestrator (Start Here)](../.gemini/skills/kotlin-multiplatform-expert/SKILL.md)
- **Layer 0 (Foundation):** [Scaffold](../.gemini/skills/kotlin-multiplatform-feature-scaffold/SKILL.md), [DI](../.gemini/skills/kotlin-multiplatform-dependency-injection/SKILL.md), [Flavors](../.gemini/skills/kotlin-multiplatform-flavor-environment/SKILL.md), [CI](../.gemini/skills/kotlin-multiplatform-ci-github-actions/SKILL.md), [Audit](../.gemini/skills/kotlin-multiplatform-audit/SKILL.md)
- **Layer 1 (Infra):** [Auth](../.gemini/skills/kotlin-multiplatform-ktor-auth-service/SKILL.md), [MongoDB](../.gemini/skills/kotlin-multiplatform-mongodb-database/SKILL.md), [Kotlin RPC](../.gemini/skills/kotlin-multiplatform-kotlin-rpc/SKILL.md), [Network](../.gemini/skills/kotlin-multiplatform-network-layer/SKILL.md), [SQLDelight](../.gemini/skills/kotlin-multiplatform-sqldelight-setup/SKILL.md), [XCFramework/SPM](../.gemini/skills/kotlin-multiplatform-xcframework-spm/SKILL.md)
- **Layer 2 (Patterns):** [Expect/Actual](../.gemini/skills/kotlin-multiplatform-expect-actual/SKILL.md), [Repository](../.gemini/skills/kotlin-multiplatform-repository-pattern/SKILL.md)
- **Layer 3 (Feature):** [Navigation](../.gemini/skills/kotlin-multiplatform-navigation/SKILL.md), [Resources](../.gemini/skills/kotlin-multiplatform-shared-resources/SKILL.md), [MVI](../.gemini/skills/kotlin-multiplatform-mvi/SKILL.md)
- **Layer 4 (UI):** [Design System](../.gemini/skills/kotlin-multiplatform-design-system/SKILL.md), [Extended UI](../.gemini/skills/kotlin-multiplatform-design-system-extended/SKILL.md), [Slot API](../.gemini/skills/kotlin-multiplatform-compose-slot-api/SKILL.md), [Hoisting](../.gemini/skills/kotlin-multiplatform-compose-state-hoisting/SKILL.md), [State Containers](../.gemini/skills/kotlin-multiplatform-compose-state-container/SKILL.md), [Graphics/Modifiers](../.gemini/skills/kotlin-multiplatform-graphics-modifiers/SKILL.md)

Working rule:
- If a decision changes architecture, add or update a referenced doc first, then implement code.
- Consult the **Skill Orchestrator** for any new KMP feature or project structural change.
