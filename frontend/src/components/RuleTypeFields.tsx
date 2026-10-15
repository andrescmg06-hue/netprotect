import { Check } from "lucide-react";
import { useId } from "react";

import { ScheduleBar } from "@/components/ui/ScheduleBar";
import { ALL_DAYS_MASK, DayPicker, Field, Input, describeDays } from "@/components/ui";
import type { RuleType } from "@/lib/apiClient";
import { minutesToTimeString, ruleTypeTone, timeStringToMinutes } from "@/lib/ruleFormatting";

import styles from "./RuleTypeFields.module.css";

export type RuleTypeFieldsValue = {
  ruleType: RuleType;
  dailyLimitMinutes: string;
  weeklyLimitMinutes: string;
  scheduleStart: string;
  scheduleEnd: string;
  scheduleDaysMask: number;
};

export const DEFAULT_RULE_TYPE_FIELDS: RuleTypeFieldsValue = {
  ruleType: "BLOCK",
  dailyLimitMinutes: "30",
  weeklyLimitMinutes: "180",
  scheduleStart: "22:00",
  scheduleEnd: "06:00",
  scheduleDaysMask: ALL_DAYS_MASK,
};

/** The five real rule types, each with the one line a parent needs to choose between them. */
const RULE_TYPE_OPTIONS: { value: RuleType; label: string; meaning: string }[] = [
  { value: "BLOCK", label: "Bloquear", meaning: "No se puede abrir en ningún momento." },
  { value: "ALLOW", label: "Permitir", meaning: "Aprobada: disponible también en horario escolar." },
  { value: "DAILY_LIMIT", label: "Límite diario", meaning: "Unos minutos al día; después se bloquea." },
  { value: "WEEKLY_LIMIT", label: "Límite semanal", meaning: "Minutos por semana, contados desde el lunes." },
  { value: "SCHEDULE", label: "Horario", meaning: "Bloqueada durante una franja y unos días." },
];

export type RuleTypeExtras = {
  daily_limit_minutes?: number;
  weekly_limit_minutes?: number;
  schedule_start_minute?: number;
  schedule_end_minute?: number;
  schedule_days_mask?: number;
};

/** Turns the form's strings into the extra fields an upsert call needs, or an error to show
 * instead of submitting. Shared by AppRulesPanel and DeviceCategoriesPanel (Sprint 24 duplicated
 * this validation; Sprint 35 extracts it once). */
export function validateRuleTypeFields(value: RuleTypeFieldsValue): { error: string } | RuleTypeExtras {
  if (value.ruleType === "DAILY_LIMIT") {
    const minutes = Number(value.dailyLimitMinutes);
    if (!Number.isInteger(minutes) || minutes <= 0) {
      return { error: "El límite diario debe ser un número de minutos mayor que 0." };
    }
    return { daily_limit_minutes: minutes };
  }
  if (value.ruleType === "WEEKLY_LIMIT") {
    const minutes = Number(value.weeklyLimitMinutes);
    if (!Number.isInteger(minutes) || minutes <= 0) {
      return { error: "El límite semanal debe ser un número de minutos mayor que 0." };
    }
    return { weekly_limit_minutes: minutes };
  }
  if (value.ruleType === "SCHEDULE") {
    const start = timeStringToMinutes(value.scheduleStart);
    const end = timeStringToMinutes(value.scheduleEnd);
    if (start === null || end === null) {
      return { error: "Indica una hora de inicio y de fin válidas." };
    }
    if (value.scheduleDaysMask === 0) {
      return { error: "Selecciona al menos un día para el horario." };
    }
    return { schedule_start_minute: start, schedule_end_minute: end, schedule_days_mask: value.scheduleDaysMask };
  }
  return {};
}

/** The part of "crear/editar regla" that's identical whether the rule targets an app or a whole
 * category: type of rule, then whichever extra fields that type needs. The caller owns the target
 * field (a package name or a category select) and the submit button.
 *
 * Sprint 56: the type is a list of native radios (keyboard and screen reader behaviour for free),
 * each with its colour marker and what it does; a schedule is edited on the same 24 h ScheduleBar
 * as school mode, with the exact time inputs kept as the precise alternative. */
export function RuleTypeFields({
  value,
  onChange,
}: {
  value: RuleTypeFieldsValue;
  onChange: (patch: Partial<RuleTypeFieldsValue>) => void;
}) {
  const groupName = useId();
  const scheduleStart = timeStringToMinutes(value.scheduleStart) ?? 22 * 60;
  const scheduleEnd = timeStringToMinutes(value.scheduleEnd) ?? 6 * 60;

  return (
    <>
      <fieldset className={styles.types}>
        <legend className={styles.legend}>Tipo de regla</legend>
        {RULE_TYPE_OPTIONS.map((option) => {
          const selected = option.value === value.ruleType;
          return (
            <label
              key={option.value}
              className={selected ? `${styles.type} ${styles.typeSelected}` : styles.type}
            >
              <input
                type="radio"
                name={groupName}
                value={option.value}
                checked={selected}
                onChange={() => onChange({ ruleType: option.value })}
                className={styles.radio}
              />
              <span className={`${styles.marker} ${styles[ruleTypeTone(option.value)]}`} aria-hidden="true" />
              <span className={styles.typeText}>
                <span className={styles.typeLabel}>{option.label}</span>
                <span className={styles.typeMeaning}>{option.meaning}</span>
              </span>
              <span className={styles.typeCheck} aria-hidden="true">
                {selected && <Check size={16} strokeWidth={2.25} />}
              </span>
            </label>
          );
        })}
      </fieldset>

      {value.ruleType === "DAILY_LIMIT" && (
        <Field label="Minutos por día">
          {(id) => (
            <Input
              id={id}
              type="number"
              min={1}
              value={value.dailyLimitMinutes}
              onChange={(event) => onChange({ dailyLimitMinutes: event.target.value })}
            />
          )}
        </Field>
      )}

      {value.ruleType === "WEEKLY_LIMIT" && (
        <Field label="Minutos por semana">
          {(id) => (
            <Input
              id={id}
              type="number"
              min={1}
              value={value.weeklyLimitMinutes}
              onChange={(event) => onChange({ weeklyLimitMinutes: event.target.value })}
            />
          )}
        </Field>
      )}

      {value.ruleType === "SCHEDULE" && (
        <div className={styles.schedule}>
          <ScheduleBar
            startMinute={scheduleStart}
            endMinute={scheduleEnd}
            onChange={(start, end) =>
              onChange({ scheduleStart: minutesToTimeString(start), scheduleEnd: minutesToTimeString(end) })
            }
            startLabel="Inicio del bloqueo"
            endLabel="Fin del bloqueo"
            activeLabel="Bloqueada"
            idleLabel="Disponible"
          />
          <div className={styles.timeRow}>
            <Field label="Hora de inicio">
              {(id) => (
                <Input
                  id={id}
                  type="time"
                  value={value.scheduleStart}
                  onChange={(event) => onChange({ scheduleStart: event.target.value })}
                />
              )}
            </Field>
            <Field label="Hora de fin">
              {(id) => (
                <Input
                  id={id}
                  type="time"
                  value={value.scheduleEnd}
                  onChange={(event) => onChange({ scheduleEnd: event.target.value })}
                />
              )}
            </Field>
          </div>
          <Field label="Días" hint={describeDays(value.scheduleDaysMask)}>
            {() => (
              <DayPicker
                value={value.scheduleDaysMask}
                onChange={(mask) => onChange({ scheduleDaysMask: mask })}
              />
            )}
          </Field>
        </div>
      )}
    </>
  );
}
