---
name: NeuroCommand System
colors:
  surface: '#111318'
  surface-dim: '#111318'
  surface-bright: '#37393e'
  surface-container-lowest: '#0c0e12'
  surface-container-low: '#1a1c20'
  surface-container: '#1e2024'
  surface-container-high: '#282a2e'
  surface-container-highest: '#333539'
  on-surface: '#e2e2e8'
  on-surface-variant: '#b9cacb'
  inverse-surface: '#e2e2e8'
  inverse-on-surface: '#2f3035'
  outline: '#849495'
  outline-variant: '#3b494b'
  surface-tint: '#00dbe9'
  primary: '#dbfcff'
  on-primary: '#00363a'
  primary-container: '#00f0ff'
  on-primary-container: '#006970'
  inverse-primary: '#006970'
  secondary: '#ffdb9d'
  on-secondary: '#412d00'
  secondary-container: '#feb700'
  on-secondary-container: '#6b4b00'
  tertiary: '#dcffe2'
  on-tertiary: '#00391d'
  tertiary-container: '#00f990'
  on-tertiary-container: '#006d3c'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#7df4ff'
  primary-fixed-dim: '#00dbe9'
  on-primary-fixed: '#002022'
  on-primary-fixed-variant: '#004f54'
  secondary-fixed: '#ffdea8'
  secondary-fixed-dim: '#ffba20'
  on-secondary-fixed: '#271900'
  on-secondary-fixed-variant: '#5e4200'
  tertiary-fixed: '#5bffa1'
  tertiary-fixed-dim: '#00e383'
  on-tertiary-fixed: '#00210e'
  on-tertiary-fixed-variant: '#00522c'
  background: '#111318'
  on-background: '#e2e2e8'
  surface-variant: '#333539'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-caps:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.1em
  label-mono:
    fontFamily: JetBrains Mono
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
  data-numeral:
    fontFamily: JetBrains Mono
    fontSize: 18px
    fontWeight: '700'
    lineHeight: 18px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  base: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  edge-margin: 16px
  gutter: 12px
---

## Brand & Style

The design system is engineered to evoke the sensation of operating a high-stakes, professional laboratory interface. The brand personality is clinical, analytical, and authoritative, yet energized by the high-stakes "game" mechanics of social simulation.

The visual style is **Cyber-Surgical Glassmorphism**. It combines the structured density of a technical command center with the depth of modern translucent UI. The atmosphere is immersive and focused, utilizing a dark-room aesthetic that minimizes eye strain while emphasizing critical data points through neon luminance. 

**Core Tenets:**
- **Analytical Precision:** Every element must look like it serves a functional purpose in a data-rich environment.
- **Glass-Depth:** High use of backdrop blurs to suggest multiple layers of simulation running simultaneously.
- **Urgency & Status:** Color is used sparingly but intensely to denote system health, progress, and psychological volatility.

## Colors

The palette is rooted in a "Deep Space" neutral base to ensure neon accents possess maximum vibrance without bleeding.

- **Primary (Cyan - #00F0FF):** Used for "Active Command" elements: primary buttons, selected tabs, and interactive data nodes.
- **Secondary (Amber - #FFB800):** Reserved for "Cognitive Alerts": warnings, critical psychological thresholds, and high-priority notifications.
- **Tertiary (Emerald - #00FF94):** Dedicated to "Growth & Progress": XP gains, competency bars, and positive simulation outcomes.
- **Neutral:** A range of ultra-dark slates. Backgrounds use a near-black (#020408), while surfaces use a semi-transparent slate to facilitate glassmorphism.

## Typography

The system utilizes a hybrid typographic approach. **Inter** provides the structural legibility required for complex social narratives and psychological descriptions. **JetBrains Mono** is utilized for the "System Layer"—labels, data readouts, timestamps, and XP counters—to reinforce the technical simulation aesthetic.

- **Headlines:** Should be tight and impactful, using Inter with slight negative letter spacing.
- **Technical Labels:** Always use JetBrains Mono. Use `label-caps` for section headers and button text to evoke a hardware-interface feel.
- **Numerical Data:** All progress percentages and point values must use the monospaced font to prevent "jitter" during real-time updates.

## Layout & Spacing

Designed for mobile-first precision, the system uses a **4px baseline grid** to ensure all elements align to a technical matrix.

- **The Grid:** Use a 4-column fluid grid for mobile. Elements should feel "docked" into the interface.
- **Margins:** 16px screen edges keep content safe, while internal gutters are kept tight (12px) to maximize the information density appropriate for a simulation tool.
- **Modular Blocks:** Use standardized heights for data rows (48px or 64px) to maintain a rhythmic, scanned-list feel common in professional monitoring software.

## Elevation & Depth

This design system eschews traditional soft shadows in favor of **Luminous Glassmorphism** and **Backdrop Filtering**.

- **Surfaces:** Use `backdrop-filter: blur(12px)` combined with a 70% opaque neutral background.
- **Borders:** Depth is defined by inner glows and "stroke-only" containers. Use a 1px solid border at 20% opacity of the primary color for interactive elements.
- **Z-Axis:** Higher elevation elements do not get darker shadows; they get *clearer* (less blur) or more vibrant border-glows.
- **Scanlines:** A subtle, low-opacity horizontal pattern overlay on the base background layer enhances the "simulation monitor" feel.

## Shapes

The shape language is **Technical-Geometric**. 

- **Corner Radius:** A consistent `0.25rem` (Soft) radius is applied to maintain a modern feel without losing the "hard" edge of a professional tool. 
- **Buttons:** Rectangular with minimal rounding. Primary action buttons may feature "clipped corners" (45-degree snips) for a more aggressive, military-grade aesthetic.
- **Indicators:** Use squares or sharp diamonds for status pips rather than circles.

## Components

- **Command Buttons:** High-contrast Cyan fill with Black text (Inter Bold Caps). Include a subtle outer glow (Cyan, 15% opacity) to suggest the button is "powered on."
- **Progress/Competency Bars:** A background track of dark slate with a "segmented" fill of Emerald. Use vertical dividers every 10% to make the bar look like a series of power cells.
- **Data Cards:** Glassmorphic containers with a 1px Cyan border. The top-right corner should include a small monospaced "Ref ID" or "Status Code" to reinforce the simulation theme.
- **Status Chips:** Small, monospaced labels with a solid background color (Cyan, Amber, or Emerald) and 0% corner radius, looking like physical dymo-tapes.
- **Neural Input Fields:** Dark background, 1px border. On focus, the border glows Cyan and a subtle "scanning" animation (a vertical line moving across the background) triggers once.
- **XP/Point Indicators:** Floating text with a `+` prefix in Emerald, utilizing a "pixel-jump" animation when values increase.