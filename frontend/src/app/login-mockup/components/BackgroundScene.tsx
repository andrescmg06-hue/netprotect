/** Full-bleed page background. Same real photo as before (plant, window light, desk), with the
 * decorative blue ribbon shapes removed: background-desk-clean.png is a processed copy
 * (public/login-mockup/background-desk.png untouched as the source) — the shape-free left ~700px
 * kept as-is, the shape region past it replaced with a stretched, blurred sample of the clean
 * strip just before it and cross-faded at the seam, so it reads as one continuous soft photo
 * with no visible join, not a patch.
 *
 * `absolute` (not `fixed`) + no z-index, positioned first in the DOM: globals.css already paints
 * an opaque gradient on `<body>` itself, which a `fixed`/negative-z-index sibling would paint
 * behind regardless of z-index — this stacks by DOM order among the page's `relative` children
 * instead, which is what actually puts it behind the content and in front of `<body>`.
 *
 * Fourth pass: the reference mockup shows the photo almost bare (clear plant, window-light
 * stripes, bright desk) — the earlier directional scrim was covering too much of it. Down to a
 * single flat wash at ≤.15 opacity, just enough to keep the dark hero text readable without
 * visibly dimming the photo.
 */
export function BackgroundScene() {
  return (
    <div
      aria-hidden="true"
      className="pointer-events-none absolute inset-0 bg-[#F4F8FF] bg-cover bg-center bg-no-repeat"
      style={{
        backgroundImage:
          "linear-gradient(rgba(248,251,255,0.15), rgba(248,251,255,0.15)), url(/login-mockup/background-desk-clean.png)",
      }}
    />
  );
}
