import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

import styles from "./EmptyState.module.css";

/** Empty / "nothing selected" placeholder. `illustration` (optional) replaces the icon disc with a
 * lighter drawing; its alt text, if any, is the caller's to set. Without it the component renders
 * exactly as before: the disc with `icon`. */
export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  illustration,
}: {
  icon: LucideIcon;
  title: string;
  description?: ReactNode;
  action?: ReactNode;
  illustration?: ReactNode;
}) {
  return (
    <div className={styles.empty}>
      {illustration ? (
        <div className={styles.illustration}>{illustration}</div>
      ) : (
        <span className={styles.icon}>
          <Icon size={26} strokeWidth={1.75} aria-hidden="true" />
        </span>
      )}
      <p className={styles.title}>{title}</p>
      {description && <p className={styles.description}>{description}</p>}
      {action}
    </div>
  );
}
