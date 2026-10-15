"use client";

import { Check, GraduationCap, ShieldCheck, ShieldOff } from "lucide-react";
import { useState } from "react";

import { ScheduleBar } from "@/components/ui/ScheduleBar";
import { Button, DayPicker, Field, Input, StatusBadge, Switch, describeDays } from "@/components/ui";
import {
  ApiError,
  type DefaultAppPolicy,
  type SchoolMode,
  updateDevicePolicy,
  updateSchoolMode,
} from "@/lib/apiClient";
import { minutesToTimeString, timeStringToMinutes } from "@/lib/ruleFormatting";
import { useDeviceRulesRealtime } from "@/lib/useDeviceRulesRealtime";

import styles from "./DevicePolicyPanel.module.css";

const ALL_DAYS_MASK = 0b111_1111;
const DEFAULT_START = "07:00";
const DEFAULT_END = "14:00";

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

const POLICY_OPTIONS: {
  value: DefaultAppPolicy;
  title: string;
  description: string;
  icon: typeof ShieldCheck;
}[] = [
  {
    value: "ALLOW",
    title: "Todo permitido salvo lo bloqueado",
    description: "Una app sin regla funciona normalmente.",
    icon: ShieldCheck,
  },
  {
    value: "BLOCK",
    title: "Sólo apps aprobadas",
    description:
      "Una app sin regla queda bloqueada. La pantalla de inicio, el teléfono y Ajustes nunca se bloquean.",
    icon: ShieldOff,
  },
];

/** Sprint 24: split out of DeviceRulesPanel (Sprints 8/9/12) — this half is device-level settings
 * (default policy and school mode), the other half (AppRulesPanel) is per-app rule management.
 * Both read/write the same backend endpoints as before; nothing changed there.
 *
 * Sprint 56 (mockup 05): two open sections. School mode is edited on one 24 h ScheduleBar (the
 * API holds ONE window and ONE days mask, so there are no per-day rows, templates or "copy to
 * all"); the time inputs stay as the exact, keyboard-first alternative. Local state is seeded
 * from props once and re-seeded by the per-device remount after every policy change — saving a
 * changed window while school mode is on calls the same enable handler with the same payload.
 */
export function DevicePolicyPanel({
  accessToken,
  deviceId,
  defaultAppPolicy,
  schoolMode,
  onPolicyChanged,
}: {
  accessToken: string;
  deviceId: string;
  defaultAppPolicy: DefaultAppPolicy;
  schoolMode: SchoolMode;
  onPolicyChanged: () => void;
}) {
  const [error, setError] = useState<string | null>(null);
  const [schoolModeStart, setSchoolModeStart] = useState(
    schoolMode.start_minute !== null ? minutesToTimeString(schoolMode.start_minute) : DEFAULT_START
  );
  const [schoolModeEnd, setSchoolModeEnd] = useState(
    schoolMode.end_minute !== null ? minutesToTimeString(schoolMode.end_minute) : DEFAULT_END
  );
  const [schoolModeDaysMask, setSchoolModeDaysMask] = useState(schoolMode.days_mask ?? ALL_DAYS_MASK);
  const [busy, setBusy] = useState(false);

  // Sprint 18: another tutor session (or the device itself) can change this device's policy or
  // school mode at any time while this screen is open.
  useDeviceRulesRealtime(accessToken, deviceId, onPolicyChanged);

  function handlePolicyChange(next: DefaultAppPolicy) {
    if (next === defaultAppPolicy) return;
    setError(null);
    setBusy(true);
    updateDevicePolicy(accessToken, deviceId, next)
      .then(() => {
        setBusy(false);
        onPolicyChanged();
      })
      .catch((err) => {
        setBusy(false);
        setError(describeError(err, "No se pudo cambiar el modo"));
      });
  }

  function handleToggleSchoolMode(checked: boolean) {
    setError(null);
    if (!checked) {
      setBusy(true);
      updateSchoolMode(accessToken, deviceId, { enabled: false })
        .then(() => {
          setBusy(false);
          onPolicyChanged();
        })
        .catch((err) => {
          setBusy(false);
          setError(describeError(err, "No se pudo desactivar el horario escolar"));
        });
      return;
    }
    const start = timeStringToMinutes(schoolModeStart);
    const end = timeStringToMinutes(schoolModeEnd);
    if (start === null || end === null || schoolModeDaysMask === 0) {
      setError("Indica una franja horaria válida con al menos un día.");
      return;
    }
    setBusy(true);
    updateSchoolMode(accessToken, deviceId, {
      enabled: true,
      start_minute: start,
      end_minute: end,
      days_mask: schoolModeDaysMask,
    })
      .then(() => {
        setBusy(false);
        onPolicyChanged();
      })
      .catch((err) => {
        setBusy(false);
        setError(describeError(err, "No se pudo activar el horario escolar"));
      });
  }

  function handleScheduleBarChange(start: number, end: number) {
    setSchoolModeStart(minutesToTimeString(start));
    setSchoolModeEnd(minutesToTimeString(end));
  }

  function handleDiscardChanges() {
    setError(null);
    setSchoolModeStart(
      schoolMode.start_minute !== null ? minutesToTimeString(schoolMode.start_minute) : DEFAULT_START
    );
    setSchoolModeEnd(schoolMode.end_minute !== null ? minutesToTimeString(schoolMode.end_minute) : DEFAULT_END);
    setSchoolModeDaysMask(schoolMode.days_mask ?? ALL_DAYS_MASK);
  }

  const startMinute = timeStringToMinutes(schoolModeStart);
  const endMinute = timeStringToMinutes(schoolModeEnd);
  const crossesMidnight = startMinute !== null && endMinute !== null && endMinute < startMinute;
  const unsavedWhileOn =
    schoolMode.enabled &&
    (startMinute !== schoolMode.start_minute ||
      endMinute !== schoolMode.end_minute ||
      schoolModeDaysMask !== schoolMode.days_mask);

  return (
    <div className={styles.page}>
      <section className={styles.section} aria-labelledby="policy-default-title">
        <header className={styles.sectionHead}>
          <h2 id="policy-default-title" className={styles.title}>
            Modo por defecto
          </h2>
          <p className={styles.lead}>Qué pasa con una app que no tiene ninguna regla propia.</p>
        </header>

        <div className={styles.options} role="group" aria-label="Modo por defecto">
          {POLICY_OPTIONS.map((option) => {
            const selected = option.value === defaultAppPolicy;
            const Icon = option.icon;
            return (
              <button
                key={option.value}
                type="button"
                aria-pressed={selected}
                className={selected ? `${styles.option} ${styles.optionSelected}` : styles.option}
                onClick={() => handlePolicyChange(option.value)}
                disabled={busy}
              >
                <Icon size={24} strokeWidth={1.6} className={styles.optionIcon} aria-hidden="true" />
                <span className={styles.optionText}>
                  <strong className={styles.optionTitle}>{option.title}</strong>
                  <span className={styles.optionDescription}>{option.description}</span>
                </span>
                <span className={styles.optionCheck} aria-hidden="true">
                  {selected && <Check size={14} strokeWidth={3} />}
                </span>
              </button>
            );
          })}
        </div>
      </section>

      <section className={styles.section} aria-labelledby="policy-school-title">
        <header className={styles.schoolHead}>
          <div className={styles.sectionHead}>
            <h2 id="policy-school-title" className={styles.title}>
              Horario escolar
            </h2>
            <p className={styles.lead}>
              Bloquea automáticamente lo no aprobado durante una franja horaria fija, sin tocar tus
              reglas.
            </p>
          </div>
          <div className={styles.toggle}>
            <Switch
              checked={schoolMode.enabled}
              onChange={handleToggleSchoolMode}
              label="Horario escolar"
              disabled={busy}
            />
            {schoolMode.enabled ? (
              <StatusBadge tone="success" dot>
                Activado
              </StatusBadge>
            ) : (
              <StatusBadge tone="neutral" dot>
                Desactivado
              </StatusBadge>
            )}
          </div>
        </header>

        <div className={styles.scheduleGrid}>
          <div className={styles.barColumn}>
            <p className={styles.summary}>
              {startMinute !== null && endMinute !== null ? (
                <>
                  De <strong>{schoolModeStart}</strong> a <strong>{schoolModeEnd}</strong>
                  {crossesMidnight ? " del día siguiente" : ""}
                  {schoolModeDaysMask !== 0 ? `, ${describeDays(schoolModeDaysMask).toLowerCase()}` : ""}.
                </>
              ) : (
                "Indica una hora de inicio y de fin."
              )}
            </p>
            <ScheduleBar
              startMinute={startMinute ?? 0}
              endMinute={endMinute ?? 0}
              onChange={handleScheduleBarChange}
              disabled={busy}
            />
            <Field label="Días" hint={describeDays(schoolModeDaysMask) || "Ningún día seleccionado"}>
              {() => <DayPicker value={schoolModeDaysMask} onChange={setSchoolModeDaysMask} disabled={busy} />}
            </Field>
          </div>

          <div className={styles.exactColumn}>
            <div className={styles.timeRow}>
              <Field label="Hora de inicio">
                {(id) => (
                  <Input id={id} type="time" value={schoolModeStart} onChange={(e) => setSchoolModeStart(e.target.value)} />
                )}
              </Field>
              <Field label="Hora de fin">
                {(id) => (
                  <Input id={id} type="time" value={schoolModeEnd} onChange={(e) => setSchoolModeEnd(e.target.value)} />
                )}
              </Field>
            </div>

            {unsavedWhileOn ? (
              <div className={styles.pending}>
                <p className={styles.pendingText}>Hay cambios sin guardar en la franja.</p>
                <div className={styles.pendingActions}>
                  <Button variant="primary" size="sm" loading={busy} onClick={() => handleToggleSchoolMode(true)}>
                    Guardar horario
                  </Button>
                  <Button variant="ghost" size="sm" disabled={busy} onClick={handleDiscardChanges}>
                    Descartar
                  </Button>
                </div>
              </div>
            ) : (
              !schoolMode.enabled && (
                <p className={styles.hint}>Al activar el interruptor se guarda esta franja con estos días.</p>
              )
            )}
          </div>
        </div>

        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}

        <aside className={styles.note}>
          <GraduationCap size={20} strokeWidth={1.6} className={styles.noteIcon} aria-hidden="true" />
          <p className={styles.noteText}>
            Durante la franja se bloquea todo lo que no esté aprobado. Una app con la regla «Permitir»
            sigue disponible, y las demás reglas se aplican igual que siempre. Si la hora de fin es
            anterior a la de inicio, la franja cruza la medianoche.
          </p>
        </aside>
      </section>
    </div>
  );
}
