import { Info, OctagonAlert, Siren, TriangleAlert, type LucideIcon } from "lucide-react";

import type { Tone } from "@/components/ui";
import type { Alert, AlertLevel } from "@/lib/apiClient";

export const ALERT_LEVEL_LABEL: Record<AlertLevel, string> = {
  INFO: "Info",
  WARNING: "Advertencia",
  HIGH: "Alta",
  CRITICAL: "Crítica",
};

export const ALERT_LEVEL_TONE: Record<AlertLevel, Tone> = {
  INFO: "info",
  WARNING: "warning",
  HIGH: "danger",
  CRITICAL: "purple",
};

export const ALERT_LEVEL_ICON: Record<AlertLevel, LucideIcon> = {
  INFO: Info,
  WARNING: TriangleAlert,
  HIGH: OctagonAlert,
  CRITICAL: Siren,
};

export function alertLabel(alert: Alert): string {
  switch (alert.alert_type) {
    case "APP_BLOCKED":
      return `Se bloqueó ${alert.package_name}`;
    case "APP_LIMIT_REACHED":
      return `Se alcanzó el límite de tiempo de ${alert.package_name}`;
    case "GEOFENCE_EXIT":
      return `Salió de ${alert.geofence_name}`;
    case "GEOFENCE_ENTER":
      return `Entró a ${alert.geofence_name}`;
    // Sprint 20 — señales de manipulación. Sin package_name ni geofence_name: describen el
    // estado del dispositivo, no una app ni una zona concreta.
    case "PERMISSION_REVOKED":
      return "El permiso de acceso a uso de apps no está activo: el dispositivo no puede aplicar reglas";
    case "SERVICE_INACTIVE":
      return "El servicio de control de apps no está en ejecución en el dispositivo";
    case "HEARTBEAT_SILENCE":
      return "El dispositivo dejó de reportarse durante un periodo anormalmente largo";
    case "CLOCK_TAMPERING":
      return "La hora del dispositivo no coincide con la del servidor";
    case "UNINSTALL_ATTEMPT":
      return "Se intentó desactivar la protección contra desinstalación";
  }
}

const TAMPER_SIGNAL_LABEL: Record<string, string> = {
  PERMISSION_REVOKED: "Permiso de acceso a uso revocado",
  SERVICE_INACTIVE: "Servicio de control de apps detenido",
  HEARTBEAT_SILENCE: "Silencio anómalo del dispositivo",
  CLOCK_TAMPERING: "Hora del dispositivo desfasada",
  UNINSTALL_ATTEMPT: "Intento de desinstalación",
};

/** AlertSilence only carries `dedup_key` (see backend/app/services/alerts.py: `"{alert_type}:
 * {package_name}"`, `"{alert_type}:{geofence_id}"`, or a bare tamper `alert_type`) — never the
 * name a tutor actually recognizes. `geofenceNameById` resolves the second case using the
 * device's own geofence list, already fetched for the Geocercas panel; a name this page can't
 * resolve (a deleted geofence) falls back to its id instead of guessing. */
export function silenceLabel(dedupKey: string, geofenceNameById: Record<string, string>): string {
  const [alertType, discriminator] = dedupKey.split(":");
  if (!discriminator) {
    return TAMPER_SIGNAL_LABEL[alertType] ?? dedupKey;
  }
  if (alertType === "APP_BLOCKED" || alertType === "APP_LIMIT_REACHED") {
    return alertType === "APP_BLOCKED" ? `Bloqueos de ${discriminator}` : `Límite de tiempo de ${discriminator}`;
  }
  if (alertType === "GEOFENCE_ENTER" || alertType === "GEOFENCE_EXIT") {
    const zoneName = geofenceNameById[discriminator] ?? discriminator;
    return alertType === "GEOFENCE_ENTER" ? `Entradas a ${zoneName}` : `Salidas de ${zoneName}`;
  }
  return dedupKey;
}
