import styles from "./BackgroundScene.module.css";

/** Full-bleed page background — the processed desk photo (/login/background-desk-clean.png). `absolute` (not `fixed`) + no z-index, positioned first in the DOM:
 * globals.css paints an opaque gradient on `<body>` itself, so this stacks by DOM order among the
 * page's `relative` children, which is what puts it behind the content and in front of `<body>`.
 *
 * The single flat wash (≤.15 opacity) keeps the dark hero text readable without visibly dimming
 * the photo. */
export function BackgroundScene() {
  return <div aria-hidden="true" className={styles.scene} />;
}
