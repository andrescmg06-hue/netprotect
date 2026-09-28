import type { ReactNode } from "react";

import type { Tone } from "./StatusBadge";
import styles from "./ProgressBar.module.css";

/** Sprint 37: a single labeled progress row — cumplimiento de límites diarios, one per rule.
 * `value`/`max` are a fraction, not a percentage the caller has to compute themselves; `null`
 * (no days evaluated yet) renders an empty track instead of a misleading full or empty bar. */
export function ProgressBar({
  label,
  detail,
  value,
  max = 1,
  tone = "info",
}: {
  label: ReactNode;
  detail?: ReactNode;
  value: number | null;
  max?: number;
  tone?: Tone;
}) {
  const fraction = value === null || max <= 0 ? 0 : Math.min(value / max, 1);

  return (
    <div className={styles.row}>
      <div className={styles.head}>
        <span className={styles.label}>{label}</span>
        {detail && <span className={styles.detail}>{detail}</span>}
      </div>
      <div
        className={styles.track}
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={value === null ? undefined : Math.round(fraction * 100)}
        aria-valuetext={value === null ? "Sin datos" : `${Math.round(fraction * 100)}%`}
        aria-label={typeof label === "string" ? label : undefined}
      >
        {value !== null && <div className={`${styles.fill} ${styles[tone]}`} style={{ transform: `scaleX(${fraction})` }} />}
      </div>
    </div>
  );
}
