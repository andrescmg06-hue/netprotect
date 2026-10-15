"use client";

import {
  ArrowRight,
  Ban,
  Bell,
  CalendarClock,
  ChevronRight,
  History,
  Link2,
  LogIn,
  LogOut,
  type LucideIcon,
  ShieldAlert,
  ShieldCheck,
  Smartphone,
} from "lucide-react";
import { type ReactNode, useEffect, useMemo, useState } from "react";

import { deviceStatusBadge, deviceSubtitle } from "@/components/shell/deviceStatus";
import {
  AppIcon,
  Button,
  Card,
  EmptyState,
  LoadState,
  type LoadStatus,
  ReadOnlyField,
  ReadOnlyFieldList,
  StatusBadge,
  Timeline,
  type TimelineItem,
} from "@/components/ui";
import {
  type Device,
  type DeviceStatisticsResponse,
  type HistoryEvent,
  type TopAppEntry,
  getDeviceStatistics,
  listDeviceHistory,
} from "@/lib/apiClient";
import type { SectionKey } from "@/lib/dashboardSections";
import { describeError } from "@/lib/errors";
import { durationParts, formatDuration, formatMoment } from "@/lib/format";
import { ruleTypeLabel } from "@/lib/ruleFormatting";

import styles from "./OverviewPanel.module.css";

/** Events shown in "Actividad reciente", merged across devices; the full list lives in Historial. */
const ACTIVITY_LIMIT = 6;
const TOP_APPS_LIMIT = 5;

/** Same icons as their sidebar items. The per-device ones open on the featured device; "Ver
 * alertas" on the device with the most unread alerts. */
const SHORTCUTS: { section: SectionKey; label: string; icon: LucideIcon }[] = [
  { section: "pairing", label: "Vincular otro dispositivo", icon: Link2 },
  { section: "rules", label: "Reglas por aplicación", icon: ShieldCheck },
  { section: "policy", label: "Política y horario escolar", icon: CalendarClock },
  { section: "alerts", label: "Ver alertas", icon: Bell },
];

type DeviceToday = { blocks: number; usageSeconds: number; topApps: TopAppEntry[] };

type TodayState =
  | { kind: "loading" }
  | { kind: "loaded"; byDevice: Record<string, DeviceToday> }
  | { kind: "error" };

type ActivityEvent = HistoryEvent & { deviceName: string };

type ActivityState =
  | { kind: "loading" }
  | { kind: "loaded"; events: ActivityEvent[] }
  | { kind: "error"; message: string };

function summarizeToday(stats: DeviceStatisticsResponse): DeviceToday {
  return {
    blocks: stats.blocks_by_reason.reduce((sum, entry) => sum + entry.count, 0),
    // categories[] covers every app with reported use (the uncategorised ones under `null`), so its
    // sum is the exact total; top_apps is capped at 10 and would undercount
    // (docs/redesign/fase-0-informe.md §5).
    usageSeconds: stats.categories.reduce((sum, entry) => sum + entry.total_seconds, 0),
    topApps: stats.top_apps,
  };
}

/** The device name only matters on the line when there is more than one device. */
function toTimelineItem(event: ActivityEvent, showDevice: boolean): TimelineItem {
  const device = showDevice ? event.deviceName : null;
  if (event.event_type === "APP_RULE") {
    const rule = event.rule_type_applied ? `Regla: ${ruleTypeLabel(event.rule_type_applied)}` : null;
    return {
      id: `rule-${event.id}`,
      icon: Ban,
      tone: "danger",
      title: `Se bloqueó ${event.package_name ?? "una aplicación"}`,
      description: [device, rule].filter(Boolean).join(" · ") || undefined,
      occurredAt: event.occurred_at,
    };
  }
  const isEnter = event.geofence_event_type === "ENTER";
  const zone = event.geofence_name ?? "una zona";
  return {
    id: `zone-${event.id}`,
    icon: isEnter ? LogIn : LogOut,
    tone: isEnter ? "success" : "neutral",
    title: isEnter ? `Entró a ${zone}` : `Salió de ${zone}`,
    description: device ?? undefined,
    occurredAt: event.occurred_at,
  };
}

function alertsLabel(count: number): string {
  return count === 1 ? "1 alerta sin leer" : `${count} alertas sin leer`;
}

function identityLine(device: Device): string {
  const system = deviceSubtitle(device.platform, device.os_version);
  return device.app_version ? `${system} · NetProtect ${device.app_version}` : system;
}

/** Figures large, units small: "3 h 24 min". The thin space keeps it reading as "3 h" aloud. */
function DurationFigure({ seconds }: { seconds: number }) {
  return (
    <>
      {durationParts(seconds).map((part, index) => (
        <span key={part.unit}>
          {index > 0 && " "}
          {part.value}
          <span className={styles.unit}>
            {" "}
            {part.unit}
          </span>
        </span>
      ))}
    </>
  );
}

/** Sprint 24 aggregate view over the devices DashboardShell already fetched; Sprint 33 added the
 * account-wide "today" figures (the Sprint 16 statistics endpoint, once per device, summed).
 *
 * Sprint 55 recomposes it as an editorial page that answers four questions at a glance: how the
 * devices are (one ledger row of figures), what needs attention (alerts, a device in ALERT), what
 * happened recently (a timeline merged from each device's real Historial, Sprint 15) and what to
 * do next (four shortcuts). Nothing here is new data: the mockup's "% vs. ayer", seven-day series,
 * tip of the day, favourites, map view and "Bloquear" button are left out because the backend has
 * none of them (docs/redesign/fase-0-informe.md §5). */
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
  const [today, setToday] = useState<TodayState>({ kind: "loading" });
  const [activity, setActivity] = useState<ActivityState>({ kind: "loading" });
  const [activityReloadToken, setActivityReloadToken] = useState(0);

  useEffect(() => {
    // Promise.all([]) resolves on its own (to an empty array, on a microtask, never
    // synchronously) when there are no devices yet — no separate early-return branch needed.
    let cancelled = false;
    Promise.all(
      devices.map((device) =>
        getDeviceStatistics(accessToken, device.id, "today").then(
          (stats) => [device.id, summarizeToday(stats)] as const
        )
      )
    )
      .then((entries) => {
        if (!cancelled) setToday({ kind: "loaded", byDevice: Object.fromEntries(entries) });
      })
      .catch(() => {
        if (!cancelled) setToday({ kind: "error" });
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, devices]);

  // Recent activity: each device's own Historial, merged newest first. All or nothing: a device
  // whose history failed would silently drop its events, so a failure is shown with a retry.
  useEffect(() => {
    let cancelled = false;
    Promise.all(
      devices.map((device) =>
        listDeviceHistory(accessToken, device.id).then(({ events }) =>
          events.map((event): ActivityEvent => ({ ...event, deviceName: device.name }))
        )
      )
    )
      .then((perDevice) => {
        if (cancelled) return;
        const merged = perDevice
          .flat()
          .sort((a, b) => Date.parse(b.occurred_at) - Date.parse(a.occurred_at))
          .slice(0, ACTIVITY_LIMIT);
        setActivity({ kind: "loaded", events: merged });
      })
      .catch((error) => {
        if (!cancelled) {
          setActivity({ kind: "error", message: describeError(error, "No se pudo cargar la actividad reciente.") });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, devices, activityReloadToken]);

  const activityItems = useMemo(
    () => (activity.kind === "loaded" ? activity.events.map((event) => toTimelineItem(event, devices.length > 1)) : []),
    [activity, devices.length]
  );

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

  function retryActivity() {
    setActivity({ kind: "loading" });
    setActivityReloadToken((current) => current + 1);
  }

  // The featured device: one that reported possible tampering comes first (it is what needs
  // attention); otherwise the first device, the one the per-device sections open on by default.
  const featured = devices.find((device) => device.status.status === "ALERT") ?? devices[0];
  const others = devices.filter((device) => device.id !== featured.id);
  const featuredBadge = deviceStatusBadge(featured.status.status);
  const featuredUnread = unreadByDevice[featured.id] ?? 0;

  const onlineCount = devices.filter((device) => device.status.status === "ONLINE").length;
  const alertDeviceCount = devices.filter((device) => device.status.status === "ALERT").length;
  const unreadTotal = devices.reduce((sum, device) => sum + (unreadByDevice[device.id] ?? 0), 0);
  const busiestAlertsDeviceId = devices.reduce<{ id: string; count: number } | null>((busiest, device) => {
    const count = unreadByDevice[device.id] ?? 0;
    return count > 0 && (!busiest || count > busiest.count) ? { id: device.id, count } : busiest;
  }, null)?.id;

  const byDevice = today.kind === "loaded" ? today.byDevice : null;
  const totals = byDevice
    ? devices.reduce(
        (sum, device) => ({
          blocks: sum.blocks + (byDevice[device.id]?.blocks ?? 0),
          usageSeconds: sum.usageSeconds + (byDevice[device.id]?.usageSeconds ?? 0),
        }),
        { blocks: 0, usageSeconds: 0 }
      )
    : null;
  const featuredToday = byDevice?.[featured.id] ?? null;
  const topApps = featuredToday ? featuredToday.topApps.slice(0, TOP_APPS_LIMIT) : [];

  const pendingFigure = (
    <span className={styles.pending}>{today.kind === "error" ? "–" : "…"}</span>
  );
  const todayUnavailable = today.kind === "error" && (
    <dd className={`${styles.figureNote} ${styles.noteQuiet}`}>No disponible</dd>
  );

  const activityStatus: LoadStatus =
    activity.kind === "loading"
      ? "loading"
      : activity.kind === "error"
        ? "error"
        : activityItems.length === 0
          ? "empty"
          : "ready";
  const appsStatus: LoadStatus =
    today.kind === "loading" ? "loading" : today.kind === "error" ? "error" : topApps.length === 0 ? "empty" : "ready";

  let alertsNote: ReactNode;
  if (unreadTotal > 0) {
    alertsNote = (
      <button
        type="button"
        className={`${styles.textLink} ${styles.textLinkAlert}`}
        onClick={() => onNavigate("alerts", busiestAlertsDeviceId)}
      >
        Revisar ahora
        <ArrowRight size={13} strokeWidth={2} className={styles.arrow} aria-hidden="true" />
      </button>
    );
  } else if (alertDeviceCount === 0) {
    alertsNote = <span className={styles.noteOk}>Todo en orden</span>;
  } else {
    alertsNote = <span className={styles.noteQuiet}>Ninguna pendiente</span>;
  }

  function openShortcut(section: SectionKey) {
    if (section === "pairing") onNavigate(section);
    else if (section === "alerts") onNavigate(section, busiestAlertsDeviceId ?? featured.id);
    else onNavigate(section, featured.id);
  }

  return (
    <div className={styles.overview}>
      <dl className={styles.figures} aria-label="Resumen">
        <div className={styles.figure}>
          <dt className={styles.figureLabel}>Dispositivos en línea</dt>
          <dd className={styles.figureValue}>
            {onlineCount}
            <span className={styles.figureOf}>de {devices.length}</span>
          </dd>
          {alertDeviceCount > 0 && (
            <dd className={`${styles.figureNote} ${styles.noteAlert}`}>
              {alertDeviceCount === 1 ? "1 con alerta" : `${alertDeviceCount} con alerta`}
            </dd>
          )}
        </div>
        <div className={styles.figure}>
          <dt className={styles.figureLabel}>Alertas sin leer</dt>
          <dd className={unreadTotal > 0 ? `${styles.figureValue} ${styles.figureAlert}` : styles.figureValue}>
            {unreadTotal}
          </dd>
          <dd className={styles.figureNote}>{alertsNote}</dd>
        </div>
        <div className={styles.figure}>
          <dt className={styles.figureLabel}>Bloqueos hoy</dt>
          <dd className={styles.figureValue}>{totals ? totals.blocks : pendingFigure}</dd>
          {todayUnavailable}
        </div>
        <div className={styles.figure}>
          <dt className={styles.figureLabel}>Tiempo de uso hoy</dt>
          <dd className={styles.figureValue}>
            {totals ? <DurationFigure seconds={totals.usageSeconds} /> : pendingFigure}
          </dd>
          {todayUnavailable}
        </div>
      </dl>

      <div className={styles.columns}>
        <div className={styles.main}>
          <section className={styles.section} aria-labelledby="overview-devices-title">
            <header className={styles.sectionHead}>
              <h2 id="overview-devices-title" className={styles.sectionTitle}>
                Tus dispositivos
              </h2>
              <button type="button" className={styles.textLink} onClick={() => onNavigate("devices")}>
                Gestionar
                <ArrowRight size={14} strokeWidth={2} className={styles.arrow} aria-hidden="true" />
              </button>
            </header>

            <article className={styles.featured} aria-labelledby="overview-featured-name">
              <span className={styles.plate} aria-hidden="true">
                <Smartphone size={28} strokeWidth={1.5} />
              </span>
              <div className={styles.featuredBody}>
                <div className={styles.featuredHead}>
                  <div className={styles.featuredTitle}>
                    <h3 id="overview-featured-name" className={styles.featuredName}>
                      {featured.name}
                    </h3>
                    <p className={styles.featuredMeta}>{identityLine(featured)}</p>
                  </div>
                  <StatusBadge tone={featuredBadge.tone} dot>
                    {featuredBadge.label}
                  </StatusBadge>
                </div>

                {featured.status.status === "ALERT" && (
                  <p className={styles.attention}>
                    <ShieldAlert size={16} strokeWidth={2} aria-hidden="true" />
                    Reportó una posible manipulación. Revisa sus alertas.
                  </p>
                )}

                <ReadOnlyFieldList columns={2}>
                  <ReadOnlyField
                    label="Uso hoy"
                    emptyText="No disponible"
                    value={
                      today.kind === "loading" ? (
                        <span className={styles.pending}>…</span>
                      ) : featuredToday ? (
                        <span className={styles.bigFigure}>
                          <DurationFigure seconds={featuredToday.usageSeconds} />
                        </span>
                      ) : null
                    }
                  />
                  <ReadOnlyField label="Última conexión" value={formatMoment(featured.status.last_seen_at)} />
                </ReadOnlyFieldList>

                <div className={styles.featuredLinks}>
                  <button type="button" className={styles.textLink} onClick={() => onNavigate("devices", featured.id)}>
                    Ver el dispositivo
                    <ArrowRight size={14} strokeWidth={2} className={styles.arrow} aria-hidden="true" />
                  </button>
                  {featuredUnread > 0 && (
                    <button
                      type="button"
                      className={`${styles.textLink} ${styles.textLinkAlert}`}
                      onClick={() => onNavigate("alerts", featured.id)}
                    >
                      <Bell size={14} strokeWidth={2} aria-hidden="true" />
                      {alertsLabel(featuredUnread)}
                    </button>
                  )}
                </div>
              </div>
            </article>

            {others.length > 0 && (
              <ul className={styles.others}>
                {others.map((device) => {
                  const badge = deviceStatusBadge(device.status.status);
                  const unread = unreadByDevice[device.id] ?? 0;
                  const usage = byDevice?.[device.id];
                  return (
                    <li key={device.id} className={styles.other}>
                      <button
                        type="button"
                        className={styles.otherButton}
                        onClick={() => onNavigate("devices", device.id)}
                      >
                        <span className={styles.otherText}>
                          <span className={styles.otherName}>{device.name}</span>
                          <span className={styles.otherMeta}>
                            {deviceSubtitle(device.platform, device.os_version)} · Visto:{" "}
                            {formatMoment(device.status.last_seen_at, { inline: true })}
                            {usage ? ` · ${formatDuration(usage.usageSeconds)} de uso hoy` : ""}
                          </span>
                        </span>
                        <StatusBadge tone={badge.tone} dot>
                          {badge.label}
                        </StatusBadge>
                        <ChevronRight size={16} className={styles.chevron} aria-hidden="true" />
                      </button>
                      {unread > 0 && (
                        <button
                          type="button"
                          className={`${styles.textLink} ${styles.textLinkAlert}`}
                          aria-label={`${alertsLabel(unread)} en ${device.name}`}
                          onClick={() => onNavigate("alerts", device.id)}
                        >
                          <Bell size={14} strokeWidth={2} aria-hidden="true" />
                          {unread}
                        </button>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </section>

          <section className={styles.section} aria-labelledby="overview-activity-title">
            <header className={styles.sectionHead}>
              <h2 id="overview-activity-title" className={styles.sectionTitle}>
                Actividad reciente
              </h2>
              <button type="button" className={styles.textLink} onClick={() => onNavigate("history", featured.id)}>
                Ver historial
                <ArrowRight size={14} strokeWidth={2} className={styles.arrow} aria-hidden="true" />
              </button>
            </header>
            <LoadState
              status={activityStatus}
              loadingLabel="Cargando actividad…"
              error={activity.kind === "error" ? activity.message : undefined}
              onRetry={retryActivity}
              empty={{
                icon: History,
                title: "Sin actividad reciente",
                description:
                  "Aquí aparecerán los bloqueos de aplicaciones y las entradas o salidas de zonas de tus dispositivos.",
              }}
            >
              <Timeline items={activityItems} headingLevel={3} />
            </LoadState>
          </section>
        </div>

        <aside className={styles.aside} aria-label="Accesos rápidos y uso de hoy">
          <section className={styles.section} aria-labelledby="overview-shortcuts-title">
            <header className={`${styles.sectionHead} ${styles.sectionHeadFlush}`}>
              <h2 id="overview-shortcuts-title" className={styles.sectionTitle}>
                Accesos rápidos
              </h2>
            </header>
            <ul className={styles.shortcuts}>
              {SHORTCUTS.map((shortcut) => {
                const Icon = shortcut.icon;
                const count = shortcut.section === "alerts" ? unreadTotal : 0;
                return (
                  <li key={shortcut.section}>
                    <button type="button" className={styles.shortcut} onClick={() => openShortcut(shortcut.section)}>
                      <Icon size={18} strokeWidth={1.75} className={styles.shortcutIcon} aria-hidden="true" />
                      <span className={styles.shortcutLabel}>{shortcut.label}</span>
                      {count > 0 && <span className={styles.shortcutCount}>{count} sin leer</span>}
                      <ArrowRight size={16} strokeWidth={2} className={styles.shortcutArrow} aria-hidden="true" />
                    </button>
                  </li>
                );
              })}
            </ul>
          </section>

          <section className={styles.section} aria-labelledby="overview-apps-title">
            <header className={styles.sectionHead}>
              <h2 id="overview-apps-title" className={styles.sectionTitle}>
                Lo más usado hoy
              </h2>
              <span className={styles.sectionNote}>{featured.name}</span>
            </header>
            <LoadState
              status={appsStatus}
              loadingLabel="Cargando el uso de hoy…"
              error="No se pudo cargar el uso de hoy."
              empty="Todavía no hay uso reportado hoy."
            >
              <ol className={styles.apps}>
                {topApps.map((app) => {
                  const label = app.app_label ?? app.package_name;
                  return (
                    <li key={app.package_name} className={styles.app}>
                      <AppIcon label={label} seed={app.package_name} />
                      <span className={styles.appName} title={app.package_name}>
                        {label}
                      </span>
                      <span className={styles.appTime}>{formatDuration(app.total_seconds)}</span>
                    </li>
                  );
                })}
              </ol>
            </LoadState>
          </section>
        </aside>
      </div>
    </div>
  );
}
