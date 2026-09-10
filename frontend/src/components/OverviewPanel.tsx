"use client";

import type { Device } from "@/lib/apiClient";

function formatLastSeen(value: string | null): string {
  if (!value) {
    return "Nunca";
  }
  return new Date(value).toLocaleString("es-CO", { dateStyle: "medium", timeStyle: "short" });
}

/** Sprint 24: a plain aggregate view over the same `devices` the dashboard already fetched
 * (listDevices, Sprint 6) — no new endpoint, no new table, same pattern as the Sprint 16
 * statistics ("agregaciones al vuelo sobre datos ya existentes"). Each card links straight into
 * that device's Alertas section, since an unread alert is the one thing here a tutor is most
 * likely to act on immediately.
 */
export function OverviewPanel({
  devices,
  onOpenAlerts,
}: {
  devices: Device[];
  onOpenAlerts: (deviceId: string) => void;
}) {
  if (devices.length === 0) {
    return (
      <div className="devicesPanel">
        <div className="devicesPanelHeader">
          <strong>Resumen</strong>
        </div>
        <p className="statusText" style={{ textAlign: "left" }}>
          Todavía no hay dispositivos vinculados. Usa la sección &ldquo;Vinculación&rdquo; para
          generar un código.
        </p>
      </div>
    );
  }

  return (
    <div className="devicesPanel">
      <div className="devicesPanelHeader">
        <strong>Resumen</strong>
      </div>
      <ul className="deviceList">
        {devices.map((device) => (
          <li key={device.id} className="deviceRow">
            <div>
              <div className="deviceName">{device.name}</div>
              <div className="deviceMeta">
                {device.platform}
                {device.timezone && ` · ${device.timezone}`} · Visto: {formatLastSeen(device.status.last_seen_at)}
              </div>
            </div>
            <div className="deviceActions">
              <span className={`statusPill ${device.status.status.toLowerCase()}`}>{device.status.status}</span>
              {device.default_app_policy === "BLOCK" && (
                <span className="statusPill allowlist">SÓLO APPS APROBADAS</span>
              )}
              {device.school_mode.enabled && <span className="statusPill allowlist">HORARIO ESCOLAR</span>}
              <button type="button" onClick={() => onOpenAlerts(device.id)}>
                Ver alertas
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
