"use client";

import { ChevronLeft, ChevronRight, Download, FileText, RefreshCw, Search } from "lucide-react";
import { useEffect, useState } from "react";

import { Button, Card, type Column, DataTable, EmptyState, Field, Input, Spinner } from "@/components/ui";
import { type AuditLogEntry, ApiError, type Device, exportMyAuditLog, listMyAuditLog } from "@/lib/apiClient";
import { auditActionLabel, auditResourceLabel } from "@/lib/auditFormatting";

import styles from "./AuditPanel.module.css";

type AuditState =
  | { kind: "loading" }
  | { kind: "loaded"; logs: AuditLogEntry[]; total: number }
  | { kind: "error"; message: string };

const PAGE_SIZE = 20;

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/** Sprint 59: the "documents" drawing of the empty state, composed from lucide line icons so it
 * stays light, sober and on the token palette (no heavy raster illustration). Decorative only. */
function DocumentsIllustration() {
  return (
    <span className={styles.illustration} aria-hidden="true">
      <FileText className={styles.docBack} size={56} strokeWidth={1.25} />
      <FileText className={styles.docFront} size={72} strokeWidth={1.25} />
      <Search className={styles.docLens} size={28} strokeWidth={1.5} />
      <span className={styles.docGround} />
    </span>
  );
}

/** Sprint 22 (logic), Sprint 38 (design), Sprint 59 (editorial recomposition). An account-level
 * activity log, not a per-device panel — it lists the caller's own audited actions
 * (backend/app/api/v1/endpoints/audit.py), which is why it takes no deviceId and is mounted once
 * alongside DevicesPanel rather than inside each device.
 *
 * `devices` is optional and display-only: when given, a row whose resource is a device shows that
 * device's name instead of its id. The mockup's "Limpiar", PDF/Excel export, "Usuario" column (it
 * would always be the caller) and device selector are omitted on purpose — none exists in the API.
 * The two filters stay free text: the closed list of actions in lib/auditFormatting.ts is not
 * exported, and there is no closed list of resource types to offer.
 */
export function AuditPanel({
  accessToken,
  devices,
}: {
  accessToken: string;
  devices?: Pick<Device, "id" | "name">[];
}) {
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
  const hasFilters = actionFilter !== "" || resourceTypeFilter !== "";
  const deviceNames = new Map((devices ?? []).map((device) => [device.id, device.name]));

  const columns: Column<AuditLogEntry>[] = [
    {
      key: "action",
      header: "Acción",
      primary: true,
      render: (entry) => (
        <span className={styles.action} title={entry.action}>
          {auditActionLabel(entry.action)}
        </span>
      ),
    },
    {
      key: "resource",
      header: "Recurso",
      render: (entry) => {
        if (!entry.resource_type) return "—";
        if (entry.resource_type === "device") {
          const name = entry.resource_id ? deviceNames.get(entry.resource_id) : undefined;
          return (
            <span className={styles.resource} title={entry.resource_id ?? undefined}>
              {auditResourceLabel("device")}
              {entry.resource_id ? (
                <span className={name ? styles.resourceName : styles.resourceId}>{name ?? entry.resource_id}</span>
              ) : null}
            </span>
          );
        }
        return (
          <span className={styles.resource}>
            {auditResourceLabel(entry.resource_type)}
            {entry.resource_id ? (
              <span className={styles.resourceId} title={entry.resource_id}>
                {entry.resource_id}
              </span>
            ) : null}
          </span>
        );
      },
    },
    {
      key: "when",
      header: "Fecha",
      render: (entry) => <span className="tabular">{new Date(entry.created_at).toLocaleString("es-CO")}</span>,
    },
    {
      key: "ip",
      header: "IP",
      align: "right",
      render: (entry) => <span className={`tabular ${styles.ip}`}>{entry.ip_address ?? "—"}</span>,
    },
  ];

  return (
    <Card padding="none">
      <header className={styles.head}>
        <div className={styles.headText}>
          <h2 className={styles.title}>Mi actividad</h2>
          <p className={styles.subtitle}>
            Consulta y exporta el registro de las acciones que realizaste en el panel.
          </p>
        </div>
        <Button size="sm" variant="ghost" icon={RefreshCw} onClick={() => setReloadToken((current) => current + 1)}>
          Actualizar
        </Button>
      </header>

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
        <Button icon={Download} onClick={handleExport} loading={exporting} className={styles.export}>
          Exportar CSV
        </Button>
      </div>
      {exportError && <p className={styles.error}>{exportError}</p>}

      {state.kind === "loading" && (
        <div className={styles.loading}>
          <Spinner label="Cargando auditoría…" />
        </div>
      )}
      {state.kind === "error" && <p className={styles.error}>{state.message}</p>}
      {state.kind === "loaded" && logs.length === 0 && (
        <div className={styles.emptyWrap}>
          <EmptyState
            icon={FileText}
            illustration={<DocumentsIllustration />}
            title={hasFilters ? "Sin acciones registradas con estos filtros" : "Aún no hay registros de auditoría"}
            description={
              hasFilters
                ? "Prueba con otra acción u otro tipo de recurso."
                : "Aquí aparecerán las acciones que hagas en el panel, como cambios en dispositivos, reglas o geocercas."
            }
          />
        </div>
      )}
      {state.kind === "loaded" && logs.length > 0 && (
        <>
          <div className={styles.table}>
            <DataTable columns={columns} rows={logs} rowKey={(entry) => entry.id} />
          </div>
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
