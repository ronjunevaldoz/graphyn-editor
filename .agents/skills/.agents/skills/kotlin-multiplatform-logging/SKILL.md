---
name: kotlin-multiplatform-logging
description: >
  Sets up Kermit KMP-native logging: log levels, pluggable writers per platform,
  crash boundary integration (Firebase Crashlytics, Sentry), and Koin wiring so
  every layer gets a logger without importing Kermit directly.
license: Apache-2.0
metadata:
  author: kmm-agent-skills
  last-updated: '2026-06-18'
  keywords:
    - Kermit
    - logging
    - KMP
    - Kotlin Multiplatform
    - log levels
    - crash reporting
    - Koin
    - pluggable writers
    - Crashlytics
    - Sentry
---

## When to Use This Skill

Use when you need to:
- Add structured logging to a KMP project
- Configure different log severity per build variant (debug vs release)
- Route logs to Crashlytics or Sentry in production
- Wire a logger into Koin so feature modules don't import Kermit directly
- Silence logs in unit tests

**Trigger keywords:** logging, Kermit, log levels, KMP logging, crash reporting,
logger setup, Crashlytics logging, Sentry logging, Koin logger, production logs.

**Freshness rule:** Kermit API and its crash reporting extensions (`kermit-crashlytics`,
`kermit-sentry`) change between minor versions — recheck the TouchLab GitHub releases before pinning.

---

## Recommendation First

Default to **Kermit with `LogWriter` per platform: `LogcatWriter` on Android debug,
`NSLogWriter` on iOS, `CrashlyticsLogWriter` on Android/iOS release**.

Why:
- Kermit is KMP-native — no `expect/actual` wrappers needed, one API across all targets
- pluggable `LogWriter` means the logging destination changes without touching call sites
- Koin-injected logger means feature modules never import Kermit; they receive `Logger` from DI
- Crash writers (Kermit-Crashlytics, Kermit-Sentry) automatically attach logs to crash reports

Use `StaticConfig` for global severity and `MutableLoggerConfig` for per-feature overrides.

---

## Gradle Setup

### `libs.versions.toml`

```toml
[versions]
kermit = "2.0.4"

[libraries]
kermit = { module = "co.touchlab:kermit", version.ref = "kermit" }
kermit-crashlytics = { module = "co.touchlab:kermit-crashlytics", version.ref = "kermit" }
```

### `build-logic` — add to `GROUP_ID.core.gradle.kts`

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation(libs.kermit)
    }
}
```

Feature modules receive Kermit transitively via `:core:common`. Do not add it per-feature.

---

## Initialization

### `commonMain` — `AppLogger.kt` in `:core:common`

```kotlin
object AppLogger {
    fun init(writers: List<LogWriter>) {
        Logger.setLogWriters(writers)
        Logger.setMinSeverity(Severity.Debug)
    }
}
```

### Android — `Application.kt`

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val writers = if (BuildConfig.DEBUG) {
            listOf(LogcatWriter())
        } else {
            listOf(CrashlyticsLogWriter())   // kermit-crashlytics
        }
        AppLogger.init(writers)
        startKoin { /* ... */ }
    }
}
```

### iOS — `AppDelegate.swift` (or `@main` entry)

```swift
AppLogger().init(writers: [
    if DEBUG { NSLogWriter() } else { CrashlyticsLogWriter() }
])
```

---

## Log Levels

| Level | Use for |
|---|---|
| `Verbose` | Highly detailed trace — only during development |
| `Debug` | Flow control, variable values — development builds |
| `Info` | Key lifecycle events — user sign-in, app start |
| `Warn` | Recoverable issues — retry triggered, fallback used |
| `Error` | Unrecoverable errors — attach to crash reports |
| `Assert` | Invariant violations — should never happen |

```kotlin
Logger.d("AuthViewModel") { "Loading user $userId" }
Logger.i("AuthViewModel") { "User loaded successfully" }
Logger.e("AuthViewModel", throwable) { "Failed to load user" }
```

---

## Koin Wiring

Inject a tagged `Logger` per class instead of importing Kermit globally:

```kotlin
// :core:common — CoreModule.kt
val coreModule = module {
    // Provide a factory so each consumer gets a logger tagged with its class name
    factory { (tag: String) -> Logger.withTag(tag) }
}
```

### Consuming in a ViewModel

```kotlin
// :feature:auth:presenter
class AuthViewModel(
    private val getUser: GetUserUseCase,
    private val logger: Logger,
) : ViewModel() {

    private fun loadUser(id: String) {
        viewModelScope.launch {
            logger.d { "Loading user $id" }
            getUser(id)
                .catch { logger.e(it) { "Failed to load user $id" } }
                .collect { _uiState.value = AuthUiState.Success(it) }
        }
    }
}

// Koin binding
@KoinViewModel
class AuthViewModel(
    getUser: GetUserUseCase,
    logger: Logger = Logger.withTag("AuthViewModel"),
) : ViewModel() { ... }
```

---

## Crash Boundary

Attach a `CrashlyticsLogWriter` (or `SentryLogWriter`) in release builds so `Warn`/`Error`
logs appear in crash reports automatically:

```kotlin
// Production writer — logs at Warn+ are sent to Crashlytics
CrashlyticsLogWriter(
    minSeverity = Severity.Warn,
    minCrashSeverity = Severity.Error,
)
```

This means every `Logger.e(...)` call automatically attaches context to the crash report
without extra instrumentation.

---

## Silencing Logs in Tests

```kotlin
// :core:testing — add to test setup
Logger.setLogWriters(NoTagWriter())   // suppress all Kermit output during tests
Logger.setMinSeverity(Severity.Assert)
```

Or in a `@BeforeTest`:
```kotlin
@BeforeTest
fun setup() {
    Logger.setMinSeverity(Severity.Assert)
}
```

---

## Related Skills

- `kotlin-multiplatform-feature-scaffold` — Kermit lives in `:core:common`; feature modules consume it via Koin
- `kotlin-multiplatform-unit-testing` — silence Kermit in `commonTest` to keep test output clean
- `kotlin-multiplatform-dependency-injection` — Koin wiring for the `Logger` factory

---

## Testing

```kotlin
class FakeLogWriter : LogWriter() {
    data class Entry(val severity: Severity, val message: String, val tag: String)
    val entries = mutableListOf<Entry>()

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        entries += Entry(severity, message, tag)
    }
}

@Test fun `error log is recorded with correct severity`() = runTest {
    val writer = FakeLogWriter()
    val kermit = Kermit(writer)
    kermit.e("Auth") { "token expired" }
    assertEquals(1, writer.entries.size)
    assertEquals(Severity.Error, writer.entries.first().severity)
    assertTrue(writer.entries.first().message.contains("token expired"))
}

@Test fun `verbose log below min severity is suppressed`() = runTest {
    val writer = FakeLogWriter()
    val kermit = Kermit(StaticConfig(minSeverity = Severity.Warn, logWriterList = listOf(writer)))
    kermit.v("Tag") { "verbose noise" }
    assertTrue(writer.entries.isEmpty())
}

@Test fun `tag is forwarded to writer`() = runTest {
    val writer = FakeLogWriter()
    val kermit = Kermit(writer)
    kermit.i("UserRepo") { "loaded" }
    assertEquals("UserRepo", writer.entries.first().tag)
}
```

---

## Common Anti-Patterns

- importing `Logger` directly in feature modules — inject it via Koin; direct imports scatter the logging config
- using `println` or `System.out.println` for debug output — Kermit is already in the project; use it
- enabling `Verbose` severity in release builds — floods crash reports with noise
- not tagging logs — `Logger.d { "..." }` without a tag makes logs impossible to filter in Logcat
- forgetting to silence Kermit in tests — noisy test output hides failures

If logs are not appearing in release, check that the crash writer's `minSeverity` is set
to `Severity.Warn` or lower and that the crash SDK is initialized before `AppLogger.init`.

---

## Output Style

When asked about logging in KMP, respond in this order:
1. Gradle dependency (toml + convention plugin)
2. initialization in the app entry point (Android + iOS)
3. log level guidelines
4. Koin injection pattern (factory with tag)
5. crash boundary writer (CrashlyticsLogWriter or SentryLogWriter)

---

## Changelog

| Date | Change |
|---|---|
| 2026-06-18 | Initial release. |
