"use client";

import { Bell, BellOff, Check, RefreshCw } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import {
  Button,
  Card,
  CardHeader,
  type Column,
  DataTable,
  EmptyState,
  MetricCard,
  MetricGrid,
  SegmentedControl,
  Spinner,
  StatusBadge,
} from "@/components/ui";
import { ALERT_LEVEL_ICON, ALERT_LEVEL_LABEL, ALERT_LEVEL_TONE, alertLabel, silenceLabel } from "@/lib/alertFormatting";
import {
  type Alert,
  type AlertLevel,
  type AlertSilence,
  ApiError,
  deleteAlertSilence,
  listAlertSilences,
  listDeviceAlerts,
  listGeofences,
  markAlertRead,
  silenceAlert,
} from "@/lib/apiClient";

import styles from "./AlertsPanel.module.css";

type AlertsState =
  | { kind: "loading" }
  | { kind: "loaded"; alerts: Alert[]; silences: AlertSilence[] }
  | { kind: "error"; message: string };

const LEVELS: AlertLevel[] = ["INFO", "WARNING", "HIGH", "CRITICAL"];

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/** Sprint 17 (logic), Sprint 37 (design): a tutor inbox generated from signals that already
 * existed (bloqueos de reglas, entradas/salidas de geocercas, señales de manipulación) — read
 * through GET /devices/{id}/alerts, deduplicated server-side while unread. Marking read and
 * silenciar son acciones de tutor, sólo desde el panel web.
 *
 * `view` splits this into two dashboard sections without splitting the fetch (Sprint 24): both
 * still load in one Promise.all, since silencing an alert here needs to update the "está
 * silenciada" state on the same alert list. "inbox" is a master-detail list (no calendario de
 * silencios en el mockup — plan-frontend.md regla 1: no existe, se omite; silenciar sigue siendo
 * indefinido, como desde el Sprint 17); "silenced" resolves each dedup_key into the app/zona a
 * tutor recognizes via a small local geofence-name lookup.
 */
export function AlertsPanel({
  accessToken,
  deviceId,
  view = "inbox",
}: {
  accessToken: string;
  deviceId: string;
  view?: "inbox" | "silenced";
}) {
  const [state, setState] = useState<AlertsState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);
  const [levelFilter, setLevelFilter] = useState<AlertLevel | "ALL">("ALL");
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [geofenceNames, setGeofenceNames] = useState<Record<string, string>>({});

  useEffect(() => {
    let cancelled = false;

    Promise.all([listDeviceAlerts(accessToken, deviceId), listAlertSilences(accessToken, deviceId)])
      .then(([alertsResponse, silencesResponse]) => {
        if (!cancelled) {
          setState({ kind: "loaded", alerts: alertsResponse.alerts, silences: silencesResponse.silences });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setState({ kind: "error", message: describeError(error, "No se pudieron cargar las alertas") });
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

  const alerts = useMemo(() => (state.kind === "loaded" ? state.alerts : []), [state]);
  const silences = state.kind === "loaded" ? state.silences : [];

  const filteredAlerts = useMemo(
    () => (levelFilter === "ALL" ? alerts : alerts.filter((alert) => alert.level === levelFilter)),
    [alerts, levelFilter]
  );
  const selectedAlert = useMemo(
    () => filteredAlerts.find((alert) => alert.id === selectedId) ?? filteredAlerts[0] ?? null,
    [filteredAlerts, selectedId]
  );
  const levelCounts = useMemo(() => {
    const counts: Record<AlertLevel, number> = { INFO: 0, WARNING: 0, HIGH: 0, CRITICAL: 0 };
    for (const alert of alerts) counts[alert.level] += 1;
    return counts;
  }, [alerts]);

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

  if (view === "silenced") {
    const silenceColumns: Column<AlertSilence>[] = [
      {
        key: "what",
        header: "Alerta silenciada",
        primary: true,
        render: (silence) => silenceLabel(silence.dedup_key, geofenceNames),
      },
      {
        key: "until",
        header: "Vence",
        render: (silence) =>
          silence.silenced_until ? new Date(silence.silenced_until).toLocaleString("es-CO") : "Indefinido",
      },
      {
        key: "actions",
        header: "",
        align: "right",
        render: (silence) => (
          <Button size="sm" variant="ghost" icon={Bell} onClick={() => handleUnsilence(silence.id)}>
            Reactivar
          </Button>
        ),
      },
    ];

    return (
      <Card padding="none">
        <div className={styles.cardHead}>
          <CardHeader
            icon={BellOff}
            title="Alertas silenciadas"
            actions={
              <Button size="sm" icon={RefreshCw} onClick={reload}>
                Actualizar
              </Button>
            }
          />
        </div>
        {state.kind === "loading" && (
          <div className={styles.emptyWrap}>
            <Spinner label="Cargando…" />
          </div>
        )}
        {state.kind === "error" && <p className={styles.error}>{state.message}</p>}
        {state.kind === "loaded" && silences.length === 0 && (
          <div className={styles.emptyWrap}>
            <EmptyState icon={BellOff} title="No hay ninguna alerta silenciada en este dispositivo" />
          </div>
        )}
        {state.kind === "loaded" && silences.length > 0 && (
          <DataTable columns={silenceColumns} rows={silences} rowKey={(silence) => silence.id} />
        )}
      </Card>
    );
  }

  return (
    <>
      <MetricGrid>
        {LEVELS.map((level) => (
          <MetricCard
            key={level}
            icon={ALERT_LEVEL_ICON[level]}
            tone={ALERT_LEVEL_TONE[level]}
            label={ALERT_LEVEL_LABEL[level]}
            value={levelCounts[level]}
          />
        ))}
      </MetricGrid>

      <div className={styles.controls}>
        <SegmentedControl
          label="Filtrar por nivel"
          value={levelFilter}
          onChange={setLevelFilter}
          options={[
            { value: "ALL" as const, label: "Todas" },
            ...LEVELS.map((level) => ({ value: level, label: ALERT_LEVEL_LABEL[level] })),
          ]}
        />
        <Button size="sm" icon={RefreshCw} onClick={reload}>
          Actualizar
        </Button>
      </div>

      <div className={styles.columns}>
        <Card padding="none">
          <div className={styles.cardHead}>
            <CardHeader icon={Bell} title="Bandeja" />
          </div>
          {state.kind === "loading" && (
            <div className={styles.emptyWrap}>
              <Spinner label="Cargando…" />
            </div>
          )}
          {state.kind === "error" && <p className={styles.error}>{state.message}</p>}
          {state.kind === "loaded" && filteredAlerts.length === 0 && (
            <div className={styles.emptyWrap}>
              <EmptyState icon={Bell} title="Sin alertas para este filtro" />
            </div>
          )}
          {state.kind === "loaded" && filteredAlerts.length > 0 && (
            <ul className={styles.list}>
              {filteredAlerts.map((alert) => {
                const Icon = ALERT_LEVEL_ICON[alert.level];
                const selected = selectedAlert?.id === alert.id;
                return (
                  <li key={alert.id}>
                    <button
                      type="button"
                      className={selected ? `${styles.row} ${styles.rowSelected}` : styles.row}
                      aria-current={selected ? "true" : undefined}
                      onClick={() => setSelectedId(alert.id)}
                    >
                      <span className={`${styles.rowIcon} ${styles[ALERT_LEVEL_TONE[alert.level]]}`}>
                        <Icon size={16} strokeWidth={2.25} aria-hidden="true" />
                      </span>
                      <span className={styles.rowBody}>
                        <span className={styles.rowTitle}>
                          {!alert.read_at && <span className={styles.unreadDot} aria-hidden="true" />}
                          {alertLabel(alert)}
                        </span>
                        <span className={styles.rowMeta}>
                          {new Date(alert.last_occurred_at).toLocaleString("es-CO")}
                          {alert.occurrence_count > 1 ? ` · x${alert.occurrence_count}` : ""}
                        </span>
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </Card>

        <Card>
          {selectedAlert ? (
            <>
              <CardHeader
                icon={ALERT_LEVEL_ICON[selectedAlert.level]}
                title={alertLabel(selectedAlert)}
                actions={<StatusBadge tone={ALERT_LEVEL_TONE[selectedAlert.level]}>{ALERT_LEVEL_LABEL[selectedAlert.level]}</StatusBadge>}
              />
              <dl className={styles.detailList}>
                <div className={styles.detailRow}>
                  <dt>Primera vez</dt>
                  <dd>{new Date(selectedAlert.first_occurred_at).toLocaleString("es-CO")}</dd>
                </div>
                <div className={styles.detailRow}>
                  <dt>Última vez</dt>
                  <dd>{new Date(selectedAlert.last_occurred_at).toLocaleString("es-CO")}</dd>
                </div>
                <div className={styles.detailRow}>
                  <dt>Repeticiones</dt>
                  <dd>{selectedAlert.occurrence_count}</dd>
                </div>
                <div className={styles.detailRow}>
                  <dt>Estado</dt>
                  <dd>
                    {selectedAlert.read_at
                      ? `Leída (${new Date(selectedAlert.read_at).toLocaleString("es-CO")})`
                      : "Sin leer"}
                  </dd>
                </div>
              </dl>
              <div className={styles.detailActions}>
                {!selectedAlert.read_at && (
                  <Button icon={Check} onClick={() => handleRead(selectedAlert.id)}>
                    Marcar leída
                  </Button>
                )}
                <Button variant="secondary" icon={BellOff} onClick={() => handleSilence(selectedAlert.id)}>
                  Silenciar
                </Button>
              </div>
            </>
          ) : (
            <EmptyState icon={Bell} title="Selecciona una alerta para ver su detalle" />
          )}
        </Card>
      </div>
    </>
  );
}
