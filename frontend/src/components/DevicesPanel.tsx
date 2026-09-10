"use client";

import { useCallback, useState } from "react";

import { ApiError, type Device, renameDevice, unlinkDevice } from "@/lib/apiClient";

export type DevicesState =
  | { kind: "loading" }
  | { kind: "loaded"; devices: Device[] }
  | { kind: "error"; message: string };

function formatLastSeen(value: string | null): string {
  if (!value) {
    return "Nunca";
  }
  return new Date(value).toLocaleString("es-CO", { dateStyle: "medium", timeStyle: "short" });
}

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/** Sprint 24: this used to fetch devices itself and also embed every per-device panel behind
 * toggle buttons (apps, rules, location, …) — that was the whole "dashboard" before this sprint.
 * Now DashboardShell owns the single devices fetch (so the header's device switcher and
 * OverviewPanel see the same list), and each of those panels is its own navigable section; this
 * component goes back to being just device management (list, rename, unlink), same as Sprint 6.
 */
export function DevicesPanel({
  accessToken,
  state,
  reload,
}: {
  accessToken: string;
  state: DevicesState;
  reload: () => void;
}) {
  const [renamingId, setRenamingId] = useState<string | null>(null);
  const [renameValue, setRenameValue] = useState("");
  const [actionError, setActionError] = useState<string | null>(null);

  const handleRename = useCallback(
    (deviceId: string) => {
      setActionError(null);
      renameDevice(accessToken, deviceId, renameValue)
        .then(() => {
          setRenamingId(null);
          reload();
        })
        .catch((error) => {
          setActionError(describeError(error, "No se pudo renombrar el dispositivo"));
        });
    },
    [accessToken, renameValue, reload]
  );

  const handleUnlink = useCallback(
    (deviceId: string) => {
      setActionError(null);
      unlinkDevice(accessToken, deviceId)
        .then(() => reload())
        .catch((error) => {
          setActionError(describeError(error, "No se pudo desvincular el dispositivo"));
        });
    },
    [accessToken, reload]
  );

  return (
    <div className="devicesPanel">
      <div className="devicesPanelHeader">
        <strong>Dispositivos vinculados</strong>
        <button type="button" onClick={reload}>
          Actualizar
        </button>
      </div>

      {actionError && <p className="authError">{actionError}</p>}

      {state.kind === "loading" && <p className="statusText">Cargando dispositivos…</p>}

      {state.kind === "error" && <p className="authError">{state.message}</p>}

      {state.kind === "loaded" && state.devices.length === 0 && (
        <p className="statusText">
          Todavía no hay dispositivos vinculados. Genera un código desde la sección &ldquo;Vinculación&rdquo;.
        </p>
      )}

      {state.kind === "loaded" && state.devices.length > 0 && (
        <ul className="deviceList">
          {state.devices.map((device) => (
            <li key={device.id} className="deviceRow">
              {renamingId === device.id ? (
                <div className="deviceRenameForm">
                  <input
                    value={renameValue}
                    onChange={(event) => setRenameValue(event.target.value)}
                    maxLength={255}
                    aria-label="Nuevo nombre del dispositivo"
                  />
                  <button type="button" onClick={() => handleRename(device.id)}>
                    Guardar
                  </button>
                  <button type="button" onClick={() => setRenamingId(null)}>
                    Cancelar
                  </button>
                </div>
              ) : (
                <>
                  <div>
                    <div className="deviceName">{device.name}</div>
                    <div className="deviceMeta">
                      {device.platform}
                      {device.timezone && ` · ${device.timezone}`} · Visto:{" "}
                      {formatLastSeen(device.status.last_seen_at)}
                    </div>
                  </div>
                  <div className="deviceActions">
                    <span className={`statusPill ${device.status.status.toLowerCase()}`}>
                      {device.status.status}
                    </span>
                    {device.default_app_policy === "BLOCK" && (
                      <span className="statusPill allowlist">SÓLO APPS APROBADAS</span>
                    )}
                    <button
                      type="button"
                      onClick={() => {
                        setRenamingId(device.id);
                        setRenameValue(device.name);
                      }}
                    >
                      Renombrar
                    </button>
                    <button type="button" className="dangerButton" onClick={() => handleUnlink(device.id)}>
                      Desvincular
                    </button>
                  </div>
                </>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
