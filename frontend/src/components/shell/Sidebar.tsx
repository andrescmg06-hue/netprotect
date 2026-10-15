"use client";

import { LogOut, X } from "lucide-react";
import { useEffect, useRef } from "react";

import { Logo } from "@/components/ui";
import type { CurrentUser } from "@/lib/apiClient";
import { DASHBOARD_SECTIONS, SECTION_GROUPS, type SectionKey } from "@/lib/dashboardSections";

import { Avatar } from "./Avatar";
import styles from "./Sidebar.module.css";

/** Three layouts from one markup: full (≥1200px), icon rail (900–1199px) and an off-canvas
 * drawer (<900px, `open` controlled by DashboardShell). Labels are only visually hidden in the
 * rail, so every button keeps its accessible name. */
export function Sidebar({
  activeSection,
  onNavigate,
  user,
  onSignOut,
  unreadAlerts,
  open,
  onClose,
}: {
  activeSection: SectionKey;
  onNavigate: (section: SectionKey) => void;
  user: CurrentUser;
  onSignOut: () => void;
  unreadAlerts: number;
  open: boolean;
  onClose: () => void;
}) {
  const displayName = user.display_name ?? user.email;
  const closeRef = useRef<HTMLButtonElement | null>(null);

  // Drawer (phone) only: move focus into it on open, since the rest of the page goes inert.
  useEffect(() => {
    if (open) closeRef.current?.focus();
  }, [open]);

  return (
    <>
      <div
        className={open ? `${styles.backdrop} ${styles.backdropVisible}` : styles.backdrop}
        onClick={onClose}
        aria-hidden="true"
      />
      <aside className={open ? `${styles.sidebar} ${styles.open}` : styles.sidebar} id="panel-sidebar">
        <div className={styles.brand}>
          <span className={styles.logoFull}>
            <Logo height={50} onDark />
          </span>
          <span className={styles.logoShield}>
            <Logo variant="shield" height={36} />
          </span>
          <button ref={closeRef} type="button" className={styles.close} onClick={onClose} aria-label="Cerrar menú">
            <X size={20} />
          </button>
        </div>

        <nav className={styles.nav} aria-label="Secciones del panel">
          {SECTION_GROUPS.map((group) => (
            <div className={styles.group} key={group ?? "root"}>
              {group && <span className={styles.groupLabel}>{group}</span>}
              {DASHBOARD_SECTIONS.filter((section) => section.group === group).map((section) => {
                const Icon = section.icon;
                const active = section.key === activeSection;
                const badge = section.key === "alerts" && unreadAlerts > 0 ? unreadAlerts : null;
                return (
                  <button
                    key={section.key}
                    type="button"
                    className={active ? `${styles.item} ${styles.active}` : styles.item}
                    aria-current={active ? "page" : undefined}
                    title={section.label}
                    onClick={() => onNavigate(section.key)}
                  >
                    <Icon size={18} strokeWidth={1.75} aria-hidden="true" className={styles.itemIcon} />
                    <span className={styles.label}>{section.label}</span>
                    {badge !== null && (
                      <>
                        <span className={styles.count} aria-hidden="true">
                          {badge > 99 ? "99+" : badge}
                        </span>
                        <span className={styles.srOnly}>, {badge} sin leer</span>
                      </>
                    )}
                  </button>
                );
              })}
            </div>
          ))}
        </nav>

        {/* S54: the account block moved to the foot of the sidebar, under the navigation, where
            the mockups keep the account out of the way of the sections. */}
        <div className={styles.profile}>
          <Avatar name={displayName} src={user.avatar_url} size={40} online />
          <div className={styles.profileText}>
            <span className={styles.profileName}>{displayName}</span>
            <span className={styles.profileEmail}>{user.email}</span>
          </div>
          <button type="button" className={styles.signOut} onClick={onSignOut} title="Cerrar sesión">
            <LogOut size={16} aria-hidden="true" />
            <span className={styles.signOutLabel}>Cerrar sesión</span>
          </button>
        </div>
      </aside>
    </>
  );
}
