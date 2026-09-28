"use client";

import { ChevronLeft, ChevronRight, Download, History, RefreshCw } from "lucide-react";
import { useEffect, useState } from "react";

import { Button, Card, CardHeader, type Column, DataTable, EmptyState, Field, Input, Spinner } from "@/components/ui";
import { type AuditLogEntry, ApiError, exportMyAuditLog, listMyAuditLog } from "@/lib/apiClient";

import styles from "./AuditPanel.module.css";

type AuditState =
  | { kind: "loading" }
  | { kind: "loaded"; logs: AuditLogEntry[]; total: number }
  | { kind: "error"; message: string };

const PAGE_SIZE = 20;

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/** Sprint 22 (logic), Sprint 38 (design). An account-level activity log, not a per-device panel —
 * it lists the caller's own audited actions (backend/app/api/v1/endpoints/audit.py), which is why
 * it takes no deviceId and is mounted once alongside DevicesPanel rather than inside each device.
 */
export function AuditPanel({ accessToken }: { accessToken: string }) {
  const [state, setState] = useState<AuditState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);
  const [page, setPage] = useState(0);
  const [actionFilter, setActionFilter] = useState("");
  const [resourceTypeFilter, setResourceTypeFilter] = useState("");
  const [exportError, setExportError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);

  useEffect(() => {
    let cancelled = false;

    listMyAuditLog(accessToken, {
      action: actionFilter || undefined,
      resource_type: resourceTypeFilter || undefined,
      limit: PAGE_SIZE,
      offset: page * PAGE_SIZE,
    })
      .then(({ logs, total }) => {
        if (!cancelled) {
          setState({ kind: "loaded", logs, total });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setState({ kind: "error", message: describeError(error, "No se pudo cargar la auditoría") });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, actionFilter, resourceTypeFilter, page, reloadToken]);

  function handleExport() {
    setExportError(null);
    setExporting(true);
    exportMyAuditLog(accessToken, {
      action: actionFilter || undefined,
      resource_type: resourceTypeFilter || undefined,
    })
      .then((blob) => {
        setExporting(false);
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = "audit-log.csv";
        link.click();
        URL.revokeObjectURL(url);
      })
      .catch((error) => {
        setExporting(false);
        setExportError(describeError(error, "No se pudo exportar la auditoría"));
      });
  }

  const totalPages = state.kind === "loaded" ? Math.max(1, Math.ceil(state.total / PAGE_SIZE)) : 1;
  const logs = state.kind === "loaded" ? state.logs : [];

  const columns: Column<AuditLogEntry>[] = [
    {
      key: "action",
      header: "Acción",
      primary: true,
      render: (entry) => <span className={styles.action}>{entry.action}</span>,
    },
    {
      key: "resource",
      header: "Recurso",
      render: (entry) =>
        entry.resource_type ? (
          <span className={styles.resource}>
            {entry.resource_type}
            {entry.resource_id ? <span className={styles.resourceId}> · {entry.resource_id}</span> : null}
          </span>
        ) : (
          "—"
        ),
    },
    {
      key: "when",
      header: "Fecha",
      render: (entry) => new Date(entry.created_at).toLocaleString("es-CO"),
    },
    {
      key: "ip",
      header: "IP",
      align: "right",
      render: (entry) => entry.ip_address ?? "—",
    },
  ];

  return (
    <Card padding="none">
      <div className={styles.cardHead}>
        <CardHeader
          icon={History}
          title="Mi actividad"
          actions={
            <Button size="sm" icon={RefreshCw} onClick={() => setReloadToken((current) => current + 1)}>
              Actualizar
            </Button>
          }
        />
      </div>

      <div className={styles.filters}>
        <Field label="Acción">
          {(id) => (
            <Input
              id={id}
              value={actionFilter}
              onChange={(event) => {
                setPage(0);
                setActionFilter(event.target.value);
              }}
              placeholder="Ej. DEVICE_RENAMED"
            />
          )}
        </Field>
        <Field label="Tipo de recurso">
          {(id) => (
            <Input
              id={id}
              value={resourceTypeFilter}
              onChange={(event) => {
                setPage(0);
                setResourceTypeFilter(event.target.value);
              }}
              placeholder="Ej. device"
            />
          )}
        </Field>
        <Button icon={Download} onClick={handleExport} loading={exporting}>
          Exportar CSV
        </Button>
      </div>
      {exportError && <p className={styles.error}>{exportError}</p>}

      {state.kind === "loading" && (
        <div className={styles.emptyWrap}>
          <Spinner label="Cargando auditoría…" />
        </div>
      )}
      {state.kind === "error" && <p className={styles.error}>{state.message}</p>}
      {state.kind === "loaded" && logs.length === 0 && (
        <div className={styles.emptyWrap}>
          <EmptyState icon={History} title="Sin acciones registradas con estos filtros" />
        </div>
      )}
      {state.kind === "loaded" && logs.length > 0 && (
        <>
          <DataTable columns={columns} rows={logs} rowKey={(entry) => entry.id} />
          <div className={styles.pagination}>
            <Button size="sm" variant="ghost" icon={ChevronLeft} disabled={page === 0} onClick={() => setPage((current) => current - 1)}>
              Anterior
            </Button>
            <span className={styles.pageInfo}>
              Página {page + 1} de {totalPages} · {state.total} en total
            </span>
            <Button
              size="sm"
              variant="ghost"
              icon={ChevronRight}
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((current) => current + 1)}
            >
              Siguiente
            </Button>
          </div>
        </>
      )}
    </Card>
  );
}
