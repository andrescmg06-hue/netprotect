---
name: NetProtect · Panel del tutor
description: A calm light-blue frame where a parent sees the current device and anything needing attention at a glance.
colors:
  signal-blue: "#246bfe"
  signal-blue-deep: "#1456d9"
  signal-blue-text: "#1f5fe6"
  blue-wash: "#eaf1ff"
  blue-mist: "#f3f7ff"
  shield-navy: "#102b63"
  sky-ground: "#f5f9ff"
  paper-white: "#ffffff"
  paper-muted: "#f7f9fc"
  hairline: "#e6ecf5"
  hairline-strong: "#d5dfee"
  ink: "#1b2b4b"
  slate-muted: "#5b6b82"
  slate-subtle: "#94a3b8"
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
  neutral-wash: "#eef2f7"
typography:
  display:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "32px"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "28px"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "-0.01em"
    fontFeature: "tnum"
  title:
    fontFamily: "Inter, system-ui, -apple-system, Segoe UI, sans-serif"
    fontSize: "18px"
    fontWeight: 600
    lineHeight: 1.2
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
rounded:
  sm: "8px"
  md: "10px"
  lg: "14px"
  xl: "16px"
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
    textColor: "{colors.signal-blue}"
    rounded: "{rounded.md}"
    padding: "0 16px"
    height: "40px"
  button-secondary-hover:
    backgroundColor: "{colors.blue-mist}"
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
  button-ghost-hover:
    backgroundColor: "{colors.blue-mist}"
    textColor: "{colors.signal-blue}"
  button-sm:
    typography: "{typography.label}"
    padding: "0 12px"
    height: "32px"
  card:
    backgroundColor: "{colors.paper-white}"
    rounded: "{rounded.xl}"
    padding: "16px"
  card-lg:
    backgroundColor: "{colors.paper-white}"
    rounded: "{rounded.xl}"
    padding: "20px 24px"
  metric-card:
    backgroundColor: "{colors.paper-white}"
    textColor: "{colors.shield-navy}"
    typography: "{typography.headline}"
    rounded: "{rounded.xl}"
    padding: "20px"
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
    rounded: "{rounded.pill}"
    padding: "0 10px"
    height: "24px"
  status-badge-danger:
    backgroundColor: "{colors.danger-wash}"
    textColor: "{colors.danger-text}"
    typography: "{typography.caption}"
    rounded: "{rounded.pill}"
    padding: "0 10px"
    height: "24px"
  status-badge-info:
    backgroundColor: "{colors.blue-wash}"
    textColor: "{colors.signal-blue-deep}"
    typography: "{typography.caption}"
    rounded: "{rounded.pill}"
    padding: "0 10px"
    height: "24px"
  nav-item:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.md}"
    padding: "0 12px"
    height: "42px"
  nav-item-hover:
    backgroundColor: "{colors.blue-mist}"
    textColor: "{colors.signal-blue}"
  nav-item-active:
    backgroundColor: "{colors.signal-blue}"
    textColor: "{colors.paper-white}"
  segmented-option-selected:
    backgroundColor: "{colors.signal-blue}"
    textColor: "{colors.paper-white}"
    typography: "{typography.label}"
    rounded: "{rounded.sm}"
    padding: "0 16px"
    height: "32px"
  search-field:
    backgroundColor: "{colors.blue-mist}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.lg}"
    padding: "0 16px 0 44px"
    height: "44px"
  switch-on:
    backgroundColor: "{colors.signal-blue}"
    rounded: "{rounded.pill}"
    width: "44px"
    height: "24px"
---

# Design System: NetProtect · Panel del tutor

<!-- Recorded from the built Sprint 31–32 shell and ui/ primitives (frontend/src/app/globals.css
     :root tokens, frontend/src/components/ui/*, frontend/src/components/shell/*,
     DashboardShell). The section views inside the shell are still the pre-redesign panels and are
     NOT described here; they are rebuilt from these primitives in Sprints 33–38. -->

## Overview

**Creative North Star: "The Calm Blue Frame"**

The tutor panel is one quiet, light-blue frame around whatever the parent is looking at. A white sidebar and a white header sit on a pale sky ground (`sky-ground`, with a very soft vertical gradient toward `#eef4ff`); inside it, white cards with hairline borders and a barely-there shadow hold the content. One saturated blue, Signal Blue, does all the pointing: the active section, the primary action, the selected option, the switch that is on. Everything else is navy, ink and slate on white. The effect is meant to read as security, trust and control without feeling like a dark admin console or a children's app.

The frame exists so that two things are always one glance away: which device the parent is looking at (the device card at the right of every page header, with its status badge), and whether anything needs attention (the unread count on the Alerts item and the dot on the header bell). Density is moderate: 14px body text, 40px controls, 24px between page blocks on desktop. Phone and desktop carry equal weight; the same markup becomes a full sidebar, an icon rail and an off-canvas drawer.

The system is honest by construction. Every number, badge and button maps to real data or a real action; stat tiles carry a plain-text hint instead of trend arrows; delays (location every ~15 minutes) are stated in section descriptions instead of dressed up as live. Light theme only (`color-scheme: light`); there is no dark mode.

**Key Characteristics:**
- Light only: white surfaces on a pale sky-blue ground, navy headings, slate secondary text.
- One accent (Signal Blue) reserved for active, primary and "on"; status colours are used only for status.
- Soft-shadow white cards with 16px corners and a 1px hairline border.
- Inter throughout, weights 400/500/600/700, no second family.
- Lucide line icons only, 16–22px, stroke around 1.9–2.
- Motion is short (150–200ms), state-only, and collapses under reduced motion.
- Three responsive shells from one markup: sidebar ≥1200px, icon rail 900–1199px, drawer <900px.

## Colors

A single cool blue family over white and pale sky, with semantic status hues held back for status.

### Primary
- **Signal Blue** (`signal-blue`): the only accent. Active navigation item (filled, white text), primary button, selected segment and day, switch "on", link colour, focus ring (at 25% alpha), caret, card titles and sidebar group labels.
- **Signal Blue Deep** (`signal-blue-deep`): hover state of the primary button; text colour of the info badge.
- **Blue Wash** (`blue-wash`): tinted containers for blue icons (device card icon tile, empty-state disc, info metric icon, avatar initials background) and the info badge fill.
- **Blue Mist** (`blue-mist`): the hover wash for every quiet control (nav items, ghost and secondary buttons, icon buttons, menu items, search results) and the resting fill of the header search field and unselected days.

### Neutral
- **Shield Navy** (`shield-navy`): all headings (h1–h4), page titles, metric values, the device name, the user's name. Taken from the logo's "Net" wordmark.
- **Ink** (`ink`): body text and nav item labels.
- **Slate Muted** (`slate-muted`): secondary text: descriptions, hints, emails, resting icons in nav and header.
- **Signal Blue Text** (`signal-blue-text`): blue text and icons (card titles, hover states, secondary buttons, sidebar group labels). ≥4.96:1 on every light surface, where Signal Blue itself drops to 4.24:1 on Blue Mist.
- **Slate Subtle** (`slate-subtle`): input placeholders only. Not for any text a user must read (2.56:1 on white).
- **Sky Ground** (`sky-ground`): the page ground behind the content column (as a 180° gradient to `#eef4ff`, fixed).
- **Paper White** (`paper-white`): sidebar, header, cards, inputs, menus, dialogs.
- **Paper Muted** (`paper-muted`): the action footer strip of the confirm dialog.
- **Hairline** (`hairline`) and **Hairline Strong** (`hairline-strong`): 1px borders. Hairline on cards, sidebar, header and dividers; Hairline Strong on inputs.

### Status (semantic only)
Each status hue comes as a triple: a solid (icon or dot), a wash (fill) and a text shade (text on the wash).
- **Success** (`success` / `success-wash` / `success-text`): device "En línea", success metrics.
- **Danger** (`danger` / `danger-wash` / `danger-text`): device "Alerta", destructive buttons and menu items, the unread count and bell dot, the confirm-dialog icon.
- **Warning** (`warning` / `warning-wash` / `warning-text`): warning tone for badges and metrics.
- **Violet** (`violet` / `violet-wash` / `violet-text`): a sixth tone available to badges and metric tiles; no shipped screen assigns it a meaning yet (undecided).
- **Neutral Wash** (`neutral-wash`): the neutral badge and metric tone ("Desconectado").

### Named Rules
**The One Blue Rule.** Signal Blue marks exactly one of: where you are, what you can do next, or what is on. It is never used for decoration, for large fills beyond a single active item or button, or as a second brand colour. Status never borrows it except as the "info" tone.

**The Tone Means Something Rule.** Badge and metric tones are chosen by meaning (online, alert, warning), never to make a screen colourful, and never as the only carrier of information: every tone ships with a text label.

**The Fill-vs-Text Blue Rule.** Signal Blue (`#246bfe`) is for fills and borders; white text on it is 4.56:1. Blue *text and icons* use Signal Blue Text (`#1f5fe6`), which stays ≥4.9:1 on white, Blue Mist, Blue Wash and Sky Ground.

**Contrast (WCAG 2.1 AA, measured).** Every text pairing clears 4.5:1: Slate Muted `#5b6b82` ≥4.8:1 on white, Sky Ground, Blue Mist and Neutral Wash; success text on its wash 4.92:1; warning 5.43:1; danger 5.04:1; info (Signal Blue Deep on Blue Wash) 5.51:1; violet 5.85:1; Signal Blue Text ≥4.96:1 on every light surface. Fixed in Sprint 32 after this document first recorded five gaps.

## Typography

**Display Font:** Inter (self-hosted by `next/font`, fallback `system-ui, -apple-system, "Segoe UI", sans-serif`)
**Body Font:** Inter (same stack)

**Character:** One humanist-geometric sans at four weights; hierarchy comes from size, weight and the navy/ink/slate colour step, not from a second family.

### Hierarchy
- **Display** (700, 32px, 1.2, -0.02em, navy): the page title in the page header, one per view.
- **Headline** (700, 28px, 1.2, -0.01em, navy): the value in a metric card. Numbers use tabular figures.
- **Title** (600, 18px, 1.2): card titles (in Signal Blue, with an optional leading icon) and dialog titles.
- **Body lead** (400, 15px): page descriptions, device name (600), sidebar profile name and group labels (600). Page description max width is 820px.
- **Body** (400, 14px, 1.5): default text, nav items (500), buttons (600), inputs.
- **Label** (600, 13px): field labels, segmented options, small buttons, metric labels (400, slate), breadcrumb.
- **Caption** (600, 12px): status badges, field hints and errors (400).

Sentence case everywhere. The only numeric text below 12px is the nav unread count (700, 11px; 10px in the rail).

### Named Rules
**The Tabular Numbers Rule.** Counts and metric values render with tabular figures (`.tabular` or `font-variant-numeric: tabular-nums`) so they do not jitter as they update.

## Layout

The shell is a two-column grid: sidebar (auto width) and a content column. The content column holds the sticky 72px header and a centred content stack with a 1440px maximum width; the header pads itself so its right edge lines up with that same column on very wide screens.

- **≥1200px:** 270px sidebar, sticky and full height. Content padding 32px (40px bottom), 24px gap between page blocks.
- **900–1199px:** the sidebar becomes an 84px icon rail (shield logo, avatar, icons only; labels visually hidden but kept as accessible names; sign-out moves to the account menu). Header and content padding drop to 24px; the user's name in the header hides.
- **<900px:** the sidebar becomes an off-canvas drawer (`min(300px, 86vw)`) over a navy 30% backdrop, opened from a menu button; the header drops to 64px and shows the shield mark; content padding 20px/16px with a 20px gap. Settings and the menu chevron hide.
- **≤720px:** the page header stacks: the device card goes full width under the title and the breadcrumb hides.

Spacing uses a 4px base scale (4, 8, 12, 16, 20, 24, 32, 40). Stat tiles lay out in an auto-fit grid with a 210px minimum column and 16px gap.

The page header is the fixed opening of every view: title and one-sentence description on the left, the active device card on the right.

## Elevation & Depth

Mostly flat with ambient lift. Surfaces separate from the sky ground by colour (white on pale blue), a 1px hairline border and a very soft, blue-tinted two-layer shadow. Anything that floats above the page (menus, search results, drawer, dialog) uses one stronger pop shadow. Filled blue elements (primary button, active nav item) carry a small blue glow.

### Shadow Vocabulary
- **Card** (`box-shadow: 0 1px 2px rgba(16, 43, 99, 0.04), 0 4px 20px rgba(30, 70, 140, 0.05)`): cards, metric tiles, the device card. Resting state, always paired with the hairline border.
- **Pop** (`box-shadow: 0 12px 32px rgba(16, 43, 99, 0.14)`): account menu, search results, phone drawer, confirm dialog.
- **Blue glow** (`0 2px 8px rgba(36, 107, 254, 0.25)` on the primary button; `0 4px 12px rgba(36, 107, 254, 0.25)` on the active nav item): only under Signal Blue fills.
- **Focus ring** (`box-shadow: 0 0 0 3px rgba(36, 107, 254, 0.25)`): every `:focus-visible` and focused input, plus a Signal Blue border on inputs and the device card.

### Named Rules
**The Hairline-Plus-Whisper Rule.** A resting container is white, has a 1px hairline border and the card shadow. Never a heavier shadow at rest; stronger elevation is reserved for things that float.

## Shapes

Softly rounded, never pill-shaped except for the smallest status elements. Radii step with the size of the thing: 8px for small inner items (menu items, segmented options, sign-out), 10px for controls (buttons, inputs, nav items, icon buttons, days), 14px for the device card, search field and floating menus, 16px for cards, metric tiles and dialogs. Pills (999px) are only for status badges, counts and the switch. Circles are for avatars, the metric icon disc, the empty-state disc and status dots.

Icon containers are tinted, not outlined: a wash-coloured circle (metric, empty state, dialog) or a 10px-rounded tile (device card) with the solid hue as the icon colour.

## Components

### Buttons
Confident but not loud: solid only when it is the primary action.
- **Shape:** gently rounded (10px), 40px tall (32px small), 600 weight, icon gap 8px.
- **Primary:** Signal Blue fill, white text, small blue glow; hover deepens to Signal Blue Deep.
- **Secondary:** white with a pale blue border (`#cfdcf7`) and Signal Blue text; hover fills Blue Mist and the border turns Signal Blue.
- **Danger:** Danger Wash fill, Danger Text, 30%-alpha danger border; hover `#fddcd9` with a solid danger border.
- **Ghost:** transparent, Slate Muted text; hover Blue Mist and Signal Blue text.
- **Disabled:** 55% opacity, not-allowed cursor.
- **Transitions:** background, border, colour and shadow at 150ms.

### Status badges
- **Style:** 24px pill, 12px/600 text, optional leading 7px dot in the current colour or a 13px icon.
- **Tones:** success, danger, warning, info, violet, neutral — each wash fill with its text shade. Device status maps ONLINE → success "En línea", OFFLINE → neutral "Desconectado", ALERT → danger "Alerta".

### Cards / Containers
- **Corner Style:** 16px.
- **Background:** Paper White on Sky Ground.
- **Shadow Strategy:** the Card shadow (see Elevation).
- **Border:** 1px Hairline.
- **Internal Padding:** 16px (md) or 20px/24px (lg); padding none for edge-to-edge lists.
- **Card header:** 18px/600 Signal Blue title with an optional icon, a 13px slate subtitle, actions on the right, 16px below.

### Metric cards
Tinted 48px circular icon (wash fill, solid icon) beside a slate label, a 28px/700 navy value and one plain-text hint line. Padding 20px, same card surface. The hint is plain text on purpose: no trend arrows or deltas unless the data exists.

### Inputs / Fields
- **Style:** 40px, white, 1px Hairline Strong border, 10px radius, 12px horizontal padding; Slate Subtle placeholder; label 13px/600 above, 6px gap, 12px hint below.
- **Hover:** border `#b9c9e4`.
- **Focus:** Signal Blue border plus the focus ring.
- **Error:** 12px Danger Text message under the field.
- **Select:** same control with a Lucide chevron drawn as an inline SVG background, right 12px.
- **Leading icon:** Slate Muted icon at left 12px, text padded to 38px.

### Segmented control, day picker, switch
- **Segmented control:** white group with a hairline border and 3px inset; 32px options at 13px/600 in Slate Muted; hover Blue Mist; selected is a Signal Blue fill with white text.
- **Day picker:** 38px day chips, Blue Mist at rest with Slate Muted text, Blue Wash on hover, Signal Blue fill when selected.
- **Switch:** 44×24 pill, off `#cbd5e1`, on Signal Blue; 18px white thumb with a soft navy shadow, slides 20px at 200ms.

### Confirm dialog
Native `<dialog>`, up to 440px, 16px radius, pop shadow, a navy 35% backdrop with 2px blur. A 44px Danger Wash circle with the danger icon, 18px/600 title, slate description; actions right-aligned in a Paper Muted footer strip above a hairline. Opens with a 200ms rise-and-settle (6px, 0.98 scale).

### Empty state
Centred: 56px Blue Wash circle with a Signal Blue icon, 15px/600 navy title, 13px slate description (max 420px), optional action. States what is missing and what to do; never fills the gap with sample data.

### Navigation
- **Sidebar:** full logo (58px), a profile block (48px avatar, name, email, sign-out), then grouped sections: "Inicio" ungrouped, then Cuenta, Dispositivos, Monitoreo, Auditoría. Group labels 15px/600 in Signal Blue.
- **Items:** 42px, 10px radius, 20px Lucide icon in Slate Muted plus 14px/500 ink label. Hover: Blue Mist with Signal Blue text and icon. Active: Signal Blue fill, white text and icon, blue glow, `aria-current="page"`.
- **Unread count:** Danger pill (11px/700, tabular) on the Alerts item; inverts to white-on-blue when that item is active; announced to screen readers as "N sin leer".
- **Rail and drawer:** see Layout. The drawer slides in 200ms and moves focus to its close button; closing returns focus to the menu button.

### Header
White, 72px (64px on phone), hairline bottom border, sticky. Left: a wide search field (up to 560px, 44px, Blue Mist fill, 14px radius; white with Signal Blue border and focus ring when focused) that only searches what the panel already has loaded (sections and linked devices) and says so through its results. Right: 40px icon buttons (bell with a danger dot when there are unread alerts, settings), a hairline divider, and the account button (36px avatar, name, chevron that rotates on open) opening a 240px menu.

### Device card (signature)
The active-device selector at the right of every page header: 280px white card, 14px radius, card shadow; a 48×56 Blue Wash tile with a Signal Blue phone icon, the device name (15px/600 navy, truncated), platform and OS in slate, and the status badge with a dot. A transparent native `<select>` covers the card, so it is keyboard- and screen-reader-native; focus puts the ring on the whole card. Full width at ≤720px.

### Motion
Tokens: `--ease: cubic-bezier(0.2, 0.8, 0.2, 1)`, `--duration-fast: 150ms` (hover and colour changes, menu drop-in), `--duration: 200ms` (drawer, switch, dialog). Motion only reports a state change; nothing moves on its own. The global reduced-motion rule collapses every animation and transition to ~0ms.

## Do's and Don'ts

### Do:
- **Do** take every colour, radius, shadow, space and duration from the `:root` tokens in `globals.css`; each component gets its own CSS Module.
- **Do** keep Signal Blue for the active item, the primary action and "on" states (The One Blue Rule).
- **Do** open every view with the page header: navy 32px title, one-sentence description, device card on the right.
- **Do** build containers as white, 16px-radius cards with a hairline border and the card shadow.
- **Do** pair every status colour with a text label, and pick tones by meaning.
- **Do** use Signal Blue Text (`#1f5fe6`) for blue text and icons, and Signal Blue (`#246bfe`) only for fills.
- **Do** keep text at or above 4.5:1 against its actual background (WCAG 2.1 AA) and give every interactive element the visible focus ring.
- **Do** state data delays in the UI copy ("cada ~15 minutos") instead of implying real time.
- **Do** design each view for 390px and 1440px with equal care; check the rail at 1024px.
- **Do** keep motion between 150 and 250ms, on state changes only, and let the reduced-motion rule apply.
- **Do** draw charts, when they arrive, as hand-made SVG using these tokens.

### Don't:
- **Don't** add a dark theme or dark surfaces; the panel is light only.
- **Don't** show any number, trend, feature, testimonial or sample data the system does not really have; no trend arrows on metric cards.
- **Don't** use Tailwind, a component library or a chart library.
- **Don't** use any icon set other than Lucide line icons, and don't use emoji or glyphs as icons.
- **Don't** introduce a second typeface or replace the NetProtect logo files in `frontend/public/brand/`.
- **Don't** hard-code hex values in components; add or reuse a token.
- **Don't** give resting cards a heavier shadow than the card shadow, or use pop shadows on anything that does not float.
- **Don't** copy the look of the not-yet-redesigned section panels (the "Legacy styles (Sprints 1–24)" block in `globals.css`, including its uppercase `.eyebrow` kicker and bare-button styling); that CSS is transitional and is removed as Sprints 33–38 rebuild each view.
- **Don't** use Slate Subtle (`#94a3b8`) for anything other than placeholders.
