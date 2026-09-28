---
version: 1
slug: "frontend-src-components-dashboardshell-tsx"
primary_target: "frontend/src/components/DashboardShell.tsx"
related_targets: ["frontend/src/components/shell"]
---

Scope: authenticated tutor panel shell (sidebar, header, page header, device selector) that every section renders inside. Mode: Operate.
Audience/job: non-technical parent checking device state and changing rules, on phone and desktop equally.
Constraints: user-pinned brief + 17 mockups set the visual world; only real functionality; hash routing and DashboardShell state unchanged.

## Direction contract
THESIS: One calm light-blue frame where the current device and anything needing attention are always one glance away; refuses the dense dark admin template.
OWN-WORLD: White sidebar and header on #F5F9FF ground, one blue (#246BFE) reserved for the active item, primary actions and state; soft-shadow white cards, Lucide line icons, Inter.
STORY: Tutor sees who they are, which device they are looking at, unread alerts, and reaches any section in one click.
FIRST VIEWPORT: 270px sidebar (logo, profile, grouped nav); 72px header (wide search left; bell with count, settings, avatar right); page title + description left, device card right.
FORM: brief-pinned mockup layout, no concept roll (pinned brief beats the roll); seed key: none (pinned).
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
