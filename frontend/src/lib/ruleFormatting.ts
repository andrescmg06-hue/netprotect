import type { AppliedRuleType, RuleType } from "@/lib/apiClient";
import { describeDays } from "@/components/ui";

/** Shared by AppRulesPanel and DeviceCategoriesPanel (Sprint 24), and by the history/statistics
 * panels that list applied rule types (Sprints 15/16). `AppliedRuleType` extends `RuleType` with
 * the three outcomes a device can report that a tutor never creates directly. */
export function ruleTypeLabel(type: AppliedRuleType): string {
  return {
    ALLOW: "Permitir",
    BLOCK: "Bloquear",
    DAILY_LIMIT: "Límite diario",
    WEEKLY_LIMIT: "Límite semanal",
    SCHEDULE: "Horario",
    CATEGORY: "Por categoría",
    SCHOOL_MODE: "Horario escolar",
    DEFAULT_POLICY: "Sin aprobar",
  }[type];
}

export function minutesToTimeString(minutes: number): string {
  const hours = Math.floor(minutes / 60)
    .toString()
    .padStart(2, "0");
  const mins = (minutes % 60).toString().padStart(2, "0");
  return `${hours}:${mins}`;
}

export function timeStringToMinutes(value: string): number | null {
  const match = /^(\d{2}):(\d{2})$/.exec(value);
  if (!match) return null;
  return Number(match[1]) * 60 + Number(match[2]);
}

/** The one line describing what a rule actually does, for a table row or a category card. */
export function describeRule(rule: {
  rule_type: RuleType;
  daily_limit_minutes: number | null;
  weekly_limit_minutes: number | null;
  schedule_start_minute: number | null;
  schedule_end_minute: number | null;
  schedule_days_mask: number | null;
}): string {
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
      )} (${describeDays(rule.schedule_days_mask ?? 0)})`;
  }
}
