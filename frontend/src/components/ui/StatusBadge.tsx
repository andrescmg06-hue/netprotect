import type { LucideIcon } from "lucide-react";
import type { ReactNode } from "react";

import styles from "./StatusBadge.module.css";

export type Tone = "success" | "danger" | "warning" | "info" | "purple" | "neutral";

/** Small pill. `dot` renders the leading status dot ("● En línea"); `icon` a leading icon
 * ("⊘ Bloqueada"). Tone is semantic — pick it by meaning, never for decoration. */
export function StatusBadge({
  tone = "neutral",
  dot = false,
  icon: Icon,
  children,
}: {
  tone?: Tone;
  dot?: boolean;
  icon?: LucideIcon;
  children: ReactNode;
}) {
  return (
    <span className={`${styles.badge} ${styles[tone]}`}>
      {dot && <span className={styles.dot} aria-hidden="true" />}
      {Icon && <Icon size={13} strokeWidth={2.25} aria-hidden="true" />}
      {children}
    </span>
  );
}
