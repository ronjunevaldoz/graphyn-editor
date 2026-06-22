---
name: kotlin-multiplatform-navigation
description: >
  Sets up type-safe navigation in a Kotlin Multiplatform Compose project using
  Jetpack Navigation Compose for KMP (org.jetbrains.androidx.navigation). Covers:
  route definitions with kotlinx.serialization, NavHost setup in a :app:shared
  or :feature:*:ui nav graph, type-safe arguments, deep links, and bottom
  navigation wiring. Works on Android, iOS, Desktop (JVM), and Web (JS/WasmJs).
  Assumes the project was scaffolded with kotlin-multiplatform-feature-scaffold.
license: Apache-2.0
metadata:
  author: kmm-agent-skills
  last-updated: '2026-06-06'
  keywords:
    - Navigation Compose
    - KMP navigation
    - type-safe routes
    - NavHost
    - NavController
    - Kotlin Multiplatform
    - Compose Multiplatform
    - deep links
    - bottom navigation
---

## Overview

Two options are available for KMP navigation. This skill covers both:

| Option | Library | Maturity | Best for |
|---|---|---|---|
| **Navigation Compose (KMP)** | `org.jetbrains.androidx.navigation:navigation-compose` | Stable (JetBrains) | CMP-first projects, familiar Jetpack API |
| **Decompose** | `com.arkivanov.decompose:decompose` | Stable (community) | Complex back-stack logic, lifecycle control |

**Recommendation**: Use Navigation Compose for KMP unless you need Decompose's fine-grained component lifecycle control.

## When to Use This Skill

Use this skill when you need to:
- Add type-safe navigation to a KMP Compose app
- Decide between Navigation Compose KMP and Decompose
- Wire nested graphs, bottom navigation, or deep links
- Recheck navigation API changes before upgrading the library

**Trigger keywords:** navigation, nav graph, navhost, route, deep link, bottom nav,
KMP navigation, type-safe routes, Decompose, Navigation Compose,
navigate to screen, go to screen, back stack, push screen, pop back,
navigate back, pass arguments, route arguments, nested navigation, screen transition.

**Freshness rule:** recheck the JetBrains Navigation Compose docs before upgrading or
copying snippets into a new project.

---

## Recommendation First

Default to **JetBrains Navigation Compose with type-safe routes + one nested graph per feature**.

Why:
- type-safe routes catch destination mismatches at compile time
- nested graphs keep feature navigation encapsulated and testable in isolation
- the JetBrains fork supports all KMP targets (Android, iOS, Desktop, Web) from `commonMain`

Use Decompose only when you need platform-specific lifecycle callbacks or deep back-stack control
that Navigation Compose does not yet support on all targets.

---

## Prerequisites

- Project scaffolded with `kotlin-multiplatform-feature-scaffold`
- `kotlinx.serialization` applied (already in `GROUP_ID.feature.api` convention plugin)
- All UI modules apply `GROUP_ID.feature.ui` convention plugin

---

## Version Reference

Add to `gradle/libs.versions.toml`:

```toml
[versions]
navigation-compose = "2.9.0"    # JetBrains KMP fork; check latest at
                                 # https://github.com/JetBrains/compose-multiplatform
decompose          = "3.3.0"    # optional alternative

[libraries]
# Navigation Compose (KMP)
navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation-compose" }

# Decompose (optional alternative)
decompose          = { module = "com.arkivanov.decompose:decompose",               version.ref = "decompose" }
decompose-extensions-compose = { module = "com.arkivanov.decompose:extensions-compose", version.ref = "decompose" }
```

---

## Option A: Navigation Compose (KMP)

### Step 1: Add dependency to navigation host module

The nav graph usually lives in a `:shared` or `:app:navigation` module that depends on all feature UI modules.

```kotlin
// :shared/build.gradle.kts  (or wherever your AppNavHost lives)
sourceSets {
    commonMain.dependencies {
        implementation(libs.navigation.compose)
        implementation(libs.kotlinx.serialization)
    }
}
```

---

### Step 2: Define type-safe routes

Create `src/commonMain/kotlin/GROUP_ID/navigation/Routes.kt`:

```kotlin
package GROUP_ID.navigation

import kotlinx.serialization.Serializable

// Top-level destinations — no arguments
@Serializable object HomeRoute
@Serializable object ProfileRoute
@Serializable object SettingsRoute

// Destinations with arguments
@Serializable data class UserDetailRoute(val userId: String)
@Serializable data class ArticleRoute(val articleId: String, val fromDeepLink: Boolean = false)
```

> Each `@Serializable` object/class becomes a type-safe navigation route.
> Arguments are constructor parameters — no string templates, no bundles.

---

### Step 3: Build the NavHost

Create `src/commonMain/kotlin/GROUP_ID/navigation/AppNavHost.kt`:

```kotlin
package GROUP_ID.navigation

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.Composable
import GROUP_ID.feature.home.ui.HomeScreen
import GROUP_ID.feature.profile.ui.ProfileScreen
import GROUP_ID.feature.userdetail.ui.UserDetailScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onNavigateToUserDetail = { userId ->
                    navController.navigate(UserDetailRoute(userId))
                }
            )
        }

        composable<UserDetailRoute> { backStackEntry ->
            val route: UserDetailRoute = backStackEntry.toRoute()
            UserDetailScreen(
                userId = route.userId,
                onBack = { navController.popBackStack() }
            )
        }

        composable<ProfileRoute> {
            ProfileScreen()
        }
    }
}
```

---

### Step 4: Nested navigation graphs

Organize feature navigation into nested graphs. Create per-feature nav graph extensions:

```kotlin
// In :feature:auth:ui
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation

@Serializable object AuthGraph
@Serializable object LoginRoute
@Serializable object RegisterRoute

fun NavGraphBuilder.authGraph(
    onLoginSuccess: () -> Unit,
) {
    navigation<AuthGraph>(startDestination = LoginRoute) {
        composable<LoginRoute> {
            LoginScreen(onLoginSuccess = onLoginSuccess)
        }
        composable<RegisterRoute> {
            RegisterScreen()
        }
    }
}
```

Then wire it in `AppNavHost`:

```kotlin
NavHost(navController = navController, startDestination = AuthGraph) {
    authGraph(onLoginSuccess = { navController.navigate(HomeRoute) })
    // other graphs...
}
```

---

### Step 5: Bottom navigation

```kotlin
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelRoutes.forEach { topLevel ->
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(topLevel.route::class) == true,
                        onClick = {
                            navController.navigate(topLevel.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(topLevel.icon, contentDescription = topLevel.label) },
                        label = { Text(topLevel.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding)
        ) { /* destinations */ }
    }
}

data class TopLevelRoute<T : Any>(val route: T, val icon: ImageVector, val label: String)

val topLevelRoutes = listOf(
    TopLevelRoute(HomeRoute, Icons.Default.Home, "Home"),
    TopLevelRoute(ProfileRoute, Icons.Default.Person, "Profile"),
)
```

---

### Step 6: Deep links

```kotlin
composable<ArticleRoute>(
    deepLinks = listOf(
        navDeepLink<ArticleRoute>(
            basePath = "https://example.com/article"
        )
    )
) { backStackEntry ->
    val route: ArticleRoute = backStackEntry.toRoute()
    ArticleScreen(articleId = route.articleId)
}
```

Add intent filters in `AndroidManifest.xml`:

```xml
<activity android:name=".MainActivity">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="https" android:host="example.com" />
    </intent-filter>
</activity>
```

---

### Step 7: Pass NavController to feature ViewModels via Koin (optional)

For navigation triggered from a ViewModel (e.g., after a successful login):

```kotlin
// In :feature:auth:domain
interface AuthNavigator {
    fun navigateToHome()
    fun navigateToRegister()
}

// In :shared (Android/common)
class AuthNavigatorImpl(private val navController: NavController) : AuthNavigator {
    override fun navigateToHome() = navController.navigate(HomeRoute)
    override fun navigateToRegister() = navController.navigate(RegisterRoute)
}
```

Register in Koin scoped to the NavController lifecycle.

---

## Option B: Decompose (alternative)

Use Decompose when you need:
- Fine-grained component lifecycle (independent of Compose recomposition)
- Deep back-stack control (e.g., tabbed navigation with independent stacks)
- Navigation logic fully in commonMain without Compose dependency

```kotlin
// :shared/build.gradle.kts
sourceSets {
    commonMain.dependencies {
        implementation(libs.decompose)
        implementation(libs.decompose.extensions.compose)
    }
}
```

Basic Decompose root component:

```kotlin
class RootComponent(
    componentContext: ComponentContext,
) : ComponentContext by componentContext {

    sealed class Child {
        class HomeChild(val component: HomeComponent) : Child()
        class ProfileChild(val component: ProfileComponent) : Child()
    }

    private val navigation = StackNavigation<Config>()

    val stack: Value<ChildStack<*, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Home,
        handleBackButton = true,
        childFactory = ::createChild,
    )

    fun onNavigateToProfile() = navigation.push(Config.Profile)
    fun onBack() = navigation.pop()

    private fun createChild(config: Config, ctx: ComponentContext): Child = when (config) {
        Config.Home    -> Child.HomeChild(HomeComponent(ctx))
        Config.Profile -> Child.ProfileChild(ProfileComponent(ctx))
    }

    @Serializable
    sealed interface Config {
        @Serializable data object Home    : Config
        @Serializable data object Profile : Config
    }
}
```

---

## Guidelines

- Always use `@Serializable` route classes — never string-based routes
- Never navigate from a `LaunchedEffect` with a null check — use `SideEffect` or ViewModel events
- Use `launchSingleTop = true` + `restoreState = true` for bottom nav tabs
- Keep route classes in a shared `:core:navigation` or `:shared` module — not in feature UI modules
- Feature UI modules receive navigation lambdas as parameters — they do NOT hold a `NavController`
- For back-stack aware logic (auth gates), use `NavHost`'s `route` parameter on nested graphs

---

## Verification

1. `./gradlew :shared:compileKotlinMetadata` — route classes and NavHost compile in commonMain
2. `./gradlew :shared:compileDebugKotlinAndroid` — Android navigation compiles
3. `./gradlew :shared:compileKotlinJvm` — Desktop navigation compiles
4. Launch app and navigate to each destination — verify back stack behavior
5. Test deep link: `adb shell am start -W -a android.intent.action.VIEW -d "https://example.com/article/123" GROUP_ID`

---

## Testing

```kotlin
// Use TestNavHostController from the Navigation testing artifact
// testImplementation("org.jetbrains.androidx.navigation:navigation-testing:<version>")
@get:Rule val composeRule = createComposeRule()

@Test fun `start destination renders home screen`() {
    composeRule.setContent {
        val navController = rememberTestNavController()
        AppNavHost(navController = navController)
    }
    composeRule.onNodeWithTag(HomeTestTags.ROOT).assertExists()
}

@Test fun `navigate to detail shows detail screen`() {
    composeRule.setContent {
        val navController = rememberTestNavController()
        AppNavHost(navController = navController)
        LaunchedEffect(Unit) { navController.navigate(Screen.Detail(id = "42")) }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(DetailTestTags.ROOT).assertExists()
}

@Test fun `back stack pops on up navigation`() {
    composeRule.setContent {
        val navController = rememberTestNavController()
        AppNavHost(navController = navController)
        LaunchedEffect(Unit) {
            navController.navigate(Screen.Detail(id = "1"))
            navController.popBackStack()
        }
    }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(HomeTestTags.ROOT).assertExists()
}
```

---

## Common Anti-Patterns

- using string-based route names instead of type-safe sealed/data classes — breaks at runtime, not compile time
- putting navigation logic inside the ViewModel — ViewModels should emit navigation `Effect`, not call `navController`
- passing `navController` deep into composables — pass lambdas or use the effect pattern instead
- defining all routes in one flat `NavHost` — leads to unnavigable spaghetti; use nested graphs per feature
- sharing a `navController` between nested graphs — each graph should own its back stack
- storing navigation state in `ViewModel` as a flag — `Effect` is the correct mechanism for navigation events

If back stack is broken or navigation effects replay, audit the above list first.

---

## Related Skills

- `kotlin-multiplatform-feature-scaffold` — `NavHost` lives in the `:app:shared` module created by the scaffold
- `kotlin-multiplatform-mvi` — navigation events should be sent as `Effect` from a ViewModel, not as state
- `kotlin-multiplatform-presenter-module` — pure ViewModel that emits nav `Effect` without a Compose dependency
- `kotlin-multiplatform-expect-actual` — platform-specific deep link and URI handling

---

## Output Style

When asked about navigation or routing, respond in this order:
1. recommendation (type-safe routes, nested graphs per feature)
2. route definition snippet
3. NavHost wiring snippet
4. why type-safe routes over string routes
5. main alternative (Decompose, manual back stack)

Keep each snippet to one route and one composable destination. Map to the user's actual screen and feature names when provided.
