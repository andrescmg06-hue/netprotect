import type { Tone } from "@/components/ui";

/** The backend's computed device status (Sprint 6, ALERT since Sprint 20) in the tutor's words. */
export function deviceStatusBadge(status: string): { tone: Tone; label: string } {
  switch (status) {
    case "ONLINE":
      return { tone: "success", label: "En línea" };
    case "OFFLINE":
      return { tone: "neutral", label: "Desconectado" };
    case "ALERT":
      return { tone: "danger", label: "Alerta" };
    default:
      return { tone: "neutral", label: status.charAt(0) + status.slice(1).toLowerCase() };
  }
}

export function deviceSubtitle(platform: string, osVersion: string | null): string {
  const name = platform === "ANDROID" ? "Android" : platform.charAt(0) + platform.slice(1).toLowerCase();
  return osVersion ? `${name} ${osVersion}` : name;
}
