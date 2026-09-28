import styles from "./DayPicker.module.css";

const DAYS = [
  { short: "Lun", long: "Lunes" },
  { short: "Mar", long: "Martes" },
  { short: "Mié", long: "Miércoles" },
  { short: "Jue", long: "Jueves" },
  { short: "Vie", long: "Viernes" },
  { short: "Sáb", long: "Sábado" },
  { short: "Dom", long: "Domingo" },
];

export const ALL_DAYS_MASK = 0b111_1111;

/** Same bitmask the backend stores (`schedule_days_mask`/`school_mode_days_mask`: bit 0 =
 * Monday, Sprint 11), so it can be handed straight to apiClient without conversion. */
export function DayPicker({
  value,
  onChange,
  disabled = false,
}: {
  value: number;
  onChange: (mask: number) => void;
  disabled?: boolean;
}) {
  return (
    <div className={styles.days} role="group" aria-label="Días">
      {DAYS.map((day, index) => {
        const bit = 1 << index;
        const active = (value & bit) !== 0;
        return (
          <button
            key={day.short}
            type="button"
            className={active ? `${styles.day} ${styles.active}` : styles.day}
            aria-pressed={active}
            aria-label={day.long}
            disabled={disabled}
            onClick={() => onChange(value ^ bit)}
          >
            {day.short}
          </button>
        );
      })}
    </div>
  );
}

export function describeDays(mask: number): string {
  if (mask === ALL_DAYS_MASK) return "Todos los días";
  if (mask === 0b001_1111) return "Lunes a viernes";
  if (mask === 0b110_0000) return "Fines de semana";
  return DAYS.filter((_, index) => (mask & (1 << index)) !== 0)
    .map((day) => day.short)
    .join(", ");
}
