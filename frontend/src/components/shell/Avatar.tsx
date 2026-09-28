"use client";

import { useState } from "react";

import styles from "./Avatar.module.css";

/** Google profile photo, falling back to initials when there is none or it fails to load. */
export function Avatar({
  name,
  src,
  size = 40,
  online = false,
}: {
  name: string;
  src: string | null;
  size?: number;
  online?: boolean;
}) {
  const [failed, setFailed] = useState(false);
  const initials = name
    .split(/[\s@.]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");

  return (
    <span className={styles.avatar} style={{ width: size, height: size, fontSize: size * 0.38 }}>
      {src && !failed ? (
        // eslint-disable-next-line @next/next/no-img-element -- remote Google avatar, no optimizer needed
        <img src={src} alt="" referrerPolicy="no-referrer" onError={() => setFailed(true)} />
      ) : (
        <span aria-hidden="true">{initials}</span>
      )}
      {online && <span className={styles.online} aria-hidden="true" />}
    </span>
  );
}
