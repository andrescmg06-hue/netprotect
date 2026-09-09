"use client";

import { useEffect, useState } from "react";

import {
  type AuditLogEntry,
  ApiError,
  exportMyAuditLog,
  listMyAuditLog,
} from "@/lib/apiClient";

type AuditState =
  | { kind: "loading" }
  | { kind: "loaded"; logs: AuditLogEntry[]; total: number }
  | { kind: "error"; message: string };

const PAGE_SIZE = 20;

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/** Sprint 22: an account-level activity log, not a per-device panel — it lists the caller's own
 * audited actions (backend/app/api/v1/endpoints/audit.py), which is why it takes no deviceId and
 * is mounted once in page.tsx alongside DevicesPanel rather than inside each device row.
 */
export function AuditPanel({ accessToken }: { accessToken: string }) {
  const [state, setState] = useState<AuditState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);
  const [page, setPage] = useState(0);
  const [actionFilter, setActionFilter] = useState("");
  const [resourceTypeFilter, setResourceTypeFilter] = useState("");
  const [exportError, setExportError] = useState<string | null>(null);

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
    exportMyAuditLog(accessToken, {
      action: actionFilter || undefined,
      resource_type: resourceTypeFilter || undefined,
    })
      .then((blob) => {
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = "audit-log.csv";
        link.click();
        URL.revokeObjectURL(url);
      })
      .catch((error) => {
        setExportError(describeError(error, "No se pudo exportar la auditoría"));
      });
  }

  const totalPages = state.kind === "loaded" ? Math.max(1, Math.ceil(state.total / PAGE_SIZE)) : 1;

  return (
    <div className="rulesPanel">
      <div className="devicesPanelHeader">
        <strong>Mi actividad (auditoría)</strong>
        <button type="button" onClick={() => setReloadToken((current) => current + 1)}>
          Actualizar
        </button>
      </div>

      <div className="deviceRenameForm">
        <input
          placeholder="Filtrar por acción (ej. DEVICE_RENAMED)"
          value={actionFilter}
          onChange={(event) => {
            setPage(0);
            setActionFilter(event.target.value);
          }}
        />
        <input
          placeholder="Filtrar por tipo de recurso (ej. device)"
          value={resourceTypeFilter}
          onChange={(event) => {
            setPage(0);
            setResourceTypeFilter(event.target.value);
          }}
        />
        <button type="button" onClick={handleExport}>
          Exportar CSV
        </button>
      </div>
      {exportError && <p className="authError">{exportError}</p>}

      {state.kind === "loading" && <p className="statusText">Cargando auditoría…</p>}
      {state.kind === "error" && <p className="authError">{state.message}</p>}
      {state.kind === "loaded" && state.logs.length === 0 && (
        <p className="statusText">Sin acciones registradas con estos filtros.</p>
      )}
      {state.kind === "loaded" && state.logs.length > 0 && (
        <>
          <ul className="appList">
            {state.logs.map((entry) => (
              <li key={entry.id} className="appRow">
                <div className="appLabel">
                  {entry.action}
                  {entry.resource_type ? ` · ${entry.resource_type}` : ""}
                  {entry.resource_id ? ` (${entry.resource_id})` : ""}
                </div>
                <span className="appUsage">
                  {new Date(entry.created_at).toLocaleString("es-CO")}
                  {entry.ip_address ? ` · ${entry.ip_address}` : ""}
                </span>
              </li>
            ))}
          </ul>
          <div className="deviceRenameForm">
            <button type="button" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>
              Anterior
            </button>
            <span className="statusText">
              Página {page + 1} de {totalPages} · {state.total} en total
            </span>
            <button
              type="button"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((current) => current + 1)}
            >
              Siguiente
            </button>
          </div>
        </>
      )}
    </div>
  );
}
