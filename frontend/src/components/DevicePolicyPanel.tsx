"use client";

import { useState } from "react";

import {
  ApiError,
  type DefaultAppPolicy,
  type SchoolMode,
  updateDevicePolicy,
  updateSchoolMode,
} from "@/lib/apiClient";
import { useDeviceRulesRealtime } from "@/lib/useDeviceRulesRealtime";

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

/** Sprint 24: split out of DeviceRulesPanel (Sprints 8/9/12) — this half is device-level
 * settings (default policy and school mode), the other half (AppRulesPanel) is per-app rule
 * management. Both read/write the same backend endpoints as before; nothing changed there.
 * defaultAppPolicy/schoolMode still come from the Device object the dashboard already fetched
 * (DevicesPanel's listDevices), not from a fetch of this panel's own — onPolicyChanged asks the
 * caller to reload that Device so this panel's props reflect the write it just made.
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

  // Sprint 18: another tutor session (or the device itself) can change this device's policy or
  // school mode at any time while this screen is open.
  useDeviceRulesRealtime(accessToken, deviceId, onPolicyChanged);

  function handlePolicyChange(next: DefaultAppPolicy) {
    setError(null);
    updateDevicePolicy(accessToken, deviceId, next)
      .then(() => onPolicyChanged())
      .catch((err) => setError(describeError(err, "No se pudo cambiar el modo")));
  }

  function handleToggleSchoolMode() {
    setError(null);
    if (schoolMode.enabled) {
      updateSchoolMode(accessToken, deviceId, { enabled: false })
        .then(() => onPolicyChanged())
        .catch((err) => setError(describeError(err, "No se pudo desactivar el horario escolar")));
      return;
    }
    const start = timeStringToMinutes(schoolModeStart);
    const end = timeStringToMinutes(schoolModeEnd);
    if (start === null || end === null || schoolModeDaysMask === 0) {
      setError("Indica una franja horaria válida con al menos un día.");
      return;
    }
    updateSchoolMode(accessToken, deviceId, {
      enabled: true,
      start_minute: start,
      end_minute: end,
      days_mask: schoolModeDaysMask,
    })
      .then(() => onPolicyChanged())
      .catch((err) => setError(describeError(err, "No se pudo activar el horario escolar")));
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
        <button type="button" onClick={() => handlePolicyChange(inAllowlistMode ? "ALLOW" : "BLOCK")}>
          {inAllowlistMode ? "Permitir todo salvo lo bloqueado" : "Sólo permitir apps aprobadas"}
        </button>
      </div>

      <div className="policyRow">
        <div>
          <div className="appLabel">
            {schoolMode.enabled
              ? `Horario escolar activo (${minutesToTimeString(schoolMode.start_minute ?? 0)}–${minutesToTimeString(schoolMode.end_minute ?? 0)})`
              : "Horario escolar desactivado"}
          </div>
          <div className="appMeta">
            {schoolMode.enabled
              ? "En esa franja, todo lo no aprobado queda bloqueado automáticamente."
              : "Bloquea automáticamente lo no aprobado durante una franja horaria fija, sin tocar tus reglas."}
          </div>
          {!schoolMode.enabled && (
            <div className="scheduleFields" style={{ marginTop: 8 }}>
              <input
                type="time"
                value={schoolModeStart}
                onChange={(event) => setSchoolModeStart(event.target.value)}
                aria-label="Hora de inicio del horario escolar"
              />
              <span>a</span>
              <input
                type="time"
                value={schoolModeEnd}
                onChange={(event) => setSchoolModeEnd(event.target.value)}
                aria-label="Hora de fin del horario escolar"
              />
              <div className="dayPicker">
                {DAY_LABELS.map((label, index) => {
                  const bit = 1 << index;
                  const active = (schoolModeDaysMask & bit) !== 0;
                  return (
                    <button
                      type="button"
                      key={label + index}
                      className={active ? "dayButton dayButtonActive" : "dayButton"}
                      onClick={() => setSchoolModeDaysMask((current) => current ^ bit)}
                      aria-pressed={active}
                    >
                      {label}
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>
        <button type="button" onClick={handleToggleSchoolMode}>
          {schoolMode.enabled ? "Desactivar" : "Activar horario escolar"}
        </button>
      </div>
      {error && <p className="authError">{error}</p>}
    </div>
  );
}
