import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

import styles from "./ReadOnlyField.module.css";

/** A set of read-only facts as one description list: label over value, each pair under a thin
 * stone rule, like the columns of a ledger. `columns` is the most it lays out side by side; it
 * drops to two and then to one as its own width shrinks (a container query, so it adapts to the
 * column it sits in, not to the window). Sprint 55: replaces the ad-hoc label/value pairs of
 * DevicesPanel, AccountPanel and AlertsPanel (docs/redesign/fase-0-informe.md §3). */
export function ReadOnlyFieldList({
  columns = 4,
  className,
  children,
}: {
  columns?: 2 | 3 | 4;
  className?: string;
  children: ReactNode;
}) {
  return (
    <div className={[styles.wrap, className ?? ""].join(" ")}>
      <dl className={`${styles.list} ${styles[`cols${columns}`]}`}>{children}</dl>
    </div>
  );
}

/** One label/value pair; it renders `<dt>`/`<dd>`, so it belongs inside a `ReadOnlyFieldList`.
 * An empty `value` (null, undefined or "") shows `emptyText` in a quieter tone, so a fact the
 * device never reported reads as such instead of as a blank. */
export function ReadOnlyField({
  label,
  value,
  icon: Icon,
  emptyText = "Sin reportar",
}: {
  label: string;
  value?: ReactNode;
  icon?: LucideIcon;
  emptyText?: string;
}) {
  const isEmpty = value === null || value === undefined || value === "";
  return (
    <div className={styles.field}>
      <dt className={styles.label}>
        {Icon && <Icon size={14} strokeWidth={2} aria-hidden="true" />}
        {label}
      </dt>
      <dd className={isEmpty ? `${styles.value} ${styles.empty}` : styles.value}>{isEmpty ? emptyText : value}</dd>
    </div>
  );
}
