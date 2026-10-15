import { ChevronRight, House } from "lucide-react";
import Image from "next/image";
import type { ReactNode } from "react";

import styles from "./PageHeader.module.css";

/** Top of every view: big title, one-line description, breadcrumb and a right-hand slot (the
 * device selector on per-device views, a primary action elsewhere).
 *
 * `band` (default false) turns it into the editorial header: a photographic band behind the
 * text, with a cream veil on the left so the title stays readable, and room for a one-line
 * `quote`. `quote` is only shown in band mode. With `band` false the markup is the plain
 * header, unchanged. */
export function PageHeader({
  title,
  description,
  breadcrumb,
  aside,
  band = false,
  quote,
}: {
  title: string;
  description?: ReactNode;
  breadcrumb?: string;
  aside?: ReactNode;
  band?: boolean;
  quote?: string;
}) {
  const breadcrumbNav = breadcrumb && (
    <nav className={styles.breadcrumb} aria-label="Ruta">
      <House size={16} aria-hidden="true" />
      <span>Inicio</span>
      <ChevronRight size={14} aria-hidden="true" />
      <span className={styles.current}>{breadcrumb}</span>
    </nav>
  );

  if (band) {
    return (
      <div className={`${styles.header} ${styles.band}`}>
        <Image className={styles.photo} src="/brand/band-alpine.jpg" alt="" fill sizes="100vw" loading="eager" />
        <div className={styles.veil} aria-hidden="true" />
        <div className={`${styles.text} ${styles.bandText}`}>
          {breadcrumbNav}
          <h1 className={`${styles.title} ${styles.bandTitle}`}>{title}</h1>
          {description && <p className={styles.description}>{description}</p>}
          {quote && <p className={`quote ${styles.quote}`}>{quote}</p>}
        </div>
        {aside && <div className={`${styles.aside} ${styles.bandAside}`}>{aside}</div>}
      </div>
    );
  }

  return (
    <div className={styles.header}>
      <div className={styles.text}>
        <h1 className={styles.title}>{title}</h1>
        {description && <p className={styles.description}>{description}</p>}
      </div>
      <div className={styles.aside}>
        {breadcrumbNav}
        {aside}
      </div>
    </div>
  );
}
