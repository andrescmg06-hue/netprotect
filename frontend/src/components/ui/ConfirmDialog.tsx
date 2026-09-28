"use client";

import { TriangleAlert } from "lucide-react";
import { type ReactNode, useEffect, useRef } from "react";

import { Button } from "./Button";
import styles from "./ConfirmDialog.module.css";

/** Native <dialog> opened with showModal(): the browser supplies the focus trap, Escape to
 * close and the inert background, instead of reimplementing them. The effect only calls DOM
 * methods, never setState, so it stays clear of react-hooks/set-state-in-effect (CLAUDE.md). */
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel,
  tone = "danger",
  busy = false,
  onConfirm,
  onCancel,
}: {
  open: boolean;
  title: string;
  description?: ReactNode;
  confirmLabel: string;
  tone?: "danger" | "primary";
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const ref = useRef<HTMLDialogElement | null>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      ref={ref}
      className={styles.dialog}
      onCancel={(event) => {
        event.preventDefault();
        onCancel();
      }}
    >
      <div className={styles.body}>
        {tone === "danger" && (
          <span className={styles.icon}>
            <TriangleAlert size={22} aria-hidden="true" />
          </span>
        )}
        <h2 className={styles.title}>{title}</h2>
        {description && <p className={styles.description}>{description}</p>}
      </div>
      <div className={styles.actions}>
        <Button variant="secondary" onClick={onCancel} disabled={busy}>
          Cancelar
        </Button>
        <Button variant={tone === "danger" ? "danger" : "primary"} onClick={onConfirm} loading={busy}>
          {confirmLabel}
        </Button>
      </div>
    </dialog>
  );
}
