"use client";

import { useEffect } from "react";

import { deviceRealtimeWebSocketUrl } from "@/lib/apiClient";

/** Sprint 18's rules_changed channel, extracted out of DeviceRulesPanel in Sprint 24 so the two
 * screens that split off from it (AppRulesPanel and DevicePolicyPanel) can each stay live without
 * duplicating the socket-handling code. Only one of the two is ever mounted at a time (dashboard
 * navigation shows one section at a time), so this never opens more than one socket per device.
 * setState only ever happens inside the "message" listener — a real async browser callback, not
 * synchronously in the effect body — so this doesn't trip react-hooks/set-state-in-effect.
 */
export function useDeviceRulesRealtime(accessToken: string, deviceId: string, onRulesChanged: () => void) {
  useEffect(() => {
    let cancelled = false;
    const socket = new WebSocket(deviceRealtimeWebSocketUrl(deviceId));

    socket.addEventListener("open", () => {
      if (!cancelled) {
        socket.send(JSON.stringify({ token: accessToken }));
      }
    });

    socket.addEventListener("message", (event) => {
      if (cancelled) return;
      try {
        const payload = JSON.parse(event.data as string);
        if (payload?.event === "rules_changed") {
          onRulesChanged();
        }
      } catch {
        // Not a frame this panel understands — ignore rather than crash the socket.
      }
    });

    return () => {
      cancelled = true;
      socket.close();
    };
  }, [accessToken, deviceId, onRulesChanged]);
}
