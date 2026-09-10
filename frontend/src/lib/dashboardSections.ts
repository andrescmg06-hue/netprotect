/** Sprint 24 — Panel web completo.
 *
 * The original 48-section brief that names "16 secciones del dashboard" is out of this repo
 * (see CLAUDE.md); the catalogue below is this project's own decision about what those 16
 * sections are, same standing as the 11 categories chosen in Sprint 10. It maps 1:1 onto
 * functionality that already exists in the backend (Sprints 3-23) — nothing here adds a new
 * endpoint except "pairing" (POST/DELETE /pairing/codes, which existed since Sprint 5 but was
 * Android-only until now). Two pairs ("rules"/"policy" and "alerts"/"silenced") used to be a
 * single component each; they were split so the dashboard has one screen per real user task
 * instead of two tasks glued into one panel — see DevicePolicyPanel/AppRulesPanel and
 * AlertsPanel's `view` prop.
 */
export type SectionKey =
  | "account"
  | "overview"
  | "devices"
  | "pairing"
  | "apps"
  | "rules"
  | "policy"
  | "categories"
  | "location"
  | "geofences"
  | "history"
  | "statistics"
  | "alerts"
  | "silenced"
  | "remote"
  | "audit";

export type SectionGroup = "Cuenta" | "Dispositivos" | "Control" | "Contexto" | "Seguridad";

export type SectionDefinition = {
  key: SectionKey;
  label: string;
  group: SectionGroup;
  /** Whether this section needs an active device selected to render anything useful. */
  perDevice: boolean;
};

export const DASHBOARD_SECTIONS: SectionDefinition[] = [
  { key: "account", label: "Perfil y sesión", group: "Cuenta", perDevice: false },
  { key: "overview", label: "Resumen", group: "Dispositivos", perDevice: false },
  { key: "devices", label: "Dispositivos", group: "Dispositivos", perDevice: false },
  { key: "pairing", label: "Vinculación", group: "Dispositivos", perDevice: false },
  { key: "apps", label: "Aplicaciones instaladas", group: "Control", perDevice: true },
  { key: "rules", label: "Reglas por aplicación", group: "Control", perDevice: true },
  { key: "policy", label: "Política y horario escolar", group: "Control", perDevice: true },
  { key: "categories", label: "Categorías", group: "Control", perDevice: true },
  { key: "location", label: "Ubicación", group: "Contexto", perDevice: true },
  { key: "geofences", label: "Geocercas", group: "Contexto", perDevice: true },
  { key: "history", label: "Historial", group: "Contexto", perDevice: true },
  { key: "statistics", label: "Estadísticas", group: "Contexto", perDevice: true },
  { key: "alerts", label: "Alertas", group: "Seguridad", perDevice: true },
  { key: "silenced", label: "Silenciadas", group: "Seguridad", perDevice: true },
  { key: "remote", label: "Vista remota", group: "Seguridad", perDevice: true },
  { key: "audit", label: "Auditoría", group: "Seguridad", perDevice: false },
];

export const SECTION_GROUPS: SectionGroup[] = ["Cuenta", "Dispositivos", "Control", "Contexto", "Seguridad"];

const SECTION_KEYS = new Set<string>(DASHBOARD_SECTIONS.map((section) => section.key));

export function isSectionKey(value: string | null): value is SectionKey {
  return value !== null && SECTION_KEYS.has(value);
}

export function sectionDefinition(key: SectionKey): SectionDefinition {
  // DASHBOARD_SECTIONS is a compile-time constant covering every SectionKey, so this is safe.
  return DASHBOARD_SECTIONS.find((section) => section.key === key)!;
}
