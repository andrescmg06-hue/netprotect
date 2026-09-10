"use client";

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
import type { CurrentUser } from "@/lib/apiClient";
import { ApiError, ensureTutorRole, listDevices } from "@/lib/apiClient";
import {
  DASHBOARD_SECTIONS,
  SECTION_GROUPS,
  type SectionKey,
  isSectionKey,
  sectionDefinition,
} from "@/lib/dashboardSections";

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
  // The user's explicit device pick (from the header switcher, a deep link, or navigate()).
  // The *effective* active device (falling back to the first device once the list loads) is
  // derived during render below instead of synchronised back into state by an effect.
  const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(initialHash.device);

  // The hash is also written to (see writeHash below) whenever navigation happens through the
  // sidebar/switcher, so this only ever fires for *external* hash changes: the browser's
  // back/forward buttons, or a link pasted into an already-open tab. setState only happens
  // inside the "hashchange" listener — a real async browser callback, not synchronously in the
  // effect body — so this doesn't trip react-hooks/set-state-in-effect.
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

  // Single devices fetch for the whole dashboard: the header's device switcher, OverviewPanel
  // and DevicesPanel all read the same state instead of each fetching their own copy.
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

  const devices = devicesState.kind === "loaded" ? devicesState.devices : [];

  // Derived, not synchronised by an effect: falls back to the first device whenever the
  // explicit selection is empty or no longer exists (e.g. after unlinking it), and updates the
  // instant `devices` changes — no cascading render from a setState-in-effect.
  const activeDeviceId =
    selectedDeviceId && devices.some((device) => device.id === selectedDeviceId)
      ? selectedDeviceId
      : (devices[0]?.id ?? null);

  useEffect(() => {
    writeHash(activeSection, activeDeviceId);
  }, [activeSection, activeDeviceId]);

  const navigate = useCallback((section: SectionKey, deviceId?: string) => {
    setActiveSection(section);
    if (deviceId) {
      setSelectedDeviceId(deviceId);
    }
  }, []);

  const activeDefinition = sectionDefinition(activeSection);
  const activeDevice = devices.find((device) => device.id === activeDeviceId) ?? null;

  return (
    <div className="dashboard">
      <nav className="sidebar" aria-label="Secciones del panel">
        {SECTION_GROUPS.map((group) => (
          <div className="sidebarGroup" key={group}>
            <span className="sidebarGroupLabel">{group}</span>
            {DASHBOARD_SECTIONS.filter((section) => section.group === group).map((section) => (
              <button
                key={section.key}
                type="button"
                className={
                  section.key === activeSection ? "sidebarButton sidebarButtonActive" : "sidebarButton"
                }
                onClick={() => navigate(section.key)}
              >
                {section.label}
              </button>
            ))}
          </div>
        ))}
      </nav>

      <main className="dashboardMain">
        <div className="dashboardHeader">
          <h2 className="dashboardTitle">{activeDefinition.label}</h2>
          {activeDefinition.perDevice && (
            <label className="deviceSwitcher">
              Dispositivo:
              <select
                value={activeDeviceId ?? ""}
                onChange={(event) => setSelectedDeviceId(event.target.value || null)}
                disabled={devices.length === 0}
              >
                {devices.length === 0 && <option value="">Sin dispositivos</option>}
                {devices.map((device) => (
                  <option key={device.id} value={device.id}>
                    {device.name}
                  </option>
                ))}
              </select>
            </label>
          )}
        </div>

        {activeDefinition.perDevice && devicesState.kind === "loading" && (
          <p className="statusText" style={{ textAlign: "left" }}>
            Cargando dispositivos…
          </p>
        )}
        {activeDefinition.perDevice && devicesState.kind === "error" && (
          <p className="authError">{devicesState.message}</p>
        )}
        {activeDefinition.perDevice && devicesState.kind === "loaded" && devices.length === 0 && (
          <p className="statusText" style={{ textAlign: "left" }}>
            Todavía no hay dispositivos vinculados. Ve a la sección &ldquo;Vinculación&rdquo; para
            generar un código.
          </p>
        )}

        {activeSection === "account" && <AccountPanel user={user} onSignOut={onSignOut} />}

        {activeSection === "overview" && (
          <OverviewPanel devices={devices} onOpenAlerts={(deviceId) => navigate("alerts", deviceId)} />
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
  );
}
