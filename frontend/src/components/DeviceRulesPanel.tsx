"use client";

import { useCallback, useEffect, useState } from "react";

import {
  ApiError,
  type AppRule,
  type AppRuleEvent,
  type AppliedRuleType,
  type DefaultAppPolicy,
  type RuleType,
  type UpsertAppRuleInput,
  deleteAppRule,
  listAppRules,
  listRuleEvents,
  updateDevicePolicy,
  upsertAppRule,
} from "@/lib/apiClient";

type RulesState =
  | { kind: "loading" }
  | { kind: "loaded"; rules: AppRule[] }
  | { kind: "error"; message: string };

type EventsState =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "loaded"; events: AppRuleEvent[] }
  | { kind: "error"; message: string };

const DAY_LABELS = ["L", "M", "X", "J", "V", "S", "D"];
const ALL_DAYS_MASK = 0b111_1111;

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function timeStringToMinutes(value: string): number | null {
  const match = /^(\d{2}):(\d{2})$/.exec(value);
  if (!match) return null;
  return Number(match[1]) * 60 + Number(match[2]);
}

function minutesToTimeString(minutes: number): string {
  const hours = Math.floor(minutes / 60)
    .toString()
    .padStart(2, "0");
  const mins = (minutes % 60).toString().padStart(2, "0");
  return `${hours}:${mins}`;
}

function daysMaskToLabel(mask: number): string {
  return DAY_LABELS.filter((_, index) => (mask & (1 << index)) !== 0).join(" ");
}

function ruleTypeLabel(type: AppliedRuleType): string {
  return {
    ALLOW: "Permitir",
    BLOCK: "Bloquear",
    DAILY_LIMIT: "Límite diario",
    SCHEDULE: "Horario",
    DEFAULT_POLICY: "Sin aprobar",
  }[type];
}

function describeRule(rule: AppRule): string {
  switch (rule.rule_type) {
    case "BLOCK":
      return "Bloqueada siempre";
    case "ALLOW":
      return "Aprobada";
    case "DAILY_LIMIT":
      return `Máximo ${rule.daily_limit_minutes} min/día`;
    case "SCHEDULE":
      return `Bloqueada ${minutesToTimeString(rule.schedule_start_minute ?? 0)}–${minutesToTimeString(
        rule.schedule_end_minute ?? 0
      )} (${daysMaskToLabel(rule.schedule_days_mask ?? 0)})`;
  }
}

export function DeviceRulesPanel({
  accessToken,
  deviceId,
  defaultAppPolicy,
  onPolicyChanged,
}: {
  accessToken: string;
  deviceId: string;
  defaultAppPolicy: DefaultAppPolicy;
  onPolicyChanged: () => void;
}) {
  const [rulesState, setRulesState] = useState<RulesState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);
  const [eventsState, setEventsState] = useState<EventsState>({ kind: "idle" });
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [packageName, setPackageName] = useState("");
  const [ruleType, setRuleType] = useState<RuleType>("BLOCK");
  const [dailyLimitMinutes, setDailyLimitMinutes] = useState("30");
  const [scheduleStart, setScheduleStart] = useState("22:00");
  const [scheduleEnd, setScheduleEnd] = useState("06:00");
  const [scheduleDaysMask, setScheduleDaysMask] = useState(ALL_DAYS_MASK);

  // Every setState below runs inside a .then()/.catch() already chained in the effect body
  // (never delegated to a helper, never awaited synchronously) — see DeviceApplicationsList.tsx
  // for why: an async function whose setState happens after an await still counts as
  // "calling setState from an effect" to the react-hooks/set-state-in-effect rule.
  useEffect(() => {
    let cancelled = false;

    listAppRules(accessToken, deviceId)
      .then(({ rules }) => {
        if (!cancelled) {
          setRulesState({ kind: "loaded", rules });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setRulesState({ kind: "error", message: describeError(error, "No se pudieron cargar las reglas") });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, reloadToken]);

  const loadRules = useCallback(() => {
    setRulesState({ kind: "loading" });
    setReloadToken((current) => current + 1);
  }, []);

  function toggleEvents() {
    if (eventsState.kind === "loaded" || eventsState.kind === "loading") {
      setEventsState({ kind: "idle" });
      return;
    }
    setEventsState({ kind: "loading" });
    listRuleEvents(accessToken, deviceId)
      .then(({ events }) => setEventsState({ kind: "loaded", events }))
      .catch((error) =>
        setEventsState({ kind: "error", message: describeError(error, "No se pudo cargar el historial") })
      );
  }

  function handlePolicyChange(next: DefaultAppPolicy) {
    setFormError(null);
    updateDevicePolicy(accessToken, deviceId, next)
      .then(() => onPolicyChanged())
      .catch((error) => setFormError(describeError(error, "No se pudo cambiar el modo")));
  }

  function handleDelete(ruleId: string) {
    setFormError(null);
    deleteAppRule(accessToken, deviceId, ruleId)
      .then(() => loadRules())
      .catch((error) => setFormError(describeError(error, "No se pudo eliminar la regla")));
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);

    if (!packageName.trim()) {
      setFormError("El nombre del paquete es obligatorio (por ejemplo: com.instagram.android).");
      return;
    }

    const input: UpsertAppRuleInput = {
      package_name: packageName.trim(),
      rule_type: ruleType,
    };

    if (ruleType === "DAILY_LIMIT") {
      const minutes = Number(dailyLimitMinutes);
      if (!Number.isInteger(minutes) || minutes <= 0) {
        setFormError("El límite diario debe ser un número de minutos mayor que 0.");
        return;
      }
      input.daily_limit_minutes = minutes;
    }

    if (ruleType === "SCHEDULE") {
      const start = timeStringToMinutes(scheduleStart);
      const end = timeStringToMinutes(scheduleEnd);
      if (start === null || end === null) {
        setFormError("Indica una hora de inicio y de fin válidas.");
        return;
      }
      if (scheduleDaysMask === 0) {
        setFormError("Selecciona al menos un día para el horario.");
        return;
      }
      input.schedule_start_minute = start;
      input.schedule_end_minute = end;
      input.schedule_days_mask = scheduleDaysMask;
    }

    setSubmitting(true);
    upsertAppRule(accessToken, deviceId, input)
      .then(() => {
        setSubmitting(false);
        setPackageName("");
        loadRules();
      })
      .catch((error) => {
        setSubmitting(false);
        setFormError(describeError(error, "No se pudo guardar la regla"));
      });
  }

  const inAllowlistMode = defaultAppPolicy === "BLOCK";

  return (
    <div className="rulesPanel">
      <div className="policyRow">
        <div>
          <div className="appLabel">
            {inAllowlistMode ? "Sólo apps aprobadas" : "Todo permitido salvo lo bloqueado"}
          </div>
          <div className="appMeta">
            {inAllowlistMode
              ? "Una app sin regla queda bloqueada. La pantalla de inicio, el teléfono y Ajustes nunca se bloquean."
              : "Una app sin regla funciona normalmente."}
          </div>
        </div>
        <button
          type="button"
          onClick={() => handlePolicyChange(inAllowlistMode ? "ALLOW" : "BLOCK")}
        >
          {inAllowlistMode ? "Permitir todo salvo lo bloqueado" : "Sólo permitir apps aprobadas"}
        </button>
      </div>

      <form className="ruleForm" onSubmit={handleSubmit}>
        <input
          value={packageName}
          onChange={(event) => setPackageName(event.target.value)}
          placeholder="com.instagram.android"
          maxLength={255}
          aria-label="Paquete de la app"
        />
        <select value={ruleType} onChange={(event) => setRuleType(event.target.value as RuleType)}>
          <option value="BLOCK">Bloquear</option>
          <option value="ALLOW">Permitir</option>
          <option value="DAILY_LIMIT">Límite diario</option>
          <option value="SCHEDULE">Horario</option>
        </select>

        {ruleType === "DAILY_LIMIT" && (
          <input
            type="number"
            min={1}
            value={dailyLimitMinutes}
            onChange={(event) => setDailyLimitMinutes(event.target.value)}
            aria-label="Minutos por día"
          />
        )}

        {ruleType === "SCHEDULE" && (
          <div className="scheduleFields">
            <input
              type="time"
              value={scheduleStart}
              onChange={(event) => setScheduleStart(event.target.value)}
              aria-label="Hora de inicio del bloqueo"
            />
            <span>a</span>
            <input
              type="time"
              value={scheduleEnd}
              onChange={(event) => setScheduleEnd(event.target.value)}
              aria-label="Hora de fin del bloqueo"
            />
            <div className="dayPicker">
              {DAY_LABELS.map((label, index) => {
                const bit = 1 << index;
                const active = (scheduleDaysMask & bit) !== 0;
                return (
                  <button
                    type="button"
                    key={label + index}
                    className={active ? "dayButton dayButtonActive" : "dayButton"}
                    onClick={() => setScheduleDaysMask((current) => current ^ bit)}
                    aria-pressed={active}
                  >
                    {label}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        <button type="submit" disabled={submitting}>
          {submitting ? "Guardando…" : "Guardar regla"}
        </button>
      </form>

      {formError && <p className="authError">{formError}</p>}

      {rulesState.kind === "loading" && <p className="statusText">Cargando reglas…</p>}
      {rulesState.kind === "error" && <p className="authError">{rulesState.message}</p>}
      {rulesState.kind === "loaded" && rulesState.rules.length === 0 && (
        <p className="statusText">Todavía no hay reglas para este dispositivo.</p>
      )}
      {rulesState.kind === "loaded" && rulesState.rules.length > 0 && (
        <ul className="appList">
          {rulesState.rules.map((rule) => (
            <li key={rule.id} className="appRow">
              <div>
                <div className="appLabel">{rule.package_name}</div>
                <div className="appMeta">
                  {ruleTypeLabel(rule.rule_type)} · {describeRule(rule)}
                </div>
              </div>
              <button type="button" className="dangerButton" onClick={() => handleDelete(rule.id)}>
                Eliminar
              </button>
            </li>
          ))}
        </ul>
      )}

      <button type="button" onClick={toggleEvents}>
        {eventsState.kind === "loaded" || eventsState.kind === "loading"
          ? "Ocultar historial de bloqueos"
          : "Ver historial de bloqueos"}
      </button>

      {eventsState.kind === "loading" && <p className="statusText">Cargando historial…</p>}
      {eventsState.kind === "error" && <p className="authError">{eventsState.message}</p>}
      {eventsState.kind === "loaded" && eventsState.events.length === 0 && (
        <p className="statusText">Todavía no se aplicó ningún bloqueo en este dispositivo.</p>
      )}
      {eventsState.kind === "loaded" && eventsState.events.length > 0 && (
        <ul className="appList">
          {eventsState.events.map((event) => (
            <li key={event.id} className="appRow">
              <div>
                <div className="appLabel">{event.package_name}</div>
                <div className="appMeta">{ruleTypeLabel(event.rule_type_applied)}</div>
              </div>
              <span className="appUsage">{new Date(event.occurred_at).toLocaleString("es-CO")}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
