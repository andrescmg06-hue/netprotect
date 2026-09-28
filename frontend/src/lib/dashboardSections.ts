import {
  Bell,
  BellOff,
  CalendarClock,
  ChartColumn,
  FileText,
  History,
  House,
  LayoutGrid,
  Link2,
  LocateFixed,
  type LucideIcon,
  MapPin,
  MonitorSmartphone,
  Shapes,
  ShieldCheck,
  Smartphone,
  UserRound,
} from "lucide-react";

/** Sprint 24 — Panel web completo; Sprint 32 — regrouped to the redesign's navigation.
 *
 * The original 48-section brief that names "16 secciones del dashboard" is out of this repo
 * (see CLAUDE.md); the catalogue below is this project's own decision about what those 16
 * sections are, same standing as the 11 categories chosen in Sprint 10. It maps 1:1 onto
 * functionality that already exists in the backend (Sprints 3-23). Keys are unchanged since
 * Sprint 24 so existing deep links (#section=…) keep working; only labels, grouping and the
 * per-section description/icon are new. Descriptions state real behaviour, including delays
 * (location every ~15 min) — never a capability the backend doesn't have.
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

/** null = rendered above every group without a heading (only "Inicio"). */
export type SectionGroup = "Cuenta" | "Dispositivos" | "Monitoreo" | "Auditoría" | null;

export type SectionDefinition = {
  key: SectionKey;
  label: string;
  group: SectionGroup;
  icon: LucideIcon;
  description: string;
  /** Whether this section needs an active device selected to render anything useful. */
  perDevice: boolean;
};

export const DASHBOARD_SECTIONS: SectionDefinition[] = [
  {
    key: "overview",
    label: "Inicio",
    group: null,
    icon: House,
    description: "El estado de tus dispositivos y lo que necesita tu atención.",
    perDevice: false,
  },
  {
    key: "account",
    label: "Perfil y sesión",
    group: "Cuenta",
    icon: UserRound,
    description: "Tu cuenta de Google y la sesión abierta en este navegador.",
    perDevice: false,
  },
  {
    key: "devices",
    label: "Dispositivos",
    group: "Dispositivos",
    icon: Smartphone,
    description: "Los dispositivos vinculados a tu cuenta: estado, nombre y desvinculación.",
    perDevice: false,
  },
  {
    key: "pairing",
    label: "Vinculación",
    group: "Dispositivos",
    icon: Link2,
    description: "Genera un código de 6 dígitos para vincular un nuevo dispositivo.",
    perDevice: false,
  },
  {
    key: "apps",
    label: "Apps del dispositivo",
    group: "Dispositivos",
    icon: LayoutGrid,
    description: "Las aplicaciones instaladas en el dispositivo y su tiempo de uso.",
    perDevice: true,
  },
  {
    key: "rules",
    label: "Reglas por aplicación",
    group: "Dispositivos",
    icon: ShieldCheck,
    description: "Bloquea, permite o limita el uso de aplicaciones concretas.",
    perDevice: true,
  },
  {
    key: "policy",
    label: "Política y horario escolar",
    group: "Dispositivos",
    icon: CalendarClock,
    description: "Qué pasa con las apps sin regla y en qué horario se aplica el modo escolar.",
    perDevice: true,
  },
  {
    key: "categories",
    label: "Categorías",
    group: "Dispositivos",
    icon: Shapes,
    description: "Agrupa aplicaciones por categoría y aplica una regla a toda la categoría.",
    perDevice: true,
  },
  {
    key: "geofences",
    label: "Geocercas",
    group: "Dispositivos",
    icon: MapPin,
    description: "Zonas que avisan al entrar o salir. La detección es aproximada, cada ~15 minutos.",
    perDevice: true,
  },
  {
    key: "location",
    label: "Ubicación",
    group: "Monitoreo",
    icon: LocateFixed,
    description: "Última ubicación aproximada que reportó el dispositivo (cada ~15 minutos).",
    perDevice: true,
  },
  {
    key: "history",
    label: "Historial",
    group: "Monitoreo",
    icon: History,
    description: "Bloqueos y entradas o salidas de zonas, del más reciente al más antiguo.",
    perDevice: true,
  },
  {
    key: "statistics",
    label: "Estadísticas",
    group: "Monitoreo",
    icon: ChartColumn,
    description: "Uso de apps, bloqueos y cumplimiento de límites por periodo.",
    perDevice: true,
  },
  {
    key: "alerts",
    label: "Alertas",
    group: "Monitoreo",
    icon: Bell,
    description: "Avisos del dispositivo que requieren tu atención.",
    perDevice: true,
  },
  {
    key: "silenced",
    label: "Silenciadas",
    group: "Monitoreo",
    icon: BellOff,
    description: "Alertas que silenciaste y hasta cuándo dejan de avisar.",
    perDevice: true,
  },
  {
    key: "remote",
    label: "Vista remota",
    group: "Monitoreo",
    icon: MonitorSmartphone,
    description: "Ve la pantalla del dispositivo en vivo, solo si la persona supervisada lo acepta.",
    perDevice: true,
  },
  {
    key: "audit",
    label: "Auditoría",
    group: "Auditoría",
    icon: FileText,
    description: "El registro de las acciones que hiciste en el panel.",
    perDevice: false,
  },
];

export const SECTION_GROUPS: SectionGroup[] = [null, "Cuenta", "Dispositivos", "Monitoreo", "Auditoría"];

const SECTION_KEYS = new Set<string>(DASHBOARD_SECTIONS.map((section) => section.key));

export function isSectionKey(value: string | null): value is SectionKey {
  return value !== null && SECTION_KEYS.has(value);
}

export function sectionDefinition(key: SectionKey): SectionDefinition {
  // DASHBOARD_SECTIONS is a compile-time constant covering every SectionKey, so this is safe.
  return DASHBOARD_SECTIONS.find((section) => section.key === key)!;
}
