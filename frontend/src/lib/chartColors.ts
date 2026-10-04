/** Sprint 37: a fixed, stable palette for chart segments/bars that don't have a semantic tone of
 * their own (categories, applied-rule types) — same hexes as the design tokens in globals.css,
 * repeated as literals because SVG `stroke`/`fill` can't read CSS custom properties that live on
 * a different element's `:root` scope reliably across browsers for `<circle>` strokes.
 */
export const CHART_COLORS = [
  "#1769ff", // brand blue as of Sprint 53 (was #246bfe); keep in step with --color-primary
  "#7a5af8",
  "#16b364",
  "#f79009",
  "#f04438",
  "#0e8a8a",
  "#c026d3",
  "#64748b",
  "#0891b2",
  "#ca8a04",
  "#be123c",
];

export function chartColor(index: number): string {
  return CHART_COLORS[index % CHART_COLORS.length];
}
