/** Sprint 37: a fixed, stable palette for chart segments/bars that don't have a semantic tone of
 * their own (categories, applied-rule types) — same hexes as the design tokens in globals.css,
 * repeated as literals because SVG `stroke`/`fill` can't read CSS custom properties that live on
 * a different element's `:root` scope reliably across browsers for `<circle>` strokes.
 *
 * Sprint 59 fix: exactly one colour per catalogue category (11), all visually distinct — no hue
 * appears twice and no two neighbours are near-identical greens/teals. "Sin categoría" is not part
 * of the cycle: it gets the neutral stone below, so it can never collide with a real category.
 */
export const CHART_COLORS = [
  "#1769ff", // blue; brand blue as of Sprint 53, keep in step with --color-primary
  "#7a5af8", // violet
  "#16b364", // green
  "#f79009", // orange
  "#f04438", // red
  "#db2777", // pink
  "#0891b2", // cyan
  "#92400e", // brown
  "#11243d", // navy ink, same as --color-navy
  "#0f766e", // deep teal
  "#ca8a04", // gold
];

/** Stone, same hex as --color-border-strong: the "no category" segment. */
export const CHART_NEUTRAL = "#8a8678";

export function chartColor(index: number): string {
  return CHART_COLORS[index % CHART_COLORS.length];
}
