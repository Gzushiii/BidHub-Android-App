# BidHub UI/UX Design System

**Version:** 1.0.0  
**Last Updated:** 2025-01-27  
**Status:** Active Implementation

---

## Table of Contents

1. [Design Principles](#1-design-principles)
2. [Color System](#2-color-system)
3. [Typography](#3-typography)
4. [Spacing & Layout](#4-spacing--layout)
5. [Components](#5-components)
6. [Accessibility Guidelines](#6-accessibility-guidelines)
7. [Responsive Design](#7-responsive-design)
8. [Interaction Patterns](#8-interaction-patterns)
9. [Implementation Guidelines](#9-implementation-guidelines)

---

## 1. Design Principles

### 1.1 Core Principles

- **Clarity First**: Information hierarchy guides user attention naturally
- **Consistency**: Unified patterns across all screens reduce cognitive load
- **Accessibility**: WCAG 2.1 AA compliance for inclusive design
- **Performance**: Optimized layouts minimize layout shifts and loading times
- **Progressive Disclosure**: Show essential information first, details on demand

### 1.2 Material Design 3 Alignment

- Follow Material Design 3 component guidelines
- Use Material 3 color system and elevation
- Implement proper motion and transitions
- Maintain platform-specific patterns (Android)

---

## 2. Color System

### 2.1 Primary Palette

| Color | Hex | Usage |
|-------|-----|-------|
| Primary | `#00695C` | Main brand color, primary actions |
| Primary Light | `#4DB6AC` | Hover states, secondary elements |
| Primary Dark | `#004D40` | Pressed states, emphasis |
| Primary Container | `#E0F2F1` | Backgrounds, cards |
| On Primary | `#FFFFFF` | Text on primary backgrounds |

### 2.2 Semantic Colors

| Color | Hex | Usage |
|-------|-----|-------|
| Success | `#2E7D32` | Success messages, completed states |
| Warning | `#F57C00` | Warnings, countdown timers |
| Error | `#D32F2F` | Errors, critical actions |
| Info | `#1976D2` | Informational messages |

### 2.3 Text Colors

| Color | Hex | Usage | Contrast Ratio |
|-------|-----|-------|---------------|
| Text Primary | `#212121` | Main content | 15.8:1 (AAA) |
| Text Secondary | `#424242` | Supporting text | 12.6:1 (AAA) |
| Text Hint | `#757575` | Placeholders, hints | 7.0:1 (AA) |
| Text Disabled | `#BDBDBD` | Disabled states | 4.5:1 (AA) |

### 2.4 Background Colors

| Color | Hex | Usage |
|-------|-----|-------|
| Background | `#FFFFFF` | Main app background |
| Surface | `#FFFFFF` | Card backgrounds |
| Surface Variant | `#F5F5F5` | Secondary surfaces |
| Surface Container | `#F0F0F0` | Grouped content |

### 2.5 Contrast Requirements

- **Text on Background**: Minimum 4.5:1 (AA), target 7:1 (AAA)
- **Large Text (18sp+)**: Minimum 3:1 (AA)
- **Interactive Elements**: Minimum 3:1 contrast with background
- **Focus Indicators**: Minimum 3:1 contrast

---

## 3. Typography

### 3.1 Type Scale

| Style | Size | Weight | Line Height | Usage |
|-------|------|--------|-------------|-------|
| Large Title | 28sp | Bold | 36sp | Hero sections, page titles |
| Title | 22sp | Bold | 28sp | Section headers |
| Headline | 18sp | Bold | 24sp | Card titles, subsections |
| Body | 16sp | Regular | 24sp | Main content, descriptions |
| Caption | 14sp | Regular | 20sp | Metadata, labels |
| Small | 12sp | Regular | 16sp | Helper text, timestamps |

### 3.2 Special Typography

| Style | Size | Weight | Color | Usage |
|-------|------|--------|-------|-------|
| Bid Amount | 20sp | Bold | Primary | Price displays |
| Credit Balance | 18sp | Bold | Credit Gold | Credit displays |
| Countdown Timer | 16sp | Bold | Warning | Time remaining |

### 3.3 Typography Rules

- **Line Height**: 1.5x font size for body text, 1.2x for headings
- **Letter Spacing**: Default for body, -0.5sp for large headings
- **Text Alignment**: Left-aligned for readability (except centered CTAs)
- **Text Truncation**: Use `ellipsize="end"` with `maxLines` for long text

---

## 4. Spacing & Layout

### 4.1 Spacing Scale (4dp Grid)

| Token | Value | Usage |
|-------|-------|-------|
| spacing_0 | 0dp | No spacing |
| spacing_1 | 4dp | Tight spacing, related elements |
| spacing_2 | 8dp | Standard spacing, form fields |
| spacing_3 | 12dp | Medium spacing, card padding |
| spacing_4 | 16dp | Large spacing, section padding |
| spacing_5 | 20dp | Extra spacing |
| spacing_6 | 24dp | Section spacing |
| spacing_8 | 32dp | Major section spacing |
| spacing_10 | 40dp | Screen margins |
| spacing_12 | 48dp | Large screen margins |

### 4.2 Component Dimensions

| Component | Height | Corner Radius | Padding |
|-----------|--------|---------------|---------|
| Button Primary | 48dp | 8dp | 16dp horizontal, 12dp vertical |
| Button Secondary | 40dp | 8dp | 12dp horizontal, 8dp vertical |
| Text Input | 56dp | 8dp | 16dp horizontal, 12dp vertical |
| Card | Auto | 12dp | 16dp |
| Chip | 32dp | 16dp | 12dp horizontal, 6dp vertical |

### 4.3 Layout Guidelines

- **Screen Margins**: 16dp on mobile, 24dp on tablets
- **Content Padding**: 16dp standard, 24dp for major sections
- **Card Spacing**: 8dp between cards in lists
- **Section Spacing**: 24dp between major sections
- **Grid Spacing**: 8dp between grid items

---

## 5. Components

### 5.1 Buttons

#### Primary Button
- **Height**: 48dp (minimum touch target)
- **Corner Radius**: 8dp
- **Background**: Primary color
- **Text**: White, 16sp, Bold
- **Padding**: 16dp horizontal, 12dp vertical
- **States**: Hover (elevation +2dp), Pressed (elevation -2dp), Disabled (opacity 0.38)

#### Secondary Button
- **Height**: 48dp
- **Corner Radius**: 8dp
- **Background**: Transparent with 2dp primary border
- **Text**: Primary color, 16sp, Bold
- **Padding**: 16dp horizontal, 12dp vertical

#### Text Button
- **Height**: 40dp
- **Background**: Transparent
- **Text**: Primary color, 16sp, Bold
- **Padding**: 12dp horizontal, 8dp vertical

### 5.2 Cards

#### Standard Card
- **Corner Radius**: 12dp
- **Elevation**: 2dp (rest), 4dp (hover), 8dp (selected)
- **Padding**: 16dp
- **Margin**: 8dp between cards
- **Background**: Surface color

#### Item Card
- **Image Aspect Ratio**: 4:3 (prevents layout shifts)
- **Image Height**: Constrained by aspect ratio
- **Content Padding**: 16dp
- **Minimum Touch Target**: Entire card (48dp minimum height)

### 5.3 Text Inputs

#### Outlined Text Input
- **Height**: 56dp
- **Corner Radius**: 8dp
- **Border Width**: 2dp (1dp when unfocused)
- **Border Color**: Outline (unfocused), Primary (focused)
- **Helper Text**: 12sp, hint color, 4dp below input
- **Error Text**: 12sp, error color, replaces helper text

#### Input States
- **Default**: Gray border, hint text
- **Focused**: Primary border, label animates up
- **Error**: Error border, error message displayed
- **Disabled**: 38% opacity, non-interactive

### 5.4 Image Handling

#### Image Ratios
- **Item Cards**: 4:3 aspect ratio
- **Item Detail Hero**: 16:9 or full width
- **Avatars**: 1:1 (circular)
- **Thumbnails**: 1:1 (square)

#### Image Loading
- **Placeholder**: Surface variant color with icon
- **Loading State**: Shimmer effect or progress indicator
- **Error State**: Error icon with retry option
- **Lazy Loading**: Load images as they enter viewport

### 5.5 Badges & Indicators

#### Time Badge
- **Height**: 20dp minimum
- **Corner Radius**: 10dp
- **Padding**: 8dp horizontal, 4dp vertical
- **Background**: Warning color (for countdown)
- **Text**: 10sp, Bold, White
- **Position**: Top-right corner of image

#### Status Badge
- **Height**: 24dp
- **Corner Radius**: 12dp
- **Padding**: 8dp horizontal, 4dp vertical
- **Colors**: Success (green), Warning (orange), Error (red)

---

## 6. Accessibility Guidelines

### 6.1 Touch Targets

- **Minimum Size**: 48dp x 48dp for all interactive elements
- **Spacing**: 8dp minimum between touch targets
- **Padding**: Use padding to increase touch area without visual size

### 6.2 Focus States

- **Focus Indicator**: 2dp outline in primary color
- **Focus Order**: Logical tab order (top to bottom, left to right)
- **Keyboard Navigation**: All interactive elements must be keyboard accessible

### 6.3 Screen Reader Support

- **Content Descriptions**: All images, icons, and interactive elements
- **Labels**: Clear, descriptive labels for form fields
- **Headings**: Proper heading hierarchy (H1 → H2 → H3)
- **Landmarks**: Use semantic HTML/Android views

### 6.4 Color & Contrast

- **Text Contrast**: Minimum 4.5:1 (AA), target 7:1 (AAA)
- **Non-Text Contrast**: 3:1 for UI components and graphics
- **Color Independence**: Don't rely solely on color to convey information
- **Dark Mode**: Support dark theme with proper contrast

### 6.5 Text Readability

- **Font Size**: Minimum 14sp for body text, 12sp for captions
- **Line Length**: 45-75 characters per line (optimal)
- **Line Height**: 1.5x font size for body text
- **Paragraph Spacing**: 16dp between paragraphs

---

## 7. Responsive Design

### 7.1 Breakpoints

| Device Type | Width | Layout |
|-------------|-------|--------|
| Mobile (Portrait) | < 600dp | Single column, stacked |
| Mobile (Landscape) | 600dp - 840dp | Single column, optimized |
| Tablet (Portrait) | 600dp - 960dp | Two columns possible |
| Tablet (Landscape) | 840dp - 1280dp | Multi-column layouts |
| Desktop | > 1280dp | Multi-column, sidebars |

### 7.2 Grid System

- **Mobile**: Single column, 16dp margins
- **Tablet**: 2-3 columns, 24dp margins
- **Desktop**: 3-4 columns, 32dp margins
- **Grid Spacing**: 8dp between items

### 7.3 Adaptive Components

- **Navigation**: Bottom navigation (mobile), Sidebar (tablet+)
- **Cards**: Full width (mobile), Grid (tablet+)
- **Forms**: Single column (mobile), Two columns (tablet+)
- **Images**: Full width (mobile), Constrained (tablet+)

---

## 8. Interaction Patterns

### 8.1 Navigation

- **Bottom Navigation**: Primary navigation (5 tabs max)
- **App Bar**: Contextual actions, search, notifications
- **Back Navigation**: Consistent back button behavior
- **Deep Linking**: Support direct navigation to items, bids, etc.

### 8.2 Feedback

- **Loading States**: Progress indicators, shimmer effects
- **Success States**: Toast messages, checkmarks, animations
- **Error States**: Clear error messages, retry options
- **Empty States**: Helpful messages, CTAs to take action

### 8.3 Gestures

- **Swipe**: Swipe to dismiss, swipe to refresh
- **Long Press**: Context menus, additional actions
- **Pull to Refresh**: Standard pull-to-refresh pattern
- **Swipe Navigation**: Image galleries, carousels

### 8.4 Animations

- **Duration**: 200-300ms for micro-interactions
- **Easing**: Material Design standard easing curves
- **Transitions**: Shared element transitions between screens
- **Loading**: Skeleton screens, progress indicators

---

## 9. Implementation Guidelines

### 9.1 Layout Structure

```xml
<!-- Recommended Layout Hierarchy -->
<CoordinatorLayout>
    <AppBarLayout>
        <MaterialToolbar />
    </AppBarLayout>
    
    <NestedScrollView>
        <LinearLayout>
            <!-- Content Sections -->
        </LinearLayout>
    </NestedScrollView>
</CoordinatorLayout>
```

### 9.2 Component Usage

#### Cards
- Use `MaterialCardView` for all card components
- Set `app:cardCornerRadius` to 12dp
- Use `app:cardElevation` for depth (2dp standard)
- Apply proper padding (16dp)

#### Buttons
- Use `MaterialButton` with appropriate styles
- Set minimum height to 48dp
- Use style attributes from `styles.xml`
- Provide proper content descriptions

#### Text Inputs
- Use `TextInputLayout` with `TextInputEditText`
- Apply `Widget.BidHub.TextInputLayout` style
- Include helper text and error handling
- Set proper input types

### 9.3 Performance Optimization

- **View Recycling**: Use RecyclerView for lists
- **Image Loading**: Lazy load images, use placeholders
- **Layout Optimization**: Avoid nested weights, use ConstraintLayout
- **View Binding**: Use ViewBinding to reduce findViewById calls

### 9.4 Code Organization

- **Layout Files**: One layout per screen/fragment
- **Styles**: Centralized in `styles.xml` and `text_styles.xml`
- **Colors**: Defined in `colors.xml` with semantic names
- **Dimensions**: Use `dimens.xml` for all spacing and sizes

---

## 10. Component Specifications

### 10.1 Homepage Components

#### Welcome Card
- **Background**: Primary container color
- **Padding**: 16dp
- **Content**: Welcome message, user alias, credit balance
- **Elevation**: 0dp (flat design)

#### Quick Action Cards
- **Layout**: 3 cards in a row, equal width
- **Height**: Minimum 100dp
- **Content**: Icon (48dp), Label (14sp bold)
- **Interaction**: Full card clickable, ripple effect

#### Featured Items Section
- **Layout**: Horizontal RecyclerView
- **Item Width**: 240dp
- **Spacing**: 16dp between items
- **Scroll**: Smooth horizontal scrolling

### 10.2 Item Card Components

#### Image Container
- **Aspect Ratio**: 4:3 (prevents layout shifts)
- **Corner Radius**: 12dp (top corners only)
- **Overlay**: Time badge (top-right)
- **Loading**: Shimmer effect or placeholder

#### Content Section
- **Title**: 14sp bold, 2 lines max, ellipsize
- **Price**: 16sp bold, primary color
- **Metadata**: 12sp regular, secondary color
- **Spacing**: 4dp between elements

### 10.3 Item Detail Components

#### Image Gallery
- **Hero Image**: Full width, 300dp height (collapsing toolbar)
- **ViewPager2**: For multiple images
- **Indicators**: Dots at bottom, semi-transparent background
- **Zoom**: Pinch-to-zoom support

#### Price Information Card
- **Layout**: Two-column grid (Starting Bid | Current Bid)
- **Divider**: 1dp vertical divider between columns
- **Time Progress**: Linear progress indicator below

#### Action Bar (Bottom)
- **Position**: Fixed at bottom
- **Background**: Surface color with elevation
- **Input**: TextInputLayout with prefix (₱)
- **Buttons**: Two equal-width buttons

### 10.4 Posting Form Components

#### Section Cards
- **Grouping**: Related fields in MaterialCardView
- **Headers**: 18sp bold section titles
- **Spacing**: 24dp between sections
- **Progressive Disclosure**: Collapsible optional sections

#### Form Fields
- **Layout**: Vertical stack with consistent spacing
- **Labels**: Above inputs (Material Design 3 style)
- **Helper Text**: Below inputs, 12sp, hint color
- **Validation**: Real-time feedback with error states

#### Image Upload
- **Grid**: 3 columns on mobile, 4 on tablet
- **Add Button**: Prominent, clear affordance
- **Progress**: Inline progress indicator during upload
- **Preview**: Thumbnail with remove option

---

## 11. Error Handling & States

### 11.1 Loading States

- **Skeleton Screens**: Shimmer effect for content loading
- **Progress Indicators**: Circular for actions, linear for progress
- **Placeholders**: Gray backgrounds with icons

### 11.2 Error States

- **Error Messages**: Clear, actionable error text
- **Retry Options**: Prominent retry buttons
- **Empty States**: Helpful messages with CTAs
- **Offline States**: Clear offline indicators

### 11.3 Success States

- **Toast Messages**: Brief success confirmations
- **Visual Feedback**: Checkmarks, animations
- **Navigation**: Automatic navigation after success (when appropriate)

---

## 12. Testing & Validation

### 12.1 Accessibility Testing

- **Screen Reader**: Test with TalkBack
- **Keyboard Navigation**: Test tab order
- **Color Contrast**: Validate with contrast checker
- **Touch Targets**: Verify 48dp minimum size

### 12.2 Responsive Testing

- **Devices**: Test on various screen sizes
- **Orientations**: Portrait and landscape
- **Densities**: Different pixel densities
- **Breakpoints**: Verify breakpoint behavior

### 12.3 Performance Testing

- **Layout Shifts**: Minimize CLS (Cumulative Layout Shift)
- **Loading Times**: Optimize image loading
- **Scroll Performance**: Smooth 60fps scrolling
- **Memory Usage**: Monitor for leaks

---

## 13. Implementation Checklist

### Homepage
- [x] Modern card-based layout
- [x] Welcome section with credit balance
- [x] Quick action cards (Browse, Post, My Listings)
- [x] Featured auctions horizontal scroll
- [x] Active bids section
- [x] Categories section
- [x] Empty state handling
- [x] Loading states

### Item Cards
- [x] Consistent 4:3 image aspect ratio
- [x] Clear visual hierarchy (image → title → price → metadata)
- [x] Time remaining badge
- [x] Image count indicator
- [x] Proper touch targets (entire card)
- [x] Accessibility support

### Item Detail Page
- [x] Collapsing toolbar with image gallery
- [x] ViewPager2 for image swiping
- [x] Image indicators
- [x] Price information card
- [x] Time progress indicator
- [x] Description section
- [x] Seller information card
- [x] Bid history section
- [x] Fixed bottom action bar
- [x] Proper scrolling behavior

### Posting Form
- [x] Section-based grouping (Photos, Basic Info, Pricing, Settings)
- [x] Material Design 3 text inputs
- [x] Helper text for all fields
- [x] Real-time validation feedback
- [x] Progressive disclosure (optional details)
- [x] Image upload with progress
- [x] Price type toggle (For Sale / Donation)
- [x] Form progress indicator
- [x] Clear action buttons

---

## 14. Future Enhancements

### Planned Improvements
- Dark mode support
- Advanced animations
- Micro-interactions
- Haptic feedback
- Gesture-based navigation
- Custom theme support

---

**End of Design System Documentation**

