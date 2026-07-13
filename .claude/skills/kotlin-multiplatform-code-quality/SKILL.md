---
name: kotlin-multiplatform-code-quality
description: >
  Sets up Ktlint (formatting) and Detekt (code smells + architecture rules) for a KMP project.
  Both run as CI gates. Ktlint is near-zero config. Detekt architecture rules enforce the
  6-layer module boundary contract from kotlin-multiplatform-clean-architecture.
license: Apache-2.0
metadata:
  author: kmm-agent-skills
  last-updated: '2026-07-10'
  keywords:
    - Ktlint
    - Detekt
    - code quality
    - formatting
    - architecture rules
    - CI gate
    - KMP
    - Kotlin Multiplatform
    - lint
    - static analysis
    - KDoc
    - comments
    - comment style
    - documentation convention
    - kdoc vs comment
    - extension function documentation
    - documentation by architectural level
---

## When to Use This Skill

Use when you need to:
- Enforce consistent Kotlin formatting across a KMP project
- Detect architecture violations (`:ui` importing from `:data`) via static analysis
- Wire Ktlint and Detekt as CI gates on every PR
- Configure Detekt architecture rules for the 6-layer module model

**Trigger keywords:** Ktlint, Detekt, code quality, formatting, architecture rules, CI gate,
static analysis, lint, import check, layer violation, code style, KDoc, comment convention,
when to use comments, documentation comment, comment style, kdoc vs line comment,
how to comment kotlin.

**Freshness rule:** Ktlint and Detekt versions change frequently — recheck the latest releases
before pinning. Detekt architecture rule DSL changes between minor versions.

---

## Recommendation First

**Install both. They solve different problems.**

| Tool | Enforces | Config effort | When to run |
|---|---|---|---|
| Ktlint | Formatting — indentation, imports, line length | Near-zero | `ktlintFormat` before commit; `ktlintCheck` in CI |
| Detekt | Code smells + architecture rules | Medium | `detekt` in CI |

Ktlint is the easier win — add it first. Detekt's architecture rule set is the more powerful
tool for catching layer violations that Gradle dependency declarations miss (import-level coupling).

---

## Ktlint Setup

### `libs.versions.toml`

```toml
[versions]
ktlint = "12.1.1"

[plugins]
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint" }
```

### Root `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.ktlint) apply false
}
```

### `build-logic/convention/build.gradle.kts`

```kotlin
dependencies {
    implementation(libs.plugins.ktlint.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
}
```

### Convention plugin — add to `GROUP_ID.core.gradle.kts` and feature plugins

```kotlin
plugins {
    // ... existing plugins ...
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    version = "1.3.1"         // Ktlint engine version (separate from Gradle plugin version)
    android = false           // KMP modules are not Android-only
    outputToConsole = true
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}
```

### `.editorconfig` (project root)

```ini
[*.{kt,kts}]
max_line_length = 120
ktlint_standard_no-wildcard-imports = disabled
ktlint_standard_import-ordering = disabled
```

### Usage

```bash
# Format all files
./gradlew ktlintFormat

# Check (CI — fails on violations)
./gradlew ktlintCheck
```

---

## Detekt Setup

### `libs.versions.toml`

```toml
[versions]
detekt = "1.23.7"

[libraries]
detekt-formatting = { module = "io.gitlab.arturbosch.detekt:detekt-formatting", version.ref = "detekt" }

[plugins]
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
```

### Root `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.detekt) apply false
}
```

### Convention plugin — add to all feature plugins

```kotlin
plugins {
    // ... existing plugins ...
    id("io.gitlab.arturbosch.detekt")
}

detekt {
    config.setFrom(rootProject.file("detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}
```

### Root `detekt.yml`

```yaml
build:
  maxIssues: 0

style:
  UnnecessaryAbstractClass:
    active: true

complexity:
  LongMethod:
    active: true
    threshold: 60
  LongParameterList:
    active: true
    functionThreshold: 6
    constructorThreshold: 7
  CyclomaticComplexMethod:
    active: true
    threshold: 15

naming:
  FunctionNaming:
    active: true
    excludes: ['**/test/**', '**/*Test.kt', '**/*Preview*']

libraries:
  rules:
    - name: 'NoComposeInPresenter'
      active: true
      includes: ['**/presenter/**']
      forbidden:
        - 'androidx.compose.*'
        - 'org.jetbrains.compose.*'

    - name: 'NoDataInUi'
      active: true
      includes: ['**/ui/**']
      forbidden:
        - '*.data.*'
        - 'io.ktor.*'
        - 'app.cash.sqldelight.*'

    - name: 'NoDomainInUi'
      active: true
      includes: ['**/ui/**']
      forbidden:
        - '*.domain.*'
```

`UnnecessaryAbstractClass` matters more in KMM than in a single-platform codebase: an
abstract class with only abstract members in `commonMain` forces every consumer into an
inheritance chain, which is exactly the pattern `kotlin-multiplatform-clean-architecture`'s
"Composition Over Inheritance" section explains how to avoid — see that section for the
full rationale and fix.

### Usage

```bash
# Run Detekt (fails on violations)
./gradlew detekt

# Generate HTML report
./gradlew detekt --report html:build/reports/detekt/detekt.html

# Fix auto-fixable issues (formatting only)
./gradlew detektFormat
```

---

## Comment & KDoc Conventions

Two comment types, two jobs — never mix them:

| | Single-line `//` | Multi-line `/** ... */` (KDoc) |
|---|---|---|
| Documents | Internal WHY — a workaround, a non-obvious constraint | Public API contract — `@param`/`@return`/`@throws`/`@sample` |
| Never used for | Restating WHAT the code does (good naming covers that) | Private members — rename instead (Detekt's `DocumentationOverPrivateFunction`/`Property` flags this) |
| Visible to | Nobody outside the source file | Dokka + IDE quick-docs |
| Grows past ~4 lines? | Split: keep the one-sentence WHY inline, move the rest to `docs/reference/` with a pointer comment (see below) | N/A — KDoc doesn't accumulate this way; if a class needs paragraphs, that's what `docs/reference/` is for too |
| Nests? | N/A | KDoc does **not** nest. Plain block comments (`/* */`) do, unlike Java/C |

### By architectural level

The table above sorts by comment *type*; this sorts by *where in the code* it lives —
use both together when reviewing or refactoring documentation.

| Level | Rule |
|---|---|
| Classes & interfaces | KDoc states the class's responsibility and architectural role only. Skip trivial openers ("Represents a X") — say what it owns and why it exists as a separate type, not what its name already tells you. |
| Functions & methods | KDoc only for complex public members, using the tag table below. Document inputs, outputs, and edge cases — never mechanics. `UndocumentedPublicFunction` requires *something*, so trivial one-liners (a getter, a pure delegate) get a single sentence, not a full `@param` breakdown. |
| Extension functions | State the receiver scope and calling context — *when* to reach for this extension, not just what it returns. Use `@receiver` for any precondition the receiver must satisfy (e.g. "must be called from inside an active `viewModelScope`"). This is the one KDoc case where "when to use it" outranks "what it does," because the same signature can exist as a member on an unrelated type. |
| Inline blocks (loops, conditionals) | No `//` that explains WHAT a block does — extract a named function or variable so the code reads as its own explanation. Keep `//` only for a non-obvious workaround or a business-logic WHY. |

```kotlin
/**
 * Retries [block] with exponential backoff, but only while this scope's job is active.
 * @receiver Must be a scope tied to a UI lifecycle (e.g. `viewModelScope`) — cancels
 * in-flight retries when the receiver is cancelled instead of leaking a delay loop.
 */
suspend fun <T> CoroutineScope.retryWhileActive(times: Int, block: suspend () -> T): T { ... }
```

### Two real mistakes this caught

**A `//` on the same line as code can swallow what follows it** — it runs to end-of-line,
including a needed closing `)`/`{`. Shipped in `kotlin-multiplatform-imagevector-generator`'s
own codegen until a test caught it:

```kotlin
// ❌ WRONG — the // comments out the rest of the line, including `) {`
path(fill = SolidColor(Color.Black)  // tint at call site) {

// ✅ CORRECT — the call is syntactically complete before the comment starts
path(fill = SolidColor(Color.Black)) {  // tint at call site
```

**A `//` block that keeps growing is a sign two audiences got merged into one comment.**
Keep only the sentence that answers "why would someone break this by simplifying it?" —
move everything else (mechanism detail, rejected alternatives, exact version numbers) to
`docs/reference/` (the lane `kotlin-multiplatform-project-docs-maintainer` already
defines for deep references), with a one-line pointer left behind:

```kotlin
// Composite build (not include()): root's apply false on org.jetbrains.compose locks
// that plugin ID to 1.11.1 build-wide. This module needs 1.12.0-beta01 for an
// experimental Compose Foundation Style API not available in the stable line.
// Full rationale: docs/reference/composite-build-style-experimental.md
includeBuild("tailwind/style-experimental")
```

### KDoc: code definition, params, samples

| Tag | Purpose |
|---|---|
| `@param` | One parameter's role — skip if the name alone makes it obvious |
| `@return` | What the return value represents — skip for `Unit` |
| `@throws` | A failure mode that's part of the contract, not every possible exception |
| `@see` | Cross-reference to a related declaration |
| `@sample` | Points at an actual, compiled function elsewhere as the usage example |
| `@property` / `@receiver` / `@constructor` | Constructor property / extension receiver / primary constructor, documented separately from the class summary |
| `@suppress` | Hides a technically-public declaration from generated docs |

**An example is warranted only when usage isn't obvious from the signature** (a builder, a
DSL) — never required per function or per file, same "why not what" rule as `//`. When one
is warranted, use `@sample`, not a pasted code block: it points at a real compiled
function, so it's type-checked and can't silently drift stale.

```kotlin
/**
 * Builds a [Result] pipeline that retries on transient failures.
 * @sample GROUP_ID.samples.retryPipelineSample
 */
fun <T> retryPipeline(times: Int, block: suspend () -> T): Flow<T> { ... }
```

Module/package-level docs (describing a whole module, not one declaration) are a separate
Dokka mechanism — `Module.md`/`Package.md` — not a KDoc tag.

### Detekt enforcement

```yaml
comments:
  UndocumentedPublicClass:
    active: true
    excludes: ['**/test/**', '**/*Test.kt', '**/*Preview*']
  UndocumentedPublicFunction:
    active: true
    excludes: ['**/test/**', '**/*Test.kt', '**/*Preview*']
  DocumentationOverPrivateFunction:
    active: true
  DocumentationOverPrivateProperty:
    active: true
  OutdatedDocumentation:
    active: true
```

`UndocumentedPublic*` requires KDoc on every public declaration; `DocumentationOverPrivate*`
forbids it on private ones; `OutdatedDocumentation` catches KDoc whose `@param`/signature
no longer matches the declaration after a refactor.

### License headers — situational, not a default

Per-file license headers were standard in the AOSP/Apache-Software-Foundation era and are
still worth it for **libraries redistributed externally** (Detekt ships
`AbsentOrWrongFileLicense`, off by default). Skip them for app code — redundant with the
root `LICENSE` file. See `kotlin-multiplatform-library-publishing`'s "Per-file license
headers" for the rule config and template.

---

## CI Integration

Add to `.github/workflows/ci.yml` lint job:

```yaml
lint:
  name: Lint
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v4

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'zulu'

    - name: Setup Gradle
      uses: gradle/actions/setup-gradle@v4
      with:
        cache-encryption-key: ${{ secrets.GRADLE_ENCRYPTION_KEY }}

    - name: Ktlint check
      run: ./gradlew ktlintCheck

    - name: Detekt
      run: ./gradlew detekt

    - name: Upload Detekt report
      if: failure()
      uses: actions/upload-artifact@v4
      with:
        name: detekt-report
        path: '**/build/reports/detekt/**'
```

---

## Related Skills

- `kotlin-multiplatform-clean-architecture` — defines the layer rules that Detekt enforces
- `kotlin-multiplatform-ci-github-actions` — the CI workflow where these gates run
- `kotlin-multiplatform-feature-scaffold` — convention plugins are where Ktlint/Detekt are applied
- `kotlin-multiplatform-project-docs-maintainer` — `docs/reference/` is where development notes go when a code comment's rationale outgrows what belongs inline
- `kotlin-multiplatform-library-publishing` — per-file license headers, a related but separate comment-placement decision
- `kotlin-multiplatform-audit` — `_detect_what_comment_in_control_flow` checks the "Inline blocks" rule below automatically; `/kmm-clean-comments` applies the fix across all four documentation levels
- `kotlin-multiplatform-docs-site` — applies this skill's `@sample` principle (a real, compiled reference beats a pasted block that can drift stale) to public developer-guide code examples via snippet extraction

---

## Common Anti-Patterns

- applying Detekt only to the root project — violations in submodules go undetected; apply via convention plugins
- setting `maxIssues > 0` — a non-zero threshold lets violations accumulate silently
- using Ktlint without `.editorconfig` — line length defaults to 80; too short for Kotlin
- running `ktlintFormat` in CI instead of `ktlintCheck` — CI should fail, not silently reformat
- excluding the `:presenter` module from `NoComposeInPresenter` — the rule only matters if applied to presenter
- using `//` to document a public API's contract instead of KDoc — Dokka and IDE quick-docs never see a `//` comment
- adding KDoc to a private member to explain unclear behavior — rename the member instead; flagged by Detekt's `DocumentationOverPrivateFunction`/`DocumentationOverPrivateProperty`
- placing a `//` comment inside a function call's argument list before its closing `)`/`{` — silently comments out the rest of the line; this exact bug shipped in `kotlin-multiplatform-imagevector-generator`'s own codegen
- writing a multi-paragraph inline comment that mixes "why this code exists" with mechanism detail, rejected alternatives, and exact version numbers — split it: the short WHY stays inline, the exhaustive rationale goes in `docs/reference/` with a one-line pointer left in the comment
- documenting an extension function's return value without stating the receiver scope or precondition it assumes — callers can't tell when it's safe to call versus when to reach for the member function instead

If Detekt reports false positives, use `@Suppress("RuleName")` at the call site, not a global exclude.

---

## Output Style

When asked about code quality, linting, or formatting for KMP, respond in this order:
1. Ktlint setup (plugin version, `.editorconfig`, `ktlintCheck` command)
2. Detekt setup (plugin, `detekt.yml`, architecture rules for the 6-layer model)
3. CI job snippet
4. which tool enforces what (table)
5. how to fix violations locally before pushing

---

## Changelog

| Date | Change |
|---|---|
| 2026-07-10 | Added "By architectural level" — a second cut through the same rules organized by Classes/Functions/Extension functions/Inline blocks instead of by comment type, closing a real gap: extension functions had no documentation guidance at all beyond a passing `@receiver` mention. New rule: extension KDoc must state receiver scope/precondition, since "when to use it" outranks "what it does" for a function that could otherwise be mistaken for a member. 1 new anti-pattern, 1 new example. Wired into automation: `kotlin-multiplatform-audit` gained a `what-comment in control flow` heuristic detector for the inline-block rule, and a new `/kmm-clean-comments` command applies the fix across all four levels (the convention was previously knowledge-only — nothing scanned or refactored comments automatically). |
| 2026-07-09 | Restructured "Comment & KDoc Conventions" around an explicit single-line (`//`) vs multi-line (KDoc `/** */`) split — a single decision table up front instead of scattered prose, so the rule is unambiguous for any agent to follow. Trimmed ~55 net lines (7 subsections → 5) while keeping every rule, both real-bug examples, the KDoc tag table, and the license-header note. |
| 2026-07-09 | Added a "Code comment vs. development notes" split, from a real 9-line inline comment that crammed a build-topology explanation, rejected alternatives, and exact version numbers into one `includeBuild()` call site. Rule: an inline comment survives only if it answers a question that would make someone break the code by "simplifying" it; the exhaustive rationale moves to `docs/reference/` (per `kotlin-multiplatform-project-docs-maintainer`'s existing convention) with a one-line pointer left in the comment. Before/after example, 1 new anti-pattern, 2 new Related Skills cross-references. |
| 2026-07-09 | Extended the "Comment & KDoc Conventions" section: a full KDoc tag reference (`@param`/`@return`/`@throws`/`@see`/`@sample`/`@property`/`@receiver`/`@constructor`/`@suppress`), guidance that an example is warranted only for non-obvious public API (never required per function/file — same "why not what" principle), `@sample`'s advantage over a raw code block (references an actual compiled function, can't silently drift stale), and a "License headers" note (situational, not a default — cross-referenced to `kotlin-multiplatform-library-publishing`). |
| 2026-07-08 | Added a "Comment & KDoc Conventions" section — KDoc for public API contracts, `//` for internal WHY notes, private members should be renamed rather than commented (backed by Detekt's `DocumentationOverPrivateFunction`/`DocumentationOverPrivateProperty`), and a real bug example (a `//` comment inside a function call's argument list silently commenting out the rest of the line, which actually shipped in `kotlin-multiplatform-imagevector-generator`'s codegen). New `comments:` Detekt rule block and 3 anti-patterns. |
| 2026-06-18 | Initial release. |
