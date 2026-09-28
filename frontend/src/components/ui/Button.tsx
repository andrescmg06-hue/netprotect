import type { LucideIcon } from "lucide-react";
import type { ButtonHTMLAttributes } from "react";

import styles from "./Button.module.css";
import { Spinner } from "./Spinner";

type Variant = "primary" | "secondary" | "danger" | "ghost";
type Size = "sm" | "md";

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  size?: Size;
  icon?: LucideIcon;
  loading?: boolean;
  fullWidth?: boolean;
};

export function Button({
  variant = "secondary",
  size = "md",
  icon: Icon,
  loading = false,
  fullWidth = false,
  type = "button",
  className,
  disabled,
  children,
  ...rest
}: ButtonProps) {
  const classes = [
    styles.button,
    styles[variant],
    styles[size],
    fullWidth ? styles.fullWidth : "",
    className ?? "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button type={type} className={classes} disabled={disabled || loading} {...rest}>
      {loading ? <Spinner size={16} /> : Icon && <Icon size={16} strokeWidth={2} aria-hidden="true" />}
      {children}
    </button>
  );
}
