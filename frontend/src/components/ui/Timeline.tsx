import type { LucideIcon } from "lucide-react";

import { dayKey, formatDayLabel, formatTime } from "@/lib/format";

import type { Tone } from "./StatusBadge";
import styles from "./Timeline.module.css";

export type TimelineItem = {
  id: string;
  icon: LucideIcon;
  tone: Tone;
  title: string;
  occurredAt: string;
  /** Optional second line under the title: which device, which rule. */
  description?: string;
};

/** Sprint 36 timeline, recomposed in Sprint 55 as the editorial one of the redesign
 * (docs/redesign/02_DESIGN_TARGET.md §3): the time on the left, one thin line joining the events
 * of a day, and the node of the most recent event in Signal Blue, the one accent of the list. The
 * tone colours only the small icon beside each title, so a long list stays calm.
 *
 * Grouping by calendar day follows the order items arrive in (the caller sorts), so it works for
 * newest-first and oldest-first lists alike. `groupByDay={false}` drops the day headings and puts
 * the day under each time instead. `headingLevel` keeps the document outline right wherever the
 * timeline sits (h4 under a card's h3, h3 under a section's h2). The Sprint 36 props are
 * unchanged. Below ~26rem of width the time moves above the title instead of beside it. */
export function Timeline({
  items,
  groupByDay = true,
  headingLevel = 4,
}: {
  items: TimelineItem[];
  groupByDay?: boolean;
  headingLevel?: 3 | 4;
}) {
  const groups: { key: string; label: string; items: TimelineItem[] }[] = [];
  for (const item of items) {
    const key = groupByDay ? dayKey(item.occurredAt) : "all";
    const current = groups.at(-1);
    if (current && current.key === key) {
      current.items.push(item);
    } else {
      groups.push({ key, label: formatDayLabel(item.occurredAt), items: [item] });
    }
  }

  // By timestamp, not by position: the caller may sort either way.
  const latestId = items.reduce<TimelineItem | null>(
    (latest, item) => (!latest || Date.parse(item.occurredAt) > Date.parse(latest.occurredAt) ? item : latest),
    null
  )?.id;
  const DayHeading = headingLevel === 3 ? "h3" : "h4";

  return (
    <div className={styles.timeline}>
      {groups.map((group) => (
        <div key={group.key} className={styles.day}>
          {groupByDay && <DayHeading className={styles.dayLabel}>{group.label}</DayHeading>}
          <ol className={styles.list}>
            {group.items.map((item) => {
              const Icon = item.icon;
              return (
                <li key={item.id} className={item.id === latestId ? `${styles.item} ${styles.latest}` : styles.item}>
                  <time className={styles.time} dateTime={item.occurredAt}>
                    {formatTime(item.occurredAt)}
                    {!groupByDay && <span className={styles.timeDay}>{formatDayLabel(item.occurredAt)}</span>}
                  </time>
                  <span className={styles.node} aria-hidden="true" />
                  <div className={styles.body}>
                    <p className={styles.title}>
                      <Icon
                        size={15}
                        strokeWidth={2}
                        className={`${styles.icon} ${styles[item.tone]}`}
                        aria-hidden="true"
                      />
                      <span>{item.title}</span>
                    </p>
                    {item.description && <p className={styles.description}>{item.description}</p>}
                  </div>
                </li>
              );
            })}
          </ol>
        </div>
      ))}
    </div>
  );
}
