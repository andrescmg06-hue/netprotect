"use client";

import { ChevronDown, Smartphone } from "lucide-react";

import { StatusBadge } from "@/components/ui";
import type { Device } from "@/lib/apiClient";

import styles from "./DeviceSelector.module.css";
import { deviceStatusBadge, deviceSubtitle } from "./deviceStatus";

/** The card the mockups show, over a transparent native <select>: keyboard, screen readers and
 * the phone's own picker all come for free instead of a hand-built listbox. */
export function DeviceSelector({
  devices,
  activeDevice,
  onChange,
}: {
  devices: Device[];
  activeDevice: Device;
  onChange: (deviceId: string) => void;
}) {
  const badge = deviceStatusBadge(activeDevice.status.status);
  const single = devices.length < 2;

  return (
    <div className={single ? `${styles.card} ${styles.single}` : styles.card}>
      <span className={styles.icon}>
        <Smartphone size={22} strokeWidth={1.8} aria-hidden="true" />
      </span>
      <span className={styles.text}>
        <span className={styles.name}>{activeDevice.name}</span>
        <span className={styles.meta}>{deviceSubtitle(activeDevice.platform, activeDevice.os_version)}</span>
        <StatusBadge tone={badge.tone} dot>
          {badge.label}
        </StatusBadge>
      </span>
      {!single && <ChevronDown size={18} className={styles.chevron} aria-hidden="true" />}
      <select
        className={styles.select}
        aria-label="Dispositivo"
        value={activeDevice.id}
        disabled={single}
        onChange={(event) => onChange(event.target.value)}
      >
        {devices.map((device) => (
          <option key={device.id} value={device.id}>
            {device.name}
          </option>
        ))}
      </select>
    </div>
  );
}
