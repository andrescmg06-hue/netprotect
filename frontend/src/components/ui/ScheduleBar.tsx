"use client";

import { useRef, useState, type KeyboardEvent, type PointerEvent } from "react";

import { minutesToTimeString } from "@/lib/ruleFormatting";

import styles from "./ScheduleBar.module.css";

const DAY_MINUTES = 1440;
const LAST_MINUTE = DAY_MINUTES - 1;
const HOUR_MARKS = [0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24];

type Thumb = "start" | "end";

type ScheduleBarProps = {
  /** Window start, minutes since midnight (0–1439). */
  startMinute: number;
  /** Window end, minutes since midnight (0–1439). Smaller than the start means an overnight window. */
  endMinute: number;
  onChange: (startMinute: number, endMinute: number) => void;
  /** Snap for dragging and arrow keys, in minutes. */
  step?: number;
  disabled?: boolean;
  /** Accessible names of the two handles. */
  startLabel?: string;
  endLabel?: string;
  /** Legend texts for the active window and the rest of the day. */
  activeLabel?: string;
  idleLabel?: string;
};

function clampMinute(value: number) {
  return Math.min(LAST_MINUTE, Math.max(0, value));
}

function snap(value: number, step: number) {
  return clampMinute(Math.round(value / step) * step);
}

/** The active window as one or two segments of the 24 h track (two when it crosses midnight). */
function segments(start: number, end: number) {
  if (start === end) return [];
  return start < end
    ? [{ left: start, width: end - start }]
    : [
        { left: start, width: DAY_MINUTES - start },
        { left: 0, width: end },
      ];
}

const toPercent = (minutes: number) => `${(minutes / DAY_MINUTES) * 100}%`;

/** A 24-hour bar where a time window (school mode) is shown and edited directly: drag either
 * handle, click the track to move the nearest one, or use the keyboard (each handle is a real
 * `role="slider"`: arrows ±step, Page Up/Down ±1 h, Home/End). An end before the start is a valid
 * overnight window and is drawn as two segments. The exact time inputs stay next to it in the
 * panel as the precise alternative; this component never rounds a value the user did not move. */
export function ScheduleBar({
  startMinute,
  endMinute,
  onChange,
  step = 15,
  disabled = false,
  startLabel = "Inicio del modo escolar",
  endLabel = "Fin del modo escolar",
  activeLabel = "Modo escolar (apps no aprobadas bloqueadas)",
  idleLabel = "Tiempo libre",
}: ScheduleBarProps) {
  const trackRef = useRef<HTMLDivElement>(null);
  const [dragging, setDragging] = useState<Thumb | null>(null);

  const start = clampMinute(startMinute);
  const end = clampMinute(endMinute);

  function update(thumb: Thumb, value: number) {
    if (thumb === "start") onChange(value, end);
    else onChange(start, value);
  }

  function minuteAt(clientX: number) {
    const track = trackRef.current;
    if (!track) return null;
    const rect = track.getBoundingClientRect();
    if (rect.width === 0) return null;
    const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
    return snap(ratio * DAY_MINUTES, step);
  }

  function handleThumbPointerDown(thumb: Thumb, event: PointerEvent<HTMLDivElement>) {
    if (disabled) return;
    event.preventDefault();
    event.currentTarget.setPointerCapture(event.pointerId);
    event.currentTarget.focus();
    setDragging(thumb);
  }

  function handleThumbPointerMove(thumb: Thumb, event: PointerEvent<HTMLDivElement>) {
    if (dragging !== thumb) return;
    const value = minuteAt(event.clientX);
    if (value !== null) update(thumb, value);
  }

  function handleThumbPointerUp(event: PointerEvent<HTMLDivElement>) {
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    setDragging(null);
  }

  function handleTrackPointerDown(event: PointerEvent<HTMLDivElement>) {
    if (disabled || event.target !== event.currentTarget) return;
    const value = minuteAt(event.clientX);
    if (value === null) return;
    // Move whichever handle is closer, measured around the clock.
    const distance = (a: number, b: number) => Math.min(Math.abs(a - b), DAY_MINUTES - Math.abs(a - b));
    update(distance(value, start) <= distance(value, end) ? "start" : "end", value);
  }

  function handleKeyDown(thumb: Thumb, event: KeyboardEvent<HTMLDivElement>) {
    if (disabled) return;
    const current = thumb === "start" ? start : end;
    const moves: Record<string, number> = {
      ArrowLeft: current - step,
      ArrowDown: current - step,
      ArrowRight: current + step,
      ArrowUp: current + step,
      PageDown: current - 60,
      PageUp: current + 60,
      Home: 0,
      End: LAST_MINUTE,
    };
    if (!(event.key in moves)) return;
    event.preventDefault();
    update(thumb, clampMinute(moves[event.key]));
  }

  const thumbs: { key: Thumb; value: number; label: string }[] = [
    { key: "start", value: start, label: startLabel },
    { key: "end", value: end, label: endLabel },
  ];

  return (
    <div className={`${styles.wrap} ${disabled ? styles.disabled : ""}`}>
      <div className={styles.hours} aria-hidden="true">
        {HOUR_MARKS.map((hour) => (
          <span
            key={hour}
            className={`${styles.hour} ${hour % 4 !== 0 ? styles.minorHour : ""}`}
            style={{ left: toPercent(hour * 60) }}
          >
            {String(hour).padStart(2, "0")}
          </span>
        ))}
      </div>

      <div ref={trackRef} className={styles.track} onPointerDown={handleTrackPointerDown}>
        {HOUR_MARKS.slice(1, -1).map((hour) => (
          <span key={hour} className={styles.tick} style={{ left: toPercent(hour * 60) }} aria-hidden="true" />
        ))}
        {segments(start, end).map((segment) => (
          <span
            key={segment.left}
            className={styles.segment}
            style={{ left: toPercent(segment.left), width: toPercent(segment.width) }}
            aria-hidden="true"
          />
        ))}
        {thumbs.map((thumb) => (
          <div
            key={thumb.key}
            role="slider"
            tabIndex={disabled ? -1 : 0}
            aria-label={thumb.label}
            aria-valuemin={0}
            aria-valuemax={LAST_MINUTE}
            aria-valuenow={thumb.value}
            aria-valuetext={minutesToTimeString(thumb.value)}
            aria-disabled={disabled || undefined}
            className={`${styles.thumb} ${dragging === thumb.key ? styles.thumbActive : ""}`}
            style={{ left: toPercent(thumb.value) }}
            onPointerDown={(event) => handleThumbPointerDown(thumb.key, event)}
            onPointerMove={(event) => handleThumbPointerMove(thumb.key, event)}
            onPointerUp={handleThumbPointerUp}
            onPointerCancel={handleThumbPointerUp}
            onKeyDown={(event) => handleKeyDown(thumb.key, event)}
          >
            <span className={styles.thumbTime}>{minutesToTimeString(thumb.value)}</span>
          </div>
        ))}
      </div>

      <div className={styles.legend}>
        <span className={styles.legendItem}>
          <span className={`${styles.swatch} ${styles.swatchIdle}`} aria-hidden="true" />
          {idleLabel}
        </span>
        <span className={styles.legendItem}>
          <span className={`${styles.swatch} ${styles.swatchActive}`} aria-hidden="true" />
          {activeLabel}
        </span>
      </div>
    </div>
  );
}
