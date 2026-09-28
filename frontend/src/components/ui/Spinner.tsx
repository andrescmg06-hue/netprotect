import { LoaderCircle } from "lucide-react";

import styles from "./Spinner.module.css";

export function Spinner({ size = 20, label }: { size?: number; label?: string }) {
  return (
    <span className={styles.spinner} role={label ? "status" : undefined}>
      <LoaderCircle size={size} strokeWidth={2} aria-hidden="true" />
      {label && <span className={styles.label}>{label}</span>}
    </span>
  );
}
