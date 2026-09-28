import type { LucideIcon } from "lucide-react";
import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from "react";
import { useId } from "react";

import styles from "./Field.module.css";

/** Label + control + hint/error. Controls below are plain native elements (so browsers keep
 * their own keyboard/accessibility behaviour) styled once, here. */
export function Field({
  label,
  hint,
  error,
  children,
}: {
  label?: string;
  hint?: ReactNode;
  error?: ReactNode;
  children: (id: string) => ReactNode;
}) {
  const id = useId();
  return (
    <div className={styles.field}>
      {label && (
        <label className={styles.label} htmlFor={id}>
          {label}
        </label>
      )}
      {children(id)}
      {error ? <span className={styles.error}>{error}</span> : hint && <span className={styles.hint}>{hint}</span>}
    </div>
  );
}

export function Input({
  icon: Icon,
  className,
  ...rest
}: InputHTMLAttributes<HTMLInputElement> & { icon?: LucideIcon }) {
  if (!Icon) {
    return <input className={[styles.control, className ?? ""].join(" ")} {...rest} />;
  }
  return (
    <span className={styles.withIcon}>
      <Icon size={16} className={styles.leadingIcon} aria-hidden="true" />
      <input className={[styles.control, styles.padded, className ?? ""].join(" ")} {...rest} />
    </span>
  );
}

export function Select({ className, children, ...rest }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select className={[styles.control, styles.select, className ?? ""].join(" ")} {...rest}>
      {children}
    </select>
  );
}
