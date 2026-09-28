import {
  Banknote,
  Clapperboard,
  Gamepad2,
  GraduationCap,
  Hammer,
  MessageCircle,
  Newspaper,
  ShieldAlert,
  ShoppingCart,
  Tags,
  Users,
  type LucideIcon,
} from "lucide-react";

import type { Category } from "@/lib/apiClient";

/** Sprint 10's 11-category catalog (own decision, see CLAUDE.md) — shared by
 * DeviceCategoriesPanel (Sprint 10/35) and StatisticsPanel (Sprint 16/37), which both need the
 * same Spanish label and icon per category. */
export const CATEGORIES: Category[] = [
  "SOCIAL_MEDIA",
  "GAMES",
  "STREAMING",
  "EDUCATION",
  "PRODUCTIVITY",
  "COMMUNICATION",
  "NEWS",
  "SHOPPING",
  "FINANCE",
  "UTILITIES",
  "ADULT_CONTENT",
];

export const CATEGORY_LABELS: Record<Category, string> = {
  SOCIAL_MEDIA: "Redes sociales",
  GAMES: "Juegos",
  STREAMING: "Streaming",
  EDUCATION: "Educación",
  PRODUCTIVITY: "Productividad",
  COMMUNICATION: "Comunicación",
  NEWS: "Noticias",
  SHOPPING: "Compras",
  FINANCE: "Finanzas",
  UTILITIES: "Utilidades",
  ADULT_CONTENT: "Contenido para adultos",
};

export const CATEGORY_ICONS: Record<Category, LucideIcon> = {
  SOCIAL_MEDIA: Users,
  GAMES: Gamepad2,
  STREAMING: Clapperboard,
  EDUCATION: GraduationCap,
  PRODUCTIVITY: Hammer,
  COMMUNICATION: MessageCircle,
  NEWS: Newspaper,
  SHOPPING: ShoppingCart,
  FINANCE: Banknote,
  UTILITIES: Tags,
  ADULT_CONTENT: ShieldAlert,
};
