---
name: kotlin-multiplatform-preview-driven-development
description: >
  Preview-Driven Development (PDD) workflow for KMP: write Content composables first,
  iterate on Desktop JVM previews (3-5x faster than Android), cover all states with
  @PreviewParameterProvider, then promote previews directly to Roborazzi screenshot tests.
license: Apache-2.0
metadata:
  author: kmm-agent-skills
  last-updated: '2026-07-08'
  keywords:
    - PDD
    - preview-driven development
    - Desktop preview
    - '@Preview'
    - PreviewParameterProvider
    - Compose preview
    - KMP
    - JVM preview
    - fast iteration
    - Roborazzi
---

## When to Use This Skill

Use when you need to:
- Build a new Compose UI screen or component
- Iterate quickly on layout and state variations without running a build
- Cover loading/error/empty/success states visually before writing logic
- Set up `@PreviewParameterProvider` to test all states in one function
- Create a workflow where previews double as Roborazzi golden screenshots

**Trigger keywords:** preview, PDD, desktop preview, @Preview, PreviewParameterProvider,
fast UI iteration, Compose preview, desktop app run, JVM preview, preview workflow.

**Freshness rule:** Compose Multiplatform desktop preview support evolves — recheck the
IntelliJ/Android Studio plugin version needed for interactive Desktop previews.

---

## Recommendation First

Default to **Desktop-first previews: write the `Content` composable, add `@Preview` functions
targeting the Desktop JVM target, iterate in the IDE without running a build**.

Make previews mandatory, not optional:
- every `*Content.kt` should have at least one preview stub
- feature UI modules should keep preview files alongside the screen/content code or in a
  `previews/` sibling folder
- the preview should exercise the states that matter for the screen, not just the happy path

Why Desktop over Android previews:
- JVM only — no `processDebugResources`, no D8, no manifest merge
- 3–5× faster compilation than the Android target
- Works without a connected device or running emulator
- Same JVM target as Roborazzi screenshot tests — golden images are stable across machines
- `./gradlew :desktopApp:run` gives a live interactive window during development

The Screen/Content split (from `kotlin-multiplatform-presenter-module`) is the technical prerequisite:
`Content` accepts state as a parameter, `Screen` holds the ViewModel. Previews only need `Content`.

---

## PDD Cycle

```
1. Define UiState variants (Loading / Error / Empty / Success)
         ↓
2. Write Content composable — accepts UiState as parameter
         ↓
3. Write @Preview functions for each state (or use @PreviewParameterProvider)
         ↓
4. Open preview panel in IDE — iterate on layout without any build
         ↓
5. Run ./gradlew :desktopApp:run for an interactive live window
         ↓
6. Promote to Roborazzi — same @Preview functions become golden screenshots
         ↓
7. CI runs ./gradlew :desktopApp:jvmTest — screenshot diffs fail the build
```

Steps 1–5 happen before any logic is wired — UI shape is validated visually first.

---

## Content Composable Pattern

```kotlin
// :feature:auth:ui — AuthContent.kt
@Composable
fun AuthContent(
    state: AuthUiState,
    onIntent: (AuthUiIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            AuthUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            is AuthUiState.Success -> UserProfile(user = state.user)
            is AuthUiState.Error -> ErrorView(
                message = state.message,
                onRetry = { onIntent(AuthUiIntent.Retry) },
            )
        }
    }
}
```

Key rules:
- `Content` has no `ViewModel` parameter — it receives state and a lambda
- All user actions go through `onIntent` — one callback, not N individual lambdas
- `modifier` parameter is the last named arg — standard Compose convention

---

## Preview Functions

### Per-state previews (simple)

```kotlin
@Preview
@Composable
private fun AuthContentLoadingPreview() {
    AppTheme {
        AuthContent(state = AuthUiState.Loading, onIntent = {})
    }
}

@Preview
@Composable
private fun AuthContentSuccessPreview() {
    AppTheme {
        AuthContent(state = AuthUiState.Success(PreviewData.user), onIntent = {})
    }
}

@Preview
@Composable
private fun AuthContentErrorPreview() {
    AppTheme {
        AuthContent(state = AuthUiState.Error("Session expired"), onIntent = {})
    }
}
```

### `@PreviewParameterProvider` (preferred for many states)

```kotlin
class AuthStateProvider : PreviewParameterProvider<AuthUiState> {
    override val values = sequenceOf(
        AuthUiState.Loading,
        AuthUiState.Success(PreviewData.user),
        AuthUiState.Error("Session expired"),
    )
}

@Preview
@Composable
private fun AuthContentAllStatesPreview(
    @PreviewParameter(AuthStateProvider::class) state: AuthUiState,
) {
    AppTheme {
        AuthContent(state = state, onIntent = {})
    }
}
```

One function, all states — the IDE renders each provider value as a separate frame.

---

## Preview Data Object

Centralise preview data so all previews use consistent models:

```kotlin
// :core:ui or :feature:<name>:ui
object PreviewData {
    val user = User(
        id = "preview-user-id",
        name = "Alex Preview",
        email = "alex@example.com",
    )
}
```

Keep `PreviewData` in the `:ui` module — it is not needed outside previews.

---

## Desktop Run Cycle

```bash
# Fast iterative preview — compiles only Desktop/JVM, no AGP
./gradlew :desktopApp:run

# Headless check — all Desktop compilations succeed
./gradlew :desktopApp:compileKotlinJvm
```

Configure the Desktop entry point in `desktopApp/src/jvmMain/kotlin/main.kt`:

```kotlin
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Preview Runner") {
        // Wire a preview composable here for live preview during development
        AuthContent(state = AuthUiState.Success(PreviewData.user), onIntent = {})
    }
}
```

---

## Promote Previews to Roborazzi

The same `@Preview` functions become Roborazzi screenshot tests with zero extra test code.
See `kotlin-multiplatform-roborazzi` for the Gradle plugin wiring.

Conceptual link:
```
@Preview function  →  Roborazzi captures bitmap  →  committed as golden image
                                                   →  CI diffs on next run
```

---

## Related Skills

- `kotlin-multiplatform-presenter-module` — the Screen/Content split that makes previews stateless
- `kotlin-multiplatform-roborazzi` — promotes previews to CI screenshot tests
- `kotlin-multiplatform-feature-scaffold` — the Desktop target must be declared in the feature's `:ui` convention plugin
- `kotlin-multiplatform-unit-testing` — unit tests for the ViewModel; PDD covers the UI layer

---

## Common Anti-Patterns

- putting ViewModel logic into the `Content` composable — previews need fixed state; a ViewModel call breaks them
- one preview per file instead of `@PreviewParameterProvider` — harder to see all states at a glance
- skipping the `AppTheme {}` wrapper in previews — colors and typography look wrong; always wrap
- using Android emulator to verify layout during development — Desktop preview is 3–5× faster
- hardcoding preview data inline — use `PreviewData` object so all previews stay in sync
- shipping a `Content` composable without a matching preview stub — that leaves the
  preview workflow optional and is exactly how drift sneaks in
- two preview states that render identically (e.g. a separate "Initial" preview next to
  "Default" when both pass the same field values) — before adding a new named state, check
  whether it's visually distinct from an existing one; a preview that duplicates another
  pixel-for-pixel is noise, not coverage. One preview per genuinely distinct visual state
- a horizontally-scrolling container (`LazyRow`, carousel) in a preview with no
  `Modifier.fillMaxWidth()` or explicit width on the container — an unbounded-width
  scrolling row can collapse to its first item's intrinsic width or render nothing
  useful in the preview panel; wrap it in a fixed-width `Box` or set `fillMaxWidth()`
  so the preview reflects the real on-screen proportions

If the Desktop preview doesn't render, check that the `:ui` convention plugin declares `jvm()` and
that the Compose plugin is applied.

---

## Output Style

When asked about UI iteration, previews, or fast Compose development, respond in this order:
1. Content composable structure (state param + onIntent lambda)
2. `@PreviewParameterProvider` with all state variants
3. Desktop run command for live iteration
4. how previews promote to Roborazzi
5. PreviewData object placement

---

## Changelog

| Date | Change |
|---|---|
| 2026-07-08 | Added 2 anti-patterns from real shadcn-compose bug reports: duplicate preview states that render identically (e.g. "Initial" vs "Default"), and horizontally-scrolling containers previewed with no explicit/fillMaxWidth container width. |
| 2026-06-18 | Initial release. |
