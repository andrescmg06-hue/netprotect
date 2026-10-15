"use client";

import {
  Ban,
  CalendarClock,
  ChevronDown,
  Clock3,
  FolderClosed,
  GraduationCap,
  Hourglass,
  LogIn,
  LogOut,
  type LucideIcon,
  RefreshCw,
  ShieldCheck,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import { Button, EmptyState, SegmentedControl, Spinner } from "@/components/ui";
import { ApiError, type AppliedRuleType, type HistoryEvent, listDeviceApplications, listDeviceHistory } from "@/lib/apiClient";
import { ruleTypeLabel } from "@/lib/ruleFormatting";

import styles from "./HistoryPanel.module.css";

type HistoryState =
  | { kind: "loading" }
  | { kind: "loaded"; events: HistoryEvent[]; loadedAt: number }
  | { kind: "error"; message: string };

type Filter = "all" | "app_rule" | "geofence";

type Tone = "block" | "enter" | "exit";

type Row = {
  id: string;
  occurredAt: string;
  time: string;
  icon: LucideIcon;
  tone: Tone;
  title: string;
  detail: string;
  reason: { icon: LucideIcon; text: string } | null;
};

type DayGroup = {
  key: string;
  eyebrow: string;
  day: string;
  monthYear: string;
  fullLabel: string;
  rows: Row[];
};

/** Open day groups by default until about this many events are on screen (at least one day). */
const DEFAULT_VISIBLE_EVENTS = 8;

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/** Why the device blocked the app, phrased from what rule_type_applied actually says. */
function blockReason(type: AppliedRuleType | null): { icon: LucideIcon; text: string } | null {
  switch (type) {
    case null:
      return null;
    case "SCHOOL_MODE":
      return { icon: GraduationCap, text: "Modo escolar activo" };
    case "DAILY_LIMIT":
      return { icon: Hourglass, text: "Límite diario alcanzado" };
    case "WEEKLY_LIMIT":
      return { icon: Hourglass, text: "Límite semanal alcanzado" };
    case "SCHEDULE":
      return { icon: CalendarClock, text: "Regla de horario" };
    case "CATEGORY":
      return { icon: FolderClosed, text: "Regla de su categoría" };
    case "DEFAULT_POLICY":
      return { icon: ShieldCheck, text: "App sin aprobar" };
    case "BLOCK":
      return { icon: Ban, text: "Bloqueo directo" };
    default:
      return { icon: Ban, text: ruleTypeLabel(type) };
  }
}

function toRow(event: HistoryEvent, appLabels: Record<string, string>): Row {
  const time = new Date(event.occurred_at).toLocaleTimeString("es-CO", { hour: "2-digit", minute: "2-digit" });
  if (event.event_type === "APP_RULE") {
    const packageName = event.package_name ?? "";
    return {
      id: event.id,
      occurredAt: event.occurred_at,
      time,
      icon: Ban,
      tone: "block",
      title: "App bloqueada",
      detail: appLabels[packageName] ?? (packageName || "Una aplicación"),
      reason: blockReason(event.rule_type_applied),
    };
  }
  const isEnter = event.geofence_event_type === "ENTER";
  return {
    id: event.id,
    occurredAt: event.occurred_at,
    time,
    icon: isEnter ? LogIn : LogOut,
    tone: isEnter ? "enter" : "exit",
    title: isEnter ? "Entrada a zona" : "Salida de zona",
    detail: event.geofence_name ?? "Zona sin nombre",
    reason: null,
  };
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

function groupByDay(rows: Row[], loadedAt: number): DayGroup[] {
  const today = new Date(loadedAt);
  const yesterday = new Date(loadedAt);
  yesterday.setDate(yesterday.getDate() - 1);
  const todayKey = today.toDateString();
  const yesterdayKey = yesterday.toDateString();

  const groups: DayGroup[] = [];
  for (const row of rows) {
    const date = new Date(row.occurredAt);
    const key = date.toDateString();
    const current = groups.at(-1);
    if (current && current.key === key) {
      current.rows.push(row);
      continue;
    }
    const weekday = capitalize(date.toLocaleDateString("es-CO", { weekday: "long" }));
    groups.push({
      key,
      eyebrow: key === todayKey ? "Hoy" : key === yesterdayKey ? "Ayer" : weekday,
      day: String(date.getDate()).padStart(2, "0"),
      monthYear: `${date.toLocaleDateString("es-CO", { month: "short" }).replace(".", "")} ${date.getFullYear()}`,
      fullLabel: date.toLocaleDateString("es-CO", { weekday: "long", day: "numeric", month: "long", year: "numeric" }),
      rows: [row],
    });
  }
  return groups;
}

/** Sprint 15 (logic), Sprint 36 (design), Sprint 57 (the timeline as protagonist): a single
 * chronological record merging AppRuleEvent (bloqueos) and GeofenceEvent (entradas/salidas)
 * through the unified GET /devices/{id}/history endpoint, grouped by day with the time on the
 * left and one continuous line. The type filter, the day grouping and the app names (looked up
 * once through listDeviceApplications) are presentation only — same events, nothing recomputed
 * on the backend. The API takes no date range, so there is no server-side date filter here, and
 * raw location fixes still don't appear (they have their own view). */
export function HistoryPanel({ accessToken, deviceId }: { accessToken: string; deviceId: string }) {
  const [state, setState] = useState<HistoryState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);
  const [refreshing, setRefreshing] = useState(false);
  const [filter, setFilter] = useState<Filter>("all");
  const [appLabels, setAppLabels] = useState<Record<string, string>>({});
  const [openDays, setOpenDays] = useState<Record<string, boolean>>({});

  useEffect(() => {
    let cancelled = false;

    listDeviceHistory(accessToken, deviceId)
      .then(({ events }) => {
        if (!cancelled) {
          setState({ kind: "loaded", events, loadedAt: Date.now() });
          setRefreshing(false);
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setState({ kind: "error", message: describeError(error, "No se pudo cargar el historial") });
          setRefreshing(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, reloadToken]);

  // App names for the block rows. Best effort: without them a row shows the package name.
  useEffect(() => {
    let cancelled = false;

    listDeviceApplications(accessToken, deviceId)
      .then(({ applications }) => {
        if (!cancelled) {
          setAppLabels(
            Object.fromEntries(applications.map((application) => [application.package_name, application.app_label]))
          );
        }
      })
      .catch(() => {
        // Package names are an acceptable fallback; nothing to tell the tutor.
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId]);

  const events = useMemo(() => (state.kind === "loaded" ? state.events : []), [state]);
  const loadedAt = state.kind === "loaded" ? state.loadedAt : 0;

  const filtered = useMemo(
    () =>
      events
        .filter((event) => {
          if (filter === "app_rule") return event.event_type === "APP_RULE";
          if (filter === "geofence") return event.event_type === "GEOFENCE";
          return true;
        })
        .sort((a, b) => Date.parse(b.occurred_at) - Date.parse(a.occurred_at)),
    [events, filter]
  );

  const groups = useMemo(
    () => groupByDay(filtered.map((event) => toRow(event, appLabels)), loadedAt),
    [filtered, appLabels, loadedAt]
  );

  const defaultOpen = useMemo(() => {
    const open = new Set<string>();
    let visible = 0;
    for (const group of groups) {
      if (open.size > 0 && visible >= DEFAULT_VISIBLE_EVENTS) break;
      open.add(group.key);
      visible += group.rows.length;
    }
    return open;
  }, [groups]);

  const latestId = groups[0]?.rows[0]?.id ?? null;

  function refresh() {
    setRefreshing(true);
    setReloadToken((current) => current + 1);
  }

  function toggleDay(key: string, open: boolean) {
    setOpenDays((current) => ({ ...current, [key]: !open }));
  }

  return (
    <section className={styles.history} aria-label="Historial de eventos">
      <div className={styles.toolbar}>
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
        <div className={styles.toolbarEnd}>
          {state.kind === "loaded" && (
            <p className={styles.count}>
              {filtered.length === 1 ? "1 evento" : `${filtered.length.toLocaleString("es-CO")} eventos`}
            </p>
          )}
          <Button size="sm" variant="ghost" icon={RefreshCw} loading={refreshing} onClick={refresh}>
            Actualizar
          </Button>
        </div>
      </div>

      {state.kind === "loading" && (
        <div className={styles.pad}>
          <Spinner label="Cargando historial…" />
        </div>
      )}
      {state.kind === "error" && (
        <p className={styles.error} role="alert">
          {state.message}
        </p>
      )}

      {state.kind === "loaded" && filtered.length === 0 && (
        <EmptyState
          icon={Clock3}
          illustration={
            <span className={styles.emptyArt} aria-hidden="true">
              <Clock3 size={22} strokeWidth={1.75} className={styles.emptyClock} />
              <span className={styles.emptyRail} />
              <span className={`${styles.emptyRow} ${styles.emptyRowLong}`}>
                <span className={styles.emptyNode} />
                <span className={styles.emptyBar} />
              </span>
              <span className={styles.emptyRow}>
                <span className={styles.emptyNode} />
                <span className={styles.emptyBar} />
              </span>
              <span className={`${styles.emptyRow} ${styles.emptyRowShort}`}>
                <span className={styles.emptyNode} />
                <span className={styles.emptyBar} />
              </span>
            </span>
          }
          title={
            events.length === 0
              ? "Todavía no hay eventos registrados para este dispositivo"
              : "Ningún evento coincide con este filtro"
          }
          description={
            events.length === 0
              ? "Aquí aparecerán, día por día, los bloqueos de apps y las entradas o salidas de tus geocercas en cuanto el dispositivo los reporte."
              : "Cambia a «Todo» para ver el resto del registro."
          }
          action={
            events.length > 0 ? (
              <Button size="sm" onClick={() => setFilter("all")}>
                Ver todo
              </Button>
            ) : undefined
          }
        />
      )}

      {state.kind === "loaded" && groups.length > 0 && (
        <div className={styles.days}>
          {groups.map((group) => {
            const open = openDays[group.key] ?? defaultOpen.has(group.key);
            const listId = `history-day-${group.key.replace(/\s+/g, "-")}`;
            return (
              <section key={group.key} className={styles.day} aria-label={group.fullLabel}>
                <header className={styles.dayHead}>
                  <p className={`eyebrow ${styles.dayEyebrow}`}>{group.eyebrow}</p>
                  <p className={styles.dayNumber}>{group.day}</p>
                  <p className={styles.dayMonth}>{group.monthYear}</p>
                </header>

                <div className={styles.dayBody}>
                  {open ? (
                    <ol id={listId} className={styles.events}>
                      {group.rows.map((row) => (
                        <li key={row.id} className={styles.event}>
                          <time className={styles.time} dateTime={row.occurredAt}>
                            {row.time}
                          </time>
                          <span
                            className={row.id === latestId ? `${styles.node} ${styles.nodeLatest}` : styles.node}
                            aria-hidden="true"
                          />
                          <span className={`${styles.icon} ${styles[row.tone]}`}>
                            <row.icon size={16} strokeWidth={2} aria-hidden="true" />
                          </span>
                          <span className={styles.body}>
                            <span className={styles.title}>{row.title}</span>
                            <span className={styles.detail}>{row.detail}</span>
                          </span>
                          {row.reason && (
                            <span className={styles.reason}>
                              <row.reason.icon size={15} strokeWidth={2} aria-hidden="true" />
                              {row.reason.text}
                            </span>
                          )}
                        </li>
                      ))}
                    </ol>
                  ) : null}
                  {/* Days open by default stay open without a control; a day the tutor
                      expanded can be folded back. */}
                  {(!open || openDays[group.key] === true) && (
                  <button
                    type="button"
                    className={styles.dayToggle}
                    aria-expanded={open}
                    aria-controls={open ? listId : undefined}
                    onClick={() => toggleDay(group.key, open)}
                  >
                    <span className={styles.dayToggleNode} aria-hidden="true" />
                    {open
                      ? "Contraer día"
                      : group.rows.length === 1
                        ? "1 evento"
                        : `${group.rows.length} eventos`}
                    <ChevronDown
                      size={16}
                      aria-hidden="true"
                      className={open ? `${styles.chevron} ${styles.chevronOpen}` : styles.chevron}
                    />
                  </button>
                  )}
                </div>
              </section>
            );
          })}
        </div>
      )}
    </section>
  );
}
