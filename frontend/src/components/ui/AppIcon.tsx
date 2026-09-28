import styles from "./AppIcon.module.css";

const TONES = ["info", "purple", "success", "warning", "danger"] as const;

function toneFor(seed: string): (typeof TONES)[number] {
  let hash = 0;
  for (let i = 0; i < seed.length; i += 1) hash = (hash * 31 + seed.charCodeAt(i)) >>> 0;
  return TONES[hash % TONES.length];
}

/** No app has an actual icon asset in this system (the backend reports a package name and a
 * label, nothing graphical) — a flat tile with the label's first letter, in one of a few tones
 * picked from the package name, gives every app row a consistent, real anchor instead of a
 * generic folder glyph or no icon at all. */
export function AppIcon({ label, seed }: { label: string; seed: string }) {
  const initial = label.trim().charAt(0).toUpperCase() || "?";
  const tone = toneFor(seed);
  return (
    <span className={`${styles.icon} ${styles[tone]}`} aria-hidden="true">
      {initial}
    </span>
  );
}
