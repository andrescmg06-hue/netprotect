import type { LucideIcon } from "lucide-react";

import type { Tone } from "./StatusBadge";
import styles from "./Timeline.module.css";

export type TimelineItem = {
  id: string;
  icon: LucideIcon;
  tone: Tone;
  title: string;
  occurredAt: string;
};

function dayKey(iso: string): string {
  return new Date(iso).toDateString();
}

function dayLabel(iso: string): string {
  return new Date(iso).toLocaleDateString("es-CO", { weekday: "long", day: "numeric", month: "long" });
}

/** Groups by calendar day in the order items already arrive (the caller controls sort order),
 * so it works for any endpoint that returns events newest-first or oldest-first. */
export function Timeline({ items }: { items: TimelineItem[] }) {
  const groups: { key: string; label: string; items: TimelineItem[] }[] = [];
  for (const item of items) {
    const key = dayKey(item.occurredAt);
    const current = groups.at(-1);
    if (current && current.key === key) {
      current.items.push(item);
    } else {
      groups.push({ key, label: dayLabel(item.occurredAt), items: [item] });
    }
  }

  return (
    <div className={styles.timeline}>
      {groups.map((group) => (
        <div key={group.key} className={styles.day}>
          <h4 className={styles.dayLabel}>{group.label}</h4>
          <ol className={styles.list}>
            {group.items.map((item) => (
              <li key={item.id} className={styles.item}>
                <span className={`${styles.marker} ${styles[item.tone]}`}>
                  <item.icon size={14} strokeWidth={2.25} aria-hidden="true" />
                </span>
                <span className={styles.itemBody}>
                  <span className={styles.itemTitle}>{item.title}</span>
                  <span className={styles.itemTime}>
                    {new Date(item.occurredAt).toLocaleTimeString("es-CO", { hour: "2-digit", minute: "2-digit" })}
                  </span>
                </span>
              </li>
            ))}
          </ol>
        </div>
      ))}
    </div>
  );
}
