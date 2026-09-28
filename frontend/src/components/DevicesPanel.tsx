"use client";

import { CalendarClock, Globe, Pencil, RefreshCw, Smartphone, Trash2, Wifi } from "lucide-react";
import { useCallback, useState } from "react";

import { deviceStatusBadge, deviceSubtitle } from "@/components/shell/deviceStatus";
import { Button, Card, CardHeader, ConfirmDialog, EmptyState, Input, StatusBadge } from "@/components/ui";
import { ApiError, type Device, renameDevice, unlinkDevice } from "@/lib/apiClient";

import styles from "./DevicesPanel.module.css";

export type DevicesState =
  | { kind: "loading" }
  | { kind: "loaded"; devices: Device[] }
  | { kind: "error"; message: string };

function formatLastSeen(value: string | null): string {
  if (!value) {
    return "Nunca";
  }
  return new Date(value).toLocaleString("es-CO", { dateStyle: "medium", timeStyle: "short" });
}

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/** Sprint 24: device management (list, rename, unlink) — the master-detail split (Sprint 33) is
 * purely a local UI choice: `selectedId` only decides what the detail card on the right shows,
 * unrelated to DashboardShell's own "active device" used by the per-device sections.
 */
export function DevicesPanel({
  accessToken,
  state,
  reload,
}: {
  accessToken: string;
  state: DevicesState;
  reload: () => void;
}) {
  const devices = state.kind === "loaded" ? state.devices : [];
  const [selectedId, setSelectedId] = useState<string | null>(null);
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

  if (state.kind === "error") {
    return (
      <Card>
        <p className={styles.error}>{state.message}</p>
        <Button onClick={reload}>Reintentar</Button>
      </Card>
    );
  }

  if (state.kind === "loaded" && devices.length === 0) {
    return (
      <Card>
        <EmptyState
          icon={Smartphone}
          title="Todavía no hay dispositivos vinculados"
          description="Genera un código de 6 dígitos desde Vinculación e ingrésalo en la app de NetProtect del dispositivo que quieres supervisar."
        />
      </Card>
    );
  }

  return (
    <div className={styles.columns}>
      <Card padding="none">
        <div className={styles.listHead}>
          <CardHeader
            icon={Smartphone}
            title={`Dispositivos vinculados (${devices.length})`}
            actions={
              <Button size="sm" icon={RefreshCw} onClick={reload}>
                Actualizar
              </Button>
            }
          />
        </div>
        {state.kind === "loading" ? (
          <p className={styles.loading}>Cargando dispositivos…</p>
        ) : (
          <ul className={styles.list}>
            {devices.map((device) => {
              const badge = deviceStatusBadge(device.status.status);
              const active = selected?.id === device.id;
              return (
                <li key={device.id}>
                  <button
                    type="button"
                    className={active ? `${styles.row} ${styles.rowActive}` : styles.row}
                    onClick={() => selectDevice(device.id)}
                  >
                    <span className={styles.rowIcon}>
                      <Smartphone size={18} strokeWidth={1.8} aria-hidden="true" />
                    </span>
                    <span className={styles.rowText}>
                      <span className={styles.rowName}>{device.name}</span>
                      <span className={styles.rowMeta}>Visto: {formatLastSeen(device.status.last_seen_at)}</span>
                    </span>
                    <StatusBadge tone={badge.tone} dot>
                      {badge.label}
                    </StatusBadge>
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </Card>

      {selected && (
        <Card>
          <div className={styles.detailHead}>
            <span className={styles.detailIcon}>
              <Smartphone size={26} strokeWidth={1.8} aria-hidden="true" />
            </span>
            <div className={styles.detailTitle}>
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
                  <Button size="sm" onClick={() => setRenaming(false)} disabled={busy}>
                    Cancelar
                  </Button>
                </div>
              ) : (
                <>
                  <strong>{selected.name}</strong>
                  <span className={styles.detailSubtitle}>
                    {deviceSubtitle(selected.platform, selected.os_version)}
                  </span>
                </>
              )}
            </div>
            {!renaming && (
              <div className={styles.detailActions}>
                <Button size="sm" icon={Pencil} onClick={startRenaming}>
                  Renombrar
                </Button>
                <Button size="sm" variant="danger" icon={Trash2} onClick={() => setConfirmingUnlink(true)}>
                  Desvincular
                </Button>
              </div>
            )}
          </div>

          {actionError && <p className={styles.error}>{actionError}</p>}

          <div className={styles.infoGrid}>
            <div className={styles.infoItem}>
              <Wifi size={18} className={styles.infoIcon} aria-hidden="true" />
              <div>
                <span className={styles.infoLabel}>Estado</span>
                <StatusBadge tone={deviceStatusBadge(selected.status.status).tone} dot>
                  {deviceStatusBadge(selected.status.status).label}
                </StatusBadge>
              </div>
            </div>
            <div className={styles.infoItem}>
              <CalendarClock size={18} className={styles.infoIcon} aria-hidden="true" />
              <div>
                <span className={styles.infoLabel}>Última vez visto</span>
                <span className={styles.infoValue}>{formatLastSeen(selected.status.last_seen_at)}</span>
              </div>
            </div>
            <div className={styles.infoItem}>
              <Globe size={18} className={styles.infoIcon} aria-hidden="true" />
              <div>
                <span className={styles.infoLabel}>Huso horario</span>
                <span className={styles.infoValue}>{selected.timezone ?? "Sin reportar"}</span>
              </div>
            </div>
            <div className={styles.infoItem}>
              <Smartphone size={18} className={styles.infoIcon} aria-hidden="true" />
              <div>
                <span className={styles.infoLabel}>Vinculado</span>
                <span className={styles.infoValue}>
                  {new Date(selected.linked_at).toLocaleDateString("es-CO", { dateStyle: "medium" })}
                </span>
              </div>
            </div>
          </div>
        </Card>
      )}

      <ConfirmDialog
        open={confirmingUnlink}
        title={`¿Desvincular ${selected?.name ?? "este dispositivo"}?`}
        description="Dejará de aplicar tus reglas y de reportar su estado. Tendrás que generar un código nuevo para volver a vincularlo."
        confirmLabel="Desvincular"
        busy={busy}
        onConfirm={handleUnlink}
        onCancel={() => setConfirmingUnlink(false)}
      />
    </div>
  );
}
