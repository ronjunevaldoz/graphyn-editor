---
name: kotlin-multiplatform-design-system
description: >
  Scaffolds a custom Compose Multiplatform design system in :core:designsystem using
  the new Compose Styles API from Android docs (developer.android.com/develop/ui/compose/styles,
  @ExperimentalStylesApi). This is the styling layer, not the Slot API. Generates:
  color/typography/shape/spacing
  tokens, AppTheme with light/dark support, StyleScope extensions for token access,
  shadcn-inspired sealed variant systems (ButtonVariant, CardVariant, BadgeVariant,
  ChipVariant, TextFieldVariant), and 6 core components (AppButton, AppCard,
  AppTextField, AppChip, AppBadge, AppText) built on BasicXxx CMP primitives.
  No Material dependency — fully custom, fully owned.
license: Apache-2.0
metadata:
  author: kmm-agent-skills
  last-updated: '2026-06-06'
  keywords:
    - design system
    - Compose Styles API
    - AppTheme
    - custom theme
    - design tokens
    - shadcn
    - ButtonVariant
    - Kotlin Multiplatform
    - Compose Multiplatform
    - CMP
    - ExperimentalStylesApi
    - dark mode
    - token system
    - UI components
    - core:designsystem
---

## When to Use This Skill

Use this skill when the user asks to:
- Set up a custom design system, theme, or component library in a KMP project
- Avoid Material Design and build unstyled/custom components
- Use the new Compose Styles API (`@ExperimentalStylesApi`)
- Create reusable UI components with a variant system (like shadcn)
- Add dark mode support via custom tokens
- Wire AppTheme, tokens, or custom Composables into `:core:designsystem`

**Trigger keywords:** design system, custom theme, AppTheme, design tokens,
ButtonVariant, shadcn KMP, Compose Styles, ExperimentalStylesApi, custom components,
unstyled components, dark mode tokens, color scheme, no Material,
typography system, spacing tokens, custom button style, Material3 alternative,
app theme setup, brand colors, design token system, custom typography.

**Freshness rule:** `@ExperimentalStylesApi` is experimental and the Compose Styles API
changes between CMP releases — recheck the Compose docs before upgrading.

---

## Recommendation First

Default to **custom tokens + `AppTheme` + `@ExperimentalStylesApi` sealed variant systems —
no Material dependency**.

Why:
- full ownership of the token layer means no Material opinion leaking into spacing, shape, or color
- sealed variant classes (e.g., `ButtonVariant.Primary`) make component APIs explicit and auditable
- `@ExperimentalStylesApi` is the sanctioned path for custom styling in CMP; Material3 is an overlay
  on top of it, not a replacement

Use Material3 only when the product targets Material Design explicitly and design token ownership
is not a concern.

---

## Screen Layout Contract

Every screen must follow this structure — no exceptions. Consistency across all pages
depends on every feature using the same scaffold shell.

```kotlin
@Composable
fun FooContent(
    state: FooContract.State,
    onIntent: (FooContract.Intent) -> Unit,
    // windowSizeClass: WindowSizeClass  // add if adaptive layout is in scope
) {
    AppScaffold(                                    // always AppScaffold, never raw Scaffold
        topBar = {
            AppTopAppBar(
                title = "Page Title",              // ← title lives HERE, nowhere else
                navigationIcon = {                 // back button lives HERE
                    AppIconButton(onClick = { onIntent(FooContract.Intent.NavigateBack) }) {
                        AppIcon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {                        // action buttons live HERE
                    AppIconButton(onClick = { onIntent(FooContract.Intent.OpenMenu) }) {
                        AppIcon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { paddingValues ->                           // always consume PaddingValues
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)            // ← prevents clipping under TopAppBar
                .padding(horizontal = AppTheme.spacing.lg)  // ← token, never 16.dp
        ) {
            // functional content only — no title text, no duplicate action buttons
        }
    }
}
```

### Rules

| What | Where it lives | Never |
|---|---|---|
| Screen title | `AppTopAppBar(title = "…")` | `Text("…")` in content body |
| Back / close | `AppTopAppBar(navigationIcon = { … })` | Custom button in content |
| Primary action (save, filter, search) | `AppTopAppBar(actions = { … })` | Floating button duplicating the TopAppBar action |
| Overflow menu | `AppTopAppBar(actions = { AppIconButton(MoreVert) { … } })` | Separate menu row inside content |
| Horizontal content padding | `spacing.lg` (`16.dp` token) | Hardcoded `.dp` literals |

### Why redundant UI in content hurts

- A title in the content AND in the TopAppBar means the title scrolls away — the
  TopAppBar title remains anchored; use it
- Duplicate action buttons create two sources of truth for the same action; one will
  inevitably be wired differently or go stale
- Not consuming `PaddingValues` clips content under the TopAppBar on devices with
  status bars

---

## Overview

```
Design system layers (top-down):

  Tokens (AppColors, AppTypography, AppShapes, AppSpacing)
      ↓ consumed via StyleScope extensions
  Styles (sealed variant objects with Style values)
      ↓ merged via `then`
  Components (AppButton, AppCard, AppTextField, AppChip, AppBadge, AppText)
      ↓ composed
  Screens (feature UIs consume AppTheme.provide { } at the top)
```

## Style Rules

- Use the Compose Styles API for visual styling, state styling, and animated transitions.
- Do not confuse Styles with the Slot API: slots are for structure/content customization, not theming.
- Keep text, borders, surfaces, and disabled states neutral-first.
- Use palette colors for brand, emphasis, status, and primary actions only.
- If the user does not specify a palette, suggest 2-3 options based on the project domain.
- If typography is unspecified, suggest a font pair and type scale before generating components.
- Use Atlassian and shadcn as references for neutral-first palettes, crisp hierarchy, and restrained component shapes.

## Naming Rule

- Keep the `App` prefix for shared design-system primitives only.
- Use plain names for feature-local or page-local components.
- Do not over-prefix layouts, canvases, or state models.
- Reserve the prefix for reusable primitives that live in `:core:designsystem`.

**Key API facts:**
- `Style { ... }` — lambda-based style descriptor; runs in Layout/Draw phase (not Composition)
- `style1 then style2` — merges styles; last-write-wins per property
- `Modifier.styleable(styleState, defaultStyle, overrideStyle)` — applies styles to a node
- `MutableStyleState(interactionSource)` — tracks hover/press/focus/disabled states
- `StyleScope` extensions — the **only** correct way to read `CompositionLocal` values inside a Style
- All Styles API classes require `@OptIn(ExperimentalStylesApi::class)`

---

## Prerequisites

- Project scaffolded with `kotlin-multiplatform-feature-scaffold`
- CMP 1.11.1+ (`compose-multiplatform = "1.11.1"` in `libs.versions.toml`)
- Convention plugin `GROUP_ID.feature.ui` or `GROUP_ID.core` available

---

## Step 1: Create `:core:designsystem` module

Create `core/designsystem/build.gradle.kts`:

```kotlin
plugins {
    id("GROUP_ID.core")          // applies KMP + Compose targets
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            // No compose.material3 — fully custom
        }
    }
}
```

Register in `settings.gradle.kts`:

```kotlin
include(":core:designsystem")
```

---

## Step 2: Design Tokens

### Palette guidance

- Prefer neutral tokens for most text, surfaces, borders, and disabled UI.
- Reserve saturated palette colors for brand accents, primary actions, and semantic states.
- If the project brief does not name a palette, propose one that fits the product:
  - enterprise / admin: zinc, slate, neutral
  - modern consumer: blue, indigo, violet
  - creative / playful: violet, fuchsia, rose
  - trust / finance: blue, teal, emerald

### `tokens/AppColors.kt`

```kotlin
package GROUP_ID.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    // Brand
    val primary: Color,
    val primaryHover: Color,
    val primaryPressed: Color,
    val primaryDisabled: Color,
    val onPrimary: Color,

    // Secondary
    val secondary: Color,
    val secondaryHover: Color,
    val onSecondary: Color,

    // Destructive
    val destructive: Color,
    val destructiveHover: Color,
    val onDestructive: Color,

    // Surface
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,

    // Border
    val border: Color,
    val borderFocus: Color,

    // Ghost / muted
    val muted: Color,
    val onMuted: Color,

    // Status
    val success: Color,
    val warning: Color,
    val error: Color,
    val onStatus: Color,

    // State overlays
    val hoverOverlay: Color,
    val pressedOverlay: Color,

    val isLight: Boolean,
)

val LightColors = AppColors(
    primary          = Color(0xFF09090B),
    primaryHover     = Color(0xFF27272A),
    primaryPressed   = Color(0xFF3F3F46),
    primaryDisabled  = Color(0xFFD4D4D8),
    onPrimary        = Color(0xFFFAFAFA),

    secondary        = Color(0xFFF4F4F5),
    secondaryHover   = Color(0xFFE4E4E7),
    onSecondary      = Color(0xFF09090B),

    destructive      = Color(0xFFDC2626),
    destructiveHover = Color(0xFFB91C1C),
    onDestructive    = Color(0xFFFEF2F2),

    background       = Color(0xFFFFFFFF),
    surface          = Color(0xFFFFFFFF),
    surfaceVariant   = Color(0xFFF4F4F5),
    onSurface        = Color(0xFF09090B),
    onSurfaceVariant = Color(0xFF71717A),

    border           = Color(0xFFE4E4E7),
    borderFocus      = Color(0xFF09090B),

    muted            = Color(0xFFF4F4F5),
    onMuted          = Color(0xFF71717A),

    success          = Color(0xFF16A34A),
    warning          = Color(0xFFD97706),
    error            = Color(0xFFDC2626),
    onStatus         = Color(0xFFFFFFFF),

    hoverOverlay     = Color(0x0A000000),
    pressedOverlay   = Color(0x1A000000),

    isLight          = true,
)

val DarkColors = AppColors(
    primary          = Color(0xFFFAFAFA),
    primaryHover     = Color(0xFFE4E4E7),
    primaryPressed   = Color(0xFFD4D4D8),
    primaryDisabled  = Color(0xFF3F3F46),
    onPrimary        = Color(0xFF09090B),

    secondary        = Color(0xFF27272A),
    secondaryHover   = Color(0xFF3F3F46),
    onSecondary      = Color(0xFFFAFAFA),

    destructive      = Color(0xFF7F1D1D),
    destructiveHover = Color(0xFF991B1B),
    onDestructive    = Color(0xFFFEF2F2),

    background       = Color(0xFF09090B),
    surface          = Color(0xFF09090B),
    surfaceVariant   = Color(0xFF18181B),
    onSurface        = Color(0xFFFAFAFA),
    onSurfaceVariant = Color(0xFFA1A1AA),

    border           = Color(0xFF27272A),
    borderFocus      = Color(0xFFFAFAFA),

    muted            = Color(0xFF27272A),
    onMuted          = Color(0xFFA1A1AA),

    success          = Color(0xFF15803D),
    warning          = Color(0xFFB45309),
    error            = Color(0xFF7F1D1D),
    onStatus         = Color(0xFFFFFFFF),

    hoverOverlay     = Color(0x0AFFFFFF),
    pressedOverlay   = Color(0x1AFFFFFF),

    isLight          = false,
)
```

### `tokens/AppTypography.kt`

```kotlin
package GROUP_ID.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class AppTypography(
    val displayLarge: TextStyle  = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold,   lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    val displayMedium: TextStyle = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold,   lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    val titleLarge: TextStyle    = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
    val titleMedium: TextStyle   = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    val titleSmall: TextStyle    = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    val bodyLarge: TextStyle     = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal,  lineHeight = 24.sp),
    val bodyMedium: TextStyle    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal,  lineHeight = 20.sp),
    val bodySmall: TextStyle     = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal,  lineHeight = 16.sp),
    val labelLarge: TextStyle    = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium,  lineHeight = 20.sp, letterSpacing = 0.1.sp),
    val labelSmall: TextStyle    = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium,  lineHeight = 16.sp, letterSpacing = 0.5.sp),
)
```

### `tokens/AppShapes.kt`

```kotlin
package GROUP_ID.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AppShapes(
    val none: Dp    = 0.dp,
    val xs: Dp      = 2.dp,
    val sm: Dp      = 4.dp,
    val md: Dp      = 6.dp,
    val lg: Dp      = 8.dp,
    val xl: Dp      = 12.dp,
    val xxl: Dp     = 16.dp,
    val full: Dp    = 9999.dp,
)
```

### `tokens/AppSpacing.kt`

```kotlin
package GROUP_ID.core.designsystem.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AppSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp  = 4.dp,
    val sm: Dp  = 8.dp,
    val md: Dp  = 12.dp,
    val lg: Dp  = 16.dp,
    val xl: Dp  = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
)
```

---

## Step 3: AppTheme + CompositionLocals

### `theme/AppTheme.kt`

```kotlin
package GROUP_ID.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import GROUP_ID.core.designsystem.tokens.AppColors
import GROUP_ID.core.designsystem.tokens.AppShapes
import GROUP_ID.core.designsystem.tokens.AppSpacing
import GROUP_ID.core.designsystem.tokens.AppTypography
import GROUP_ID.core.designsystem.tokens.DarkColors
import GROUP_ID.core.designsystem.tokens.LightColors

@Immutable
data class AppTheme(
    val colors: AppColors,
    val typography: AppTypography,
    val shapes: AppShapes,
    val spacing: AppSpacing,
) {
    companion object {
        val LocalAppTheme: ProvidableCompositionLocal<AppTheme> =
            staticCompositionLocalOf { AppTheme.light() }

        fun light(
            colors: AppColors     = LightColors,
            typography: AppTypography = AppTypography(),
            shapes: AppShapes     = AppShapes(),
            spacing: AppSpacing   = AppSpacing(),
        ) = AppTheme(colors, typography, shapes, spacing)

        fun dark(
            colors: AppColors     = DarkColors,
            typography: AppTypography = AppTypography(),
            shapes: AppShapes     = AppShapes(),
            spacing: AppSpacing   = AppSpacing(),
        ) = AppTheme(colors, typography, shapes, spacing)
    }
}

@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    theme: AppTheme = if (darkTheme) AppTheme.dark() else AppTheme.light(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        AppTheme.LocalAppTheme provides theme,
        content = content,
    )
}

// Convenience accessor in Composable scope
val appTheme: AppTheme
    @Composable get() = AppTheme.LocalAppTheme.current
```

---

## Step 4: StyleScope Extensions

These are the **only** correct way to read `CompositionLocal` values inside a `Style`. Styles run outside Composition, so you cannot call `AppTheme.LocalAppTheme.current` directly.

### `theme/StyleScopeExtensions.kt`

```kotlin
package GROUP_ID.core.designsystem.theme

import androidx.compose.foundation.style.StyleScope
import androidx.compose.ui.ExperimentalComposeUiApi
import GROUP_ID.core.designsystem.tokens.AppColors
import GROUP_ID.core.designsystem.tokens.AppShapes
import GROUP_ID.core.designsystem.tokens.AppSpacing
import GROUP_ID.core.designsystem.tokens.AppTypography

// Note: @ExperimentalStylesApi — check actual annotation in your CMP version.
// In CMP 1.11.x this may be @OptIn(ExperimentalStylesApi::class)

val StyleScope.appTheme: AppTheme
    get() = AppTheme.LocalAppTheme.currentValue

val StyleScope.colors: AppColors
    get() = AppTheme.LocalAppTheme.currentValue.colors

val StyleScope.typography: AppTypography
    get() = AppTheme.LocalAppTheme.currentValue.typography

val StyleScope.shapes: AppShapes
    get() = AppTheme.LocalAppTheme.currentValue.shapes

val StyleScope.spacing: AppSpacing
    get() = AppTheme.LocalAppTheme.currentValue.spacing
```

> **Critical rule**: Never capture token values before the Style block:
> ```kotlin
> // ❌ WRONG — stale at creation time
> val color = AppTheme.LocalAppTheme.current.colors.primary
> val myStyle = Style { background(color) }
>
> // ✅ CORRECT — read at consume time via StyleScope extension
> val myStyle = Style { background(colors.primary) }
> ```

---

## Step 5: Variant Systems

### `styles/ButtonStyles.kt`

Mirrors shadcn Button: `default | outline | secondary | ghost | destructive | link`
Plus sizes: `xs | sm | md | lg | icon`

```kotlin
package GROUP_ID.core.designsystem.styles

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.Style
import androidx.compose.ui.unit.dp
import GROUP_ID.core.designsystem.theme.colors
import GROUP_ID.core.designsystem.theme.shapes
import GROUP_ID.core.designsystem.theme.spacing

// ── Interaction atoms (shared across variants) ────────────────────────────────

internal val buttonInteractionStyle = Style {
    hovered  { animate { alpha(0.90f) } }
    pressed  { animate { alpha(0.80f) } }
    disabled { animate { alpha(0.38f) } }
    focused  { animate { borderWidth(2.dp); borderColor(colors.borderFocus) } }
}

// ── Variant styles ─────────────────────────────────────────────────────────────

sealed interface ButtonVariant {
    val style: Style

    data object Default : ButtonVariant {
        override val style = Style {
            background(colors.primary)
            contentColor(colors.onPrimary)
            shape(RoundedCornerShape(shapes.md))
        } then buttonInteractionStyle
    }

    data object Outline : ButtonVariant {
        override val style = Style {
            background(colors.background)
            contentColor(colors.onSurface)
            borderWidth(1.dp)
            borderColor(colors.border)
            shape(RoundedCornerShape(shapes.md))
            hovered { animate { background(colors.secondary) } }
            pressed { animate { background(colors.secondary) } }
        } then buttonInteractionStyle
    }

    data object Secondary : ButtonVariant {
        override val style = Style {
            background(colors.secondary)
            contentColor(colors.onSecondary)
            shape(RoundedCornerShape(shapes.md))
            hovered { animate { background(colors.secondaryHover) } }
        } then buttonInteractionStyle
    }

    data object Ghost : ButtonVariant {
        override val style = Style {
            contentColor(colors.onSurface)
            shape(RoundedCornerShape(shapes.md))
            hovered { animate { background(colors.secondary) } }
            pressed { animate { background(colors.secondary) } }
        } then buttonInteractionStyle
    }

    data object Destructive : ButtonVariant {
        override val style = Style {
            background(colors.destructive)
            contentColor(colors.onDestructive)
            shape(RoundedCornerShape(shapes.md))
            hovered { animate { background(colors.destructiveHover) } }
        } then buttonInteractionStyle
    }

    data object Link : ButtonVariant {
        override val style = Style {
            contentColor(colors.primary)
            hovered { animate { alpha(0.70f) } }
        }
    }
}

// ── Size styles ────────────────────────────────────────────────────────────────

sealed interface ButtonSize {
    val style: Style

    data object Xs : ButtonSize {
        override val style = Style {
            padding(horizontal = spacing.sm, vertical = spacing.xs)
            fontSize(12.sp)
            height(28.dp)
        }
    }

    data object Sm : ButtonSize {
        override val style = Style {
            padding(horizontal = spacing.md, vertical = spacing.xs)
            fontSize(14.sp)
            height(32.dp)
        }
    }

    data object Md : ButtonSize {
        override val style = Style {
            padding(horizontal = spacing.lg, vertical = spacing.sm)
            fontSize(14.sp)
            height(40.dp)
        }
    }

    data object Lg : ButtonSize {
        override val style = Style {
            padding(horizontal = spacing.xl, vertical = spacing.md)
            fontSize(16.sp)
            height(48.dp)
        }
    }

    data object Icon : ButtonSize {
        override val style = Style {
            padding(all = spacing.sm)
            width(40.dp)
            height(40.dp)
        }
    }
}
```

### `styles/BadgeStyles.kt`

Mirrors shadcn Badge: `default | secondary | destructive | outline | ghost`

```kotlin
package GROUP_ID.core.designsystem.styles

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.Style
import androidx.compose.ui.unit.dp
import GROUP_ID.core.designsystem.theme.colors
import GROUP_ID.core.designsystem.theme.shapes
import GROUP_ID.core.designsystem.theme.spacing

sealed interface BadgeVariant {
    val style: Style

    data object Default : BadgeVariant {
        override val style = Style {
            background(colors.primary)
            contentColor(colors.onPrimary)
            shape(RoundedCornerShape(shapes.full))
            padding(horizontal = spacing.sm, vertical = spacing.xxs)
            fontSize(12.sp)
            fontWeight(FontWeight.SemiBold)
        }
    }

    data object Secondary : BadgeVariant {
        override val style = Style {
            background(colors.secondary)
            contentColor(colors.onSecondary)
            shape(RoundedCornerShape(shapes.full))
            padding(horizontal = spacing.sm, vertical = spacing.xxs)
            fontSize(12.sp)
            fontWeight(FontWeight.SemiBold)
        }
    }

    data object Destructive : BadgeVariant {
        override val style = Style {
            background(colors.destructive)
            contentColor(colors.onDestructive)
            shape(RoundedCornerShape(shapes.full))
            padding(horizontal = spacing.sm, vertical = spacing.xxs)
            fontSize(12.sp)
            fontWeight(FontWeight.SemiBold)
        }
    }

    data object Outline : BadgeVariant {
        override val style = Style {
            contentColor(colors.onSurface)
            borderWidth(1.dp)
            borderColor(colors.border)
            shape(RoundedCornerShape(shapes.full))
            padding(horizontal = spacing.sm, vertical = spacing.xxs)
            fontSize(12.sp)
            fontWeight(FontWeight.SemiBold)
        }
    }

    data object Ghost : BadgeVariant {
        override val style = Style {
            background(colors.muted)
            contentColor(colors.onMuted)
            shape(RoundedCornerShape(shapes.full))
            padding(horizontal = spacing.sm, vertical = spacing.xxs)
            fontSize(12.sp)
        }
    }
}
```

### `styles/CardStyles.kt`

```kotlin
package GROUP_ID.core.designsystem.styles

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.Style
import androidx.compose.ui.unit.dp
import GROUP_ID.core.designsystem.theme.colors
import GROUP_ID.core.designsystem.theme.shapes
import GROUP_ID.core.designsystem.theme.spacing

sealed interface CardVariant {
    val style: Style

    data object Default : CardVariant {
        override val style = Style {
            background(colors.surface)
            contentColor(colors.onSurface)
            borderWidth(1.dp)
            borderColor(colors.border)
            shape(RoundedCornerShape(shapes.xxl))
            padding(all = spacing.lg)
        }
    }

    data object Elevated : CardVariant {
        override val style = Style {
            background(colors.surface)
            contentColor(colors.onSurface)
            shape(RoundedCornerShape(shapes.xxl))
            padding(all = spacing.lg)
            // elevation via shadow — add Modifier.shadow in component
        }
    }

    data object Filled : CardVariant {
        override val style = Style {
            background(colors.surfaceVariant)
            contentColor(colors.onSurface)
            shape(RoundedCornerShape(shapes.xxl))
            padding(all = spacing.lg)
        }
    }
}

sealed interface CardSize {
    val contentPadding: androidx.compose.ui.unit.Dp
    val headerSpacing: androidx.compose.ui.unit.Dp

    data object Default : CardSize {
        override val contentPadding = 24.dp
        override val headerSpacing  = 6.dp
    }
    data object Sm : CardSize {
        override val contentPadding = 16.dp
        override val headerSpacing  = 4.dp
    }
}
```

### `styles/ChipStyles.kt`

```kotlin
package GROUP_ID.core.designsystem.styles

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.Style
import androidx.compose.ui.unit.dp
import GROUP_ID.core.designsystem.theme.colors
import GROUP_ID.core.designsystem.theme.shapes
import GROUP_ID.core.designsystem.theme.spacing

sealed interface ChipVariant {
    val style: Style

    data object Default : ChipVariant {
        override val style = Style {
            background(colors.secondary)
            contentColor(colors.onSecondary)
            borderWidth(1.dp)
            borderColor(colors.border)
            shape(RoundedCornerShape(shapes.full))
            padding(horizontal = spacing.md, vertical = spacing.xs)
            fontSize(13.sp)
            hovered { animate { background(colors.secondaryHover) } }
            pressed { animate { background(colors.secondaryHover) } }
        }
    }

    data object Selected : ChipVariant {
        override val style = Style {
            background(colors.primary)
            contentColor(colors.onPrimary)
            shape(RoundedCornerShape(shapes.full))
            padding(horizontal = spacing.md, vertical = spacing.xs)
            fontSize(13.sp)
        }
    }

    data object Outline : ChipVariant {
        override val style = Style {
            borderWidth(1.dp)
            borderColor(colors.border)
            contentColor(colors.onSurface)
            shape(RoundedCornerShape(shapes.full))
            padding(horizontal = spacing.md, vertical = spacing.xs)
            fontSize(13.sp)
            hovered { animate { background(colors.secondary) } }
        }
    }
}
```

### `styles/TextFieldStyles.kt`

```kotlin
package GROUP_ID.core.designsystem.styles

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.Style
import androidx.compose.ui.unit.dp
import GROUP_ID.core.designsystem.theme.colors
import GROUP_ID.core.designsystem.theme.shapes
import GROUP_ID.core.designsystem.theme.spacing

sealed interface TextFieldVariant {
    val style: Style

    data object Default : TextFieldVariant {
        override val style = Style {
            background(colors.background)
            contentColor(colors.onSurface)
            borderWidth(1.dp)
            borderColor(colors.border)
            shape(RoundedCornerShape(shapes.md))
            padding(horizontal = spacing.md, vertical = spacing.sm)
            fontSize(14.sp)
            focused { animate { borderWidth(2.dp); borderColor(colors.borderFocus) } }
            disabled { animate { alpha(0.38f) } }
        }
    }

    data object Filled : TextFieldVariant {
        override val style = Style {
            background(colors.surfaceVariant)
            contentColor(colors.onSurface)
            shape(RoundedCornerShape(topStart = shapes.md, topEnd = shapes.md, bottomStart = 0.dp, bottomEnd = 0.dp))
            padding(horizontal = spacing.md, vertical = spacing.sm)
            fontSize(14.sp)
            focused { animate { borderWidth(2.dp); borderColor(colors.borderFocus) } }
        }
    }

    data object Ghost : TextFieldVariant {
        override val style = Style {
            contentColor(colors.onSurface)
            padding(horizontal = spacing.xs, vertical = spacing.xs)
            fontSize(14.sp)
            focused { animate { borderBottomWidth(1.dp); borderColor(colors.borderFocus) } }
        }
    }
}
```

---

## Step 6: Core Components

### `components/AppButton.kt`

```kotlin
package GROUP_ID.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.style.MutableStyleState
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.styleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import GROUP_ID.core.designsystem.styles.ButtonSize
import GROUP_ID.core.designsystem.styles.ButtonVariant

/**
 * shadcn-inspired AppButton.
 *
 * Usage:
 * ```
 * AppButton(onClick = {}) { Text("Click me") }
 * AppButton(onClick = {}, variant = ButtonVariant.Outline, size = ButtonSize.Sm) { Text("Outline") }
 * AppButton(onClick = {}, variant = ButtonVariant.Destructive) { Text("Delete") }
 * // One-off style override:
 * AppButton(onClick = {}, style = Style { shape(CircleShape) }) { Text("Pill") }
 * ```
 */
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Default,
    size: ButtonSize = ButtonSize.Md,
    style: Style = Style,        // ← empty; DO NOT set a default Style here
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = remember(interactionSource) { MutableStyleState(interactionSource) }

    // Propagate enabled state to StyleState for `disabled {}` blocks
    styleState.enabled = enabled

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,          // no ripple — use Style animations
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            // defaultStyle = variant.style then size.style; override via incoming `style`
            .styleable(styleState, variant.style then size.style, style),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}
```

### `components/AppBadge.kt`

```kotlin
package GROUP_ID.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.MutableStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import GROUP_ID.core.designsystem.styles.BadgeVariant

/**
 * Label/tag component. Maps to shadcn Badge.
 *
 * Usage:
 * ```
 * AppBadge { Text("New") }
 * AppBadge(variant = BadgeVariant.Destructive) { Text("Error") }
 * AppBadge(variant = BadgeVariant.Outline) { Text("Draft") }
 * ```
 */
@Composable
fun AppBadge(
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Default,
    style: Style = Style,
    content: @Composable () -> Unit,
) {
    // Non-interactive — no interaction source needed, use a static StyleState
    val styleState = remember { MutableStyleState() }

    Box(
        modifier = modifier.styleable(styleState, variant.style, style),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
```

### `components/AppCard.kt`

```kotlin
package GROUP_ID.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.style.MutableStyleState
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.styleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import GROUP_ID.core.designsystem.styles.CardSize
import GROUP_ID.core.designsystem.styles.CardVariant

/**
 * Maps to shadcn Card with slots: header, content, footer.
 *
 * Usage:
 * ```
 * AppCard(
 *     header = { CardHeader(title = "Title", description = "Subtitle") },
 *     footer = { AppButton(onClick = {}) { Text("Action") } }
 * ) {
 *     Text("Card body content")
 * }
 * ```
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.Default,
    size: CardSize = CardSize.Default,
    style: Style = Style,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val styleState = remember { MutableStyleState() }

    Column(
        modifier = modifier.styleable(styleState, variant.style, style),
    ) {
        if (header != null) {
            header()
            Spacer(Modifier.height(size.headerSpacing))
        }
        content()
        if (footer != null) {
            Spacer(Modifier.height(size.headerSpacing))
            footer()
        }
    }
}

@Composable
fun CardHeader(
    title: String,
    description: String? = null,
    action: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column {
            AppText(text = title, style = TextStyle.TitleSmall)
            if (description != null) {
                Spacer(Modifier.height(4.dp))
                AppText(text = description, style = TextStyle.BodySmall, muted = true)
            }
        }
        if (action != null) {
            Box(modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd)) {
                action()
            }
        }
    }
}
```

### `components/AppChip.kt`

```kotlin
package GROUP_ID.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.style.MutableStyleState
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.styleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import GROUP_ID.core.designsystem.styles.ChipVariant

/**
 * Selectable chip / filter tag.
 *
 * Usage:
 * ```
 * AppChip(label = "Kotlin", selected = true, onClick = { toggle() })
 * AppChip(label = "Swift", variant = ChipVariant.Outline, onClick = {})
 * ```
 */
@Composable
fun AppChip(
    label: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    variant: ChipVariant = if (selected) ChipVariant.Selected else ChipVariant.Default,
    style: Style = Style,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = remember(interactionSource) { MutableStyleState(interactionSource) }
    styleState.enabled = enabled

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
    } else Modifier

    Row(
        modifier = modifier
            .then(clickableModifier)
            .styleable(styleState, variant.style, style),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
    ) {
        AppText(text = label)
    }
}
```

### `components/AppText.kt`

```kotlin
package GROUP_ID.core.designsystem.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.style.TextOverflow
import GROUP_ID.core.designsystem.theme.appTheme

enum class TextStyle {
    DisplayLarge, DisplayMedium,
    TitleLarge, TitleMedium, TitleSmall,
    BodyLarge, BodyMedium, BodySmall,
    LabelLarge, LabelSmall,
}

/**
 * Usage:
 * ```
 * AppText("Hello world")
 * AppText("Title", style = TextStyle.TitleLarge)
 * AppText("Subtitle", style = TextStyle.BodySmall, muted = true)
 * ```
 */
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.BodyMedium,
    muted: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    color: Color = Color.Unspecified,
) {
    val theme = appTheme
    val resolvedStyle = when (style) {
        TextStyle.DisplayLarge  -> theme.typography.displayLarge
        TextStyle.DisplayMedium -> theme.typography.displayMedium
        TextStyle.TitleLarge    -> theme.typography.titleLarge
        TextStyle.TitleMedium   -> theme.typography.titleMedium
        TextStyle.TitleSmall    -> theme.typography.titleSmall
        TextStyle.BodyLarge     -> theme.typography.bodyLarge
        TextStyle.BodyMedium    -> theme.typography.bodyMedium
        TextStyle.BodySmall     -> theme.typography.bodySmall
        TextStyle.LabelLarge    -> theme.typography.labelLarge
        TextStyle.LabelSmall    -> theme.typography.labelSmall
    }

    val textColor = when {
        color != Color.Unspecified -> color
        muted                       -> theme.colors.onSurfaceVariant
        else                        -> theme.colors.onSurface
    }

    BasicText(
        text = text,
        modifier = modifier,
        style = resolvedStyle.copy(color = textColor),
        maxLines = maxLines,
        overflow = overflow,
    )
}
```

---

## Step 7: Wire AppTheme in platform entry points

### Android — `androidApp/src/main/kotlin/.../MainActivity.kt`

```kotlin
setContent {
    AppTheme(darkTheme = isSystemInDarkTheme()) {
        AppNavHost()
    }
}
```

### Desktop — `desktopApp/src/jvmMain/kotlin/main.kt`

```kotlin
application {
    Window(onCloseRequest = ::exitApplication, title = "App") {
        AppTheme(darkTheme = false) {
            AppNavHost()
        }
    }
}
```

### iOS — `shared/src/iosMain/kotlin/AppView.kt`

```kotlin
@Composable
fun AppView() {
    AppTheme {
        AppNavHost()
    }
}
```

---

## Step 8: Usage patterns

### Basic usage

```kotlin
// Default button
AppButton(onClick = { /* ... */ }) {
    AppText("Save")
}

// Variant + size
AppButton(
    onClick = { /* ... */ },
    variant = ButtonVariant.Outline,
    size = ButtonSize.Sm,
) {
    AppText("Cancel")
}

// Destructive with icon
AppButton(
    onClick = { deleteItem() },
    variant = ButtonVariant.Destructive,
) {
    Icon(Icons.Default.Delete, contentDescription = null)
    Spacer(Modifier.width(4.dp))
    AppText("Delete")
}
```

### One-off style override (escape hatch)

```kotlin
// Override just the corner radius on this specific instance
AppButton(
    onClick = {},
    style = Style { shape(CircleShape) },
) {
    AppText("Pill button")
}
```

### Style composition for custom variants

```kotlin
// Compose multiple styles — reuse without touching the design system
val accentButtonStyle = ButtonVariant.Default.style then Style {
    background(Color(0xFF7C3AED))   // brand purple
    contentColor(Color.White)
}

AppButton(onClick = {}, style = accentButtonStyle) {
    AppText("Accent")
}
```

### Card composition (shadcn-style slots)

```kotlin
AppCard(
    variant = CardVariant.Default,
    size = CardSize.Sm,
    header = {
        CardHeader(
            title = "Account",
            description = "Manage your account settings",
            action = { AppBadge(variant = BadgeVariant.Secondary) { AppText("Pro") } },
        )
    },
    footer = {
        Row(horizontalArrangement = Arrangement.End) {
            AppButton(onClick = {}, variant = ButtonVariant.Ghost, size = ButtonSize.Sm) { AppText("Cancel") }
            Spacer(Modifier.width(8.dp))
            AppButton(onClick = {}) { AppText("Save") }
        }
    },
) {
    AppText("Card body content here.")
}
```

### Chips as filter group

```kotlin
val tags = listOf("Kotlin", "Swift", "Rust")
var selected by remember { mutableStateOf(setOf("Kotlin")) }

Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    tags.forEach { tag ->
        AppChip(
            label = tag,
            selected = tag in selected,
            onClick = { selected = if (tag in selected) selected - tag else selected + tag },
        )
    }
}
```

---

## Step 9: Add to `libs.versions.toml` (no extra deps needed)

The design system uses only:
- `compose.foundation` — `BasicText`, `BasicTextField`, Modifier APIs
- `compose.runtime` — CompositionLocal
- `compose.ui` — Modifier, Color, Dp, TextStyle

All of these are already in `compose-multiplatform`. No new catalog entries required.

---

## Guidelines

- **Never capture CompositionLocal in a Style lambda** — use `StyleScope` extensions (see Step 4)
- **Never set a default Style in a component parameter** — always pass `Style` (empty) and merge defaults inside `Modifier.styleable()`
- **You own this code** — the skill scaffolds a starting point; customize tokens and add variants freely
- **`@OptIn(ExperimentalStylesApi::class)`** required on every file using the Styles API; add to each component/style file
- **`indication = null`** on all clickable components — let Style `pressed {}` / `hovered {}` blocks handle visual feedback
- **Infinite animations** are not supported in Styles — use `rememberInfiniteTransition()` in the component body instead
- **Disabled state**: set `styleState.enabled = enabled` after creating `MutableStyleState`
- **Dark mode**: swap `AppTheme.dark()` vs `AppTheme.light()` at the entry point; all Styles pick up correct tokens automatically via `StyleScope` extensions

---

## Verification

1. `./gradlew :core:designsystem:compileCommonMainKotlinMetadata` — tokens and styles compile in commonMain
2. `./gradlew :androidApp:assembleDevDebug` — AppTheme, AppButton, AppBadge, AppCard render
3. `./gradlew :desktopApp:run` — Desktop renders with same tokens
4. Toggle `darkTheme = true` in entry point — all component colors update correctly
5. Hover a button on Desktop — verify `hovered {}` style animation fires (JVM only)
6. Set `enabled = false` on `AppButton` — verify `disabled { alpha(0.38f) }` applies
7. Call `./gradlew :core:designsystem:jsTest` and `:wasmJsTest` — web targets compile clean

---

## Testing

```kotlin
// Design system testing is primarily visual — Roborazzi screenshot pairs (light + dark)
// for every token category and component, plus interaction tests for interactive tokens.

@Test fun `color_tokens_light screenshot`() {
    captureRoboImage("ds_color_tokens_light.png") {
        AppTheme(darkTheme = false) {
            Column(modifier = Modifier.padding(AppTheme.spacing.lg)) {
                Box(Modifier.size(48.dp).background(AppTheme.colors.primary))
                Box(Modifier.size(48.dp).background(AppTheme.colors.secondary))
                Box(Modifier.size(48.dp).background(AppTheme.colors.surface))
                Box(Modifier.size(48.dp).background(AppTheme.colors.error))
            }
        }
    }
}

@Test fun `color_tokens_dark screenshot`() {
    captureRoboImage("ds_color_tokens_dark.png") {
        AppTheme(darkTheme = true) {
            Column(modifier = Modifier.padding(AppTheme.spacing.lg)) {
                Box(Modifier.size(48.dp).background(AppTheme.colors.primary))
                Box(Modifier.size(48.dp).background(AppTheme.colors.secondary))
                Box(Modifier.size(48.dp).background(AppTheme.colors.surface))
                Box(Modifier.size(48.dp).background(AppTheme.colors.error))
            }
        }
    }
}

@Test fun `typography_scale screenshot`() {
    captureRoboImage("ds_typography_scale.png") {
        AppTheme {
            Column(modifier = Modifier.padding(AppTheme.spacing.lg)) {
                Text("Display Large", style = AppTheme.typography.displayLarge)
                Text("Headline Medium", style = AppTheme.typography.headlineMedium)
                Text("Body Large", style = AppTheme.typography.bodyLarge)
                Text("Label Small", style = AppTheme.typography.labelSmall)
            }
        }
    }
}

@Test fun `spacing tokens match expected dp values`() {
    // Assert the compile-time constants — catches accidental token renames
    assertEquals(16.dp, AppTheme.spacing.lg)
    assertEquals(8.dp, AppTheme.spacing.sm)
    assertEquals(4.dp, AppTheme.spacing.xs)
}
```

---

## Common Anti-Patterns

- magic color literals in composables — `Color(0xFF6200EE)` written directly inside a `@Composable` instead of `AppTheme.colors.primary`; the audit script flags `Color(0x…)` in any `/ui/` or `/presentation/` file that is not a token definition file
- hardcoded spacing in composables — `padding(16.dp)` or `padding(horizontal = 8.dp)` written directly instead of `padding(horizontal = AppTheme.spacing.lg)`; the audit script flags `.dp` literals inside `padding(…)` calls in UI files
- title text in content body — a `Text("Screen Title")` composable inside the content column when it should be `AppTopAppBar(title = "Screen Title")`; makes the title scroll away and duplicates chrome
- action buttons outside the TopAppBar — a "Save" `AppButton` at the bottom of a form when it belongs in `AppTopAppBar(actions = { … })`; creates two interaction points for the same operation
- not consuming `PaddingValues` from `AppScaffold` — `AppScaffold { MyContent() }` without `Modifier.padding(paddingValues)` clips the content under the TopAppBar on status-bar devices
- using Material3 `MaterialTheme.colorScheme` alongside `AppTheme` — the two token systems conflict
- defining component variants as boolean parameters (`isOutlined`, `isDanger`) — use a sealed variant class
- putting design system tokens in `:feature:*` modules — tokens belong in `:core:designsystem` only
- skipping the `StyleScope` extension layer — leads to token access scattered across composables

If the design system feels inconsistent, check: (1) are all pages using `AppScaffold` + `AppTopAppBar`? (2) are spacing and colors coming from tokens or from hardcoded literals? (3) is there duplicated chrome (title, actions) in the content body?

---

## Related Skills

- `kotlin-multiplatform-feature-scaffold` — `:core:designsystem` follows the same convention plugin pattern
- `kotlin-multiplatform-design-system-extended` — additional components (`AppDialog`, `AppToast`, `AppTabs`, etc.) built on this foundation
- `kotlin-multiplatform-shared-resources` — fonts and icons loaded via `Res` accessors inside the design system
- `kotlin-multiplatform-preview-driven-development` — Desktop previews for each component variant using `PreviewParameterProvider`

---

## Output Style

When asked about design system setup or components, respond in this order:
1. recommendation (default token/component approach)
2. project structure (`:core:designsystem` layout)
3. code snippet (smallest useful token or component)
4. why that approach is preferred (no Material, full ownership)
5. main alternative (Material3 wrapper)

Keep snippets small. Use the user's package name and token names when provided.
