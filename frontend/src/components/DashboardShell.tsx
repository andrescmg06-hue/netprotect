"use client";

import { Link2, Smartphone } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { AccountPanel } from "@/components/AccountPanel";
import { AlertsPanel } from "@/components/AlertsPanel";
import { AppRulesPanel } from "@/components/AppRulesPanel";
import { AuditPanel } from "@/components/AuditPanel";
import { DeviceApplicationsList } from "@/components/DeviceApplicationsList";
import { DeviceCategoriesPanel } from "@/components/DeviceCategoriesPanel";
import { DeviceLocationPanel } from "@/components/DeviceLocationPanel";
import { DevicePolicyPanel } from "@/components/DevicePolicyPanel";
import { DevicesPanel, type DevicesState } from "@/components/DevicesPanel";
import { GeofencePanel } from "@/components/GeofencePanel";
import { HistoryPanel } from "@/components/HistoryPanel";
import { OverviewPanel } from "@/components/OverviewPanel";
import { PairingPanel } from "@/components/PairingPanel";
import { RemoteViewPanel } from "@/components/RemoteViewPanel";
import { StatisticsPanel } from "@/components/StatisticsPanel";
import { DeviceSelector } from "@/components/shell/DeviceSelector";
import { Header } from "@/components/shell/Header";
import { Sidebar } from "@/components/shell/Sidebar";
import { Button, Card, EmptyState, PageHeader, Spinner } from "@/components/ui";
import type { CurrentUser } from "@/lib/apiClient";
import { ApiError, ensureTutorRole, listDeviceAlerts, listDevices } from "@/lib/apiClient";
import { type SectionKey, isSectionKey, sectionDefinition } from "@/lib/dashboardSections";

import styles from "./DashboardShell.module.css";

const ALERTS_REFRESH_MS = 60_000;

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/** Deep-linkable state without Next's dynamic routing: a plain URL hash
 * (#section=apps&device=<uuid>), read on mount and kept in sync with history.replaceState. This
 * avoids the Suspense-boundary requirement useSearchParams imposes on a fully client-rendered
 * page, at the cost of not being crawlable — acceptable for a tutor-only, auth-gated dashboard.
 */
function readHash(): { section: SectionKey | null; device: string | null } {
  if (typeof window === "undefined") {
    return { section: null, device: null };
  }
  const params = new URLSearchParams(window.location.hash.replace(/^#/, ""));
  const section = params.get("section");
  return { section: isSectionKey(section) ? section : null, device: params.get("device") };
}

function writeHash(section: SectionKey, deviceId: string | null) {
  const params = new URLSearchParams();
  params.set("section", section);
  if (deviceId) {
    params.set("device", deviceId);
  }
  const next = `#${params.toString()}`;
  if (window.location.hash !== next) {
    window.history.replaceState(null, "", next);
  }
}

/** Sprint 24 routing and data ownership; Sprint 32 frame (Sidebar, Header, PageHeader,
 * DeviceSelector). The panels below are unchanged and get their own redesign in Sprints 33–38. */
export function DashboardShell({
  accessToken,
  user,
  onSignOut,
}: {
  accessToken: string;
  user: CurrentUser;
  onSignOut: () => void;
}) {
  const [devicesState, setDevicesState] = useState<DevicesState>({ kind: "loading" });
  const [devicesReloadToken, setDevicesReloadToken] = useState(0);
  const initialHash = useMemo(() => readHash(), []);
  const [activeSection, setActiveSection] = useState<SectionKey>(initialHash.section ?? "overview");
  // The user's explicit device pick (from the device card, a deep link, search or navigate()).
  // The *effective* active device (falling back to the first device once the list loads) is
  // derived during render below instead of synchronised back into state by an effect.
  const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(initialHash.device);
  const [menuOpen, setMenuOpen] = useState(false);
  const [unreadByDevice, setUnreadByDevice] = useState<Record<string, number>>({});
  const [alertsTick, setAlertsTick] = useState(0);

  // Only *external* hash changes land here (back/forward, a pasted link); setState happens
  // inside the listener, never synchronously in the effect body.
  useEffect(() => {
    function handleHashChange() {
      const next = readHash();
      if (next.section) {
        setActiveSection(next.section);
      }
      setSelectedDeviceId(next.device);
    }
    window.addEventListener("hashchange", handleHashChange);
    return () => window.removeEventListener("hashchange", handleHashChange);
  }, []);

  // Single devices fetch for the whole dashboard: the device card, search, OverviewPanel and
  // DevicesPanel all read the same state instead of each fetching their own copy.
  useEffect(() => {
    let cancelled = false;

    ensureTutorRole(accessToken)
      .then(() => listDevices(accessToken))
      .then(({ devices }) => {
        if (!cancelled) {
          setDevicesState({ kind: "loaded", devices });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setDevicesState({
            kind: "error",
            message: describeError(error, "No se pudo cargar la lista de dispositivos"),
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, devicesReloadToken]);

  const reloadDevices = useCallback(() => {
    setDevicesState({ kind: "loading" });
    setDevicesReloadToken((current) => current + 1);
  }, []);

  const devices = useMemo(() => (devicesState.kind === "loaded" ? devicesState.devices : []), [devicesState]);

  // Unread alerts for the header bell and the sidebar badge: the same per-device endpoint the
  // Alertas section reads (there is no account-wide one). Refreshed every minute, and whenever the
  // tutor enters or leaves Alertas, since reading them there changes the count.
  const inAlerts = activeSection === "alerts" || activeSection === "silenced";
  useEffect(() => {
    if (devices.length === 0) return;
    let cancelled = false;
    Promise.all(
      devices.map((device) =>
        listDeviceAlerts(accessToken, device.id)
          .then(({ alerts }) => [device.id, alerts.filter((alert) => alert.read_at === null).length] as const)
          .catch(() => [device.id, 0] as const)
      )
    ).then((entries) => {
      if (!cancelled) setUnreadByDevice(Object.fromEntries(entries));
    });
    return () => {
      cancelled = true;
    };
  }, [accessToken, devices, inAlerts, alertsTick]);

  useEffect(() => {
    const timer = window.setInterval(() => setAlertsTick((current) => current + 1), ALERTS_REFRESH_MS);
    return () => window.clearInterval(timer);
  }, []);

  const unreadAlerts = Object.values(unreadByDevice).reduce((sum, count) => sum + count, 0);

  // Derived, not synchronised by an effect: falls back to the first device whenever the
  // explicit selection is empty or no longer exists (e.g. after unlinking it).
  const activeDeviceId =
    selectedDeviceId && devices.some((device) => device.id === selectedDeviceId)
      ? selectedDeviceId
      : (devices[0]?.id ?? null);

  useEffect(() => {
    writeHash(activeSection, activeDeviceId);
  }, [activeSection, activeDeviceId]);

  useEffect(() => {
    if (!menuOpen) return;
    function onKey(event: KeyboardEvent) {
      if (event.key === "Escape") setMenuOpen(false);
    }
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [menuOpen]);

  const navigate = useCallback((section: SectionKey, deviceId?: string) => {
    setActiveSection(section);
    if (deviceId) {
      setSelectedDeviceId(deviceId);
    }
    setMenuOpen(false);
    window.scrollTo({ top: 0 });
  }, []);

  const activeDefinition = sectionDefinition(activeSection);
  const activeDevice = devices.find((device) => device.id === activeDeviceId) ?? null;

  const selectDeviceFromSearch = useCallback(
    (deviceId: string) => {
      navigate(activeDefinition.perDevice ? activeSection : "apps", deviceId);
    },
    [activeDefinition.perDevice, activeSection, navigate]
  );

  const openAlerts = useCallback(() => {
    const [busiest] = Object.entries(unreadByDevice).sort(([, a], [, b]) => b - a);
    navigate("alerts", busiest && busiest[1] > 0 ? busiest[0] : (activeDeviceId ?? undefined));
  }, [unreadByDevice, activeDeviceId, navigate]);

  return (
    <div className={`dashboard ${styles.app}`}>
      <Sidebar
        activeSection={activeSection}
        onNavigate={(section) => navigate(section)}
        user={user}
        onSignOut={onSignOut}
        unreadAlerts={unreadAlerts}
        open={menuOpen}
        onClose={() => setMenuOpen(false)}
      />

      <div className={styles.column} inert={menuOpen || undefined}>
        <Header
          user={user}
          devices={devices}
          unreadAlerts={unreadAlerts}
          menuOpen={menuOpen}
          onOpenMenu={() => setMenuOpen(true)}
          onNavigate={(section) => navigate(section)}
          onSelectDevice={selectDeviceFromSearch}
          onOpenAlerts={openAlerts}
          onSignOut={onSignOut}
        />

        <main className={styles.content}>
          <PageHeader
            title={activeDefinition.label}
            description={activeDefinition.description}
            // Per-device views show the device card where other views show the breadcrumb (mockups).
            breadcrumb={activeSection === "overview" || activeDefinition.perDevice ? undefined : activeDefinition.label}
            aside={
              activeDefinition.perDevice && activeDevice ? (
                <DeviceSelector devices={devices} activeDevice={activeDevice} onChange={setSelectedDeviceId} />
              ) : undefined
            }
          />

          {activeDefinition.perDevice && devicesState.kind === "loading" && (
            <Card>
              <Spinner label="Cargando dispositivos…" />
            </Card>
          )}
          {activeDefinition.perDevice && devicesState.kind === "error" && (
            <Card>
              <p className={styles.error}>{devicesState.message}</p>
              <Button onClick={reloadDevices}>Reintentar</Button>
            </Card>
          )}
          {activeDefinition.perDevice && devicesState.kind === "loaded" && devices.length === 0 && (
            <Card>
              <EmptyState
                icon={Smartphone}
                title="Todavía no hay dispositivos vinculados"
                description="Esta sección muestra datos de un dispositivo. Vincula uno con un código de 6 dígitos para empezar."
                action={
                  <Button variant="primary" icon={Link2} onClick={() => navigate("pairing")}>
                    Ir a Vinculación
                  </Button>
                }
              />
            </Card>
          )}

          {activeSection === "account" && <AccountPanel user={user} onSignOut={onSignOut} />}

          {activeSection === "overview" && (
            <OverviewPanel
              accessToken={accessToken}
              devices={devices}
              unreadByDevice={unreadByDevice}
              onNavigate={navigate}
            />
          )}

          {activeSection === "devices" && (
            <DevicesPanel accessToken={accessToken} state={devicesState} reload={reloadDevices} />
          )}

          {activeSection === "pairing" && <PairingPanel accessToken={accessToken} />}

          {activeSection === "audit" && <AuditPanel accessToken={accessToken} />}

          {activeDefinition.perDevice && activeDevice && (
            <>
              {activeSection === "apps" && (
                <DeviceApplicationsList accessToken={accessToken} deviceId={activeDevice.id} />
              )}
              {activeSection === "rules" && <AppRulesPanel accessToken={accessToken} deviceId={activeDevice.id} />}
              {activeSection === "policy" && (
                <DevicePolicyPanel
                  accessToken={accessToken}
                  deviceId={activeDevice.id}
                  defaultAppPolicy={activeDevice.default_app_policy}
                  schoolMode={activeDevice.school_mode}
                  onPolicyChanged={reloadDevices}
                />
              )}
              {activeSection === "categories" && (
                <DeviceCategoriesPanel accessToken={accessToken} deviceId={activeDevice.id} />
              )}
              {activeSection === "location" && (
                <DeviceLocationPanel accessToken={accessToken} deviceId={activeDevice.id} />
              )}
              {activeSection === "geofences" && <GeofencePanel accessToken={accessToken} deviceId={activeDevice.id} />}
              {activeSection === "history" && <HistoryPanel accessToken={accessToken} deviceId={activeDevice.id} />}
              {activeSection === "statistics" && (
                <StatisticsPanel accessToken={accessToken} deviceId={activeDevice.id} />
              )}
              {activeSection === "alerts" && (
                <AlertsPanel accessToken={accessToken} deviceId={activeDevice.id} view="inbox" />
              )}
              {activeSection === "silenced" && (
                <AlertsPanel accessToken={accessToken} deviceId={activeDevice.id} view="silenced" />
              )}
              {activeSection === "remote" && <RemoteViewPanel accessToken={accessToken} deviceId={activeDevice.id} />}
            </>
          )}
        </main>
      </div>
    </div>
  );
}
