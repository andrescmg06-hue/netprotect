"use client";

import { ArrowRight, Bell, BellOff, Check, RefreshCw } from "lucide-react";
import { type KeyboardEvent, useEffect, useId, useMemo, useRef, useState } from "react";

import { Button, EmptyState, Spinner, StatusBadge } from "@/components/ui";
import {
  ALERT_LEVELS,
  ALERT_LEVEL_ICON,
  ALERT_LEVEL_LABEL,
  ALERT_LEVEL_TONE,
  alertLabel,
  silenceLabel,
} from "@/lib/alertFormatting";
import {
  type Alert,
  type AlertLevel,
  type AlertSilence,
  ApiError,
  type Device,
  deleteAlertSilence,
  listAlertSilences,
  listDeviceAlerts,
  listDevices,
  listGeofences,
  markAlertRead,
  silenceAlert,
} from "@/lib/apiClient";

import styles from "./AlertsPanel.module.css";

// `key` is the reloadToken the result belongs to: while it differs from the current one a
// refresh is in flight, which lets "Actualizar" show progress without a synchronous setState
// in the effect (react-hooks/set-state-in-effect).
type AlertsState =
  | { kind: "loading" }
  | { kind: "loaded"; key: number; alerts: Alert[]; silences: AlertSilence[] }
  | { kind: "error"; key: number; message: string };

type LevelFilter = AlertLevel | "ALL";

const LEVEL_CLASS: Record<AlertLevel, string> = {
  INFO: styles.levelInfo,
  WARNING: styles.levelWarning,
  HIGH: styles.levelHigh,
  CRITICAL: styles.levelCritical,
};

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function formatShort(iso: string): string {
  return new Date(iso).toLocaleString("es-CO", {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatLong(iso: string): string {
  return new Date(iso).toLocaleString("es-CO", { dateStyle: "long", timeStyle: "short" });
}

function timesLabel(count: number): string {
  return count === 1 ? "1 vez" : `${count} veces`;
}

/** Light line composition for the empty states (D6: the package has no bell drawings yet): a
 * lucide icon inside a hairline ring, with an optional small companion mark. Decorative only. */
function BellIllustration({ off = false }: { off?: boolean }) {
  const Icon = off ? BellOff : Bell;
  return (
    <span className={styles.illustration} aria-hidden="true">
      <Icon size={36} strokeWidth={1.25} />
      {!off && (
        <span className={styles.illustrationMark}>
          <Check size={12} strokeWidth={2} />
        </span>
      )}
    </span>
  );
}

/** Sprint 17 (logic), Sprint 37 (design), Sprint 58 (editorial recomposition): a tutor inbox
 * generated from signals that already existed (bloqueos de reglas, entradas/salidas de geocercas,
 * señales de manipulación) — read through GET /devices/{id}/alerts, deduplicated server-side while
 * unread. Marking read and silenciar son acciones de tutor, sólo desde el panel web.
 *
 * `view` splits this into two dashboard sections without splitting the fetch (Sprint 24): both
 * still load in one Promise.all, since silencing an alert here needs to update the "está
 * silenciada" state on the same alert list. "inbox" is a level strip (counts that double as the
 * filter) over a master-detail list; "silenced" is a plain ledger of silences with when each one
 * warns again. No calendar, no "silenced on" date and no "silence an alert" button: the API keeps
 * no creation date and a silence only exists from a real alert (docs/redesign/fase-0-informe.md §5).
 */
export function AlertsPanel({
  accessToken,
  deviceId,
  view = "inbox",
  devices,
}: {
  accessToken: string;
  deviceId: string;
  view?: "inbox" | "silenced";
  /** Devices the dashboard already holds. When given, the device name comes from here and the
   * panel makes no listDevices call of its own. */
  devices?: Pick<Device, "id" | "name">[];
}) {
  const [state, setState] = useState<AlertsState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);
  const [levelFilter, setLevelFilter] = useState<LevelFilter>("ALL");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [geofenceNames, setGeofenceNames] = useState<Record<string, string>>({});
  const [fetchedDeviceName, setFetchedDeviceName] = useState<string | null>(null);
  const deviceName = devices
    ? (devices.find((device) => device.id === deviceId)?.name ?? null)
    : fetchedDeviceName;

  useEffect(() => {
    let cancelled = false;

    Promise.all([listDeviceAlerts(accessToken, deviceId), listAlertSilences(accessToken, deviceId)])
      .then(([alertsResponse, silencesResponse]) => {
        if (!cancelled) {
          setState({
            kind: "loaded",
            key: reloadToken,
            alerts: alertsResponse.alerts,
            silences: silencesResponse.silences,
          });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setState({ kind: "error", key: reloadToken, message: describeError(error, "No se pudieron cargar las alertas") });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, reloadToken]);

  useEffect(() => {
    if (view !== "silenced") return;
    let cancelled = false;

    listGeofences(accessToken, deviceId)
      .then(({ geofences }) => {
        if (!cancelled) {
          setGeofenceNames(Object.fromEntries(geofences.map((geofence) => [geofence.id, geofence.name])));
        }
      })
      .catch(() => {
        // Best-effort: silenceLabel falls back to the raw geofence id when this hasn't loaded.
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, view, reloadToken]);

  // Best-effort device name for the detail pane when the caller did not pass the device list. A
  // failure just leaves the row out; the device card in the page header still names the device.
  const hasDeviceList = devices !== undefined;
  useEffect(() => {
    if (view !== "inbox" || hasDeviceList) return;
    let cancelled = false;

    listDevices(accessToken)
      .then(({ devices: fetched }) => {
        if (!cancelled) {
          setFetchedDeviceName(fetched.find((device) => device.id === deviceId)?.name ?? null);
        }
      })
      .catch(() => {
        // Optional detail: nothing to report.
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, view, hasDeviceList]);

  const refreshing = state.kind === "loading" || state.key !== reloadToken;
  const alerts = useMemo(() => (state.kind === "loaded" ? state.alerts : []), [state]);
  const silences = useMemo(() => (state.kind === "loaded" ? state.silences : []), [state]);

  // "Más recientes" is the only order the mockup offers, so it is applied, not offered.
  const sortedAlerts = useMemo(
    () => [...alerts].sort((a, b) => Date.parse(b.last_occurred_at) - Date.parse(a.last_occurred_at)),
    [alerts]
  );
  const filteredAlerts = useMemo(
    () => (levelFilter === "ALL" ? sortedAlerts : sortedAlerts.filter((alert) => alert.level === levelFilter)),
    [sortedAlerts, levelFilter]
  );
  const selectedAlert = useMemo(
    () => filteredAlerts.find((alert) => alert.id === selectedId) ?? filteredAlerts[0] ?? null,
    [filteredAlerts, selectedId]
  );
  const levelCounts = useMemo(
    () =>
      alerts.reduce<Record<AlertLevel, number>>(
        (counts, alert) => ({ ...counts, [alert.level]: counts[alert.level] + 1 }),
        { INFO: 0, WARNING: 0, HIGH: 0, CRITICAL: 0 }
      ),
    [alerts]
  );
  const silenceByKey = useMemo(
    () => Object.fromEntries(silences.map((silence) => [silence.dedup_key, silence])),
    [silences]
  );

  function reload() {
    setReloadToken((current) => current + 1);
  }

  function handleRead(alertId: string) {
    markAlertRead(accessToken, deviceId, alertId)
      .then(reload)
      .catch(reload);
  }

  function handleSilence(alertId: string) {
    silenceAlert(accessToken, deviceId, alertId, null)
      .then(reload)
      .catch(reload);
  }

  function handleUnsilence(silenceId: string) {
    deleteAlertSilence(accessToken, deviceId, silenceId)
      .then(reload)
      .catch(reload);
  }

  const refreshButton = (
    <Button size="sm" variant="ghost" icon={RefreshCw} loading={refreshing && state.kind !== "loading"} onClick={reload}>
      Actualizar
    </Button>
  );

  if (state.kind === "loading") {
    return (
      <div className={styles.status}>
        <Spinner label={view === "silenced" ? "Cargando alertas silenciadas…" : "Cargando alertas…"} />
      </div>
    );
  }

  if (state.kind === "error") {
    return (
      <div className={styles.status}>
        <p className={styles.error}>{state.message}</p>
        <Button size="sm" icon={RefreshCw} loading={refreshing} onClick={reload}>
          Reintentar
        </Button>
      </div>
    );
  }

  const alertsHref = `#${new URLSearchParams({ section: "alerts", device: deviceId }).toString()}`;

  if (view === "silenced") {
    return (
      <div className={styles.panel}>
        <div className={styles.toolbar}>
          <p className={styles.caption}>
            {silences.length === 0
              ? "Ningún aviso silenciado en este dispositivo."
              : silences.length === 1
                ? "1 aviso silenciado en este dispositivo."
                : `${silences.length} avisos silenciados en este dispositivo.`}
          </p>
          {refreshButton}
        </div>

        {silences.length === 0 ? (
          <div className={styles.emptyBlock}>
            <EmptyState
              icon={BellOff}
              illustration={<BellIllustration off />}
              title="No hay alertas silenciadas"
              description="Cuando silencies un aviso desde Alertas, aparecerá aquí con la fecha en que vuelve a avisar."
              action={
                <a className={styles.quietLink} href={alertsHref}>
                  Ir a Alertas
                  <ArrowRight size={14} strokeWidth={2} aria-hidden="true" />
                </a>
              }
            />
          </div>
        ) : (
          <>
            <ul className={styles.silenceList}>
              {silences.map((silence) => (
                <li key={silence.id} className={styles.silenceRow}>
                  <BellOff className={styles.silenceIcon} size={18} strokeWidth={1.75} aria-hidden="true" />
                  <span className={styles.silenceWhat}>{silenceLabel(silence.dedup_key, geofenceNames)}</span>
                  <span className={styles.silenceUntil}>
                    <span className={styles.fieldLabel}>Vuelve a avisar</span>
                    <span className={styles.fieldValue}>
                      {silence.silenced_until ? formatLong(silence.silenced_until) : "Indefinido"}
                    </span>
                  </span>
                  <span className={styles.silenceAction}>
                    <Button size="sm" variant="ghost" icon={Bell} onClick={() => handleUnsilence(silence.id)}>
                      Reactivar
                    </Button>
                  </span>
                </li>
              ))}
            </ul>
            <p className={styles.footnote}>
              Un aviso se silencia desde su detalle en Alertas.{" "}
              <a className={styles.quietLink} href={alertsHref}>
                Ir a Alertas
                <ArrowRight size={14} strokeWidth={2} aria-hidden="true" />
              </a>
            </p>
          </>
        )}
      </div>
    );
  }

  if (alerts.length === 0) {
    return (
      <div className={styles.panel}>
        <div className={styles.toolbar}>
          <p className={styles.caption}>Bandeja del dispositivo</p>
          {refreshButton}
        </div>
        <div className={styles.emptyBlock}>
          <EmptyState
            icon={Bell}
            illustration={<BellIllustration />}
            title="No hay alertas"
            description="Cuando el dispositivo genere un aviso (un bloqueo, una entrada o salida de una zona o una señal de manipulación) aparecerá aquí."
          />
        </div>
      </div>
    );
  }

  const detail = selectedAlert ? (
    <AlertDetail
      alert={selectedAlert}
      deviceName={deviceName}
      silence={silenceByKey[selectedAlert.dedup_key] ?? null}
      onRead={() => handleRead(selectedAlert.id)}
      onSilence={() => handleSilence(selectedAlert.id)}
    />
  ) : null;

  return (
    <div className={styles.panel}>
      <LevelStrip value={levelFilter} onChange={setLevelFilter} counts={levelCounts} total={alerts.length} />

      <div className={styles.toolbar}>
        <p className={styles.caption}>Del más reciente al más antiguo</p>
        {refreshButton}
      </div>

      <div className={styles.inbox}>
        <div className={styles.master}>
          {filteredAlerts.length === 0 ? (
            <div className={styles.emptyBlock}>
              <EmptyState
                icon={Bell}
                title={`Ningún aviso de nivel ${ALERT_LEVEL_LABEL[levelFilter as AlertLevel]}`}
                action={
                  <Button size="sm" variant="ghost" onClick={() => setLevelFilter("ALL")}>
                    Ver todas
                  </Button>
                }
              />
            </div>
          ) : (
            <ul className={styles.list}>
              {filteredAlerts.map((alert) => {
                const Icon = ALERT_LEVEL_ICON[alert.level];
                const selected = selectedAlert?.id === alert.id;
                const unread = !alert.read_at;
                return (
                  <li key={alert.id}>
                    <button
                      type="button"
                      className={selected ? `${styles.row} ${styles.rowSelected}` : styles.row}
                      aria-current={selected ? "true" : undefined}
                      onClick={() => setSelectedId(alert.id)}
                    >
                      <span className={`${styles.rowIcon} ${LEVEL_CLASS[alert.level]}`}>
                        <Icon size={18} strokeWidth={1.75} aria-hidden="true" />
                      </span>
                      <span className={styles.rowBody}>
                        <span className={unread ? `${styles.rowTitle} ${styles.rowUnread}` : styles.rowTitle}>
                          {alertLabel(alert)}
                        </span>
                        <span className={styles.rowMeta}>
                          <span className={`${styles.levelWord} ${LEVEL_CLASS[alert.level]}`}>
                            {ALERT_LEVEL_LABEL[alert.level]}
                          </span>
                          {alert.occurrence_count > 1 && <span> · {timesLabel(alert.occurrence_count)}</span>}
                          {unread && <span> · Sin leer</span>}
                        </span>
                      </span>
                      <span className={styles.rowTime}>
                        {formatShort(alert.last_occurred_at)}
                        {unread && <span className={styles.unreadDot} aria-hidden="true" />}
                      </span>
                    </button>
                    {selected && <div className={styles.inlineDetail}>{detail}</div>}
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        <aside className={styles.aside} aria-label="Detalle de la alerta">
          {detail ?? (
            <EmptyState icon={Bell} title="Selecciona una alerta" description="Elige un aviso de la lista para ver su detalle." />
          )}
        </aside>
      </div>
    </div>
  );
}

/** The level hierarchy as one quiet strip: each level's real count, and the strip is also the
 * filter (radio-group keyboard pattern, same as SegmentedControl) — one element instead of a row
 * of metric cards plus a row of tabs. */
function LevelStrip({
  value,
  onChange,
  counts,
  total,
}: {
  value: LevelFilter;
  onChange: (value: LevelFilter) => void;
  counts: Record<AlertLevel, number>;
  total: number;
}) {
  const buttonsRef = useRef<(HTMLButtonElement | null)[]>([]);
  const options: { value: LevelFilter; label: string; count: number }[] = [
    { value: "ALL", label: "Todas", count: total },
    ...ALERT_LEVELS.map((level) => ({ value: level as LevelFilter, label: ALERT_LEVEL_LABEL[level], count: counts[level] })),
  ];

  function select(index: number) {
    const next = (index + options.length) % options.length;
    onChange(options[next].value);
    buttonsRef.current[next]?.focus();
  }

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>, index: number) {
    const moves: Record<string, number> = {
      ArrowRight: index + 1,
      ArrowDown: index + 1,
      ArrowLeft: index - 1,
      ArrowUp: index - 1,
      Home: 0,
      End: options.length - 1,
    };
    if (event.key in moves) {
      event.preventDefault();
      select(moves[event.key]);
    }
  }

  return (
    <div className={styles.strip} role="radiogroup" aria-label="Filtrar por nivel">
      {options.map((option, index) => {
        const selected = option.value === value;
        const Icon = option.value === "ALL" ? null : ALERT_LEVEL_ICON[option.value];
        return (
          <button
            key={option.value}
            ref={(element) => {
              buttonsRef.current[index] = element;
            }}
            type="button"
            role="radio"
            aria-checked={selected}
            tabIndex={selected ? 0 : -1}
            className={selected ? `${styles.stripOption} ${styles.stripSelected}` : styles.stripOption}
            onClick={() => onChange(option.value)}
            onKeyDown={(event) => handleKeyDown(event, index)}
          >
            <span className={styles.stripCount}>{option.count}</span>
            <span className={styles.stripLabel}>
              {Icon && option.value !== "ALL" && (
                <Icon className={LEVEL_CLASS[option.value]} size={14} strokeWidth={2} aria-hidden="true" />
              )}
              {option.label}
            </span>
          </button>
        );
      })}
    </div>
  );
}

function AlertDetail({
  alert,
  deviceName,
  silence,
  onRead,
  onSilence,
}: {
  alert: Alert;
  deviceName: string | null;
  silence: AlertSilence | null;
  onRead: () => void;
  onSilence: () => void;
}) {
  const titleId = useId();

  return (
    <article className={styles.detail} aria-labelledby={titleId}>
      <StatusBadge tone={ALERT_LEVEL_TONE[alert.level]} icon={ALERT_LEVEL_ICON[alert.level]}>
        {ALERT_LEVEL_LABEL[alert.level]}
      </StatusBadge>
      <h2 id={titleId} className={styles.detailTitle}>
        {alertLabel(alert)}
      </h2>

      <dl className={styles.detailList}>
        {deviceName && (
          <div className={styles.detailRow}>
            <dt>Dispositivo</dt>
            <dd>{deviceName}</dd>
          </div>
        )}
        <div className={styles.detailRow}>
          <dt>Primera vez</dt>
          <dd>{formatLong(alert.first_occurred_at)}</dd>
        </div>
        <div className={styles.detailRow}>
          <dt>Última vez</dt>
          <dd>{formatLong(alert.last_occurred_at)}</dd>
        </div>
        <div className={styles.detailRow}>
          <dt>Repeticiones</dt>
          <dd>{timesLabel(alert.occurrence_count)}</dd>
        </div>
        <div className={styles.detailRow}>
          <dt>Estado</dt>
          <dd>{alert.read_at ? `Leída el ${formatLong(alert.read_at)}` : "Sin leer"}</dd>
        </div>
      </dl>

      <div className={styles.detailActions}>
        {!alert.read_at && (
          <Button variant="primary" icon={Check} onClick={onRead}>
            Marcar como leída
          </Button>
        )}
        {!silence && (
          <Button variant="ghost" icon={BellOff} onClick={onSilence}>
            Silenciar
          </Button>
        )}
      </div>
      <p className={styles.detailNote}>
        {silence
          ? `Silenciada: los avisos de este tipo no vuelven a llegar ${
              silence.silenced_until ? `hasta el ${formatLong(silence.silenced_until)}` : "hasta que la reactives"
            }.`
          : "Silenciar detiene los avisos futuros de este mismo tipo (por ejemplo, todos los bloqueos de esta app), no solo este."}
      </p>
    </article>
  );
}
