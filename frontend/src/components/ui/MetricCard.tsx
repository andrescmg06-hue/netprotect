import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

import styles from "./MetricCard.module.css";
import type { Tone } from "./StatusBadge";

/** The stat tiles at the top of every view: tinted round icon, label, big value, one line of
 * context. `hint` is plain text on purpose — no invented trend arrows, only real data. */
export function MetricCard({
  icon: Icon,
  tone = "info",
  label,
  value,
  hint,
}: {
  icon: LucideIcon;
  tone?: Tone;
  label: string;
  value: ReactNode;
  hint?: ReactNode;
}) {
  return (
    <div className={styles.metric}>
      <span className={`${styles.iconWrap} ${styles[tone]}`}>
        <Icon size={22} strokeWidth={2} aria-hidden="true" />
      </span>
      <div className={styles.body}>
        <span className={styles.label}>{label}</span>
        <span className={styles.value}>{value}</span>
        {hint && <span className={styles.hint}>{hint}</span>}
      </div>
    </div>
  );
}

export function MetricGrid({ children }: { children: ReactNode }) {
  return <div className={styles.grid}>{children}</div>;
}
