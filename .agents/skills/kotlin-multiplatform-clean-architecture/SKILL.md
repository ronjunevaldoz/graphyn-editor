---
name: kotlin-multiplatform-clean-architecture
description: >
  Defines the 6-layer clean architecture contract for KMP feature modules:
  :model / :api / :domain / :data / :presenter / :ui. Covers layer dependency
  rules, :model vs :api split, internal visibility enforcement, and Detekt
  architecture fitness functions that make violations fail the build.
license: Apache-2.0
metadata:
  author: kmm-agent-skills
  last-updated: '2026-06-18'
  keywords:
    - clean architecture
    - Kotlin Multiplatform
    - KMP
    - multi-module
    - layer dependency
    - internal visibility
    - Detekt
    - architecture rules
    - model module
    - presenter module
---

## When to Use This Skill

Use when you need to:
- Understand or enforce the 6-layer dependency contract across feature modules
- Decide what belongs in `:model` vs `:api` vs `:domain`
- Enforce `internal` visibility so module internals do not leak across layer boundaries
- Write Detekt architecture rules that fail the build on layer violations
- Review a pull request for architecture compliance

**Trigger keywords:** clean architecture, layer contract, dependency rule, model vs api,
internal visibility, architecture violation, Detekt architecture, layer rule, feature layers,
module boundaries, 6-layer architecture, domain isolation, which layer, domain model,
api contract, dependency inversion, layer ownership, where does this code go.

**Freshness rule:** Detekt rule set API changes between minor versions — recheck the
`ArchitectureRule` DSL when upgrading Detekt.

---

## Recommendation First

Default to **strict unidirectional dependency flow: `:model` → `:api` → `:domain` → `:presenter` → `:ui`** with `:data` as a sibling of `:presenter` (both depend on `:api`, neither depends on the other).

Why:
- `:presenter` with no Compose dependency = ViewModels testable on plain JVM
- `:model` as the dependency root = types shared across all layers with no circular risk
- `:ui` depending only on `:presenter` = Compose screens are pure render functions
- `internal` at module boundaries = no accidental cross-layer coupling

Enforce the contract with Gradle dependency declarations first, Detekt rules second.
The Gradle graph makes illegal dependencies impossible to compile; Detekt catches import-level violations.

---

## Layer Contract

```
:model      pure KMP — data classes, sealed types, enums
              ↑ (no deps)
:api        pure KMP — repository interfaces, nav contracts
              ↑ (depends on :model only)
:domain     pure KMP — use cases, business logic
              ↑ (depends on :api)
:data       KMP + platform — Ktor/SQLDelight repository impls
              (depends on :api, NOT :domain or :presenter)
:presenter  pure KMP — ViewModels, MVI state/intent types
              (depends on :domain, NO Compose)
              ↑
:ui         CMP — Compose screens, previews
              (depends on :presenter ONLY)
```

### What goes where

| Layer | Contains | Does NOT contain |
|---|---|---|
| `:model` | `data class`, `sealed class`, `enum class`, `typealias` | Interfaces, business logic, framework deps |
| `:api` | Repository interfaces, nav route contracts | Implementations, data classes |
| `:domain` | Use cases (`operator fun invoke`), pure business rules | Framework deps, DI annotations |
| `:data` | `RepositoryImpl`, DTOs, mappers, data sources | UI state, ViewModels |
| `:presenter` | `ViewModel`, MVI `UiState`, `UiIntent` sealed classes | Compose imports, UI framework |
| `:ui` | `@Composable` screens, `@Preview` functions | Business logic, direct repo/use-case calls |

---

## Internal Visibility Rules

Every declaration that is not part of the module's public surface should be `internal`.
The public surface of each layer:

| Layer | Public API |
|---|---|
| `:model` | All types — they are shared across every layer |
| `:api` | Repository interfaces, nav contracts |
| `:domain` | Use case classes (consumed by `:presenter`) |
| `:data` | Only the DI module (e.g., `val authDataModule`) — impl classes are `internal` |
| `:presenter` | `ViewModel` class, `UiState`, `UiIntent` sealed types |
| `:ui` | Top-level `@Composable` screen entry point only |

```kotlin
// :feature:auth:data — implementation is internal
internal class AuthRepositoryImpl(
    private val remote: AuthRemoteDataSource,
    private val local: AuthLocalDataSource,
) : AuthRepository { ... }

// :feature:auth:data — only the module is public
val authDataModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
}
```

The Gradle dependency graph enforces layer isolation. `internal` enforces encapsulation
within a layer's public API surface.

---

## Detekt Architecture Rules

Add to `detekt.yml` to fail the build when import-level violations are detected:

```yaml
libraries:
  rules:
    - name: 'NoPresentationInDomain'
      active: true
      includes: ['**/domain/**']
      excludes: []
      forbidden:
        - 'androidx.lifecycle.*'
        - 'androidx.compose.*'
        - '*.presenter.*'
        - '*.ui.*'

    - name: 'NoDataInUi'
      active: true
      includes: ['**/ui/**']
      excludes: []
      forbidden:
        - '*.data.*'
        - '*.domain.*'
        - 'io.ktor.*'
        - 'app.cash.sqldelight.*'

    - name: 'NoComposeInPresenter'
      active: true
      includes: ['**/presenter/**']
      excludes: []
      forbidden:
        - 'androidx.compose.*'
        - 'org.jetbrains.compose.*'
```

These rules complement the Gradle dependency graph — they catch cases where a developer
adds a compile dep and imports it directly rather than through a proper module boundary.

---

## Fitness Functions

Run these checks in CI to detect architecture drift:

```bash
# 1. Verify :presenter has no Compose dep in any feature module
grep -r "compose" feature/*/presenter/build.gradle.kts && echo "VIOLATION" || echo "OK"

# 2. Verify :ui does not depend on :data or :domain
grep -r "projects\.feature\.\w*\.\(data\|domain\)" feature/*/ui/build.gradle.kts && echo "VIOLATION" || echo "OK"

# 3. Detekt with architecture rules
./gradlew detekt
```

Wire these as CI gates via `kotlin-multiplatform-ci-github-actions`.

---

## Related Skills

- `kotlin-multiplatform-feature-scaffold` — creates the 6-layer module structure this skill governs
- `kotlin-multiplatform-presenter-module` — `:presenter` layer in depth: MVI contracts, ViewModel, Koin wiring
- `kotlin-multiplatform-unit-testing` — JVM-based ViewModel tests enabled by the `:presenter`/`:ui` split
- `kotlin-multiplatform-code-quality` — Ktlint + Detekt setup; Detekt is the enforcement mechanism here

---

## Common Anti-Patterns

- putting data classes in `:api` — they belong in `:model`; `:api` should be interfaces only
- adding Compose to `:presenter` — kills JVM testability; Compose belongs only in `:ui`
- `:ui` importing from `:data` directly — all state must route through `:presenter`
- `:domain` depending on `:data` — use cases should depend on repository *interfaces* from `:api`, not implementations
- skipping `internal` on `RepositoryImpl` — leaks the implementation type across modules

If a layer violation is hard to fix, it usually means a type belongs one layer lower (closer to `:model`).

---

## Output Style

When asked about architecture layers or module boundaries, respond in this order:
1. which layer the concept belongs to and why
2. the dependency rule it must satisfy
3. concrete file/class placement
4. how to enforce it (Gradle dep or Detekt rule)
5. the anti-pattern it avoids
