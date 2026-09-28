# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

(The tutor web panel. The Android app — one app acting as Tutor or Supervised — is a separate
surface with its own future brief.)

## Users

- **Primary:** parents or guardians ("tutores") without technical background who supervise the
  phone of a child in their care. They check in briefly and often: is the device online, did
  anything alarming happen, is a rule doing what they meant, where is the child approximately.
  They use the panel as much from a phone browser as from a computer.
- **Evaluators:** today the project is presented and graded at university by professors/jury, who
  judge both product quality and engineering rigor.

## Product Purpose

NetProtect lets a tutor supervise and set limits on a child's Android device from one place:
link devices, define app/category rules and school hours, see approximate location and
geofences, review history, statistics and alerts, and — only with the child's explicit consent
each time — view the screen live. Success: a non-technical parent understands the device's state
at a glance and can change a rule without help.

## Positioning

Supervision that stays honest with the supervised person: every sensitive capability is visible
to the child and consent-based (persistent notifications, per-session screen-share consent,
nothing recorded), and the backend is the single source of truth for both apps.

## Operating Context

- Short, frequent check-ins on desktop and phone browsers alike; occasional longer sessions to set
  up rules, schedules and geofences.
- Signed in only with Google; the Google account is the NetProtect account.
- Data arrives with real delays: location every ~15 min, geofence enter/exit detected on the next
  report, usage synced periodically. The UI must never present these as real time.
- Spanish-language UI (es-CO formatting).

## Capabilities and Constraints

- Real capabilities are exactly those of `frontend/src/components/` (inventory in
  `docs/planning/plan-frontend.md`): devices, pairing by 6-digit code, installed apps and usage,
  per-app and per-category rules (block, allow, daily/weekly limit, schedule), default policy and
  school mode, location, geofences, unified history, statistics (today/7d/30d aggregates),
  alerts with levels INFO/WARNING/HIGH/CRITICAL, silences, remote view, own-actions audit with CSV
  export.
- No backend changes as part of UI work; nothing is shown that the system cannot do.
- Remote view never records, captures or controls the device (Sprint 23 decision).
- The audit log is immutable by design; no deleting records.

## Brand Commitments

- Name **NetProtect**, subtitle **PANEL DEL TUTOR**.
- Logo: blue shield with an abstract adult and child, wordmark "Net" dark blue + "Protect"
  bright blue (`frontend/public/brand/`). Not to be replaced.
- Must feel: security, trust, technology, simplicity, control, family protection. Not childish,
  not dark, not generic admin template (user's binding brief, 27/09/2026).

## Evidence on Hand

- 17 reference mockups delivered 27/09/2026 (visual direction only; several show features that do
  not exist and must not be built — listed in `docs/planning/plan-frontend.md`).
- No real testimonials, customers or metrics exist; none may be invented.

## Product Principles

1. Truth over decoration: every number, badge and button maps to real data or a real action.
2. Glanceable first: device state and anything needing attention visible in seconds.
3. Consent is visible: privacy-sensitive features explain themselves where they are used.
4. Delays are stated, not hidden.
5. Equal on phone and desktop.

## Accessibility & Inclusion

Reasonable WCAG 2.1 AA: sufficient contrast, full keyboard navigation with visible focus,
readable text sizes, no information conveyed by color alone, respects reduced motion.
