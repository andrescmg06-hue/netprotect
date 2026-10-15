"use client";

import {
  ArrowRight,
  History,
  LayoutGrid,
  Link2,
  LocateFixed,
  type LucideIcon,
  Pencil,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  Smartphone,
  Trash2,
} from "lucide-react";
import { useCallback, useState } from "react";

import { deviceStatusBadge, deviceSubtitle } from "@/components/shell/deviceStatus";
import {
  Button,
  Card,
  ConfirmDialog,
  Input,
  LoadState,
  type LoadStatus,
  ReadOnlyField,
  ReadOnlyFieldList,
  StatusBadge,
} from "@/components/ui";
import { type Device, renameDevice, unlinkDevice } from "@/lib/apiClient";
import type { SectionKey } from "@/lib/dashboardSections";
import { describeError } from "@/lib/errors";
import { formatDate, formatMoment } from "@/lib/format";

import styles from "./DevicesPanel.module.css";

export type DevicesState =
  | { kind: "loading" }
  | { kind: "loaded"; devices: Device[] }
  | { kind: "error"; message: string };

/** In place of the mockup's tabs (there is no API behind them): the per-device sections that
 * already exist, opened on this device. Labels never include the device name, so the list row
 * stays the only button named after it (frontend/e2e/dashboard.spec.ts). */
const DEVICE_SECTIONS: { section: SectionKey; label: string; icon: LucideIcon }[] = [
  { section: "apps", label: "Apps del dispositivo", icon: LayoutGrid },
  { section: "rules", label: "Reglas por aplicación", icon: ShieldCheck },
  { section: "location", label: "Ubicación", icon: LocateFixed },
  { section: "history", label: "Historial", icon: History },
];

type Security = { tone: "success" | "danger" | "neutral"; title: string; detail: string };

/** The security state is derived from the backend's computed status, nothing else: ALERT is set by
 * the Sprint 20 tamper signals and only a healthy heartbeat clears it; heartbeat age never
 * downgrades it to OFFLINE (backend/app/services/device_status.py), so an OFFLINE device's last
 * report carried no tamper signal. */
function securityState(device: Device): Security {
  switch (device.status.status) {
    case "ALERT":
      return {
        tone: "danger",
        title: "Requiere atención",
        detail:
          "El dispositivo reportó una posible manipulación: permiso de uso revocado, servicio de reglas detenido, hora desfasada, un silencio anómalo o un intento de desinstalación. Sus alertas dicen cuál.",
      };
    case "ONLINE":
      return {
        tone: "success",
        title: "Sin señales de manipulación",
        detail: "Su último reporte no trae ninguna señal de manipulación.",
      };
    case "OFFLINE":
      return device.status.last_seen_at
        ? {
            tone: "neutral",
            title: "Sin conexión reciente",
            detail:
              "Su último reporte no traía señales de manipulación. Mientras no vuelva a conectarse no hay datos nuevos.",
          }
        : {
            tone: "neutral",
            title: "Sin reportes todavía",
            detail: "El dispositivo aún no ha enviado su primer reporte.",
          };
    default:
      return {
        tone: "neutral",
        title: deviceStatusBadge(device.status.status).label,
        detail: "No hay información de seguridad adicional para este estado.",
      };
  }
}

/** Sprint 24: device management (list, rename, unlink). Sprint 33 split it master-detail;
 * `selectedId` is local to this panel, seeded from the shell's active device so a link from
 * Inicio ("Ver el dispositivo") opens on that device.
 *
 * Sprint 55 recomposes it in the editorial direction: the list as quiet rows on the page (the
 * selected one marked by the screen's one blue rule), the detail as the one open panel: identity,
 * the key facts, the security state derived from ALERT, links to the per-device sections and,
 * last and apart, unlinking. The mockup's photo, device type, "−18 % vs. ayer", "+2 nuevas" and
 * tabs are left out: the backend has none of them (docs/redesign/fase-0-informe.md §5). */
export function DevicesPanel({
  accessToken,
  state,
  reload,
  onNavigate,
  initialSelectedId = null,
}: {
  accessToken: string;
  state: DevicesState;
  reload: () => void;
  onNavigate: (section: SectionKey, deviceId?: string) => void;
  initialSelectedId?: string | null;
}) {
  const devices = state.kind === "loaded" ? state.devices : [];
  const [selectedId, setSelectedId] = useState<string | null>(initialSelectedId);
  const selected = devices.find((device) => device.id === selectedId) ?? devices[0] ?? null;

  const [renaming, setRenaming] = useState(false);
  const [renameValue, setRenameValue] = useState("");
  const [confirmingUnlink, setConfirmingUnlink] = useState(false);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  // Selecting a device cancels whatever the previous one's card was mid-edit; a plain assignment
  // in an event handler (onClick below), not something an effect needs to synchronise.
  const selectDevice = useCallback((deviceId: string) => {
    setSelectedId(deviceId);
    setRenaming(false);
    setActionError(null);
  }, []);

  const startRenaming = useCallback(() => {
    if (!selected) return;
    setRenameValue(selected.name);
    setRenaming(true);
  }, [selected]);

  const handleRename = useCallback(() => {
    if (!selected) return;
    setActionError(null);
    setBusy(true);
    renameDevice(accessToken, selected.id, renameValue)
      .then(() => {
        setBusy(false);
        setRenaming(false);
        reload();
      })
      .catch((error) => {
        setBusy(false);
        setActionError(describeError(error, "No se pudo renombrar el dispositivo"));
      });
  }, [accessToken, selected, renameValue, reload]);

  const handleUnlink = useCallback(() => {
    if (!selected) return;
    setActionError(null);
    setBusy(true);
    unlinkDevice(accessToken, selected.id)
      .then(() => {
        setBusy(false);
        setConfirmingUnlink(false);
        setSelectedId(null);
        reload();
      })
      .catch((error) => {
        setBusy(false);
        setConfirmingUnlink(false);
        setActionError(describeError(error, "No se pudo desvincular el dispositivo"));
      });
  }, [accessToken, selected, reload]);

  const status: LoadStatus =
    state.kind === "loading" ? "loading" : state.kind === "error" ? "error" : devices.length === 0 ? "empty" : "ready";

  if (status !== "ready" || !selected) {
    return (
      <Card>
        <LoadState
          status={status}
          loadingLabel="Cargando dispositivos…"
          error={state.kind === "error" ? state.message : undefined}
          onRetry={reload}
          empty={{
            icon: Smartphone,
            title: "Todavía no hay dispositivos vinculados",
            description:
              "Genera un código de 6 dígitos desde Vinculación e ingrésalo en la app de NetProtect del dispositivo que quieres supervisar.",
            action: (
              <Button variant="primary" icon={Link2} onClick={() => onNavigate("pairing")}>
                Ir a Vinculación
              </Button>
            ),
          }}
        />
      </Card>
    );
  }

  const selectedBadge = deviceStatusBadge(selected.status.status);
  const security = securityState(selected);

  return (
    <div className={styles.layout}>
      <div className={styles.columns}>
        <section className={styles.listColumn} aria-labelledby="devices-list-title">
          <header className={styles.listHead}>
            <h2 id="devices-list-title" className={styles.listTitle}>
              Vinculados <span className={styles.count}>{devices.length}</span>
            </h2>
            <Button size="sm" variant="ghost" icon={RefreshCw} onClick={reload}>
              Actualizar
            </Button>
          </header>

          <ul className={styles.list}>
            {devices.map((device) => {
              const badge = deviceStatusBadge(device.status.status);
              const active = selected.id === device.id;
              return (
                <li key={device.id}>
                  <button
                    type="button"
                    className={active ? `${styles.row} ${styles.rowActive}` : styles.row}
                    aria-current={active ? "true" : undefined}
                    onClick={() => selectDevice(device.id)}
                  >
                    <span className={styles.rowText}>
                      <span className={styles.rowName}>{device.name}</span>
                      <span className={styles.rowMeta}>
                        {deviceSubtitle(device.platform, device.os_version)} · Visto:{" "}
                        {formatMoment(device.status.last_seen_at, { inline: true })}
                      </span>
                    </span>
                    <StatusBadge tone={badge.tone} dot>
                      {badge.label}
                    </StatusBadge>
                  </button>
                </li>
              );
            })}
          </ul>

          <button type="button" className={styles.textLink} onClick={() => onNavigate("pairing")}>
            <Link2 size={14} strokeWidth={2} aria-hidden="true" />
            Vincular otro dispositivo
          </button>
        </section>

        <Card className={styles.detail} aria-labelledby="device-detail-title">
          <header className={styles.detailHead}>
            <span className={styles.plate} aria-hidden="true">
              <Smartphone size={28} strokeWidth={1.5} />
            </span>
            <div className={styles.identity}>
              {renaming ? (
                <div className={styles.renameForm}>
                  <Input
                    autoFocus
                    value={renameValue}
                    maxLength={255}
                    aria-label="Nuevo nombre del dispositivo"
                    onChange={(event) => setRenameValue(event.target.value)}
                  />
                  <Button size="sm" variant="primary" loading={busy} onClick={handleRename}>
                    Guardar
                  </Button>
                  <Button size="sm" variant="ghost" onClick={() => setRenaming(false)} disabled={busy}>
                    Cancelar
                  </Button>
                </div>
              ) : (
                <>
                  <h2 id="device-detail-title" className={styles.detailName}>
                    {selected.name}
                  </h2>
                  <p className={styles.identityMeta}>
                    {deviceSubtitle(selected.platform, selected.os_version)}
                    {" · "}
                    {selected.app_version ? `NetProtect ${selected.app_version}` : "Versión de la app sin reportar"}
                  </p>
                </>
              )}
            </div>
            {!renaming && (
              <Button size="sm" variant="ghost" icon={Pencil} onClick={startRenaming}>
                Renombrar
              </Button>
            )}
          </header>

          {actionError && (
            <p className={styles.error} role="alert">
              {actionError}
            </p>
          )}

          <ReadOnlyFieldList columns={4}>
            <ReadOnlyField
              label="Estado"
              value={
                <StatusBadge tone={selectedBadge.tone} dot>
                  {selectedBadge.label}
                </StatusBadge>
              }
            />
            <ReadOnlyField label="Última vez visto" value={formatMoment(selected.status.last_seen_at)} />
            <ReadOnlyField label="Vinculado" value={formatDate(selected.linked_at)} />
            <ReadOnlyField label="Zona horaria" value={selected.timezone} />
          </ReadOnlyFieldList>

          <section className={styles.block} aria-labelledby="device-security-title">
            <h3 id="device-security-title" className={styles.blockTitle}>
              Estado de seguridad
            </h3>
            <div className={`${styles.security} ${styles[security.tone]}`}>
              {security.tone === "danger" ? (
                <ShieldAlert size={20} strokeWidth={1.75} className={styles.securityIcon} aria-hidden="true" />
              ) : (
                <ShieldCheck size={20} strokeWidth={1.75} className={styles.securityIcon} aria-hidden="true" />
              )}
              <div className={styles.securityText}>
                <p className={styles.securityTitle}>{security.title}</p>
                <p className={styles.securityDetail}>{security.detail}</p>
                {security.tone === "danger" && (
                  <button
                    type="button"
                    className={`${styles.textLink} ${styles.textLinkAlert}`}
                    onClick={() => onNavigate("alerts", selected.id)}
                  >
                    Ver sus alertas
                    <ArrowRight size={14} strokeWidth={2} className={styles.arrow} aria-hidden="true" />
                  </button>
                )}
              </div>
            </div>
          </section>

          <section className={styles.block} aria-labelledby="device-sections-title">
            <h3 id="device-sections-title" className={styles.blockTitle}>
              En este dispositivo
            </h3>
            <ul className={styles.sectionLinks}>
              {DEVICE_SECTIONS.map((entry) => {
                const Icon = entry.icon;
                return (
                  <li key={entry.section}>
                    <button
                      type="button"
                      className={styles.sectionLink}
                      onClick={() => onNavigate(entry.section, selected.id)}
                    >
                      <Icon size={18} strokeWidth={1.75} className={styles.sectionIcon} aria-hidden="true" />
                      <span className={styles.sectionLabel}>{entry.label}</span>
                      <ArrowRight size={16} strokeWidth={2} className={styles.sectionArrow} aria-hidden="true" />
                    </button>
                  </li>
                );
              })}
            </ul>
          </section>

          <div className={styles.unlink}>
            <div className={styles.unlinkText}>
              <p className={styles.unlinkTitle}>Desvincular este dispositivo</p>
              <p className={styles.unlinkDetail}>
                Dejará de aplicar tus reglas y de reportar su estado. Para volver a vincularlo hará falta un código
                nuevo.
              </p>
            </div>
            <Button size="sm" variant="danger" icon={Trash2} onClick={() => setConfirmingUnlink(true)}>
              Desvincular
            </Button>
          </div>
        </Card>
      </div>

      <ConfirmDialog
        open={confirmingUnlink}
        title={`¿Desvincular ${selected.name}?`}
        description="Dejará de aplicar tus reglas y de reportar su estado. Tendrás que generar un código nuevo para volver a vincularlo."
        confirmLabel="Desvincular"
        busy={busy}
        onConfirm={handleUnlink}
        onCancel={() => setConfirmingUnlink(false)}
      />
    </div>
  );
}
