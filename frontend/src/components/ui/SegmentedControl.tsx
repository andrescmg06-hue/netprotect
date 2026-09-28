import { type KeyboardEvent, useRef } from "react";

import styles from "./SegmentedControl.module.css";

export type SegmentOption<T extends string> = { value: T; label: string };

/** A single choice among a few options (Hoy / 7 días / 30 días, alert tabs…). Radiogroup
 * semantics: it selects, it doesn't navigate — so it follows the ARIA radio-group keyboard
 * pattern: one tab stop (the selected option), arrow keys move and select, Home/End jump. */
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
  const buttonsRef = useRef<(HTMLButtonElement | null)[]>([]);

  function select(index: number) {
    const option = options[(index + options.length) % options.length];
    onChange(option.value);
    buttonsRef.current[(index + options.length) % options.length]?.focus();
  }

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>, index: number) {
    switch (event.key) {
      case "ArrowRight":
      case "ArrowDown":
        event.preventDefault();
        select(index + 1);
        break;
      case "ArrowLeft":
      case "ArrowUp":
        event.preventDefault();
        select(index - 1);
        break;
      case "Home":
        event.preventDefault();
        select(0);
        break;
      case "End":
        event.preventDefault();
        select(options.length - 1);
        break;
    }
  }

  return (
    <div className={styles.group} role="radiogroup" aria-label={label}>
      {options.map((option, index) => {
        const selected = option.value === value;
        return (
          <button
            key={option.value}
            ref={(element) => {
              buttonsRef.current[index] = element;
            }}
            type="button"
            role="radio"
            aria-checked={selected}
            tabIndex={selected ? 0 : -1}
            className={selected ? `${styles.option} ${styles.selected}` : styles.option}
            onClick={() => onChange(option.value)}
            onKeyDown={(event) => handleKeyDown(event, index)}
          >
            {option.label}
          </button>
        );
      })}
    </div>
  );
}
