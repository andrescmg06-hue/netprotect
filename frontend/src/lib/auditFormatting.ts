/** Sprint 47: Spanish labels for the audited actions — the same texts as the Android app
 * (mobile/.../ui/format/AuditLabels.kt). Checked against every `record_audit_event` call in the
 * backend; an unknown code is shown as-is rather than hidden. The CSV export keeps the raw codes. */
const AUDIT_ACTION_LABEL: Record<string, string> = {
  LOGIN: "Inicio de sesión",
  LOGOUT: "Cierre de sesión",
  TOKEN_REFRESH: "Renovación de sesión",
  ROLE_GRANTED: "Modo asignado",
  PAIRING_CODE_GENERATED: "Código de vinculación generado",
  PAIRING_CODE_REVOKED: "Código de vinculación revocado",
  DEVICE_LINKED: "Dispositivo vinculado",
  DEVICE_UNLINKED: "Dispositivo desvinculado",
  DEVICE_RENAMED: "Dispositivo renombrado",
  DEVICE_POLICY_CHANGED: "Política del dispositivo cambiada",
  SCHOOL_MODE_CHANGED: "Horario escolar cambiado",
  APP_RULE_CREATED: "Regla de app creada",
  APP_RULE_UPDATED: "Regla de app actualizada",
  APP_RULE_DELETED: "Regla de app eliminada",
  CATEGORY_RULE_CREATED: "Regla de categoría creada",
  CATEGORY_RULE_UPDATED: "Regla de categoría actualizada",
  CATEGORY_RULE_DELETED: "Regla de categoría eliminada",
  APP_CATEGORY_ASSIGNED: "App asignada a una categoría",
  APP_CATEGORY_REASSIGNED: "App cambiada de categoría",
  APP_CATEGORY_UNASSIGNED: "App quitada de su categoría",
  GEOFENCE_CREATED: "Geocerca creada",
  GEOFENCE_UPDATED: "Geocerca actualizada",
  GEOFENCE_DELETED: "Geocerca eliminada",
  ALERT_READ: "Alerta marcada como leída",
  ALERT_SILENCED: "Alerta silenciada",
  ALERT_SILENCE_REMOVED: "Silencio de alerta quitado",
  LOCATION_VIEWED: "Ubicación consultada",
  LOCATION_HISTORY_VIEWED: "Historial de ubicación consultado",
  SCREEN_SHARE_REQUESTED: "Vista remota solicitada",
  SCREEN_SHARE_CONSENT_GRANTED: "Vista remota autorizada",
  SCREEN_SHARE_CONSENT_DENIED: "Vista remota rechazada",
  SCREEN_SHARE_STARTED: "Vista remota iniciada",
  SCREEN_SHARE_STOPPED: "Vista remota terminada",
};

/** Spanish names for the `resource_type` values the backend writes (every `record_audit_event`
 * call; `device` is rendered by the panel itself, with the device name). Like the actions, an
 * unknown type is shown as-is, and the CSV export keeps the raw value. */
const AUDIT_RESOURCE_LABEL: Record<string, string> = {
  device: "Dispositivo",
  pairing_code: "Código de vinculación",
  role: "Modo de la cuenta",
  app_rule: "Regla de app",
  category_rule: "Regla de categoría",
  app_category_assignment: "Categoría de una app",
  geofence: "Geocerca",
  alert: "Alerta",
  alert_silence: "Silencio de alerta",
};

export function auditResourceLabel(resourceType: string): string {
  return AUDIT_RESOURCE_LABEL[resourceType] ?? resourceType;
}

export function auditActionLabel(action: string): string {
  return AUDIT_ACTION_LABEL[action] ?? action;
}
