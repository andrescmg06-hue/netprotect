import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

import styles from "./EmptyState.module.css";

export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
}: {
  icon: LucideIcon;
  title: string;
  description?: ReactNode;
  action?: ReactNode;
}) {
  return (
    <div className={styles.empty}>
      <span className={styles.icon}>
        <Icon size={26} strokeWidth={1.75} aria-hidden="true" />
      </span>
      <p className={styles.title}>{title}</p>
      {description && <p className={styles.description}>{description}</p>}
      {action}
    </div>
  );
}
