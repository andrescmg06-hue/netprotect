import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

import { Card, CardHeader } from "./Card";
import { EmptyState } from "./EmptyState";
import styles from "./ChartCard.module.css";

/** Sprint 37: the "Card + CardHeader + padded body (+ empty state)" shell that StatisticsPanel's
 * four charts all repeat, formalized once instead of copy-pasted three more times (rule 3 of
 * plan-frontend.md — components before views). */
export function ChartCard({
  icon: Icon,
  title,
  subtitle,
  actions,
  isEmpty,
  emptyLabel,
  children,
}: {
  icon: LucideIcon;
  title: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
  isEmpty?: boolean;
  emptyLabel?: string;
  children: ReactNode;
}) {
  return (
    <Card padding="none">
      <div className={styles.head}>
        <CardHeader icon={Icon} title={title} subtitle={subtitle} actions={actions} />
      </div>
      <div className={styles.body}>
        {isEmpty ? <EmptyState icon={Icon} title={emptyLabel ?? "Sin datos en este periodo"} /> : children}
      </div>
    </Card>
  );
}
