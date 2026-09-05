"use client";

import { useEffect, useState } from "react";

import { ApiError, type DeviceApplication, listDeviceApplications } from "@/lib/apiClient";

type AppsState =
  | { kind: "loading" }
  | { kind: "loaded"; apps: DeviceApplication[] }
  | { kind: "error"; message: string };

function formatUsageDuration(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  if (hours > 0) {
    return `${hours} h ${minutes} min`;
  }
  if (minutes > 0) {
    return `${minutes} min`;
  }
  return "< 1 min";
}

export function DeviceApplicationsList({
  accessToken,
  deviceId,
}: {
  accessToken: string;
  deviceId: string;
}) {
  const [state, setState] = useState<AppsState>({ kind: "loading" });

  useEffect(() => {
    let cancelled = false;

    listDeviceApplications(accessToken, deviceId)
      .then(({ applications }) => {
        if (!cancelled) {
          setState({ kind: "loaded", apps: applications });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setState({
            kind: "error",
            message:
              error instanceof ApiError ? error.message : "No se pudo cargar la lista de apps",
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId]);

  if (state.kind === "loading") {
    return <p className="statusText">Cargando apps…</p>;
  }

  if (state.kind === "error") {
    return <p className="authError">{state.message}</p>;
  }

  if (state.apps.length === 0) {
    return (
      <p className="statusText">
        Todavía no se sincronizó ninguna app desde este dispositivo.
      </p>
    );
  }

  const sorted = [...state.apps].sort(
    (a, b) =>
      (b.latest_usage?.foreground_seconds ?? -1) - (a.latest_usage?.foreground_seconds ?? -1)
  );

  return (
    <ul className="appList">
      {sorted.map((app) => (
        <li key={app.package_name} className="appRow">
          <div>
            <div className="appLabel">{app.app_label}</div>
            {app.uninstalled_at && <div className="appMeta">Desinstalada</div>}
          </div>
          <span className="appUsage">
            {app.latest_usage
              ? formatUsageDuration(app.latest_usage.foreground_seconds)
              : "Sin datos de uso"}
          </span>
        </li>
      ))}
    </ul>
  );
}
