import type { ReactNode } from "react";

import styles from "./BarList.module.css";

export type BarListItem = {
  key: string;
  label: ReactNode;
  value: number;
  color?: string;
  icon?: ReactNode;
};

/** Sprint 37: ranked horizontal bars (T4, no chart library) — apps más usadas, bloqueos por
 * tipo. Bars scale against the list's own max, same "relative to itself" rule
 * DeviceApplicationsList's usage bar already uses. `transform: scaleX` (not `width`), same fix
 * PairingPanel's progress bar needed in Sprint 34 to avoid a layout-thrash repaint. */
export function BarList({
  items,
  formatValue,
}: {
  items: BarListItem[];
  formatValue?: (value: number) => string;
}) {
  const max = Math.max(...items.map((item) => item.value), 1);

  return (
    <ul className={styles.list}>
      {items.map((item) => (
        <li key={item.key} className={styles.row}>
          <div className={styles.head}>
            <span className={styles.label}>
              {item.icon}
              {item.label}
            </span>
            <span className={styles.value}>{formatValue ? formatValue(item.value) : item.value}</span>
          </div>
          <div className={styles.track}>
            <div
              className={styles.fill}
              style={{
                transform: `scaleX(${item.value / max})`,
                background: item.color ?? "var(--color-primary)",
              }}
            />
          </div>
        </li>
      ))}
    </ul>
  );
}
