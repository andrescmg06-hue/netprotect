"use client";

import { Ban, History, LogIn, LogOut, RefreshCw } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { Button, Card, CardHeader, EmptyState, SegmentedControl, Spinner, Timeline, type TimelineItem } from "@/components/ui";
import { ApiError, type HistoryEvent, listDeviceHistory } from "@/lib/apiClient";
import { ruleTypeLabel } from "@/lib/ruleFormatting";

import styles from "./HistoryPanel.module.css";

type HistoryState =
  | { kind: "loading" }
  | { kind: "loaded"; events: HistoryEvent[] }
  | { kind: "error"; message: string };

type Filter = "all" | "app_rule" | "geofence";

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function toTimelineItem(event: HistoryEvent): TimelineItem {
  if (event.event_type === "APP_RULE") {
    const typeLabel = event.rule_type_applied ? ruleTypeLabel(event.rule_type_applied) : "Bloqueo";
    return {
      id: event.id,
      icon: Ban,
      tone: "danger",
      title: `${typeLabel} de ${event.package_name ?? "una aplicación"}`,
      occurredAt: event.occurred_at,
    };
  }
  const isEnter = event.geofence_event_type === "ENTER";
  const zoneName = event.geofence_name ?? "una zona";
  return {
    id: event.id,
    icon: isEnter ? LogIn : LogOut,
    tone: isEnter ? "success" : "neutral",
    title: isEnter ? `Entró a ${zoneName}` : `Salió de ${zoneName}`,
    occurredAt: event.occurred_at,
  };
}

/** Sprint 15 (logic), Sprint 36 (design): a single chronological timeline merging AppRuleEvent
 * (bloqueos) and GeofenceEvent (entradas/salidas) through the unified GET /devices/{id}/history
 * endpoint. The type filter and day grouping are presentation only — same events, same order,
 * nothing recomputed on the backend. Ubicación cruda still doesn't appear here (its own view). */
export function HistoryPanel({ accessToken, deviceId }: { accessToken: string; deviceId: string }) {
  const [state, setState] = useState<HistoryState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);
  const [filter, setFilter] = useState<Filter>("all");

  useEffect(() => {
    let cancelled = false;

    listDeviceHistory(accessToken, deviceId)
      .then(({ events }) => {
        if (!cancelled) {
          setState({ kind: "loaded", events });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setState({ kind: "error", message: describeError(error, "No se pudo cargar el historial") });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, reloadToken]);

  const events = useMemo(() => (state.kind === "loaded" ? state.events : []), [state]);
  const filtered = useMemo(
    () =>
      events.filter((event) => {
        if (filter === "app_rule") return event.event_type === "APP_RULE";
        if (filter === "geofence") return event.event_type === "GEOFENCE";
        return true;
      }),
    [events, filter]
  );
  const items = useMemo(() => filtered.map(toTimelineItem), [filtered]);

  return (
    <Card padding="none">
      <div className={styles.head}>
        <CardHeader icon={History} title="Historial de eventos" />
        <div className={styles.headActions}>
          <SegmentedControl
            label="Filtrar por tipo de evento"
            value={filter}
            onChange={setFilter}
            options={[
              { value: "all", label: "Todo" },
              { value: "app_rule", label: "Bloqueos" },
              { value: "geofence", label: "Geocercas" },
            ]}
          />
          <Button size="sm" icon={RefreshCw} onClick={() => setReloadToken((current) => current + 1)}>
            Actualizar
          </Button>
        </div>
      </div>

      <div className={styles.body}>
        {state.kind === "loading" && <Spinner label="Cargando historial…" />}
        {state.kind === "error" && <p className={styles.error}>{state.message}</p>}
        {state.kind === "loaded" && filtered.length === 0 && (
          <EmptyState
            icon={History}
            title={
              events.length === 0
                ? "Todavía no hay eventos registrados para este dispositivo"
                : "Ningún evento coincide con este filtro"
            }
          />
        )}
        {state.kind === "loaded" && filtered.length > 0 && <Timeline items={items} />}
      </div>
    </Card>
  );
}
