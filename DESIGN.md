---
name: NetProtect · Panel del tutor
description: An editorial, calm control panel for a parent: cream paper, navy ink, one blue accent, serif titles over a sans interface.
colors:
  signal-blue: "#1769ff"
  signal-blue-deep: "#1456d9"
  signal-blue-text: "#1456d9"
  signal-blue-on-dark: "#7fa8ff"
  blue-wash: "#eaf1ff"
  warm-wash: "#f3f1ec"
  ink-navy: "#11243d"
  navy-950: "#0d1b2a"
  navy-900: "#10233b"
  navy-700: "#17345c"
  cream: "#f5f3ee"
  paper-white: "#ffffff"
  paper-muted: "#f8f7f3"
  stone: "#d9d6cf"
  stone-strong: "#8a8678"
  ink: "#11243d"
  slate-muted: "#5b6779"
  slate-subtle: "#6f6b60"
  success: "#16b364"
  success-wash: "#e7f8ef"
  success-text: "#0b7a42"
  danger: "#f04438"
  danger-wash: "#feeceb"
  danger-text: "#c4271c"
  warning: "#f79009"
  warning-wash: "#fef3e2"
  warning-text: "#9a5000"
  violet: "#7a5af8"
  violet-wash: "#f1edfe"
  violet-text: "#5b3fd6"
typography:
  display-xl:
    fontFamily: "Newsreader, Libre Caslon Text, Georgia, serif"
    fontSize: "64px"
    fontWeight: 600
    lineHeight: 1.2
  display:
    fontFamily: "Newsreader, Libre Caslon Text, Georgia, serif"
    fontSize: "44px"
    fontWeight: 600
    lineHeight: 1.2
  headline:
    fontFamily: "Newsreader, Libre Caslon Text, Georgia, serif"
    fontSize: "28px"
    fontWeight: 600
    lineHeight: 1.2
    fontFeature: "tnum"
  title:
    fontFamily: "Newsreader, Libre Caslon Text, Georgia, serif"
    fontSize: "18px"
    fontWeight: 600
    lineHeight: 1.2
  quote:
    fontFamily: "Newsreader, Libre Caslon Text, Georgia, serif"
    fontSize: "17px"
    fontWeight: 400
    lineHeight: "24px"
  body-lead:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.5
  body:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "13px"
    fontWeight: 600
    lineHeight: 1.5
  caption:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "12px"
    fontWeight: 600
    lineHeight: 1.5
  eyebrow:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "11px"
    fontWeight: 600
    letterSpacing: "0.14em"
rounded:
  sm: "4px"
  md: "6px"
  lg: "8px"
  xl: "8px"
  pill: "999px"
spacing:
  "1": "4px"
  "2": "8px"
  "3": "12px"
  "4": "16px"
  "5": "20px"
  "6": "24px"
  "8": "32px"
  "10": "40px"
  "12": "48px"
  "16": "64px"
  "24": "96px"
components:
  button-primary:
    backgroundColor: "{colors.signal-blue}"
    textColor: "{colors.paper-white}"
    typography: "{typography.body}"
    rounded: "{rounded.md}"
    padding: "0 16px"
    height: "40px"
  button-primary-hover:
    backgroundColor: "{colors.signal-blue-deep}"
  button-secondary:
    backgroundColor: "{colors.paper-white}"
    textColor: "{colors.signal-blue-text}"
    rounded: "{rounded.md}"
    padding: "0 16px"
    height: "40px"
  button-danger:
    backgroundColor: "{colors.danger-wash}"
    textColor: "{colors.danger-text}"
    rounded: "{rounded.md}"
    padding: "0 16px"
    height: "40px"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.slate-muted}"
    rounded: "{rounded.md}"
    padding: "0 16px"
    height: "40px"
  button-sm:
    typography: "{typography.label}"
    padding: "0 12px"
    height: "32px"
  card:
    backgroundColor: "{colors.paper-muted}"
    rounded: "{rounded.lg}"
  input:
    backgroundColor: "{colors.paper-white}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.md}"
    padding: "0 12px"
    height: "40px"
  status-badge-success:
    backgroundColor: "{colors.success-wash}"
    textColor: "{colors.success-text}"
    typography: "{typography.caption}"
    rounded: "{rounded.sm}"
    padding: "0 10px"
    height: "24px"
  status-badge-danger:
    backgroundColor: "{colors.danger-wash}"
    textColor: "{colors.danger-text}"
    typography: "{typography.caption}"
    rounded: "{rounded.sm}"
    padding: "0 10px"
    height: "24px"
  switch-on:
    backgroundColor: "{colors.signal-blue}"
    rounded: "{rounded.pill}"
    width: "44px"
    height: "24px"
---

# Design System: NetProtect · Panel del tutor

<!-- Sprint 53 (docs/sprint-53.md). Recorded from the shipped tokens in frontend/src/app/globals.css and
     the evolved primitives in frontend/src/components/ui/*. SCOPE OF WHAT IS SHIPPED: tokens, serif
     headings, the primitives and the /design-system gallery. NOT yet migrated: the shell (Sidebar,
     Header) and the login are still the previous light design until Sprint 54, and the 16 section
     views are recomposed in Sprints 55-59; they inherit the new colours, radii and typography today
     but not the new composition. The component entries below describe the primitives only. -->

## Overview

**Creative North Star: "The Quiet Editorial Desk"**

NetProtect is a parent's desk, not an admin console. The page is warm cream paper; content sits on it as open panels bounded by thin stone lines rather than as floating white cards; titles are set in a serif, as in a magazine, and everything you operate (buttons, forms, tables, navigation, data) is set in a plain sans. One saturated blue, Signal Blue, does all the pointing and is never a surface. A deep navy appears where the interface needs weight (the sidebar from Sprint 54, the brand card). The product speaks of security, control and accompaniment, so the page must feel calm, serious and human.

Reference direction and the per-view decisions live in `docs/redesign/` (`02_DESIGN_TARGET.md`, `03_VISTAS.md`, `fase-0-informe.md`).

**Key characteristics**
- Cream ground (`cream`), flat colour, no gradient.
- Open panels: a 1px `stone` line and `paper-muted` tone, no drop shadow.
- Almost square corners: 4–8px. Pills only for switches and status dots.
- Serif for titles (h1–h4, `quote`), sans for everything else. Figures use tabular numerals.
- Blue is an accent: one point of emphasis per screen.
- Space is generous on purpose; fewer, better elements.

## Colors

### Primary
- **Signal Blue** (`--color-primary`, `#1769ff`): fills only (primary button, active item, switch on). White text on it is 4.67:1.
- **Signal Blue Deep** (`--color-primary-hover`, `#1456d9`): hover of the fill.
- **Signal Blue Text** (`--color-primary-text`, `#1456d9`): blue for text and icons on cream or white (5.63:1 and 6.24:1).
- **Blue Wash** (`--color-primary-soft`, `#eaf1ff`): the selected-row and informational wash.
- **Signal Blue on Dark** (`--color-primary-on-dark`, `#7fa8ff`): blue on the navy surfaces (7.41:1).

### Neutral
- **Cream** (`--color-bg`, `#f5f3ee`): the page. `--color-bg-gradient` is kept for compatibility and is now the same flat colour.
- **Paper White** (`--color-surface`, `#ffffff`) and **Paper Muted** (`--color-surface-muted`, `#f8f7f3`, the panel tone).
- **Warm Wash** (`--color-primary-softer` and `--color-neutral-soft`, `#f3f1ec`): hover and neutral fills. The token name still says "primary" for compatibility; it is a warm neutral, not blue.
- **Stone** (`--color-border`, `#d9d6cf`): lines and dividers. **Stone Strong** (`--color-border-strong`, `#8a8678`): control borders only (3.29:1 on cream, 3.64:1 on white).
- **Ink Navy** (`--color-navy` and `--color-text`, `#11243d`): titles, values and body text (14.09:1 on cream).
- **Slate Muted** (`--color-text-muted`, `#5b6779`): secondary text (5.17:1 on cream). **Slate Subtle** (`--color-text-subtle`, `#6f6b60`): placeholders and tertiary text (4.80:1 on cream, 5.32:1 on white).
- **Navy 950 / 900 / 700** (`#0d1b2a` / `#10233b` / `#17345c`): dark surfaces. `--color-sidebar-*` aliases them for Sprint 54.

### Status (semantic only)
Green only for positive states, red only for alerts, blocks and critical states; amber for warnings; violet for schedules. Each has a base, a wash and a text colour (all text/wash pairs clear 4.5:1).

### Named Rules
**The One Accent Rule.** Blue is the only saturated colour that is not a status, and it marks one point of emphasis per screen. If two things are blue, one is wrong.
**The Text Ladder Rule.** Text on cream uses `--color-text`, `--color-text-muted` or `--color-text-subtle`, never `#697586` (4.22:1 on cream) and never `--color-border-strong` (3.29:1) as text.
**The Names Are Stable Rule.** Token names do not change when values do; a name that no longer fits (`--color-primary-softer`) is documented, not renamed, until every consumer migrates.

## Typography

**Display and titles: Newsreader** (served by `next/font/google`, weights 400 and 600 plus italic). Provisional: Playfair Display was the first choice (closest to the mockups) but its hairline strokes, such as the crossbar of the "e", vanished in 1x renders so titles read "Gcoccrcas"; Newsreader keeps them solid. The final face is still an open decision for the designer (D1 in `docs/redesign/fase-0-informe.md`); changing it is `--font-serif` plus one constant in `app/layout.tsx`. **Interface: Inter.**

### Hierarchy
- **Display XL** (64px, serif 600): only the login "Inicia sesión". **Display** (44px): page title. **Headline** (28px): large values. **Title** (18px): card and section titles.
- **Quote** (17/24, serif, italic): the editorial phrase of a page (`.quote`).
- **Body** (14px) and **Body Lead** (15px), **Label** (13px/600), **Caption** (12px/600), **Eyebrow** (11px/600, uppercase, 0.14em: `.eyebrow`).
- Figures use `font-variant-numeric: tabular-nums` (`.tabular`).

### Named Rules
**The Two Voices Rule.** h1–h4 are serif; nothing else is. Buttons, forms, tables, navigation, states, data and labels stay sans.
**The Real Weights Rule.** Only 400 and 600 of the serif are loaded; do not ask for 700 (the browser would pick 600 anyway, so write 600).

## Layout

Cream page, content in open panels. The shell has three layouts: sidebar at 1200px and up, a rail from 900 to 1199px, a drawer below 900px. Their sizes are tokens (`--sidebar-width` 270px, `--sidebar-rail-width` 84px, `--sidebar-drawer-width` 300px, `--content-max` 1440px, `--header-height` 72px and `--header-height-mobile` 64px); the breakpoints themselves stay literals because media queries cannot read custom properties. `--band-height` (168px) is the minimum height of the photographic band of `PageHeader`.

Mobile reinterprets the composition instead of compressing it. Do not repeat the "N cards on top, M cards below" grid on every page.

## Elevation & Depth

Depth comes from lines and tone, not shadows.

- `--shadow-card`: `0 1px 2px rgba(17, 36, 61, 0.06)`, almost invisible; most panels use none.
- `--shadow-pop`: `0 12px 32px rgba(13, 27, 42, 0.14)`, only for menus and dialogs.
- `--focus-ring`: a 2px cream gap and a 4px Signal Blue ring. On navy surfaces use `--focus-ring-on-dark`.

**The Flat By Default Rule.** A surface is separated from the page by a 1px stone line and a tone change. A shadow is a response to state (a floating menu), never a resting style.

## Shapes

`--radius-sm` 4px (default of focus, badges, small controls), `--radius-md` 6px (buttons, inputs), `--radius-lg` and `--radius-xl` 8px (panels, the ceiling), `--radius-pill` only for switches and status dots. Round icon discs that still exist in `MetricCard` and `EmptyState` are a known leftover, to be replaced by the shared tone icon in Sprint 55.

## Components

### Buttons
Primary (Signal Blue fill, white text), secondary (paper white, `stone-strong` border, blue text), danger (danger wash, token-derived border), ghost (transparent, slate). Heights 40px and 32px (`sm`). The primary button has a quiet 1px shadow derived from the brand blue.

### Status badges
Radius 4px; the optional dot is a circle. Always text as well as colour. Tones: success, danger, warning, info, violet, neutral.

### Cards / containers (open panels)
`Card` is a panel: `--color-surface-muted`, 1px `--color-border`, `--radius-lg`, no shadow. `CardHeader` titles are navy serif; its icon keeps the blue text colour. Because the panel tone is `surface-muted`, anything inside that needs its own hover or fill uses `--color-primary-softer` (hover) or `--color-primary-soft` (selected), never `--color-surface-muted`.

### Metric cards
No background, shadow or radius: a top rule, a label and a tabular figure. `MetricGrid` is unchanged; the redesign avoids repeating four of them in a row on every page.

### Inputs / fields
White field, `--color-border-strong` border, 6px radius, placeholder in `--color-text-subtle`, focus ring.

### Segmented control, day picker, switch
Selected option in Signal Blue fill; the group border is `--color-border-strong`; the switch track off is `--color-border-strong`, on is Signal Blue.

### Empty state
Icon in a warm neutral disc, or an optional `illustration` node that replaces it. Illustrations must be light and small; the three in `docs/redesign/assets/ilustraciones/` are heavier than the direction and unoptimised, so they are not used yet.

### Page header
`PageHeader` keeps its plain layout by default. With `band`, it paints a photographic band (`/brand/band-alpine.jpg`, decorative, `alt=""`) with a cream veil on the left, the breadcrumb, a serif title, the description, an optional `quote` and the `aside` slot (the device selector). It is opt-in; `DashboardShell` turns it on in Sprint 54. There is exactly one h1 per view (the e2e depends on it).

### Navigation, header, login, device selector
Still the previous light design; they are rebuilt in Sprint 54 (navy sidebar with grouped navigation and a blue active item, a clean header with search, notifications, settings and profile, and the login with a full-bleed photograph). Do not describe them here until they ship.

### Motion
`--ease`, 150/200/250ms. One authored moment per screen (the section fade-in). Animate `transform` and `opacity` only; bars use `scaleX`. `prefers-reduced-motion` collapses all of it.

## Do's and Don'ts

### Do:
- **Do** use only tokens from `globals.css`; if a value is missing, add a token, do not hard-code.
- **Do** keep blue for one point of emphasis per screen and green/red for meaning only.
- **Do** separate surfaces with a line and a tone, and leave generous space.
- **Do** keep the real data rule: the look follows the mockup, the function follows the backend (`.claude/rules/frontend.md`).
- **Do** check contrast on the surface the text actually sits on, cream and `paper-muted` included.

### Don't:
- **Don't** use drop shadows, glass or gradients as decoration; the only gradient is the cream veil of the page band.
- **Don't** round beyond 8px, except switches and status dots.
- **Don't** set anything but titles in the serif, and don't use more than the two loaded weights.
- **Don't** recreate the logo: the official PNGs in `public/brand/` rule over any wordmark drawn in a mockup, and the product name is always NetProtect.
- **Don't** turn real components (buttons, inputs, timelines, charts, maps, schedules) into images.
- **Don't** repeat a grid of equal cards as a page structure.
