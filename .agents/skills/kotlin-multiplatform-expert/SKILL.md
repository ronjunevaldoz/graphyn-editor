---
name: kotlin-multiplatform-expert
description: >
  KMP Expert Orchestrator — maps all skills in this collection, their dependency
  order, and how to sequence them for any Kotlin Multiplatform project. Use this skill
  first to decide which other skill to invoke, in what order, for a given task. Covers:
  skill dependency graph, layer-by-layer build order, feature-slice assembly sequence,
  decision trees for the most common "what do I use here?" questions, and when to hand
  off to the project audit skill. This is a meta-skill; it delegates to domain skills
  for implementation and review, and it can turn confirmed audit findings into issue
  drafts or question drafts when the repo needs tracking. The long-term goal is to keep
  this skills collection aligned with the cleanest KMM architecture patterns possible.
license: Apache-2.0
metadata:
  author: kmm-agent-skills
  last-updated: '2026-06-21'
  keywords:
    - KMP expert
    - orchestrator
    - skill sequencing
    - dependency graph
    - project setup order
    - KMM architecture
    - Kotlin Multiplatform expert
    - what skill should I use
    - skill map
    - meta-skill
    - feature assembly
    - KMP decision tree
    - audit
    - project review
    - architecture review
    - issue draft
    - question draft
---

## When to Use This Skill

Use this skill when you need to:
- Start a new KMP project and don't know which skills to invoke or in what order
- Start a new KMP project from the Kotlin/kmp-wizard baseline and don't know which
  skills to invoke or in what order
- Add a new full feature to an existing KMP project (network + DB + UI + navigation)
- Decide which skill answers a specific question ("where do I put this?", "which pattern fits?")
- Route an existing KMP project into the audit skill before making changes
- Convert confirmed audit findings into GitHub issue drafts or question drafts before
  fixing if the user wants repo tracking
- Get a high-level roadmap before diving into implementation

**Branch recommendation:** use `Kotlin/kmp-wizard` `all-targets` by default for new
full-stack KMP projects. Use `all-frontends-shared` only if you want to omit the server.

**Build-logic rule:** route plugin and dependency versions through `build-logic/`
convention plugins and `gradle/libs.versions.toml`; do not scatter version strings
across module build files when creating or updating KMM projects.

**Trigger keywords:** where do I start KMP, full KMP setup, new KMP feature, which skill,
skill order, KMP architecture decision, KMM expert, KMP project plan, which pattern KMP,
KMP checklist, review my KMP project.

**Freshness rule:** recheck the Skill Invocation Map and dependency graph entries whenever
a new skill is added or removed — the routing table and skill count must stay in sync with
the actual `skills/` directories. Run `python3 skills/kotlin-multiplatform-expert/scripts/validate_skill_map.py .`
after any skill addition.

---

## Recommendation First

Default to **reading the current skill list and dependency graph before recommending anything**.

Why:
- the skill collection grows; a recommendation based on a stale skill list misroutes work
- the dependency graph in this skill defines the correct build order — skipping foundation skills
  causes downstream failures
- routing to the wrong skill wastes a context window on the wrong patterns

Use this skill as an entry point for open-ended KMP questions. Then hand off to the specific skill.
Do not implement — route and explain.

---

## Freshness Rule

At the start of every session, treat the repo files in front of you as the source of
truth. Re-read the current `README.md` and the relevant `skills/*/SKILL.md` files before
recommending an approach. Do not rely on a previous session's skill list or remembered
versions when the local repo can be checked directly.

---

## The 47 Skills and What They Own

### Layer 0 — Architecture Contract
| Skill | Owns |
|---|---|
| `kotlin-multiplatform-clean-architecture` | 6-layer dependency contract, `:model` vs `:api` split, `internal` visibility rules, Detekt architecture enforcement |
| `kotlin-multiplatform-feature-scaffold` | Project structure, 6-layer module graph, AGP 9, build-logic, version catalog, Koin 4 |
| `kotlin-multiplatform-presenter-module` | Pure-Kotlin ViewModel, MVI `UiState`/`UiIntent` contracts, no Compose dep, Koin wiring, Screen/Content split |

### Layer 1 — Project Foundation
| Skill | Owns |
|---|---|
| `kotlin-multiplatform-dependency-injection` | Koin module organization, manual vs annotated wiring, app/feature/ViewModel scopes, test overrides |
| `kotlin-multiplatform-flavor-environment` | Dev/staging/prod config, BuildKonfig, secrets, `AppConfig` facade |
| `kotlin-multiplatform-ci-github-actions` | GitHub Actions, test matrix, XCFramework release workflow |
| `kotlin-multiplatform-audit` | Existing project health checks, boundary review, architecture drift, readiness gaps |

### Layer 2 — Core Infrastructure
| Skill | Owns |
|---|---|
| `kotlin-multiplatform-ktor-auth-service` | Ktor auth service, bearer/JWT, sessions, Ktor RPC, login/refresh/logout flows, protected routes |
| `kotlin-multiplatform-mongodb-database` | MongoDB coroutine driver, repository boundary, document mapping, reactive reads with Flow, change streams |
| `kotlin-multiplatform-kotlin-rpc` | Kotlin RPC boundaries, shared service contracts, client/server layout, Ktor auth integration |
| `kotlin-multiplatform-network-layer` | Ktor 3 client, `NetworkResult<T>`, `safeRequest {}`, token refresh interceptor |
| `kotlin-multiplatform-sqldelight-setup` | SQLDelight 2, platform drivers, schema files, migrations, Flow queries |
| `kotlin-multiplatform-datastore` | Preferences DataStore + Proto DataStore, expect/actual factory, Koin wiring, SharedPreferences migration |
| `kotlin-multiplatform-xcframework-spm` | XCFramework build, SPM binary target, Xcode integration |
| `kotlin-multiplatform-logging` | Kermit, log levels, pluggable writers, crash boundary, Koin-injected logger |

### Layer 3 — Platform Patterns
| Skill | Owns |
|---|---|
| `kotlin-multiplatform-expect-actual` | `expect/actual` mechanism, interface-injection alternative, `@ObjCName`, Kotlin/Native memory |
| `kotlin-multiplatform-repository-pattern` | Data layer, single source of truth, fetch strategies, domain mapping, optimistic updates |
| `jni-kotlin-pro` | JNI bridge from Kotlin to native C/C++ libraries, `@JvmStatic` entry points, `CPointer`, memory-safe interop patterns |

### Layer 4 — Feature Building Blocks
| Skill | Owns |
|---|---|
| `kotlin-multiplatform-navigation` | Type-safe routes, nested graphs, bottom nav, deep links |
| `kotlin-multiplatform-shared-resources` | Strings, images, fonts, plurals, localization |
| `kotlin-multiplatform-mvi` | MVI architecture, Contract pattern, `MviViewModel`, State/Intent/Effect, one-shot effects |
| `kotlin-multiplatform-paging` | Paging 3 — `PagingSource`, `Pager`, `PagingData`, cursor vs offset, `RemoteMediator`, load-state handling |
| `kotlin-multiplatform-analytics` | Sealed `AnalyticsEvent`, `Analytics` interface, Firebase/Amplitude impls, screen tracking, `FakeAnalytics` |
| `kotlin-multiplatform-form-validation` | `ValidationResult`, `FieldState`, synchronous + async validators, submit gating, `ValidatedTextField` |
| `kotlin-multiplatform-image-loading` | Coil 3 — `AsyncImage`, `AvatarImage`, `HeroImage`, single `ImageLoader`, memory/disk cache |
| `kotlin-multiplatform-permissions` | `PermissionState` sealed type, `expect/actual PermissionController`, Android launcher, iOS Info.plist |
| `kotlin-multiplatform-deep-linking` | App Links + Universal Links, `DeepLinkParser`, NavHost `navDeepLink`, intent handling, AASA |
| `kotlin-multiplatform-biometric-auth` | `BiometricResult`, `expect/actual BiometricAuthenticator`, `BiometricPrompt`, `LAContext` |
| `kotlin-multiplatform-push-notifications` | FCM + APNs, `PushToken`, `FirebaseMessagingService`, `NotificationHandler` expect/actual, deep-link routing |
| `kotlin-multiplatform-workmanager` | `CoroutineWorker`, `BGTaskScheduler`, `expect/actual BackgroundScheduler`, one-time + periodic, retry |
| `kotlin-multiplatform-feature-flags` | `FeatureFlag` enum, `FeatureFlagProvider`, Firebase Remote Config, A/B variants, kill switch, fake provider |
| `kotlin-multiplatform-offline-first` | `SyncState` sealed class, `SyncManager` interface, optimistic updates with rollback, conflict resolution, local-first read pattern |
| `kotlin-multiplatform-crash-reporting` | `CrashReporter` interface, Firebase Crashlytics + Sentry actuals, Kermit `CrashReporterLogWriter`, dSYM symbolication |

### Layer 5 — UI System
| Skill | Owns |
|---|---|
| `kotlin-multiplatform-design-system` | Tokens (colors, typography, shapes, spacing), dark mode, 6 core components, no Material dependency |
| `kotlin-multiplatform-design-system-extended` | 27 additional components: Dialog, Sheet, Toast, Tabs, TopAppBar, Checkbox, etc. |
| `kotlin-multiplatform-adaptive-layout` | WindowSizeClass, Compact/Medium/Expanded breakpoints, list-detail split, adaptive navigation, cross-session pattern consistency |
| `kotlin-multiplatform-compose-slot-api` | `@Composable () -> Unit` slots, scoped slots, CompositionLocal, component API shape |
| `kotlin-multiplatform-compose-state-hoisting` | Hoist-until-shared rule, controlled components, stateless vs stateful composables |
| `kotlin-multiplatform-compose-state-container` | `remember` vs `rememberSaveable` vs `ViewModel` survival matrix, custom Saver |
| `kotlin-multiplatform-graphics-modifiers` | `graphicsLayer`, Canvas, drawBehind, drawWithCache, workflow node shells, custom drawing performance |
| `kotlin-multiplatform-preview-driven-development` | Desktop-first `@Preview` workflow, `@PreviewParameterProvider`, PDD cycle, `./gradlew :desktopApp:run` |

### Layer 6 — Testing & Quality
| Skill | Owns |
|---|---|
| `kotlin-multiplatform-unit-testing` | `runTest`, Turbine, fake-over-mock, `:core:testing` fixtures module, JVM ViewModel tests |
| `kotlin-multiplatform-roborazzi` | Screenshot tests from `@Preview` on JVM/Desktop, golden images, CI diff job |
| `kotlin-multiplatform-code-quality` | Ktlint (formatting) + Detekt (architecture rules), CI gates |
| `kotlin-multiplatform-accessibility` | Semantic roles, `contentDescription`, `mergeDescendants`, touch targets, traversal order, Roborazzi a11y snapshots |
| `kotlin-multiplatform-compose-animation` | `AnimatedVisibility`, `animateContentSize`, `Crossfade`, `AnimatedContent`, `animateXAsState`, shared elements, reduced motion |

---

## Dependency Graph

```
kotlin-multiplatform-clean-architecture     ← read first (defines the rules)
kotlin-multiplatform-feature-scaffold       ← scaffold second (implements the rules)
├── kotlin-multiplatform-presenter-module   (depends on: scaffold, clean-architecture)
├── kotlin-multiplatform-flavor-environment (no deps)
├── kotlin-multiplatform-ci-github-actions  (no deps)
├── kotlin-multiplatform-dependency-injection (no deps)
├── kotlin-multiplatform-audit              (no deps for review work)
├── kotlin-multiplatform-logging            (depends on: scaffold)
├── kotlin-multiplatform-ktor-auth-service  (no deps)
├── kotlin-multiplatform-mongodb-database   (no deps)
├── kotlin-multiplatform-kotlin-rpc         (no deps)
├── kotlin-multiplatform-network-layer      (depends on: scaffold)
├── kotlin-multiplatform-sqldelight-setup   (depends on: scaffold)
├── kotlin-multiplatform-xcframework-spm    (depends on: scaffold, ci)
├── kotlin-multiplatform-expect-actual      (depends on: scaffold)
├── kotlin-multiplatform-repository-pattern (depends on: network-layer, sqldelight-setup)
├── kotlin-multiplatform-navigation         (depends on: scaffold)
├── kotlin-multiplatform-shared-resources   (depends on: scaffold)
├── kotlin-multiplatform-mvi                (depends on: scaffold, navigation)
├── kotlin-multiplatform-design-system      (depends on: scaffold, shared-resources)
├── kotlin-multiplatform-design-system-extended (depends on: design-system)
├── kotlin-multiplatform-compose-slot-api   (depends on: design-system)
├── kotlin-multiplatform-compose-state-hoisting (depends on: mvi)
├── kotlin-multiplatform-compose-state-container (depends on: mvi, navigation)
├── kotlin-multiplatform-graphics-modifiers (depends on: design-system, compose-state-container)
├── kotlin-multiplatform-preview-driven-development (depends on: presenter-module, design-system)
├── kotlin-multiplatform-unit-testing       (depends on: presenter-module)
├── kotlin-multiplatform-roborazzi          (depends on: preview-driven-development)
├── kotlin-multiplatform-code-quality       (depends on: scaffold, clean-architecture)
├── kotlin-multiplatform-paging             (depends on: mvi, network-layer, repository-pattern)
├── kotlin-multiplatform-analytics          (depends on: mvi, dependency-injection)
├── kotlin-multiplatform-form-validation    (depends on: mvi, design-system)
├── kotlin-multiplatform-image-loading      (depends on: design-system, network-layer)
├── kotlin-multiplatform-permissions        (depends on: mvi, dependency-injection)
├── kotlin-multiplatform-deep-linking       (depends on: navigation)
├── kotlin-multiplatform-biometric-auth     (depends on: mvi, dependency-injection)
├── kotlin-multiplatform-push-notifications (depends on: permissions, deep-linking, workmanager)
├── kotlin-multiplatform-workmanager        (depends on: dependency-injection)
├── kotlin-multiplatform-feature-flags      (depends on: dependency-injection, analytics)
├── kotlin-multiplatform-accessibility      (depends on: design-system, roborazzi, compose-animation)
├── kotlin-multiplatform-compose-animation  (depends on: design-system)
├── kotlin-multiplatform-offline-first      (depends on: repository-pattern, sqldelight-setup, workmanager)
└── kotlin-multiplatform-crash-reporting    (depends on: logging, dependency-injection)
```

---

## Build Order for a New Project

### Phase 1: Foundation (do once per project)
1. **`clean-architecture`** — read the layer contract before writing any code
2. **`feature-scaffold`** — create the project from Kotlin/kmp-wizard, 6-layer module structure
3. **`flavor-environment`** — set up dev/staging/prod before writing any API code
4. **`network-layer`** — Ktor client, `NetworkResult`, auth interceptor
5. **`sqldelight-setup`** — local database, platform drivers, Koin wiring
6. **`logging`** — Kermit setup before any feature adds log calls
7. **`ci-github-actions`** — CI before any feature merges
8. **`code-quality`** — Ktlint + Detekt as CI gates from day one

### Phase 2: iOS/Desktop Readiness (if shipping to those platforms)
9. **`xcframework-spm`** — SPM binary target for iOS team
10. **`expect-actual`** — platform-specific code (UUID, SecureStorage, dispatchers)

### Phase 3: First Feature (repeat for each feature)
11. **`design-system`** — tokens and core components (once per project, before first feature)
12. **`navigation`** — add the feature's routes to the nav graph
13. **`shared-resources`** — add strings/assets the feature needs
14. **`repository-pattern`** — wire `RemoteDataSource` + `LocalDataSource` → `FooRepository`
15. **`presenter-module`** — `FooViewModel` (no Compose dep) + `FooUiState`/`FooUiIntent`
16. **`mvi`** — `FooScreen`/`FooContent` split consuming the presenter
17. **`preview-driven-development`** — Desktop `@Preview` for all states before wiring logic
18. **`unit-testing`** — `runTest` + Turbine tests for the ViewModel before shipping

### Phase 4: Richer UI & Quality (as needed)
19. **`design-system-extended`** — pull in Dialog, Sheet, Toast etc. when the feature needs them
20. **`compose-slot-api`** — when designing reusable components for the design system
21. **`compose-state-hoisting`** — when a component hierarchy gets complex
22. **`compose-state-container`** — when debugging state survival across rotation/back-nav
23. **`roborazzi`** — screenshot golden tests once the UI is stable

---

## Feature Slice Checklist

For every new feature module group (`:feature:x:model/:api/:domain/:data/:presenter/:ui`), verify:

**`:feature:x:model` (pure types)**
- [ ] Only `data class`, `sealed class`, `enum class` — no interfaces, no framework imports
- [ ] No dependency on any other module

**`:feature:x:api` (interfaces)**
- [ ] `FooRepository` interface returns domain types and `Flow<T>` / `Result<T>` only
- [ ] `sealed interface FooError` defined for typed error cases
- [ ] Depends only on `:model` — no logic, no framework deps

**`:feature:x:data` (implementation)**
- [ ] `FooRemoteDataSource` returns `NetworkResult<FooDto>`
- [ ] `FooLocalDataSource` returns `FooEntity` / `Flow<FooEntity?>`
- [ ] `FooRepositoryImpl` maps all types — no DTO or entity escapes to `:api`
- [ ] `FooDataModule` (Koin) wires both data sources and `FooRepository`

**`:feature:x:domain` (use cases, if complexity warrants)**
- [ ] Use cases have a single `invoke` operator
- [ ] Use cases depend only on `:api` — no `:data` imports

**`:feature:x:presenter` (ViewModel — no Compose)**
- [ ] `FooViewModel` has zero Compose imports
- [ ] `FooUiState` and `FooUiIntent` sealed classes defined here
- [ ] Exposes `StateFlow<FooUiState>` — no `SharedFlow` as state holder
- [ ] `_state.update { it.copy(...) }` — never `_state.value = _state.value.copy(...)`

**`:feature:x:ui` (Compose screens)**
- [ ] `FooScreen` wires ViewModel via `koinViewModel()` only
- [ ] `FooContent` is a stateless `@Composable` — accepts `FooUiState` as parameter
- [ ] `@Preview` functions cover Loading / Error / Empty / Success states
- [ ] No direct `:domain` or `:data` imports

---

## Decision Trees

### "Where does this code go?"

```
Is it platform-specific behavior?
├── YES: Does it wrap a platform SDK or require a platform type?
│   ├── YES → expect/actual (kotlin-multiplatform-expect-actual)
│   └── NO  → interface + Koin injection in platform sourcesets
└── NO:
    ├── Is it a domain type (data class, sealed, enum)?  → :feature:x:model
    ├── Is it a repository interface or nav contract?    → :feature:x:api
    ├── Is it network communication?     → :core:network + network-layer skill
    ├── Is it local persistence?         → :core:database + sqldelight-setup skill
    ├── Is it domain logic?              → :feature:x:domain use cases
    ├── Is it data fetching + mapping?   → :feature:x:data repository-pattern skill
    ├── Is it ViewModel / UiState?       → :feature:x:presenter (presenter-module skill)
    ├── Is it a Compose screen?          → :feature:x:ui (mvi skill, Content composable)
    ├── Is it a reusable UI component?   → :core:designsystem slot-api + state-hoisting skills
    └── Is it app-wide config?           → :core:common or flavor-environment skill
```

### "Which state container?"

```
Does the state involve async, IO, or repository calls?
├── YES → ViewModel (mvi skill)
└── NO:
    ├── Must survive rotation? YES
    │   ├── Bundle-safe type? → rememberSaveable {}
    │   └── Complex type?     → rememberSaveable(stateSaver = customSaver)
    └── Must survive rotation? NO → remember {}
    └── Shared with another screen? → ViewModel (graph-scoped)
```

Full survival matrix: see `kotlin-multiplatform-compose-state-container`.

### "Which transport for a backend call?"

```
grep -r "RemoteService\|@Rpc\|withRpc\|KtorRPCClient\|rpcClient\|\.rpc(" */src --include="*.kt" -l

Results found?
├── YES (kRPC is in the project):
│   ├── Does an existing RPC service interface expose this operation?
│   │   ├── YES → call through the RPC client; do NOT add safeRequest
│   │   └── NO  → extend the service interface with a new method; do NOT add a parallel HTTP call
│   └── Is the call to a DIFFERENT backend (external REST API, third-party service)?
│       └── YES → safeRequest is correct; this is a separate network boundary
└── NO (kRPC not present):
    ├── Is the backend a Kotlin-first Ktor server you control?
    │   ├── YES → consider kRPC (kotlin-multiplatform-kotlin-rpc skill) before adding HTTP
    │   └── NO  → use safeRequest (kotlin-multiplatform-network-layer skill)
    └── Is the backend a third-party REST API?
        └── YES → safeRequest is correct
```

### "expect/actual or interface?"

```
Is it a pure behavior difference (same API, different platform behavior)?
→ Interface + Koin injection

Does it require a platform-specific constructor argument (Context, UIViewController)?
→ expect class / typealias actual

Does it wrap a platform SDK with no clean interface abstraction?
→ expect class (Category 3 in expect-actual skill)

Is it a stateless primitive with no constructor (UUID, currentTimeMillis)?
→ expect fun (Category 4 in expect-actual skill)
```

Full guide: see `kotlin-multiplatform-expect-actual`.

### "What layer does this DTO/entity/model belong to?"

```
NetworkDto (from Ktor JSON)      → stays inside :feature:x:data/remote/dto/
DatabaseEntity (from SQLDelight) → stays inside :feature:x:data/local/
DomainModel (data class)         → lives in :feature:x:model/
RepositoryInterface              → lives in :feature:x:api/
UiState / UiIntent               → lives in :feature:x:presenter/
Composable screen                → lives in :feature:x:ui/
```

The rule: data flows **inward** through mappers. DTOs and entities never cross the `:data`
boundary. Domain types (in `:model`) are the lingua franca across `:api`, `:domain`, and `:presenter`.

### "How do I handle audit findings?"

```
Finding confirmed?
├── NO → keep it as a question and ask the user for clarification
└── YES:
    ├── Needs tracking in the repo? → draft a GitHub issue
    └── Needs design/product input?  → draft a GitHub question
```

Include the skill name in every draft so attribution stays visible.

---

## Common Anti-Patterns

Review each of these before shipping a feature:

- [ ] **DTO leaking to ViewModel**: `state.userDto.name` in a Screen composable
- [ ] **NetworkResult in MVI State**: `State(result: NetworkResult<User>)` — map to domain first
- [ ] **Direct DB query in ViewModel**: `db.userQueries.select()` in `handleIntent()` — use Repository
- [ ] **`GlobalScope` coroutine**: anywhere in the codebase — use `viewModelScope` or `CoroutineScope(SupervisorJob())`
- [ ] **Mutable `LaunchedEffect` key**: `LaunchedEffect(state.someFlag)` — restarts the effect on every change; use `Channel<Effect>` instead
- [ ] **`isLoading = true` without reset on error**: every `updateState { copy(isLoading = true) }` must have a matching `false` in the error branch
- [ ] **State in `remember` that must survive rotation**: registration form, search query, scroll offset with meaning
- [ ] **ViewModel state for dropdown/tooltip open state**: pure ephemeral UI → `remember`
- [ ] **`@Preview` impossible because state is buried**: composable has internal `remember` that can't be injected — hoist it
- [ ] **`actual everywhere` for pure Kotlin logic**: identical actuals on all platforms → move to `commonMain`
- [ ] **No local cache — pass-through repository**: `override suspend fun getUser() = remote.getUser().toDomain()` — no resilience, no offline support
- [ ] **`observeProducts()` triggers a network call**: the Flow should be reactive (SQLDelight); refresh is a separate `suspend fun`

---

## Skill Invocation Map

When the user asks about one of these topics, invoke the corresponding skill:

| User asks about | Invoke skill |
|---|---|
| "layer contract", "clean architecture", "which layer", ":model vs :api", "internal visibility" | `kotlin-multiplatform-clean-architecture` |
| "set up a new KMP project", "create feature module", "6-layer scaffold" | `kotlin-multiplatform-feature-scaffold` |
| "presenter module", "ViewModel no Compose", "MVI ViewModel", "UiState UiIntent" | `kotlin-multiplatform-presenter-module` |
| "Koin", "dependency injection", "manual modules", "annotated mode" | `kotlin-multiplatform-dependency-injection` |
| "review my KMP project", "audit this repo", "what's wrong with this architecture" | `kotlin-multiplatform-audit` |
| "logging", "Kermit", "log level", "crash reporting", "Crashlytics logging" | `kotlin-multiplatform-logging` |
| "auth", "authentication", "authorization", "JWT", "sessions", "Ktor RPC" | `kotlin-multiplatform-ktor-auth-service` |
| "MongoDB", "database", "collection", "Flow", "change stream", "server-side Kotlin" | `kotlin-multiplatform-mongodb-database` |
| "kotlin rpc", "kRPC", "kotlinx rpc", "RPC service", "shared RPC models" | `kotlin-multiplatform-kotlin-rpc` |
| "add Ktor", "network layer", "API calls", "token refresh" | `kotlin-multiplatform-network-layer` |
| "local database", "SQLite", "SQLDelight", "offline storage" | `kotlin-multiplatform-sqldelight-setup` |
| "CI", "GitHub Actions", "run KMP tests" | `kotlin-multiplatform-ci-github-actions` |
| "dev/staging/prod", "BuildKonfig", "environment config" | `kotlin-multiplatform-flavor-environment` |
| "XCFramework", "Swift Package Manager", "SPM", "iOS binary" | `kotlin-multiplatform-xcframework-spm` |
| "expect actual", "platform-specific", "@ObjCName", "iOS interop" | `kotlin-multiplatform-expect-actual` |
| "repository", "data layer", "offline-first", "cache", "single source of truth" | `kotlin-multiplatform-repository-pattern` |
| "navigation", "screen routing", "NavHost", "deep links" | `kotlin-multiplatform-navigation` |
| "paging", "Paging 3", "PagingSource", "infinite scroll", "load more", "next page", "cursor pagination", "offset pagination", "LazyPagingItems", "paginate" | `kotlin-multiplatform-paging` |
| "shared strings", "localization", "image assets", "fonts" | `kotlin-multiplatform-shared-resources` |
| "MVI", "ViewModel state", "one-shot effects", "Screen/Content split" | `kotlin-multiplatform-mvi` |
| "design system", "AppTheme", "design tokens", "dark mode", "spacing tokens", "screen layout", "layout consistency", "AppScaffold", "AppTopAppBar", "page title", "top bar", "action button placement" | `kotlin-multiplatform-design-system` |
| "adaptive layout", "WindowSizeClass", "tablet layout", "desktop layout", "list detail", "split screen", "navigation rail", "Compact Medium Expanded", "responsive UI", "master detail" | `kotlin-multiplatform-adaptive-layout` |
| "dialog", "bottom sheet", "toast", "tabs", "TopAppBar", "Checkbox" | `kotlin-multiplatform-design-system-extended` |
| "slot API", "content lambda", "composable parameter", "scoped slot" | `kotlin-multiplatform-compose-slot-api` |
| "state hoisting", "hoist state", "controlled component", "where does state go" | `kotlin-multiplatform-compose-state-hoisting` |
| "remember vs ViewModel", "rememberSaveable", "state survival", "config change" | `kotlin-multiplatform-compose-state-container` |
| "graphicsLayer", "Canvas", "drawWithCache", "workflow node", "custom drawing" | `kotlin-multiplatform-graphics-modifiers` |
| "@Preview", "desktop preview", "PDD", "fast UI iteration", "PreviewParameterProvider" | `kotlin-multiplatform-preview-driven-development` |
| "unit test", "runTest", "Turbine", "Flow test", "fake repository", ":core:testing" | `kotlin-multiplatform-unit-testing` |
| "screenshot test", "Roborazzi", "golden image", "visual regression", "CI diff" | `kotlin-multiplatform-roborazzi` |
| "test canvas layout", "canvas screenshot", "layout regression test", "visual accuracy", "pixel-perfect test", "arrangement test", "test node placement", "UI layout verification", "100% accuracy test" | `kotlin-multiplatform-roborazzi` |
| "Ktlint", "Detekt", "code quality", "formatting", "architecture rules", "CI gate" | `kotlin-multiplatform-code-quality` |
| "analytics", "event tracking", "track event", "Firebase Analytics", "screen tracking", "AnalyticsTracker", "event schema", "amplitude KMP", "mixpanel KMP" | `kotlin-multiplatform-analytics` |
| "form validation", "field validation", "required field", "email validation", "inline error", "submit disabled", "async validation", "FieldState", "ValidationResult" | `kotlin-multiplatform-form-validation` |
| "image loading", "Coil", "Coil 3", "AsyncImage", "network image", "image placeholder", "circular image", "avatar image", "image cache", "disk cache" | `kotlin-multiplatform-image-loading` |
| "permissions", "runtime permission", "camera permission", "location permission", "permission denied", "PermissionState", "permission rationale", "iOS permission" | `kotlin-multiplatform-permissions` |
| "deep linking", "App Links", "Universal Links", "deep link", "AASA", "Digital Asset Links", "intent filter", "route parsing", "notification deep link" | `kotlin-multiplatform-deep-linking` |
| "biometric", "fingerprint", "Face ID", "Touch ID", "BiometricPrompt", "LocalAuthentication", "biometric result", "device credential" | `kotlin-multiplatform-biometric-auth` |
| "push notifications", "FCM", "APNs", "Firebase Messaging", "push token", "FirebaseMessagingService", "remote notification", "notification tap" | `kotlin-multiplatform-push-notifications` |
| "WorkManager", "background work", "background task", "BGTaskScheduler", "BGProcessingTask", "one-time work", "periodic work", "CoroutineWorker", "background sync" | `kotlin-multiplatform-workmanager` |
| "feature flags", "feature toggle", "remote config", "Firebase Remote Config", "A/B test", "experiment", "kill switch", "flag evaluation", "FeatureFlagProvider" | `kotlin-multiplatform-feature-flags` |
| "accessibility", "a11y", "TalkBack", "VoiceOver", "contentDescription", "semantic role", "screen reader", "touch target", "WCAG", "traversal order", "mergeDescendants" | `kotlin-multiplatform-accessibility` |
| "animation", "AnimatedVisibility", "animateContentSize", "Crossfade", "AnimatedContent", "animateFloatAsState", "shared element", "enter transition", "exit transition", "reduced motion", "spring animation" | `kotlin-multiplatform-compose-animation` |
| "offline first", "offline-first", "local first", "sync", "optimistic update", "conflict resolution", "background sync", "SyncManager", "single source of truth", "cache then network" | `kotlin-multiplatform-offline-first` |
| "crash reporting", "crashlytics", "firebase crashes", "sentry", "non-fatal", "symbolication", "dSYM", "kermit crash", "crash handler", "breadcrumb crash" | `kotlin-multiplatform-crash-reporting` |
| "DataStore", "Preferences DataStore", "Proto DataStore", "save settings", "persist user prefs", "SharedPreferences migration", "createDataStore", "local key-value store" | `kotlin-multiplatform-datastore` |
| "JNI", "native bridge", "C library", "C++ interop", "JNI bridge", "CPointer", "JvmStatic native", "kotlin native interop", "call C from Kotlin" | `jni-kotlin-pro` |

---

## Quick Health Check for Existing Projects

Run through these 6 questions for any KMP project audit:

1. **Dependency direction**: do `:ui` or `:domain` modules ever import from `:data`?
   If yes → architectural violation; data layer details are leaking.

2. **Presenter boundary**: does `:presenter` import `androidx.compose.*` or `org.jetbrains.compose.*`?
   If yes → ViewModels cannot be tested on JVM; move Compose to `:ui` only.

3. **Network/DB types at the boundary**: does any `UiState` contain a `Dto`, `Entity`,
   or `NetworkResult`? If yes → mapping is missing at the repository boundary.

4. **Effect delivery**: are effects `SharedFlow` or `StateFlow`? They should be `Channel<Effect>`.
   `SharedFlow` can replay effects (double navigation, double toast).

5. **State atomicity**: are there any `_state.value = _state.value.copy(...)` calls?
   They should be `_state.update { it.copy(...) }` to be thread-safe under concurrent intents.

6. **Expect/actual ratio**: what fraction of platform files have identical implementations?
   High ratio → probable over-use of expect/actual; move shared logic to `commonMain`.

## Docs-First Rule

Before coding a feature, check the official docs and the project docs. The Carpool
project showed the right shape:

- verify official Android / Compose guidance first
- prefer standard APIs over custom wrappers unless the docs force a custom path
- record the decision in the project docs before implementation

Use this when the user asks to audit or extend an existing project:

1. Read the project architecture docs
2. Confirm the module boundary
3. Check whether the feature belongs in an existing pattern skill
4. Only then write code or a new skill

## Recommendation Format

When recommending an approach, always present it in this order:

1. Recommend the default first.
2. Show the relevant project structure.
3. Show a small code snippet.
4. Explain why that path is preferred.
5. Mention the main alternative only after the default is clear.

Use this format when the user asks what to build next, which pattern to use, or how a
skill should be applied. Keep the snippet small and directly tied to the structure.

## Naming Rule

Use neutral names by default. Prefix only when the prefix adds clarity at the boundary.

- Shared design-system primitives may use an `App` prefix: `AppButton`, `AppCard`,
  `AppText`, `AppIcon`.
- Feature-local UI should usually stay plain: `UsersScreen`, `UsersList`, `GraphSurface`.
- Layout and state types should be descriptive, not branded: `ViewportState`,
  `LayoutMode`, `Breakpoint`, `SelectionState`.
- Avoid repeating the layer in the name: prefer `Toolbar` over `GraphUiToolbar`,
  `Canvas` over `GraphUiCanvas`, unless a collision actually exists.

If a name feels noisy, remove the prefix first. Add a prefix only when the codebase
already has multiple same-named concepts or the component is part of a shared library.

## Bundled Script

- `scripts/validate_skill_map.py` — checks that the README and expert map still list
  the current skill folders and that the declared skill count matches the repo.

---

## Related Skills

- `kotlin-multiplatform-audit` — run this after every feature to verify no architecture smells were introduced
- `kotlin-multiplatform-clean-architecture` — the 6-layer contract that all skill routing assumes
- `kotlin-multiplatform-feature-scaffold` — establishes the module structure before any feature skills are loaded
- `kotlin-multiplatform-dependency-injection` — every feature plan must include Koin wiring; load this if the plan references bindings

---

## Output Style

When asked for a KMP recommendation, routing decision, or anti-pattern check, respond in this order:
1. recommendation (name the skill and the default choice)
2. the decision rule or dependency graph node that applies
3. why that skill or pattern fits
4. skills to use next (if the task spans multiple domains)

Keep the response concise — this skill routes to other skills, not implements. Name the exact skill to invoke for follow-up work.
