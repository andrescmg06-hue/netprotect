import { type LucideIcon, RefreshCw } from "lucide-react";
import type { ReactNode } from "react";

import { Button } from "./Button";
import { EmptyState } from "./EmptyState";
import styles from "./LoadState.module.css";
import { Spinner } from "./Spinner";

export type LoadStatus = "loading" | "error" | "empty" | "ready";

type EmptyConfig = { icon: LucideIcon; title: string; description?: ReactNode; action?: ReactNode };

/** The four states every data view goes through, drawn the same way everywhere: a spinner while
 * loading, the error with a retry button, the empty state, or the content (`children`, only when
 * `status` is "ready"). Sprint 55: replaces the per-panel trio of Spinner, EmptyState and a red
 * `<p>` with four different paddings (docs/redesign/fase-0-informe.md §3).
 *
 * `empty` is either a full `EmptyState` (icon, title, description, action) or a plain sentence
 * for a small aside where a centred illustration would be too much. */
export function LoadState({
  status,
  loadingLabel = "Cargando…",
  error,
  onRetry,
  empty,
  children,
}: {
  status: LoadStatus;
  loadingLabel?: string;
  error?: string;
  onRetry?: () => void;
  empty?: EmptyConfig | string;
  children?: ReactNode;
}) {
  if (status === "loading") {
    return (
      <div className={styles.state}>
        <Spinner label={loadingLabel} />
      </div>
    );
  }

  if (status === "error") {
    return (
      <div className={styles.error} role="alert">
        <p className={styles.message}>{error ?? "No se pudo cargar esta información."}</p>
        {onRetry && (
          <Button size="sm" icon={RefreshCw} onClick={onRetry}>
            Reintentar
          </Button>
        )}
      </div>
    );
  }

  if (status === "empty") {
    if (!empty) return null;
    if (typeof empty === "string") {
      return <p className={styles.quiet}>{empty}</p>;
    }
    return <EmptyState {...empty} />;
  }

  return <>{children}</>;
}
