import Image from "next/image";

/** Product showcase: a laptop + phone render with an illustrative "Dispositivos" screen — static
 * example data, not a live view (the real one is DevicesPanel/StatisticsPanel, behind sign-in).
 * Purely illustrative, so it's out of the accessibility tree entirely rather than described as if
 * it were real content.
 *
 * public/login-mockup/devices-showcase.png is a real transparent export (verified: alpha 0 at
 * every corner, opaque only over the devices themselves), so it sits directly on the page
 * background with no frame needed. Its real size is 1536×1024; `next/image` needs that to reserve
 * layout space and avoid a layout shift while it loads.
 *
 * Self-positioned (`lg:absolute`) instead of sized by its flex parent: the reference mockup gives
 * an exact box — left edge 14.7vw, top 38vh, at most 49vw wide and 47vh tall — not a flow
 * relationship to the hero text above it. `max-w`/`max-h` + `object-contain` + `object-left-top`
 * scale the real image down to fit inside that box, preserving its aspect ratio, anchored to the
 * box's top-left corner (matching where the mockup measured the laptop's top edge) rather than
 * centered inside slack space. Hidden below `lg`, same as before — the mockup's coordinates only
 * ever targeted the desktop sizes it was checked against.
 */
export function HeroShowcase() {
  return (
    <div
      aria-hidden="true"
      className="pointer-events-none absolute left-[14.7vw] top-[38vh] hidden max-h-[47vh] max-w-[49vw] select-none lg:block"
    >
      <Image
        src="/login-mockup/devices-showcase.png"
        alt=""
        width={1536}
        height={1024}
        priority
        className="h-auto max-h-[47vh] w-auto max-w-[49vw] object-contain object-left-top"
      />
    </div>
  );
}
