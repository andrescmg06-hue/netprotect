import Image from "next/image";

import styles from "./HeroShowcase.module.css";

/** Product showcase: a laptop + phone render with an illustrative "Dispositivos" screen — static
 * example data (D3: kept as-is by explicit decision), not a live view. Purely illustrative, so it
 * stays out of the accessibility tree (`aria-hidden`, `alt=""`).
 *
 * Self-positioned (absolute at lg) instead of sized by a flex parent, in the box measured from the
 * design reference: left 14.7vw, top 38vh, at most 49vw wide and 47vh tall, scaled with
 * `object-contain` anchored top-left. Hidden below lg — the reference coordinates only targeted
 * desktop sizes. */
export function HeroShowcase() {
  return (
    <div aria-hidden="true" className={styles.wrap}>
      <Image
        src="/login/devices-showcase.png"
        alt=""
        width={1536}
        height={1024}
        priority
        className={styles.image}
      />
    </div>
  );
}
