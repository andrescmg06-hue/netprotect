"use client";

import { useCallback, useEffect, useState } from "react";

import {
  ApiError,
  type Device,
  ensureTutorRole,
  listDevices,
  renameDevice,
  unlinkDevice,
} from "@/lib/apiClient";

import { DeviceApplicationsList } from "./DeviceApplicationsList";
import { DeviceRulesPanel } from "./DeviceRulesPanel";

type PanelState =
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

export function DevicesPanel({ accessToken }: { accessToken: string }) {
  const [state, setState] = useState<PanelState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);
  const [renamingId, setRenamingId] = useState<string | null>(null);
  const [renameValue, setRenameValue] = useState("");
  const [actionError, setActionError] = useState<string | null>(null);
  const [expandedAppsId, setExpandedAppsId] = useState<string | null>(null);
  const [expandedRulesId, setExpandedRulesId] = useState<string | null>(null);

  // The web panel is tutor-only: make sure this account holds TUTOR, then list its devices.
  // Every state update happens inside a .then/.catch callback rather than synchronously in
  // the effect body, so this only ever reacts to the fetch settling.
  useEffect(() => {
    let cancelled = false;

    ensureTutorRole(accessToken)
      .then(() => listDevices(accessToken))
      .then(({ devices }) => {
        if (!cancelled) {
          setState({ kind: "loaded", devices });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setState({ kind: "error", message: describeError(error, "No se pudo cargar la lista de dispositivos") });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, reloadToken]);

  const reload = useCallback(() => {
    setState({ kind: "loading" });
    setReloadToken((current) => current + 1);
  }, []);

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
          Todavía no hay dispositivos vinculados. Genera un código de vinculación desde la app
          del tutor.
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
                      {device.platform} · Visto: {formatLastSeen(device.status.last_seen_at)}
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
                    <button
                      type="button"
                      onClick={() =>
                        setExpandedAppsId(expandedAppsId === device.id ? null : device.id)
                      }
                    >
                      {expandedAppsId === device.id ? "Ocultar apps" : "Ver apps"}
                    </button>
                    <button
                      type="button"
                      onClick={() =>
                        setExpandedRulesId(expandedRulesId === device.id ? null : device.id)
                      }
                    >
                      {expandedRulesId === device.id ? "Ocultar reglas" : "Gestionar reglas"}
                    </button>
                  </div>
                  {expandedAppsId === device.id && (
                    <DeviceApplicationsList accessToken={accessToken} deviceId={device.id} />
                  )}
                  {expandedRulesId === device.id && (
                    <DeviceRulesPanel
                      accessToken={accessToken}
                      deviceId={device.id}
                      defaultAppPolicy={device.default_app_policy}
                      onPolicyChanged={reload}
                    />
                  )}
                </>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
