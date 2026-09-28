import styles from "./DonutChart.module.css";

export type DonutSegment = {
  key: string;
  label: string;
  value: number;
  color: string;
};

const RADIUS = 60;
const STROKE = 22;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

/** Sprint 37: a hand-made SVG donut (T4, no chart library) — each segment is a full-circle
 * `<circle>` clipped to its share with `stroke-dasharray`/`stroke-dashoffset`, rotated -90° so
 * the first segment starts at 12 o'clock like every donut chart convention. Values are already
 * real aggregates from the backend (Sprint 16's statistics endpoint); this component only draws
 * them and never computes totals or percentages beyond what it needs to size an arc. */
export function DonutChart({
  segments,
  centerValue,
  centerLabel,
  formatValue,
}: {
  segments: DonutSegment[];
  centerValue?: string;
  centerLabel?: string;
  formatValue?: (value: number) => string;
}) {
  const total = segments.reduce((sum, segment) => sum + segment.value, 0);
  const arcs = segments
    .filter((segment) => segment.value > 0)
    .reduce<{ segment: DonutSegment; dash: number; offset: number }[]>((acc, segment) => {
      const cumulative = acc.reduce((sum, arc) => sum + arc.dash, 0) / CIRCUMFERENCE;
      const fraction = total > 0 ? segment.value / total : 0;
      acc.push({ segment, dash: fraction * CIRCUMFERENCE, offset: -cumulative * CIRCUMFERENCE });
      return acc;
    }, []);

  return (
    <div className={styles.wrap}>
      <svg viewBox="0 0 160 160" className={styles.svg} role="img" aria-label="Distribución por categoría">
        <circle cx="80" cy="80" r={RADIUS} fill="none" stroke="var(--color-neutral-soft)" strokeWidth={STROKE} />
        {total > 0 && (
          <g transform="rotate(-90 80 80)">
            {arcs.map(({ segment, dash, offset }) => (
              <circle
                key={segment.key}
                cx="80"
                cy="80"
                r={RADIUS}
                fill="none"
                stroke={segment.color}
                strokeWidth={STROKE}
                strokeDasharray={`${dash} ${CIRCUMFERENCE - dash}`}
                strokeDashoffset={offset}
              />
            ))}
          </g>
        )}
        {centerValue && (
          <text x="80" y={centerLabel ? "76" : "86"} textAnchor="middle" className={styles.centerValue}>
            {centerValue}
          </text>
        )}
        {centerLabel && (
          <text x="80" y="96" textAnchor="middle" className={styles.centerLabel}>
            {centerLabel}
          </text>
        )}
      </svg>

      <ul className={styles.legend}>
        {segments.map((segment) => (
          <li key={segment.key} className={styles.legendRow}>
            <span className={styles.swatch} style={{ background: segment.color }} aria-hidden="true" />
            <span className={styles.legendLabel}>{segment.label}</span>
            <span className={styles.legendValue}>
              {formatValue ? formatValue(segment.value) : segment.value}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
