/** Sprint 55: the date, time and duration formats of the redesigned views, in one place. Before
 * this file each panel carried its own copy and the panel showed dates in four different shapes
 * (docs/redesign/fase-0-informe.md §3). Everything is es-CO, the locale the panel already used,
 * and is read in the browser's own time zone. Panels not yet recomposed keep their local copies
 * until their own sprint moves them here. */

const LOCALE = "es-CO";
const DAY_MS = 86_400_000;

function startOfLocalDay(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}

/** Whole calendar days between `iso` and today, in local time: 0 today, 1 yesterday. Rounded so a
 * 23- or 25-hour day around a daylight-saving change still counts as one. */
function daysAgo(iso: string): number {
  return Math.round((startOfLocalDay(new Date()) - startOfLocalDay(new Date(iso))) / DAY_MS);
}

function lowerFirst(text: string): string {
  return text.charAt(0).toLowerCase() + text.slice(1);
}

/** "4:12 p. m." */
export function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString(LOCALE, { hour: "numeric", minute: "2-digit" });
}

/** "4 oct 2026" */
export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(LOCALE, { dateStyle: "medium" });
}

/** A moment the way a person says it: "Hoy, 4:12 p. m.", "Ayer, 9:03 a. m." or, further back,
 * "2 oct 2026, 1:03 a. m.". `null` (a device that never reported) gives `fallback`. `inline`
 * lowercases the first letter for use mid-sentence ("Visto: hoy, 4:12 p. m."). */
export function formatMoment(
  iso: string | null,
  { fallback = "Nunca", inline = false }: { fallback?: string; inline?: boolean } = {}
): string {
  let text: string;
  if (!iso) {
    text = fallback;
  } else {
    const days = daysAgo(iso);
    if (days === 0) text = `Hoy, ${formatTime(iso)}`;
    else if (days === 1) text = `Ayer, ${formatTime(iso)}`;
    else text = new Date(iso).toLocaleString(LOCALE, { dateStyle: "medium", timeStyle: "short" });
  }
  return inline ? lowerFirst(text) : text;
}

/** The calendar day an event belongs to (local time), for grouping a timeline. */
export function dayKey(iso: string): string {
  return new Date(iso).toDateString();
}

/** "Hoy", "Ayer" or "Lunes, 3 de octubre" (with the year when it is not the current one). */
export function formatDayLabel(iso: string): string {
  const days = daysAgo(iso);
  if (days === 0) return "Hoy";
  if (days === 1) return "Ayer";
  const date = new Date(iso);
  const sameYear = date.getFullYear() === new Date().getFullYear();
  const label = date.toLocaleDateString(LOCALE, {
    weekday: "long",
    day: "numeric",
    month: "long",
    ...(sameYear ? {} : { year: "numeric" }),
  });
  return label.charAt(0).toUpperCase() + label.slice(1);
}

export type DurationPart = { value: string; unit: string };

/** A duration as figure/unit pairs, so a view can set the figures large and the units small:
 * 12 720 s → 3 h 32 min. Under a minute of real use reads "< 1 min", none at all "0 min". */
export function durationParts(totalSeconds: number): DurationPart[] {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  if (hours > 0) {
    return minutes > 0
      ? [
          { value: String(hours), unit: "h" },
          { value: String(minutes), unit: "min" },
        ]
      : [{ value: String(hours), unit: "h" }];
  }
  if (minutes > 0) return [{ value: String(minutes), unit: "min" }];
  return [{ value: totalSeconds > 0 ? "< 1" : "0", unit: "min" }];
}

/** "3 h 24 min", "24 min", "< 1 min" or "0 min". */
export function formatDuration(totalSeconds: number): string {
  return durationParts(totalSeconds)
    .map((part) => `${part.value} ${part.unit}`)
    .join(" ");
}

/** A countdown as m:ss: "2:59", "0:07". */
export function formatCountdown(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  return `${Math.floor(safe / 60)}:${String(safe % 60).padStart(2, "0")}`;
}
