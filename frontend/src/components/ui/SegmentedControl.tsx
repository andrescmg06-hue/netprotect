import styles from "./SegmentedControl.module.css";

export type SegmentOption<T extends string> = { value: T; label: string };

/** A single choice among a few options (Hoy / 7 días / 30 días, alert tabs…). Radiogroup
 * semantics: it selects, it doesn't navigate. */
export function SegmentedControl<T extends string>({
  options,
  value,
  onChange,
  label,
}: {
  options: SegmentOption<T>[];
  value: T;
  onChange: (value: T) => void;
  label: string;
}) {
  return (
    <div className={styles.group} role="radiogroup" aria-label={label}>
      {options.map((option) => {
        const selected = option.value === value;
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={selected}
            className={selected ? `${styles.option} ${styles.selected}` : styles.option}
            onClick={() => onChange(option.value)}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}
