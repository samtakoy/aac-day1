# Theme Module

**Package:** `com.example.day.core.ui.theme`  
**Module:** `:app`  
**Type:** Android UI Theme

Material 3 theming for the Day application.

## Overview

The Theme module provides:
- Material 3 color schemes (light/dark)
- Typography definitions
- Dynamic color support (Android 12+)

## Purpose

The Theme module defines the **visual identity** of the application using Material Design 3. It provides:
- Consistent color palette across light and dark modes
- Typography scale for readable text
- Support for Android 12+ dynamic colors (wallpaper-based theming)

## Who Uses This Module

| Consumer | Purpose |
|----------|---------|
| `MainActivity` | Wraps content in `Day1Theme` |
| All Compose UI | Accesses colors via `MaterialTheme.colorScheme` |
| All Compose UI | Accesses typography via `MaterialTheme.typography` |
| UIKit Components | Use theme colors for bubbles and bars |

## Theme Configuration

```kotlin
Day1Theme(
    darkTheme: Boolean = true,        // Default to dark mode
    dynamicColor: Boolean = false,    // Disable by default
    content: @Composable () -> Unit
)
```

### Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `darkTheme` | `true` | Whether to use dark color scheme |
| `dynamicColor` | `false` | Whether to use Android 12+ wallpaper colors |

### Dynamic Color

When `dynamicColor = true` on Android 12+:
- Primary color extracted from user's wallpaper
- Provides personalized, Material You experience
- Falls back to static colors on older devices or if system disables it

## Color System

### Primary Colors

| Mode | Color | Usage |
|------|-------|-------|
| Dark | `Purple80` (#D0BCFF) | Primary buttons, active states |
| Light | `Purple40` (#6650a4) | Primary buttons, active states |

### Secondary Colors

| Mode | Color | Usage |
|------|-------|-------|
| Dark | `PurpleGrey80` (#CCC2DC) | Secondary actions |
| Light | `PurpleGrey40` (#625b71) | Secondary actions |

### Tertiary Colors

| Mode | Color | Usage |
|------|-------|-------|
| Dark | `Pink80` (#EFB8C8) | Accents, highlights |
| Light | `Pink40` (#7D5260) | Accents, highlights |

## Key Classes

### [`Theme.kt`](app/src/main/java/com/example/day/core/ui/theme/Theme.kt)

Main theme composable.

```kotlin
@Composable
fun Day1Theme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
)
```

**Parameters:**
- `darkTheme` - Use dark color scheme (default: `true`)
- `dynamicColor` - Use dynamic color from system (Android 12+, default: `false`)

**Usage:**
```kotlin
Day1Theme(darkTheme = true) {
    // App content
}
```

### [`Color.kt`](app/src/main/java/com/example/day/core/ui/theme/Color.kt)

Color palette definitions.

**Dark Theme Colors:**
| Color | Hex | Usage |
|-------|-----|-------|
| Purple80 | `#D0BCFF` | Primary |
| PurpleGrey80 | `#CCC2DC` | Secondary |
| Pink80 | `#EFB8C8` | Tertiary |

**Light Theme Colors:**
| Color | Hex | Usage |
|-------|-----|-------|
| Purple40 | `#6650a4` | Primary |
| PurpleGrey40 | `#625b71` | Secondary |
| Pink40 | `#7D5260` | Tertiary |

### [`Type.kt`](app/src/main/java/com/example/day/core/ui/theme/Type.kt)

Typography definitions.

Default styles:
- `bodyLarge` - Default body text (16sp, normal weight)

## Color Scheme

### Dark Color Scheme

```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)
```

### Light Color Scheme

```kotlin
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)
```

## Dynamic Color

When `dynamicColor = true` and running on Android 12+:
- `dynamicDarkColorScheme(context)` for dark theme
- `dynamicLightColorScheme(context)` for light theme

## Module Structure

```
core/ui/theme/
├── Theme.kt    # Main theme composable
├── Color.kt   # Color definitions
└── Type.kt    # Typography definitions
```
