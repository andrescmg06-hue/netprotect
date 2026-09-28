"use client";

import { Ban, Bell, ChevronRight, Clock, Link2, Smartphone } from "lucide-react";
import { useEffect, useState } from "react";

import { deviceStatusBadge, deviceSubtitle } from "@/components/shell/deviceStatus";
import { Button, Card, CardHeader, EmptyState, MetricCard, MetricGrid, StatusBadge } from "@/components/ui";
import type { Device } from "@/lib/apiClient";
import { getDeviceStatistics } from "@/lib/apiClient";
import type { SectionKey } from "@/lib/dashboardSections";

import styles from "./OverviewPanel.module.css";

function formatLastSeen(value: string | null): string {
  if (!value) return "Nunca";
  return new Date(value).toLocaleString("es-CO", { dateStyle: "medium", timeStyle: "short" });
}

function formatDuration(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  if (hours > 0) return `${hours} h ${minutes} min`;
  if (minutes > 0) return `${minutes} min`;
  return totalSeconds > 0 ? "< 1 min" : "0 min";
}

type TodayTotals = { blocks: number; usageSeconds: number };

/** Sprint 24: a plain aggregate view over the same `devices` the dashboard already fetched
 * (listDevices, Sprint 6) — no new endpoint, no new table, same pattern as the Sprint 16
 * statistics ("agregaciones al vuelo sobre datos ya existentes").
 *
 * Sprint 33: adds account-wide "today" metrics by calling the existing per-device statistics
 * endpoint (period=today) once per linked device and summing — still no new backend, just more of
 * what Sprint 16 already returns. Unread alerts come from DashboardShell, which already polls
 * them for the header bell.
 */
export function OverviewPanel({
  accessToken,
  devices,
  unreadByDevice,
  onNavigate,
}: {
  accessToken: string;
  devices: Device[];
  unreadByDevice: Record<string, number>;
  onNavigate: (section: SectionKey, deviceId?: string) => void;
}) {
  const [today, setToday] = useState<TodayTotals | null>(null);

  useEffect(() => {
    // Promise.all([]) resolves on its own (to an empty array, on a microtask, never
    // synchronously) when there are no devices yet — no separate early-return branch needed.
    let cancelled = false;
    Promise.all(devices.map((device) => getDeviceStatistics(accessToken, device.id, "today")))
      .then((results) => {
        if (cancelled) return;
        const totals = results.reduce<TodayTotals>(
          (acc, result) => ({
            blocks: acc.blocks + result.blocks_by_reason.reduce((sum, entry) => sum + entry.count, 0),
            usageSeconds: acc.usageSeconds + result.top_apps.reduce((sum, entry) => sum + entry.total_seconds, 0),
          }),
          { blocks: 0, usageSeconds: 0 }
        );
        setToday(totals);
      })
      .catch(() => {
        if (!cancelled) setToday(null);
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, devices]);

  const unreadTotal = Object.values(unreadByDevice).reduce((sum, count) => sum + count, 0);
  const onlineCount = devices.filter((device) => device.status.status === "ONLINE").length;

  if (devices.length === 0) {
    return (
      <Card>
        <EmptyState
          icon={Smartphone}
          title="Todavía no hay dispositivos vinculados"
          description="Genera un código de 6 dígitos desde Vinculación e ingrésalo en la app de NetProtect del dispositivo que quieres supervisar."
          action={
            <Button variant="primary" icon={Link2} onClick={() => onNavigate("pairing")}>
              Ir a Vinculación
            </Button>
          }
        />
      </Card>
    );
  }

  return (
    <>
      <MetricGrid>
        <MetricCard
          icon={Smartphone}
          tone="info"
          label="Dispositivos en línea"
          value={`${onlineCount}/${devices.length}`}
        />
        <MetricCard
          icon={Bell}
          tone={unreadTotal > 0 ? "danger" : "neutral"}
          label="Alertas sin leer"
          value={unreadTotal}
        />
        <MetricCard icon={Ban} tone="warning" label="Bloqueos hoy" value={today ? today.blocks : "…"} />
        <MetricCard
          icon={Clock}
          tone="purple"
          label="Tiempo de uso hoy"
          value={today ? formatDuration(today.usageSeconds) : "…"}
        />
      </MetricGrid>

      <Card padding="none">
        <div className={styles.cardHead}>
          <CardHeader icon={Smartphone} title="Tus dispositivos" />
        </div>
        <ul className={styles.list}>
          {devices.map((device) => {
            const badge = deviceStatusBadge(device.status.status);
            const unread = unreadByDevice[device.id] ?? 0;
            return (
              <li key={device.id} className={styles.row}>
                <button type="button" className={styles.rowButton} onClick={() => onNavigate("devices", device.id)}>
                  <span className={styles.rowIcon}>
                    <Smartphone size={20} strokeWidth={1.8} aria-hidden="true" />
                  </span>
                  <span className={styles.rowText}>
                    <span className={styles.rowName}>{device.name}</span>
                    <span className={styles.rowMeta}>
                      {deviceSubtitle(device.platform, device.os_version)} · Visto:{" "}
                      {formatLastSeen(device.status.last_seen_at)}
                    </span>
                  </span>
                  <StatusBadge tone={badge.tone} dot>
                    {badge.label}
                  </StatusBadge>
                  <ChevronRight size={18} className={styles.chevron} aria-hidden="true" />
                </button>
                {unread > 0 && (
                  <Button
                    size="sm"
                    variant="ghost"
                    icon={Bell}
                    onClick={() => onNavigate("alerts", device.id)}
                    className={styles.alertButton}
                  >
                    {unread} sin leer
                  </Button>
                )}
              </li>
            );
          })}
        </ul>
      </Card>

      <Card>
        <CardHeader title="Accesos rápidos" />
        <div className={styles.quickActions}>
          <Button icon={Link2} onClick={() => onNavigate("pairing")}>
            Vincular otro dispositivo
          </Button>
          <Button icon={Ban} onClick={() => onNavigate("rules", devices[0]?.id)}>
            Reglas por aplicación
          </Button>
          <Button icon={Bell} onClick={() => onNavigate("alerts", devices[0]?.id)}>
            Ver alertas
          </Button>
        </div>
      </Card>
    </>
  );
}
