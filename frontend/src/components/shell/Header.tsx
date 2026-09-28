"use client";

import { Bell, ChevronDown, LogOut, Menu, Search, Settings, Smartphone, UserRound } from "lucide-react";
import { useEffect, useId, useMemo, useRef, useState, useSyncExternalStore } from "react";

import type { CurrentUser, Device } from "@/lib/apiClient";
import { DASHBOARD_SECTIONS, type SectionKey } from "@/lib/dashboardSections";

import { Logo } from "@/components/ui";

import { Avatar } from "./Avatar";
import styles from "./Header.module.css";

type Result =
  | { kind: "section"; key: SectionKey; label: string; hint: string }
  | { kind: "device"; id: string; label: string; hint: string };

function normalize(value: string): string {
  return value
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase();
}

const NARROW_QUERY = "(max-width: 899px)";

function subscribeNarrow(onChange: () => void) {
  const query = window.matchMedia(NARROW_QUERY);
  query.addEventListener("change", onChange);
  return () => query.removeEventListener("change", onChange);
}

/** Local navigation search: it only looks through what the panel already has loaded (sections
 * and linked devices). There is no search endpoint, so it never promises to find apps or rules. */
function SearchBox({
  devices,
  onSection,
  onDevice,
}: {
  devices: Device[];
  onSection: (key: SectionKey) => void;
  onDevice: (deviceId: string) => void;
}) {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(0);
  const listId = useId();
  // The full placeholder is cut off on a phone-width header; say less there.
  const narrow = useSyncExternalStore(
    subscribeNarrow,
    () => window.matchMedia(NARROW_QUERY).matches,
    () => false
  );

  const results = useMemo<Result[]>(() => {
    const needle = normalize(query.trim());
    if (!needle) return [];
    const sections: Result[] = DASHBOARD_SECTIONS.filter(
      (section) => normalize(section.label).includes(needle) || normalize(section.description).includes(needle)
    ).map((section) => ({ kind: "section", key: section.key, label: section.label, hint: "Sección" }));
    const matchedDevices: Result[] = devices
      .filter((device) => normalize(device.name).includes(needle))
      .map((device) => ({ kind: "device", id: device.id, label: device.name, hint: "Dispositivo" }));
    return [...matchedDevices, ...sections].slice(0, 8);
  }, [query, devices]);

  function choose(result: Result) {
    if (result.kind === "section") onSection(result.key);
    else onDevice(result.id);
    setQuery("");
    setOpen(false);
  }

  const showList = open && query.trim().length > 0;

  return (
    <div className={styles.search}>
      <Search size={18} className={styles.searchIcon} aria-hidden="true" />
      <input
        type="search"
        className={styles.searchInput}
        placeholder={narrow ? "Buscar…" : "Buscar secciones o dispositivos…"}
        aria-label="Buscar secciones o dispositivos"
        role="combobox"
        aria-expanded={showList}
        aria-controls={listId}
        aria-activedescendant={showList && results[activeIndex] ? `${listId}-${activeIndex}` : undefined}
        value={query}
        onChange={(event) => {
          setQuery(event.target.value);
          setActiveIndex(0);
          setOpen(true);
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setOpen(false)}
        onKeyDown={(event) => {
          if (event.key === "ArrowDown") {
            event.preventDefault();
            setActiveIndex((current) => Math.min(current + 1, Math.max(results.length - 1, 0)));
          } else if (event.key === "ArrowUp") {
            event.preventDefault();
            setActiveIndex((current) => Math.max(current - 1, 0));
          } else if (event.key === "Enter" && results[activeIndex]) {
            event.preventDefault();
            choose(results[activeIndex]);
          } else if (event.key === "Escape") {
            setQuery("");
            setOpen(false);
          }
        }}
      />
      {showList && (
        <ul className={styles.results} id={listId} role="listbox">
          {results.length === 0 && <li className={styles.noResults}>Sin resultados para “{query.trim()}”.</li>}
          {results.map((result, index) => (
            <li
              key={result.kind === "section" ? result.key : result.id}
              id={`${listId}-${index}`}
              role="option"
              aria-selected={index === activeIndex}
              className={index === activeIndex ? `${styles.result} ${styles.resultActive}` : styles.result}
              // mousedown, not click: it must fire before the input's blur closes the list.
              onMouseDown={(event) => {
                event.preventDefault();
                choose(result);
              }}
              onMouseEnter={() => setActiveIndex(index)}
            >
              {result.kind === "device" ? (
                <Smartphone size={16} aria-hidden="true" />
              ) : (
                (() => {
                  const Icon = DASHBOARD_SECTIONS.find((section) => section.key === result.key)!.icon;
                  return <Icon size={16} aria-hidden="true" />;
                })()
              )}
              <span className={styles.resultLabel}>{result.label}</span>
              <span className={styles.resultHint}>{result.hint}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function UserMenu({
  user,
  onOpenAccount,
  onSignOut,
}: {
  user: CurrentUser;
  onOpenAccount: () => void;
  onSignOut: () => void;
}) {
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement | null>(null);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const panelId = useId();
  const displayName = user.display_name ?? user.email;

  // Listeners only; state changes happen inside the event callbacks.
  useEffect(() => {
    if (!open) return;
    function onPointer(event: PointerEvent) {
      if (!wrapperRef.current?.contains(event.target as Node)) setOpen(false);
    }
    function onKey(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setOpen(false);
        triggerRef.current?.focus();
      }
    }
    document.addEventListener("pointerdown", onPointer);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("pointerdown", onPointer);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  return (
    <div className={styles.userMenu} ref={wrapperRef}>
      <button
        ref={triggerRef}
        type="button"
        className={styles.userButton}
        aria-label={`Cuenta de ${displayName}`}
        aria-expanded={open}
        aria-controls={panelId}
        onClick={() => setOpen((current) => !current)}
      >
        <Avatar name={displayName} src={user.avatar_url} size={36} online />
        <span className={styles.userName}>{displayName}</span>
        <ChevronDown size={16} className={styles.chevron} aria-hidden="true" />
      </button>
      {open && (
        <div className={styles.menu} id={panelId}>
          <div className={styles.menuHeader}>
            <span className={styles.menuName}>{displayName}</span>
            <span className={styles.menuEmail}>{user.email}</span>
          </div>
          <button
            type="button"
            className={styles.menuItem}
            onClick={() => {
              setOpen(false);
              onOpenAccount();
            }}
          >
            <UserRound size={16} aria-hidden="true" />
            Perfil y sesión
          </button>
          <button
            type="button"
            className={`${styles.menuItem} ${styles.menuDanger}`}
            onClick={() => {
              setOpen(false);
              onSignOut();
            }}
          >
            <LogOut size={16} aria-hidden="true" />
            Cerrar sesión
          </button>
        </div>
      )}
    </div>
  );
}

export function Header({
  user,
  devices,
  unreadAlerts,
  menuOpen,
  onOpenMenu,
  onNavigate,
  onSelectDevice,
  onOpenAlerts,
  onSignOut,
}: {
  user: CurrentUser;
  devices: Device[];
  unreadAlerts: number;
  menuOpen: boolean;
  onOpenMenu: () => void;
  onNavigate: (section: SectionKey) => void;
  onSelectDevice: (deviceId: string) => void;
  onOpenAlerts: () => void;
  onSignOut: () => void;
}) {
  const menuButtonRef = useRef<HTMLButtonElement | null>(null);
  const wasOpen = useRef(menuOpen);

  // When the phone drawer closes (button, Escape or backdrop), give focus back to what opened it.
  useEffect(() => {
    if (wasOpen.current && !menuOpen) menuButtonRef.current?.focus();
    wasOpen.current = menuOpen;
  }, [menuOpen]);

  const bellLabel =
    unreadAlerts > 0 ? `Alertas: ${unreadAlerts} sin leer` : "Alertas: ninguna sin leer";

  return (
    <header className={styles.header}>
      <button
        ref={menuButtonRef}
        type="button"
        className={`${styles.iconButton} ${styles.menuButton}`}
        onClick={onOpenMenu}
        aria-label="Abrir menú"
        aria-expanded={menuOpen}
        aria-controls="panel-sidebar"
      >
        <Menu size={20} />
      </button>
      <span className={styles.mobileMark}>
        <Logo variant="shield" height={30} />
      </span>

      <SearchBox devices={devices} onSection={onNavigate} onDevice={onSelectDevice} />

      <div className={styles.actions}>
        <button type="button" className={styles.iconButton} onClick={onOpenAlerts} aria-label={bellLabel} title={bellLabel}>
          <Bell size={20} aria-hidden="true" />
          {unreadAlerts > 0 && <span className={styles.bellDot} aria-hidden="true" />}
        </button>
        <button
          type="button"
          className={`${styles.iconButton} ${styles.settingsButton}`}
          onClick={() => onNavigate("account")}
          aria-label="Perfil y sesión"
          title="Perfil y sesión"
        >
          <Settings size={20} aria-hidden="true" />
        </button>
        <span className={styles.divider} aria-hidden="true" />
        <UserMenu user={user} onOpenAccount={() => onNavigate("account")} onSignOut={onSignOut} />
      </div>
    </header>
  );
}
