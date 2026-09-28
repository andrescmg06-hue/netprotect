import type { LucideIcon } from "lucide-react";
import type { HTMLAttributes, ReactNode } from "react";

import styles from "./Card.module.css";

type Padding = "none" | "md" | "lg";

export function Card({
  padding = "lg",
  className,
  children,
  ...rest
}: HTMLAttributes<HTMLElement> & { padding?: Padding }) {
  return (
    <section className={[styles.card, styles[padding], className ?? ""].join(" ")} {...rest}>
      {children}
    </section>
  );
}

/** Title row of a card: blue icon, title, optional subtitle, and actions pushed to the right. */
export function CardHeader({
  icon: Icon,
  title,
  subtitle,
  actions,
}: {
  icon?: LucideIcon;
  title: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
}) {
  return (
    <header className={styles.header}>
      <div className={styles.headerText}>
        <h3 className={styles.title}>
          {Icon && <Icon size={20} strokeWidth={2} className={styles.icon} aria-hidden="true" />}
          {title}
        </h3>
        {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
      </div>
      {actions && <div className={styles.actions}>{actions}</div>}
    </header>
  );
}
