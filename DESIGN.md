---
name: Nocturnal Scholar
colors:
  surface: '#0b1326'
  surface-dim: '#0b1326'
  surface-bright: '#31394d'
  surface-container-lowest: '#060e20'
  surface-container-low: '#131b2e'
  surface-container: '#171f33'
  surface-container-high: '#222a3d'
  surface-container-highest: '#2d3449'
  on-surface: '#dae2fd'
  on-surface-variant: '#c6c5d5'
  inverse-surface: '#dae2fd'
  inverse-on-surface: '#283044'
  outline: '#908f9e'
  outline-variant: '#454653'
  surface-tint: '#bdc2ff'
  primary: '#bdc2ff'
  on-primary: '#131e8c'
  primary-container: '#818cf8'
  on-primary-container: '#101b8a'
  inverse-primary: '#4953bc'
  secondary: '#b9c8de'
  on-secondary: '#233143'
  secondary-container: '#39485a'
  on-secondary-container: '#a7b6cc'
  tertiary: '#f7bd3e'
  on-tertiary: '#402d00'
  tertiary-container: '#c08d00'
  on-tertiary-container: '#3e2b00'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e0e0ff'
  primary-fixed-dim: '#bdc2ff'
  on-primary-fixed: '#000767'
  on-primary-fixed-variant: '#2f3aa3'
  secondary-fixed: '#d4e4fa'
  secondary-fixed-dim: '#b9c8de'
  on-secondary-fixed: '#0d1c2d'
  on-secondary-fixed-variant: '#39485a'
  tertiary-fixed: '#ffdea3'
  tertiary-fixed-dim: '#f7bd3e'
  on-tertiary-fixed: '#261900'
  on-tertiary-fixed-variant: '#5d4200'
  background: '#0b1326'
  on-background: '#dae2fd'
  surface-variant: '#2d3449'
typography:
  display-lg:
    fontFamily: Geist
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Geist
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Geist
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  title-md:
    fontFamily: Geist
    fontSize: 18px
    fontWeight: '500'
    lineHeight: 24px
  body-md:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 26px
  body-sm:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  base: 4px
  container-padding-desktop: 32px
  container-padding-mobile: 16px
  gutter: 24px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 32px
---

## Brand & Style
The brand personality is intellectual, precise, and authoritative, tailored for researchers, developers, and academic professionals. It evokes an environment of deep focus—reminiscent of a quiet university library at night.

The design style is **Corporate / Modern** with a lean towards **Minimalism**. It prioritizes information density and structural clarity. The interface uses a dark color palette to reduce eye strain during prolonged sessions of reading and data analysis, maintaining a sophisticated and "developer-friendly" aesthetic through technical typography and a disciplined grid.

## Colors
The palette is anchored by a deep navy/charcoal base (`#0f172a`), providing a stable, low-distraction environment. 

- **Primary Indigo:** Adjusted to a lighter, more vibrant tone (`#818cf8`) for the dark background to maintain AA accessibility standards while retaining its scholarly character.
- **Surface Hierarchy:** Depth is achieved through "Tonal Layers" rather than shadows. Lighter shades of navy/slate define nested containers, creating a clear information architecture without breaking the dark-mode immersion.
- **Text Contrast:** High-priority content uses `slate-50` for maximum legibility, while secondary metadata uses `slate-300` to create a visual "recede" for less critical information.

## Typography
This design system utilizes **Geist** as its primary typeface to convey a clean, technical, and modern academic feel. The tight tracking and systematic weights are ideal for data-heavy layouts.

For technical metadata, citations, and code snippets, **JetBrains Mono** is introduced as a secondary label font. This reinforces the "Developer/Researcher" niche. 

- **Headlines:** Use Semi-Bold or Bold weights with slight negative letter-spacing to maintain a compact, authoritative presence.
- **Body:** Standardized at 16px for desktop with a generous 1.6x line height to ensure readability of long-form academic text in dark mode.
- **Labels:** Always uppercase when using the monospaced font to differentiate from standard UI prose.

## Layout & Spacing
The layout follows a **Fixed Grid** philosophy for desktop to maintain optimal line lengths for reading (max-width 1280px). 

- **Grid:** A 12-column system with 24px gutters.
- **Rhythm:** An 8px base grid governs all vertical spacing. Components are separated by 16px (md) or 32px (lg) depending on their semantic relationship.
- **Mobile Adaptivity:** On mobile, the grid collapses to 4 columns with 16px side margins. Typography scales down (e.g., `headline-lg` to `headline-lg-mobile`) to prevent awkward word breaks in narrow viewports.

## Elevation & Depth
In this dark-themed system, elevation is communicated through **Tonal Layers** and **Low-contrast outlines** rather than traditional shadows, which can appear "dirty" on deep navy backgrounds.

- **Level 0 (Background):** `#0f172a` — The base canvas.
- **Level 1 (Cards/Sidebar):** `#1e293b` — Raised slightly from the background.
- **Level 2 (Modals/Popovers):** `#334155` — The highest functional elevation.
- **Outlines:** All containers use a 1px solid border (`#475569`) to maintain crisp edges between similar tonal values.
- **Interaction:** Hover states on interactive elements should slightly lighten the background color (e.g., from `surface-container` to `surface-container-high`).

## Shapes
The shape language is **Soft**, utilizing a 4px (0.25rem) base radius. This creates a professional and rigorous feel that is more approachable than sharp corners but more serious than highly rounded styles.

- **Small Components:** (Checkboxes, Tags) use 4px.
- **Medium Components:** (Buttons, Input Fields) use 4px.
- **Large Components:** (Cards, Modals) use 8px (`rounded-lg`).

## Components
- **Buttons:** Primary buttons use the indigo accent with white text. Ghost buttons use the `on-surface-variant` color with a `slate-700` border.
- **Input Fields:** Use a dark fill (`#1e293b`) with a 1px border. Focus states use a 2px indigo outline.
- **Chips/Tags:** Use the monospaced label font. They should be styled with a subtle indigo tint background (`rgba(129, 140, 248, 0.1)`) and indigo text for high visibility.
- **Lists:** Separated by horizontal rules in `slate-800`. Selected items use a vertical 2px indigo "active" indicator on the left edge.
- **Cards:** Cards should be flat, using the `surface-container-low` color and a 1px border. No shadows.
- **Data Tables:** High-density, using `slate-900` for zebra-striping. Headers are `slate-400` in the monospaced label font.