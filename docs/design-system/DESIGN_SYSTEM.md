# BidHub Design System

**Version:** 1.0  
**Primary Hue:** `#068D9D`  
**Last Updated:** 2024

A compact, production-ready design system built for modern Android apps that follows Material Design 3, Android accessibility standards, and HCI best practices.

---

## Table of Contents

1. [Color System](#1-color-system)
2. [Typography](#2-typography)
3. [Spacing & Layout](#3-spacing--layout-rules)
4. [Component Library](#4-component-library)
5. [Iconography & Imagery](#5-iconography--imagery)
6. [Interaction & Motion](#6-interaction--motion)
7. [Accessibility](#7-accessibility-requirements)
8. [Dark Mode](#8-dark-mode)
9. [Implementation Guide](#9-implementation-guide)

---

## 1. Color System

### Core Tokens

| Token             | Hex       | Use                                                                           |
| ----------------- | --------- | ----------------------------------------------------------------------------- |
| `primary`         | `#068D9D` | Brand accent, interactive controls (default)                                  |
| `primary-variant` | `#057F8D` | Darker primary — used for on-primary contrast-critical text & elevated states |
| `on-primary`     | `#FFFFFF` | Text/iconography on primary surfaces (use primary-variant for small text)   |
| `primary-light`   | `#77D7DE` | Light tint for subtle highlights and backgrounds                             |

### Secondary & Tertiary

| Token               | Hex       | Use / on-color                                                      |
| ------------------- | --------- | ------------------------------------------------------------------- |
| `secondary`         | `#FF7A59` | Accent alternative (use `on-secondary = #000000` for best contrast) |
| `secondary-variant` | `#E06145` | Darker secondary                                                    |
| `tertiary`          | `#7C4DFF` | Accent for labels, tags (use `on-tertiary = #FFFFFF`)               |

### Semantic Colors

| Role      | Hex       | Use                                    |
| --------- | --------- | -------------------------------------- |
| `success` | `#2E7D32` | Success states, positive badges        |
| `warning` | `#F9A825` | Warnings / cautions                    |
| `error`   | `#D32F2F` | Validation errors, destructive actions |
| `info`    | `#0288D1` | Informational highlights               |

### Neutral Scale (10-step)

| Token         | Hex       | Use                                        |
| ------------- | --------- | ------------------------------------------ |
| `neutral-0`   | `#FFFFFF` | App background (light)                     |
| `neutral-50`  | `#FAFBFB` | Page surfaces                              |
| `neutral-100` | `#F2F6F6` | Cards, subtle surfaces                     |
| `neutral-200` | `#E6EEEE` | Elevated surface shade                     |
| `neutral-300` | `#D1DEDE` | Dividers, borders                          |
| `neutral-400` | `#9FB1B1` | Secondary text                             |
| `neutral-500` | `#6D8A8A` | Body text (muted)                          |
| `neutral-700` | `#3A5A5A` | Primary body text (dark)                   |
| `neutral-900` | `#0B2222` | App background (dark) / high-contrast text  |

### Elevation Tonal Mapping (Light Theme)

- Surface (0dp): `neutral-0`
- Surface (1dp): `neutral-100`
- Surface (2dp): `neutral-200`
- Surface (3dp+): progressively darker neutrals as needed; keep shadow subtle.

### Component States

- **Hover**: overlay `primary` with `8%` black on light surfaces, or increase elevation by 2dp.
- **Focus**: 2px outline using `primary` at 60% opacity (or `primary-light` glow for dark backgrounds).
- **Pressed**: overlay `primary` with `16%` black (or `primary-variant` fill for tactile feedback).
- **Disabled**: `alpha = 38%` on text/icon; disabled surface: `neutral-100` with `disabled-content` alpha 38%.

### Contrast & WCAG Notes

- `#068D9D` on white contrast ratio ≈ **3.96:1** — **insufficient** for normal body text (WCAG AA requires 4.5:1). Use `primary-variant (#057F8D)` where white-on-primary must meet at least **4.5:1** for small text; `primary-variant` contrast on white ≈ **4.74:1** (passes).
- `#068D9D` on black contrast ≈ **5.30:1** — acceptable for body text on dark backgrounds.

**Guideline:** For filled buttons or icons on `primary` surfaces:
- Use `on-primary = #FFFFFF` **only** for large/button text (>= 14sp bold) or when using `primary-variant` for smaller labels.
- Wherever small white text is required on primary, use `primary-variant` as the background or increase size/weight.

---

## 2. Typography

**Font families**: Use Roboto (system) or Google Sans for brand — Roboto is default on Android. Provide fallback: `sans-serif`.

### Scale (sp)

| Style            | Size | Weight | Line Height | Use                          |
| ---------------- | ---: | -----: | ----------: | ---------------------------- |
| Display / Hero   | 34sp |    700 |        40sp | App homepage hero            |
| H1               | 28sp |    600 |        36sp | Page titles                  |
| H2               | 24sp |    600 |        32sp | Section headings             |
| H3               | 20sp |    500 |        28sp | Subsection headings          |
| Body 1           | 16sp |    400 |        24sp | Main body text               |
| Body 2           | 14sp |    400 |        20sp | Secondary body               |
| Button           | 14sp |    500 |        20sp | Buttons (uppercase optional) |
| Caption          | 12sp |    400 |        16sp | Tertiary info                |
| Overline / Label | 10sp |    500 |        14sp | Small labels, chips          |

### Accessibility Adjustments

- Use dynamic type (respect `fontScale` accessibility setting).
- Minimum readable body: 14sp for standard content; allow user to opt into larger sizes.
- Use 1.25–1.5x line height for long-form readability on mobile.

### Implementation

- **XML**: Use `TextAppearance.BidHub.*` styles defined in `styles.xml`
- **Compose**: Define `Typography` object with matching `TextStyle` values

---

## 3. Spacing & Layout Rules

### Spacing Scale (dp)

`4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 / 64`

### Grid & Container Rules

- **Baseline grid**: 4dp increments.
- **Container widths**: full-bleed mobile (0dp — edge to edge with safe insets), interior content padding default `16dp` (small screens), `24dp` for tablet/large screens.
- **Columns**: Mobile single-column layout with a 16dp gutter. For larger views use a responsive 2–4 column grid.

### Standard Paddings & Gaps

- App screen content padding: `16dp` left/right, `12dp` vertical between major sections.
- Card internal padding: `16dp` (compact: `12dp`).
- List item spacing: vertical 8–12dp, horizontal padding 16dp.

### Alignment Rules

- Left-aligned primary content. Center-align only for short content, hero text, or empty-state illustrations.
- Use consistent visual rhythm — apply spacers in multiples of the spacing scale.

---

## 4. Component Library

All components follow Material principles: elevation, clear affordances, consistent spacing.

### Buttons

**Filled (Primary Button)**
- Background: `primary` (`#068D9D`) or `primary-variant` for small text
- Text: `on-primary = #FFFFFF` (14sp medium)
- Height: `48dp` (min)
- Corner radius: `12dp`
- Elevation: 2dp default, 6dp pressed
- Disabled: `alpha 38%` on text + reduced elevation

**Outlined Button**
- Border: `1dp` `primary` (or `neutral-300` on subtle contexts)
- Background: transparent; text `primary`

**Text Button**
- Text only, min touch target `48dp`, provide ripple feedback

### App Bars & Navigation Bars

- App bar height: `56dp` (top app bar), center on large titles optionally.
- Colors: `surface` background with `neutral-700`/`neutral-900` text.
- Navigation bar (bottom): `56dp`, icons: 24dp.

### Tabs

- Indicator: `4dp` high, `primary` color
- Label: 14sp medium, selected `primary`, unselected `neutral-500`

### Cards

- Surface: `neutral-0` or `neutral-100`
- Radius: `12dp`
- Padding: `16dp`
- Elevation: 1–3dp depending on importance

### Form Fields

- Height: `56dp` (text field)
- Label: floating label pattern (12sp when shrunk)
- Border: `1dp` neutral, focus state border `2dp` primary
- Error text: `error` color 12sp below field
- Checkbox / Switch: use `primary` for checked state, `neutral-300` for unchecked

### Modals & Bottom Sheets

- Modal max width: 560dp center on tablets, full-width on phones
- Corner radius: `16dp` on top for bottom sheets
- Background overlay: `rgba(0,0,0,0.32)`
- Dismissal affordance: visible drag handle

### Toasts, Banners, Notifications

- Toast: `neutral-900` text on `neutral-50` background with subtle elevation, autodismiss 2–3s
- Banner: full-width, message + primary action button; use `info` or `warning` tone as needed

### Lists & Item Rows

- Row min height: `48dp`
- Leading icon: 40dp square (content 24dp centered)
- Trailing actions: 40dp touch targets
- Dividers: `neutral-300` 1dp

---

## 5. Iconography & Imagery

### Icon Style

- Style: rounded 2px stroke for line icons; filled versions for emphasis.
- Standard sizes: 24dp (primary), 20dp (secondary), 16dp (tiny)
- Padding: center icons in a 40dp touch area with 8dp internal padding.

### Imagery

- Image aspect ratios: cards `16:9`, thumbnails `1:1`, list images `3:2`.
- Corner radius: images follow card radii — default `12dp`; avatars `50%` circle.
- Photo treatment: always provide an overlay or gradient when placing text on top; ensure foreground text meets contrast.

### Asset Formats

- Use vector drawables (SVG/VectorAsset) for icons; use WebP/AVIF for photos where possible for compression.

---

## 6. Interaction & Motion

### Motion Tokens

| Token           | Duration | Use                                                |
| --------------- | -------: | -------------------------------------------------- |
| `motion-fast`   |    100ms | Quick feedback (ripple, small fades)               |
| `motion-medium` |    200ms | Standard transitions (navigation, expand/collapse) |
| `motion-slow`   |    300ms | Floating motion, modal enter/exit                  |

### Easing Curves

- Standard: `FastOutSlowIn` (CubicBezier(0.4, 0.0, 0.2, 1)) — primary
- Deceleration: `LinearOutSlowIn` (CubicBezier(0, 0, 0.2, 1)) — exit animations
- Acceleration: `FastOutLinearIn` (CubicBezier(0.4, 0, 1, 1)) — entrance animations

### Interaction States

- **Ripple**: bounded ripple on buttons and list items using `rippleColor = onSurface at 12% alpha`.
- **Loading**: show an indeterminate circular progress (24dp) inside the button or full-width overlay. Use `primary` for progress indicator.
- **Disabled**: show reduced opacity and remove elevation and click handlers.

### Navigation

- Use semantic, shallow navigation: top-level destinations via bottom nav or top tabs; deep links map to unique screens.
- Use shared element transitions sparingly and only for meaningful continuity (image -> detail view).

---

## 7. Accessibility Requirements

- **Contrast**: meet WCAG AA: 4.5:1 for normal text, 3:1 for large text (≥ 18.66sp regular or 14sp bold). Prefer contrast >= 7:1 for critical text.
- **Touch targets**: minimum `48dp` × `48dp` tappable area.
- **Focus indicators**: 2dp outline of `primary` (or `primary-light`) for keyboard / accessibility focus.
- **Text scaling**: support system font-size scaling and ensure layouts adapt without clipping.
- **Color blindness**: do not rely on hue alone — pair colors with icons/labels.
- **Motion reduction**: honor `reduce motion` setting — provide minimal or shorter animations.
- **Labels & hints**: provide content descriptions for icons and images; implement `android:hint` and error messages for form fields.
- **Dark mode**: provide alternate neutral palette (see below) and ensure semantic colors still meet contrast.

---

## 8. Dark Mode

### Dark Mode Quick Tokens

- `surface` -> `neutral-900` (`#0B2222`)
- `on-surface` -> `neutral-50` (`#FAFBFB`)
- `primary` -> `#77D7DE` (lighter primary used for emphasis)
- `primary-variant` -> `#068D9D` (use as accent over dark surfaces)

---

## 9. Implementation Guide

### Quick Adoption Steps

1. **Add color tokens** to `colors.xml` and Compose `Color` palette.
2. **Define `Typography` tokens** in `styles.xml` and Compose `Typography`.
3. **Replace hard-coded values** in components with tokens: colors, spacing, radii.
4. **Run an accessibility contrast audit** (tools: Android Studio Accessibility Scanner) and adjust small-text uses of `primary` to `primary-variant` where needed.

### Android XML Implementation

All color tokens are defined in `res/values/colors.xml`.  
All typography styles are defined in `res/values/styles.xml`.  
Theme is configured in `res/values/themes.xml`.

### Component Usage Examples

**Button (XML):**
```xml
<com.google.android.material.button.MaterialButton
    style="@style/Widget.BidHub.Button.Primary"
    android:text="Primary Button" />
```

**Text Input (XML):**
```xml
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.BidHub.TextInputLayout"
    android:hint="Email">
    <com.google.android.material.textfield.TextInputEditText
        android:inputType="textEmailAddress" />
</com.google.android.material.textfield.TextInputLayout>
```

**Card (XML):**
```xml
<com.google.android.material.card.MaterialCardView
    style="@style/Widget.BidHub.Card"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
    <!-- Card content -->
</com.google.android.material.card.MaterialCardView>
```

### Jetpack Compose Implementation

```kotlin
private val LightColorPalette = lightColors(
    primary = Color(0xFF068D9D),
    primaryVariant = Color(0xFF057F8D),
    secondary = Color(0xFFFF7A59),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF2F6F6),
    onPrimary = Color.White,
    onBackground = Color(0xFF3A5A5A)
)

val AppTypography = Typography(
    h1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp),
    body1 = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
    button = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
)
```

---

## Appendix — Quick Token Reference

**Primary**: `#068D9D` | **Primary-Variant**: `#057F8D` | **Secondary**: `#FF7A59` | **Success**: `#2E7D32` | **Error**: `#D32F2F`

**Neutrals**: `#FFFFFF`, `#FAFBFB`, `#F2F6F6`, `#D1DEDE`, `#6D8A8A`, `#3A5A5A`, `#0B2222`

**Motion**: `100ms / 200ms / 300ms` with `FastOutSlowIn`

**Spacing**: `4 / 8 / 12 / 16 / 24 / 32 / 40 / 48`

---

## Resources

- **Color Tokens**: `bidhub/app/src/main/res/values/colors.xml`
- **Typography Styles**: `bidhub/app/src/main/res/values/styles.xml`
- **Theme Configuration**: `bidhub/app/src/main/res/values/themes.xml`
- **Spacing Dimensions**: `bidhub/app/src/main/res/values/dimens.xml`

---

**Last Updated**: 2024  
**Maintained by**: BidHub Development Team

