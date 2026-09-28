"use client";

import { Check, ShieldCheck, ShieldOff } from "lucide-react";
import { useState } from "react";

import { DayPicker, Field, Input, Switch, describeDays } from "@/components/ui";
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

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/** A 24-hour bar with the configured window highlighted — purely a visual summary of real
 * numbers already on screen (start/end minute, days), never a clock or a live indicator. Handles
 * a window that crosses midnight (e.g. 22:00–06:00) as two segments. */
function DayTimeline({ startMinute, endMinute }: { startMinute: number; endMinute: number }) {
  const toPercent = (minutes: number) => (minutes / 1440) * 100;
  const segments =
    startMinute <= endMinute
      ? [{ left: startMinute, width: endMinute - startMinute }]
      : [
          { left: startMinute, width: 1440 - startMinute },
          { left: 0, width: endMinute },
        ];

  return (
    <div className={styles.timeline}>
      {segments.map((segment, index) => (
        <span
          key={index}
          className={styles.timelineHighlight}
          style={{ left: `${toPercent(segment.left)}%`, width: `${toPercent(segment.width)}%` }}
        />
      ))}
      <div className={styles.timelineTicks}>
        <span>0:00</span>
        <span>6:00</span>
        <span>12:00</span>
        <span>18:00</span>
        <span>24:00</span>
      </div>
    </div>
  );
}

/** Sprint 24: split out of DeviceRulesPanel (Sprints 8/9/12) — this half is device-level settings
 * (default policy and school mode), the other half (AppRulesPanel) is per-app rule management.
 * Both read/write the same backend endpoints as before; nothing changed there.
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
    schoolMode.start_minute !== null ? minutesToTimeString(schoolMode.start_minute) : "07:00"
  );
  const [schoolModeEnd, setSchoolModeEnd] = useState(
    schoolMode.end_minute !== null ? minutesToTimeString(schoolMode.end_minute) : "14:00"
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

  const inAllowlistMode = defaultAppPolicy === "BLOCK";

  return (
    <div className={styles.stack}>
      <section className={styles.card}>
        <h3 className={styles.cardTitle}>Modo por defecto</h3>
        <p className={styles.cardSubtitle}>Qué pasa con una app que no tiene ninguna regla propia.</p>
        <div className={styles.optionGrid}>
          <button
            type="button"
            className={!inAllowlistMode ? `${styles.option} ${styles.optionSelected}` : styles.option}
            onClick={() => handlePolicyChange("ALLOW")}
            disabled={busy}
          >
            {!inAllowlistMode && (
              <span className={styles.optionCheck}>
                <Check size={14} strokeWidth={3} aria-hidden="true" />
              </span>
            )}
            <ShieldCheck size={26} strokeWidth={1.7} className={styles.optionIcon} aria-hidden="true" />
            <strong>Todo permitido salvo lo bloqueado</strong>
            <span>Una app sin regla funciona normalmente.</span>
          </button>
          <button
            type="button"
            className={inAllowlistMode ? `${styles.option} ${styles.optionSelected}` : styles.option}
            onClick={() => handlePolicyChange("BLOCK")}
            disabled={busy}
          >
            {inAllowlistMode && (
              <span className={styles.optionCheck}>
                <Check size={14} strokeWidth={3} aria-hidden="true" />
              </span>
            )}
            <ShieldOff size={26} strokeWidth={1.7} className={styles.optionIcon} aria-hidden="true" />
            <strong>Sólo apps aprobadas</strong>
            <span>
              Una app sin regla queda bloqueada. La pantalla de inicio, el teléfono y Ajustes nunca
              se bloquean.
            </span>
          </button>
        </div>
      </section>

      <section className={styles.card}>
        <div className={styles.schoolHead}>
          <div>
            <h3 className={styles.cardTitle}>Horario escolar</h3>
            <p className={styles.cardSubtitle}>
              {schoolMode.enabled
                ? "En esa franja, todo lo no aprobado queda bloqueado automáticamente."
                : "Bloquea automáticamente lo no aprobado durante una franja horaria fija, sin tocar tus reglas."}
            </p>
          </div>
          <Switch checked={schoolMode.enabled} onChange={handleToggleSchoolMode} label="Horario escolar" />
        </div>

        {schoolMode.enabled ? (
          <>
            <p className={styles.summary}>
              Activo de <strong>{minutesToTimeString(schoolMode.start_minute ?? 0)}</strong> a{" "}
              <strong>{minutesToTimeString(schoolMode.end_minute ?? 0)}</strong>,{" "}
              {describeDays(schoolMode.days_mask ?? 0).toLowerCase()}.
            </p>
            <DayTimeline startMinute={schoolMode.start_minute ?? 0} endMinute={schoolMode.end_minute ?? 0} />
          </>
        ) : (
          <div className={styles.scheduleForm}>
            <div className={styles.timeRow}>
              <Field label="Hora de inicio">
                {(id) => <Input id={id} type="time" value={schoolModeStart} onChange={(e) => setSchoolModeStart(e.target.value)} />}
              </Field>
              <Field label="Hora de fin">
                {(id) => <Input id={id} type="time" value={schoolModeEnd} onChange={(e) => setSchoolModeEnd(e.target.value)} />}
              </Field>
            </div>
            <Field label="Días" hint={describeDays(schoolModeDaysMask)}>
              {() => <DayPicker value={schoolModeDaysMask} onChange={setSchoolModeDaysMask} />}
            </Field>
            <DayTimeline
              startMinute={timeStringToMinutes(schoolModeStart) ?? 0}
              endMinute={timeStringToMinutes(schoolModeEnd) ?? 0}
            />
          </div>
        )}

        {error && <p className={styles.error}>{error}</p>}
      </section>
    </div>
  );
}
