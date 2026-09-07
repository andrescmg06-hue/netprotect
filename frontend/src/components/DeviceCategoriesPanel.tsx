"use client";

import { useCallback, useEffect, useState } from "react";

import {
  ApiError,
  type Category,
  type CategoryAssignment,
  type CategoryRule,
  type RuleType,
  type UpsertCategoryRuleInput,
  deleteAppCategory,
  deleteCategoryRule,
  listAppCategories,
  listCategoryRules,
  upsertAppCategory,
  upsertCategoryRule,
} from "@/lib/apiClient";

type AssignmentsState =
  | { kind: "loading" }
  | { kind: "loaded"; assignments: CategoryAssignment[] }
  | { kind: "error"; message: string };

type CategoryRulesState =
  | { kind: "loading" }
  | { kind: "loaded"; categoryRules: CategoryRule[] }
  | { kind: "error"; message: string };

const CATEGORIES: Category[] = [
  "SOCIAL_MEDIA",
  "GAMES",
  "STREAMING",
  "EDUCATION",
  "PRODUCTIVITY",
  "COMMUNICATION",
  "NEWS",
  "SHOPPING",
  "FINANCE",
  "UTILITIES",
  "ADULT_CONTENT",
];

const CATEGORY_LABELS: Record<Category, string> = {
  SOCIAL_MEDIA: "Redes sociales",
  GAMES: "Juegos",
  STREAMING: "Streaming",
  EDUCATION: "Educación",
  PRODUCTIVITY: "Productividad",
  COMMUNICATION: "Comunicación",
  NEWS: "Noticias",
  SHOPPING: "Compras",
  FINANCE: "Finanzas",
  UTILITIES: "Utilidades",
  ADULT_CONTENT: "Contenido para adultos",
};

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

function describeCategoryRule(rule: CategoryRule): string {
  switch (rule.rule_type) {
    case "BLOCK":
      return "Bloqueada siempre";
    case "ALLOW":
      return "Aprobada";
    case "DAILY_LIMIT":
      return `Máximo ${rule.daily_limit_minutes} min/día`;
    case "WEEKLY_LIMIT":
      return `Máximo ${rule.weekly_limit_minutes} min/semana`;
    case "SCHEDULE":
      return `Bloqueada ${minutesToTimeString(rule.schedule_start_minute ?? 0)}–${minutesToTimeString(
        rule.schedule_end_minute ?? 0
      )} (${daysMaskToLabel(rule.schedule_days_mask ?? 0)})`;
  }
}

export function DeviceCategoriesPanel({
  accessToken,
  deviceId,
}: {
  accessToken: string;
  deviceId: string;
}) {
  const [assignmentsState, setAssignmentsState] = useState<AssignmentsState>({ kind: "loading" });
  const [assignmentsReloadToken, setAssignmentsReloadToken] = useState(0);
  const [rulesState, setRulesState] = useState<CategoryRulesState>({ kind: "loading" });
  const [rulesReloadToken, setRulesReloadToken] = useState(0);
  const [formError, setFormError] = useState<string | null>(null);

  const [packageName, setPackageName] = useState("");
  const [assignCategory, setAssignCategory] = useState<Category>(CATEGORIES[0]);

  const [ruleCategory, setRuleCategory] = useState<Category>(CATEGORIES[0]);
  const [ruleType, setRuleType] = useState<RuleType>("BLOCK");
  const [dailyLimitMinutes, setDailyLimitMinutes] = useState("30");
  const [weeklyLimitMinutes, setWeeklyLimitMinutes] = useState("180");
  const [scheduleStart, setScheduleStart] = useState("22:00");
  const [scheduleEnd, setScheduleEnd] = useState("06:00");
  const [scheduleDaysMask, setScheduleDaysMask] = useState(ALL_DAYS_MASK);

  // Same reasoning as DeviceRulesPanel.tsx: every setState here runs inside a .then()/.catch()
  // already chained in the effect body, never delegated to a helper — react-hooks/set-state-in-effect.
  useEffect(() => {
    let cancelled = false;
    listAppCategories(accessToken, deviceId)
      .then(({ assignments }) => {
        if (!cancelled) setAssignmentsState({ kind: "loaded", assignments });
      })
      .catch((error) => {
        if (!cancelled) {
          setAssignmentsState({
            kind: "error",
            message: describeError(error, "No se pudieron cargar las categorías asignadas"),
          });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, assignmentsReloadToken]);

  useEffect(() => {
    let cancelled = false;
    listCategoryRules(accessToken, deviceId)
      .then(({ category_rules: categoryRules }) => {
        if (!cancelled) setRulesState({ kind: "loaded", categoryRules });
      })
      .catch((error) => {
        if (!cancelled) {
          setRulesState({
            kind: "error",
            message: describeError(error, "No se pudieron cargar las reglas de categoría"),
          });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, rulesReloadToken]);

  const reloadAssignments = useCallback(() => {
    setAssignmentsState({ kind: "loading" });
    setAssignmentsReloadToken((current) => current + 1);
  }, []);

  const reloadRules = useCallback(() => {
    setRulesState({ kind: "loading" });
    setRulesReloadToken((current) => current + 1);
  }, []);

  function handleAssign(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);
    if (!packageName.trim()) {
      setFormError("El nombre del paquete es obligatorio (por ejemplo: com.instagram.android).");
      return;
    }
    upsertAppCategory(accessToken, deviceId, packageName.trim(), assignCategory)
      .then(() => {
        setPackageName("");
        reloadAssignments();
      })
      .catch((error) => setFormError(describeError(error, "No se pudo asignar la categoría")));
  }

  function handleUnassign(assignmentId: string) {
    setFormError(null);
    deleteAppCategory(accessToken, deviceId, assignmentId)
      .then(() => reloadAssignments())
      .catch((error) => setFormError(describeError(error, "No se pudo quitar la categoría")));
  }

  function handleSetCategoryRule(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);

    const input: UpsertCategoryRuleInput = { category: ruleCategory, rule_type: ruleType };

    if (ruleType === "DAILY_LIMIT") {
      const minutes = Number(dailyLimitMinutes);
      if (!Number.isInteger(minutes) || minutes <= 0) {
        setFormError("El límite diario debe ser un número de minutos mayor que 0.");
        return;
      }
      input.daily_limit_minutes = minutes;
    }

    if (ruleType === "WEEKLY_LIMIT") {
      const minutes = Number(weeklyLimitMinutes);
      if (!Number.isInteger(minutes) || minutes <= 0) {
        setFormError("El límite semanal debe ser un número de minutos mayor que 0.");
        return;
      }
      input.weekly_limit_minutes = minutes;
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

    upsertCategoryRule(accessToken, deviceId, input)
      .then(() => reloadRules())
      .catch((error) => setFormError(describeError(error, "No se pudo guardar la regla de categoría")));
  }

  function handleDeleteCategoryRule(categoryRuleId: string) {
    setFormError(null);
    deleteCategoryRule(accessToken, deviceId, categoryRuleId)
      .then(() => reloadRules())
      .catch((error) => setFormError(describeError(error, "No se pudo eliminar la regla de categoría")));
  }

  return (
    <div className="rulesPanel">
      <strong>Categorías</strong>

      <form className="ruleForm" onSubmit={handleAssign}>
        <input
          value={packageName}
          onChange={(event) => setPackageName(event.target.value)}
          placeholder="com.instagram.android"
          maxLength={255}
          aria-label="Paquete a categorizar"
        />
        <select
          value={assignCategory}
          onChange={(event) => setAssignCategory(event.target.value as Category)}
        >
          {CATEGORIES.map((category) => (
            <option key={category} value={category}>
              {CATEGORY_LABELS[category]}
            </option>
          ))}
        </select>
        <button type="submit">Asignar categoría</button>
      </form>

      {formError && <p className="authError">{formError}</p>}

      {assignmentsState.kind === "loading" && <p className="statusText">Cargando categorías…</p>}
      {assignmentsState.kind === "error" && <p className="authError">{assignmentsState.message}</p>}
      {assignmentsState.kind === "loaded" && assignmentsState.assignments.length === 0 && (
        <p className="statusText">Ninguna app tiene categoría asignada todavía.</p>
      )}
      {assignmentsState.kind === "loaded" && assignmentsState.assignments.length > 0 && (
        <ul className="appList">
          {assignmentsState.assignments.map((assignment) => (
            <li key={assignment.id} className="appRow">
              <div>
                <div className="appLabel">{assignment.package_name}</div>
                <div className="appMeta">{CATEGORY_LABELS[assignment.category]}</div>
              </div>
              <button
                type="button"
                className="dangerButton"
                onClick={() => handleUnassign(assignment.id)}
              >
                Quitar
              </button>
            </li>
          ))}
        </ul>
      )}

      <form className="ruleForm" onSubmit={handleSetCategoryRule}>
        <select
          value={ruleCategory}
          onChange={(event) => setRuleCategory(event.target.value as Category)}
        >
          {CATEGORIES.map((category) => (
            <option key={category} value={category}>
              {CATEGORY_LABELS[category]}
            </option>
          ))}
        </select>
        <select value={ruleType} onChange={(event) => setRuleType(event.target.value as RuleType)}>
          <option value="BLOCK">Bloquear</option>
          <option value="ALLOW">Permitir</option>
          <option value="DAILY_LIMIT">Límite diario</option>
          <option value="WEEKLY_LIMIT">Límite semanal</option>
          <option value="SCHEDULE">Horario</option>
        </select>

        {ruleType === "DAILY_LIMIT" && (
          <input
            type="number"
            min={1}
            value={dailyLimitMinutes}
            onChange={(event) => setDailyLimitMinutes(event.target.value)}
            aria-label="Minutos por día para la categoría"
          />
        )}

        {ruleType === "WEEKLY_LIMIT" && (
          <input
            type="number"
            min={1}
            value={weeklyLimitMinutes}
            onChange={(event) => setWeeklyLimitMinutes(event.target.value)}
            aria-label="Minutos por semana para la categoría"
          />
        )}

        {ruleType === "SCHEDULE" && (
          <div className="scheduleFields">
            <input
              type="time"
              value={scheduleStart}
              onChange={(event) => setScheduleStart(event.target.value)}
              aria-label="Hora de inicio del bloqueo de categoría"
            />
            <span>a</span>
            <input
              type="time"
              value={scheduleEnd}
              onChange={(event) => setScheduleEnd(event.target.value)}
              aria-label="Hora de fin del bloqueo de categoría"
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

        <button type="submit">Guardar regla de categoría</button>
      </form>

      {rulesState.kind === "loading" && <p className="statusText">Cargando reglas de categoría…</p>}
      {rulesState.kind === "error" && <p className="authError">{rulesState.message}</p>}
      {rulesState.kind === "loaded" && rulesState.categoryRules.length === 0 && (
        <p className="statusText">Todavía no hay reglas por categoría.</p>
      )}
      {rulesState.kind === "loaded" && rulesState.categoryRules.length > 0 && (
        <ul className="appList">
          {rulesState.categoryRules.map((rule) => (
            <li key={rule.id} className="appRow">
              <div>
                <div className="appLabel">{CATEGORY_LABELS[rule.category]}</div>
                <div className="appMeta">{describeCategoryRule(rule)}</div>
              </div>
              <button
                type="button"
                className="dangerButton"
                onClick={() => handleDeleteCategoryRule(rule.id)}
              >
                Eliminar
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
