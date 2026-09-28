import { ChevronRight, House } from "lucide-react";
import type { ReactNode } from "react";

import styles from "./PageHeader.module.css";

/** Top of every view: big title, one-line description, breadcrumb and a right-hand slot (the
 * device selector on per-device views, a primary action elsewhere). */
export function PageHeader({
  title,
  description,
  breadcrumb,
  aside,
}: {
  title: string;
  description?: ReactNode;
  breadcrumb?: string;
  aside?: ReactNode;
}) {
  return (
    <div className={styles.header}>
      <div className={styles.text}>
        <h1 className={styles.title}>{title}</h1>
        {description && <p className={styles.description}>{description}</p>}
      </div>
      <div className={styles.aside}>
        {breadcrumb && (
          <nav className={styles.breadcrumb} aria-label="Ruta">
            <House size={16} aria-hidden="true" />
            <span>Inicio</span>
            <ChevronRight size={14} aria-hidden="true" />
            <span className={styles.current}>{breadcrumb}</span>
          </nav>
        )}
        {aside}
      </div>
    </div>
  );
}
