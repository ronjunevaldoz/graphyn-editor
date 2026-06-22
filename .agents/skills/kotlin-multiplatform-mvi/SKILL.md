---
name: kotlin-multiplatform-mvi
description: >
  MVI (Model-View-Intent) architecture pattern for Kotlin Multiplatform + Compose
  Multiplatform. Covers: the Contract pattern (State/Intent/Effect per screen),
  MviViewModel base class with StateFlow for state and Channel for one-shot effects,
  atomic state updates, Compose screen/content split, testing ViewModels with Turbine,
  and the most common MVI pitfalls in KMP. Zero new dependencies — builds on
  androidx.lifecycle.ViewModel and kotlinx.coroutines already present in feature-scaffold.
license: Apache-2.0
metadata:
  author: kmm-agent-skills
  last-updated: '2026-06-06'
  keywords:
    - MVI
    - Model-View-Intent
    - ViewModel
    - StateFlow
    - Channel
    - Effect
    - Intent
    - UiState
    - unidirectional data flow
    - UDF
    - Kotlin Multiplatform
    - Compose Multiplatform
    - CMP
    - Koin ViewModel
    - Turbine
    - architecture pattern
    - screen state
    - side effect
    - one-shot event
---

## When to Use This Skill

Use when you need to:
- Implement a screen with observable UI state in a Kotlin Multiplatform + CMP project
- Handle one-shot side effects (navigation, toasts, dialogs) safely without replay bugs
- Structure a ViewModel that's testable without a Compose/Android dependency
- Explain or implement MVI, UDF (unidirectional data flow), or a screen state machine

**Requires:** `kotlin-multiplatform-feature-scaffold` project structure.
**Zero new dependencies** — `androidx.lifecycle.ViewModel`, `kotlinx.coroutines`, Koin, and
Turbine are already present.

**Trigger keywords:** MVI, Model-View-Intent, screen state, UiState, UiIntent, UiEffect,
unidirectional data flow, ViewModel state, one-shot effects, side effects, screen architecture,
StateFlow screen, channel effect, Contract pattern,
navigation effect, one-shot event, single event, show toast from ViewModel,
trigger navigation, event driven UI, MVVM vs MVI, unidirectional event.

**Freshness rule:** `lifecycle-viewmodel-compose` and CMP lifecycle integration change between
releases — recheck the AndroidX lifecycle and JetBrains CMP docs before upgrading.

---

## Recommendation First

Default to the **Contract pattern + MviViewModel + `Channel<Effect>`**.

Why:
- sealed `State`, `Intent`, and `Effect` types make the screen contract explicit and testable
- `Channel<Effect>` prevents one-shot effects from replaying on recomposition
- `MutableStateFlow.update {}` avoids state-copy races under concurrent updates

Use a different state container only when the screen has no side effects and no ViewModel
boundary — for example, a purely presentational component owned by a parent screen.

---

## Core Concepts

### Why MVI?

MVI enforces **one direction of data flow**:

```
UI → Intent → ViewModel → State update → UI re-render
                        ↘ Effect → UI side effect (navigate, toast, dialog)
```

- **State** (`StateFlow`) — what the screen renders. Always up-to-date, never missed.
- **Intent** — what the user did. A sealed interface of user-triggered events.
- **Effect** — one-shot side effects that should NOT be replayed on recomposition
  (navigation, showing a snackbar, triggering a dialog).

### Why `Channel<Effect>` and not `SharedFlow<Effect>`?

This is the most common MVI mistake in KMP/Android.

`SharedFlow(replay = 0)` **drops effects** if no collector is active (e.g., during
process restart, screen rotation, or Compose lifecycle pause). `SharedFlow(replay = 1)`
**replays the last effect on re-subscription**, causing double-navigation.

`Channel` delivers each effect **exactly once** to exactly one collector. If no collector
is active the effect is buffered (up to `Channel.BUFFERED` capacity) and delivered when
one subscribes. This matches what "one-shot event" actually means.

```kotlin
// ❌ Wrong — replays navigation event on recomposition
private val _effect = MutableSharedFlow<Effect>(replay = 1)

// ✓ Correct — exactly-once delivery, buffered until collector is ready
private val _effect = Channel<Effect>(Channel.BUFFERED)
val effect: Flow<Effect> = _effect.receiveAsFlow()
```

### Why `MutableStateFlow.update {}` and not direct assignment?

`StateFlow.update {}` is **atomic** — it uses compare-and-swap under the hood. Direct
assignment is not:

```kotlin
// ❌ Race condition — reads value, updates, writes back; concurrent coroutines can stomp each other
_state.value = _state.value.copy(isLoading = true)

// ✓ Atomic — compare-and-swap, safe under concurrent intent handling
_state.update { it.copy(isLoading = true) }
```

---

## The Contract Pattern

Group `State`, `Intent`, and `Effect` together in a single `Contract` object per screen.
This makes the full interface of a screen visible in one place.

```kotlin
// :feature:auth:ui/src/commonMain/kotlin/GROUP_ID/feature/auth/ui/AuthContract.kt
package GROUP_ID.feature.auth.ui

object AuthContract {

    data class State(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val error: String? = null,
    )

    sealed interface Intent {
        data class EmailChanged(val value: String) : Intent
        data class PasswordChanged(val value: String) : Intent
        data object LoginClicked : Intent
        data object ForgotPasswordClicked : Intent
    }

    sealed interface Effect {
        data object NavigateToHome : Effect
        data object NavigateToForgotPassword : Effect
        data class ShowError(val message: String) : Effect
    }
}
```

**Rules for State:**
- Always a `data class` — enables `copy()` and structural equality
- All fields have defaults — the initial state needs no arguments
- No business objects (domain models) directly in state — map to UI-specific types

**Rules for Intent:**
- `sealed interface`, not `sealed class` — Kotlin 1.9+ `data object` for no-arg intents
- Names are past-tense user actions, not commands: `LoginClicked` not `DoLogin`
- No callbacks or lambdas — intents are data, not behavior

**Rules for Effect:**
- One-shot only — navigation, toasts, dialogs, haptic feedback
- State changes are NOT effects — if the screen needs to show a success banner persistently,
  put it in `State`, not `Effect`

## Screen / Content Split

Split every screen into two composables:

- `FooScreen(viewModel = ...)` owns DI, state collection, and effect collection.
- `FooContent(state, onIntent)` is pure, previewable, and testable.
- Navigation callbacks stay as lambdas (`onBack`, `onNavigateToX`) instead of being
  pushed into `Intent` unless they are true in-screen actions.
- If a screen has multiple nav callbacks, group them into a `FooNavActions` data class.

```kotlin
@Composable
fun FooScreen(
    onBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    viewModel: FooViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                FooContract.Effect.Close -> onBack()
                is FooContract.Effect.OpenDetails -> onNavigateToDetails(effect.id)
            }
        }
    }
    FooContent(state = state, onIntent = viewModel::onIntent)
}
```

---

## MviViewModel Base Class

Place this in `:core:common` (or `:core:ui`) so all feature ViewModels can extend it.

```kotlin
// :core:common/src/commonMain/kotlin/GROUP_ID/core/mvi/MviViewModel.kt
package GROUP_ID.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel for MVI pattern.
 *
 * - [State]  — immutable data class representing everything the screen needs to render
 * - [Intent] — sealed interface of user actions / events
 * - [Effect] — sealed interface of one-shot side effects (navigation, toasts, dialogs)
 *
 * Usage:
 * ```
 * class AuthViewModel(private val repo: AuthRepository) :
 *     MviViewModel<AuthContract.State, AuthContract.Intent, AuthContract.Effect>(
 *         initialState = AuthContract.State()
 *     ) {
 *
 *     override fun handleIntent(intent: AuthContract.Intent) {
 *         when (intent) {
 *             is AuthContract.Intent.LoginClicked -> login()
 *             ...
 *         }
 *     }
 * }
 * ```
 */
abstract class MviViewModel<State : Any, Intent : Any, Effect : Any>(
    initialState: State,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect: Flow<Effect> = _effect.receiveAsFlow()

    /**
     * Called by the UI layer. Routes the intent to [handleIntent] on [viewModelScope].
     */
    fun onIntent(intent: Intent) {
        viewModelScope.launch { handleIntent(intent) }
    }

    /**
     * Implement per-ViewModel intent handling. Runs on [viewModelScope].
     * Can be a suspend function — safe to call suspend APIs directly.
     */
    protected abstract suspend fun handleIntent(intent: Intent)

    /**
     * Atomically update state. Uses compare-and-swap — safe under concurrent intent handling.
     */
    protected fun updateState(block: State.() -> State) {
        _state.update(block)
    }

    /**
     * Send a one-shot effect. Buffered — delivered when a collector is active.
     */
    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
```

---

## Implementing a ViewModel

```kotlin
// :feature:auth:ui/src/commonMain/kotlin/GROUP_ID/feature/auth/ui/AuthViewModel.kt
package GROUP_ID.feature.auth.ui

import GROUP_ID.core.mvi.MviViewModel
import GROUP_ID.feature.auth.domain.AuthRepository
import GROUP_ID.feature.auth.domain.LoginResult

class AuthViewModel(
    private val authRepository: AuthRepository,
) : MviViewModel<AuthContract.State, AuthContract.Intent, AuthContract.Effect>(
    initialState = AuthContract.State(),
) {

    override suspend fun handleIntent(intent: AuthContract.Intent) {
        when (intent) {
            is AuthContract.Intent.EmailChanged ->
                updateState { copy(email = intent.value, error = null) }

            is AuthContract.Intent.PasswordChanged ->
                updateState { copy(password = intent.value, error = null) }

            is AuthContract.Intent.LoginClicked -> login()

            is AuthContract.Intent.ForgotPasswordClicked ->
                sendEffect(AuthContract.Effect.NavigateToForgotPassword)
        }
    }

    private suspend fun login() {
        val current = state.value
        if (current.isLoading) return   // guard — debounce rapid taps

        updateState { copy(isLoading = true, error = null) }

        when (val result = authRepository.login(current.email, current.password)) {
            is LoginResult.Success -> {
                updateState { copy(isLoading = false) }
                sendEffect(AuthContract.Effect.NavigateToHome)
            }
            is LoginResult.Error -> {
                // ✓ Always reset isLoading on error — forgetting this is a common bug
                updateState { copy(isLoading = false, error = result.message) }
                sendEffect(AuthContract.Effect.ShowError(result.message))
            }
        }
    }
}
```

---

## Compose Integration: Screen / Content Split

Split every screen into two composables:

- **`AuthScreen`** — wired to ViewModel, handles navigation, collects effects.
  No preview annotation.
- **`AuthContent`** — pure composable, receives `state` + `onIntent` lambda.
  Fully previewable and testable without a ViewModel.

```kotlin
// :feature:auth:ui/src/commonMain/kotlin/GROUP_ID/feature/auth/ui/AuthScreen.kt
package GROUP_ID.feature.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import GROUP_ID.core.designsystem.components.LocalToastHostState
import GROUP_ID.core.designsystem.components.ToastVariant
import org.koin.compose.viewmodel.koinViewModel

/**
 * Wired screen — owns navigation and side-effect handling.
 * Never use this in Compose @Preview.
 */
@Composable
fun AuthScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = LocalToastHostState.current

    // Collect effects exactly once, scoped to this composable's lifecycle
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AuthContract.Effect.NavigateToHome ->
                    onNavigateToHome()

                is AuthContract.Effect.NavigateToForgotPassword ->
                    onNavigateToForgotPassword()

                is AuthContract.Effect.ShowError ->
                    toast.show(effect.message, variant = ToastVariant.Destructive)
            }
        }
    }

    AuthContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}
```

```kotlin
// :feature:auth:ui/src/commonMain/kotlin/GROUP_ID/feature/auth/ui/AuthContent.kt
package GROUP_ID.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import GROUP_ID.core.designsystem.components.AppButton
import GROUP_ID.core.designsystem.components.AppText
import GROUP_ID.core.designsystem.components.AppTextField
import GROUP_ID.core.designsystem.components.AppSpinner
import GROUP_ID.core.designsystem.styles.ButtonVariant

/**
 * Pure composable — no ViewModel dependency, fully previewable.
 */
@Composable
fun AuthContent(
    state: AuthContract.State,
    onIntent: (AuthContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        AppTextField(
            value = state.email,
            onValueChange = { onIntent(AuthContract.Intent.EmailChanged(it)) },
            placeholder = "Email",
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        AppTextField(
            value = state.password,
            onValueChange = { onIntent(AuthContract.Intent.PasswordChanged(it)) },
            placeholder = "Password",
            isPassword = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            AppText(text = state.error, style = TextStyle.BodySmall, color = colors.destructive)
        }

        Spacer(Modifier.height(24.dp))

        AppButton(
            onClick = { onIntent(AuthContract.Intent.LoginClicked) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) AppSpinner(color = colors.onPrimary)
            else AppText("Sign in")
        }

        Spacer(Modifier.height(8.dp))

        AppButton(
            onClick = { onIntent(AuthContract.Intent.ForgotPasswordClicked) },
            variant = ButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AppText("Forgot password?")
        }
    }
}

@Preview
@Composable
private fun AuthContentPreview() {
    AuthContent(state = AuthContract.State(), onIntent = {})
}

@Preview
@Composable
private fun AuthContentLoadingPreview() {
    AuthContent(state = AuthContract.State(isLoading = true), onIntent = {})
}

@Preview
@Composable
private fun AuthContentErrorPreview() {
    AuthContent(
        state = AuthContract.State(error = "Invalid credentials"),
        onIntent = {},
    )
}
```

### Why `LaunchedEffect(viewModel)` not `LaunchedEffect(Unit)`?

`LaunchedEffect(Unit)` is started once per composition entry and cancelled when the
composable leaves the tree. `LaunchedEffect(viewModel)` ties the lifecycle to the ViewModel
instance — if the screen is re-entered with the same ViewModel (e.g., bottom nav tab
switch), the same coroutine resumes rather than starting a new one. Either works for most
cases, but `viewModel` is more correct when the ViewModel outlives a single composition.

---

## Koin Wiring

```kotlin
// :feature:auth:ui/src/commonMain/kotlin/GROUP_ID/feature/auth/ui/di/AuthUiModule.kt
package GROUP_ID.feature.auth.ui.di

import GROUP_ID.feature.auth.ui.AuthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authUiModule = module {
    viewModel { AuthViewModel(get()) }
}
```

With **Koin annotated mode** (Koin Compiler Plugin):
```kotlin
@KoinViewModel
class AuthViewModel(private val authRepository: AuthRepository) : MviViewModel<...>(...) { ... }
```

---

## Testing

### Test state transitions

Use Turbine to test `StateFlow` emissions as a sequence:

```kotlin
// :feature:auth:ui/src/commonTest/kotlin/.../AuthViewModelTest.kt
class AuthViewModelTest {

    @Test
    fun `login success transitions Loading then clears state and sends NavigateToHome effect`() = runTest {
        val viewModel = AuthViewModel(FakeAuthRepository())

        viewModel.state.test {
            // Initial state
            assertEquals(AuthContract.State(), awaitItem())

            viewModel.onIntent(AuthContract.Intent.LoginClicked)

            // Loading
            assertEquals(AuthContract.State(isLoading = true), awaitItem())

            // Cleared
            assertEquals(AuthContract.State(isLoading = false), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `login failure resets isLoading and sends ShowError effect`() = runTest {
        val viewModel = AuthViewModel(FakeAuthRepository(failsWith = "Invalid credentials"))

        // Collect effects alongside state
        val effects = mutableListOf<AuthContract.Effect>()
        val effectJob = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.state.test {
            awaitItem()  // initial
            viewModel.onIntent(AuthContract.Intent.LoginClicked)
            awaitItem()  // loading = true
            val errorState = awaitItem()  // loading = false, error set
            assertFalse(errorState.isLoading)
            assertEquals("Invalid credentials", errorState.error)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(
            listOf(AuthContract.Effect.ShowError("Invalid credentials")),
            effects,
        )
        effectJob.cancel()
    }

    @Test
    fun `email change updates state and clears error`() = runTest {
        val viewModel = AuthViewModel(FakeAuthRepository())

        viewModel.state.test {
            awaitItem()  // initial

            viewModel.onIntent(AuthContract.Intent.EmailChanged("new@example.com"))

            val updated = awaitItem()
            assertEquals("new@example.com", updated.email)
            assertNull(updated.error)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### Test content composable independently

```kotlin
class AuthContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `login button disabled when loading`() {
        composeTestRule.setContent {
            AuthContent(
                state = AuthContract.State(isLoading = true),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithText("Sign in").assertIsNotEnabled()
    }

    @Test
    fun `error message shown when error in state`() {
        composeTestRule.setContent {
            AuthContent(
                state = AuthContract.State(error = "Invalid credentials"),
                onIntent = {},
            )
        }
        composeTestRule.onNodeWithText("Invalid credentials").assertIsDisplayed()
    }
}
```

### Fake repository pattern

```kotlin
// :core:testing/src/commonMain/kotlin/GROUP_ID/core/testing/fakes/FakeAuthRepository.kt
class FakeAuthRepository(
    private val failsWith: String? = null,
) : AuthRepository {

    val loginCalls = mutableListOf<Pair<String, String>>()

    override suspend fun login(email: String, password: String): LoginResult {
        loginCalls.add(email to password)
        return if (failsWith != null) LoginResult.Error(failsWith)
        else LoginResult.Success(FakeUser)
    }
}
```

---

## State Patterns

### Loading / Success / Error (LSE) state machine

For screens that load async data, model the full lifecycle explicitly:

```kotlin
object UserProfileContract {

    sealed interface State {
        data object Loading : State
        data class Success(val user: UserProfile) : State
        data class Error(val message: String, val retryable: Boolean = true) : State
    }

    sealed interface Intent {
        data object Retry : Intent
        data class UpdateBio(val bio: String) : Intent
    }

    sealed interface Effect {
        data object ShowSaveSuccess : Effect
    }
}
```

Then in the ViewModel:

```kotlin
class UserProfileViewModel(
    private val repo: UserProfileRepository,
    private val userId: String,
) : MviViewModel<UserProfileContract.State, UserProfileContract.Intent, UserProfileContract.Effect>(
    initialState = UserProfileContract.State.Loading,
) {

    init {
        loadProfile()
    }

    override suspend fun handleIntent(intent: UserProfileContract.Intent) {
        when (intent) {
            is UserProfileContract.Intent.Retry -> {
                updateState { UserProfileContract.State.Loading }
                loadProfile()
            }
            is UserProfileContract.Intent.UpdateBio -> saveBio(intent.bio)
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            when (val result = repo.getProfile(userId)) {
                is Result.Success -> updateState { UserProfileContract.State.Success(result.data) }
                is Result.Error   -> updateState { UserProfileContract.State.Error(result.message) }
            }
        }
    }

    private suspend fun saveBio(bio: String) {
        val current = state.value as? UserProfileContract.State.Success ?: return
        repo.updateBio(bio)
        updateState { UserProfileContract.State.Success(current.user.copy(bio = bio)) }
        sendEffect(UserProfileContract.Effect.ShowSaveSuccess)
    }
}
```

### Inline loading flags vs sealed state

| Pattern | Use when |
|---|---|
| `data class State(isLoading: Boolean, ...)` | Screen shows content AND a loading overlay simultaneously (e.g., saving while form is visible) |
| `sealed interface State { Loading; Success; Error }` | Screen shows fundamentally different UI in each phase (skeleton vs content vs error page) |

---

## Common Anti-Patterns

- using `SharedFlow` for effects — events replay on new collectors and break "fire once" guarantees
- emitting `Effect` from `init {}` — fires on every ViewModel recreation, not just on user action
- putting navigation logic inside `State` — navigation is an effect, not persisted state
- using `copy {}` with a stale `state` reference instead of `update {}` — causes lost updates under concurrency
- exposing mutable `StateFlow` from the ViewModel — UI should never mutate state directly
- missing `isLoading` guard on submit actions — lets rapid taps fire multiple network calls
- forgetting to reset `isLoading` on error — every branch that sets it `true` must reset it in success, error, and cancellation
- navigating by observing a `navigateTo: Route?` field in `State` — fires on every recomposition; use `Effect` instead
- holding domain objects (DTOs, entities) directly in `State` — map to UI-specific types at the ViewModel boundary
- using `GlobalScope` or bare `CoroutineScope()` in a ViewModel — always use `viewModelScope`
- calling `onIntent` from inside the ViewModel — `onIntent` is a UI-layer API; call private suspend functions directly
- using `LaunchedEffect(state.someField)` for effect collection — restarts on every state change; use `LaunchedEffect(viewModel)` instead

If effects are replaying or the state machine is hard to test, audit the above list first.

---

## Related Skills

- `kotlin-multiplatform-presenter-module` — simpler ViewModel pattern without `Effect`; use for screens with no one-shot events
- `kotlin-multiplatform-unit-testing` — `runTest` + Turbine for testing `StateFlow` transitions and `Channel` effects
- `kotlin-multiplatform-compose-state-container` — when to use `remember` vs ViewModel as the state container
- `kotlin-multiplatform-preview-driven-development` — `FooContent` stateless composables are the fast-preview target

---

## Output Style

When asked about MVI or screen architecture, respond in this order:
1. recommendation (Contract pattern + MviViewModel)
2. Contract snippet (State, Intent, Effect sealed types)
3. ViewModel snippet (processIntent + emit pattern)
4. Screen / Content split
5. why Channel over SharedFlow for effects

Keep each snippet to one block. Use the user's actual screen name and state fields when provided.
