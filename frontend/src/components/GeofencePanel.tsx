"use client";

import { LocateFixed, LogIn, LogOut, MapPinned, Pencil, Plus, Trash2, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { GeofenceMap } from "@/components/GeofenceMap";
import {
  Button,
  Card,
  CardHeader,
  type Column,
  ConfirmDialog,
  DataTable,
  EmptyState,
  Field,
  Input,
  MetricCard,
  MetricGrid,
  Spinner,
  StatusBadge,
} from "@/components/ui";
import {
  ApiError,
  type Geofence,
  type GeofenceEvent,
  type LocationReport,
  type UpsertGeofenceInput,
  createGeofence,
  deleteGeofence,
  getLatestLocation,
  listGeofenceEvents,
  listGeofences,
  updateGeofence,
} from "@/lib/apiClient";

import styles from "./GeofencePanel.module.css";

type GeofencesState =
  | { kind: "loading" }
  | { kind: "loaded"; geofences: Geofence[] }
  | { kind: "error"; message: string };

type LocationState = { kind: "loading" } | { kind: "loaded"; report: LocationReport | null } | { kind: "error" };

type EventsState =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "loaded"; events: GeofenceEvent[] }
  | { kind: "error"; message: string };

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function eventLabel(event: GeofenceEvent): string {
  return event.event_type === "ENTER" ? `Entró a ${event.geofence_name}` : `Salió de ${event.geofence_name}`;
}

/** Sprint 14 (logic), Sprint 36 (design). No Android/GMS Geofencing API — the backend detects
 * ENTER/EXIT from consecutive location reports (see docs/sprint-14.md), so creating a zone here
 * needs no map click handler: a numeric form plus "usar última ubicación conocida" to prefill it
 * from the same endpoint DeviceLocationPanel uses. The map above the form is a hand-made SVG
 * schematic (docs/planning/plan-frontend.md, Sprint 36) — no Leaflet/OpenStreetMap, no coordinates
 * of a minor sent to a third-party tile server. */
export function GeofencePanel({ accessToken, deviceId }: { accessToken: string; deviceId: string }) {
  const [state, setState] = useState<GeofencesState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);
  const [locationState, setLocationState] = useState<LocationState>({ kind: "loading" });
  const [eventsState, setEventsState] = useState<EventsState>({ kind: "idle" });
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [prefilling, setPrefilling] = useState(false);
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [latitude, setLatitude] = useState("");
  const [longitude, setLongitude] = useState("");
  const [radiusMeters, setRadiusMeters] = useState("300");

  useEffect(() => {
    let cancelled = false;

    listGeofences(accessToken, deviceId)
      .then(({ geofences }) => {
        if (!cancelled) {
          setState({ kind: "loaded", geofences });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setState({ kind: "error", message: describeError(error, "No se pudieron cargar las geocercas") });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, reloadToken]);

  // Fetched once for the map's "última ubicación conocida" marker — same endpoint the prefill
  // button below already calls, just read eagerly so the schematic has something to show.
  useEffect(() => {
    let cancelled = false;

    getLatestLocation(accessToken, deviceId)
      .then(({ report }) => {
        if (!cancelled) {
          setLocationState({ kind: "loaded", report });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setLocationState({ kind: "error" });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, reloadToken]);

  const reload = useCallback(() => {
    setState({ kind: "loading" });
    setReloadToken((current) => current + 1);
  }, []);

  const geofences = useMemo(() => (state.kind === "loaded" ? state.geofences : []), [state]);
  const lastLocation = locationState.kind === "loaded" ? locationState.report : null;

  function resetForm() {
    setEditingId(null);
    setName("");
    setLatitude("");
    setLongitude("");
    setRadiusMeters("300");
    setFormError(null);
  }

  function startEditing(geofence: Geofence) {
    setEditingId(geofence.id);
    setName(geofence.name);
    setLatitude(String(geofence.latitude));
    setLongitude(String(geofence.longitude));
    setRadiusMeters(String(geofence.radius_meters));
    setFormError(null);
  }

  function usePrefillFromLastKnownLocation() {
    setFormError(null);
    setPrefilling(true);
    getLatestLocation(accessToken, deviceId)
      .then(({ report }) => {
        setPrefilling(false);
        if (report === null) {
          setFormError("Este dispositivo todavía no tiene una ubicación reciente para usar como punto de partida.");
          return;
        }
        setLatitude(String(report.latitude));
        setLongitude(String(report.longitude));
      })
      .catch((error) => {
        setPrefilling(false);
        setFormError(describeError(error, "No se pudo obtener la última ubicación"));
      });
  }

  function toggleEvents() {
    if (eventsState.kind === "loaded" || eventsState.kind === "loading") {
      setEventsState({ kind: "idle" });
      return;
    }
    setEventsState({ kind: "loading" });
    listGeofenceEvents(accessToken, deviceId)
      .then(({ events }) => setEventsState({ kind: "loaded", events }))
      .catch((error) =>
        setEventsState({ kind: "error", message: describeError(error, "No se pudo cargar el historial") })
      );
  }

  function confirmDelete() {
    if (!pendingDeleteId) return;
    const geofenceId = pendingDeleteId;
    setFormError(null);
    setDeleting(true);
    deleteGeofence(accessToken, deviceId, geofenceId)
      .then(() => {
        setDeleting(false);
        setPendingDeleteId(null);
        if (editingId === geofenceId) resetForm();
        reload();
      })
      .catch((error) => {
        setDeleting(false);
        setFormError(describeError(error, "No se pudo eliminar la geocerca"));
      });
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);

    if (!name.trim()) {
      setFormError("El nombre es obligatorio (por ejemplo: Casa, Colegio).");
      return;
    }
    const parsedLatitude = Number(latitude);
    const parsedLongitude = Number(longitude);
    const parsedRadius = Number(radiusMeters);
    if (!Number.isFinite(parsedLatitude) || parsedLatitude < -90 || parsedLatitude > 90) {
      setFormError("La latitud debe ser un número entre -90 y 90.");
      return;
    }
    if (!Number.isFinite(parsedLongitude) || parsedLongitude < -180 || parsedLongitude > 180) {
      setFormError("La longitud debe ser un número entre -180 y 180.");
      return;
    }
    if (!Number.isFinite(parsedRadius) || parsedRadius <= 0) {
      setFormError("El radio debe ser un número de metros mayor que 0.");
      return;
    }

    const input: UpsertGeofenceInput = {
      name: name.trim(),
      latitude: parsedLatitude,
      longitude: parsedLongitude,
      radius_meters: parsedRadius,
    };

    setSubmitting(true);
    const request = editingId
      ? updateGeofence(accessToken, deviceId, editingId, input)
      : createGeofence(accessToken, deviceId, input);
    request
      .then(() => {
        setSubmitting(false);
        resetForm();
        reload();
      })
      .catch((error) => {
        setSubmitting(false);
        setFormError(describeError(error, "No se pudo guardar la geocerca"));
      });
  }

  const zoneColumns: Column<Geofence>[] = [
    {
      key: "name",
      header: "Zona",
      primary: true,
      render: (geofence) => <span className={styles.zoneName}>{geofence.name}</span>,
    },
    {
      key: "coords",
      header: "Coordenadas",
      render: (geofence) => (
        <span className={styles.coords}>
          {geofence.latitude.toFixed(5)}, {geofence.longitude.toFixed(5)}
        </span>
      ),
    },
    {
      key: "radius",
      header: "Radio",
      render: (geofence) => `${Math.round(geofence.radius_meters)} m`,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (geofence) => (
        <span className={styles.rowActions}>
          <Button size="sm" variant="ghost" icon={Pencil} onClick={() => startEditing(geofence)}>
            Editar
          </Button>
          <Button size="sm" variant="ghost" icon={Trash2} onClick={() => setPendingDeleteId(geofence.id)}>
            Eliminar
          </Button>
        </span>
      ),
    },
  ];

  const eventColumns: Column<GeofenceEvent>[] = [
    {
      key: "event",
      header: "Evento",
      primary: true,
      render: (event) => (
        <span className={styles.eventCell}>
          <StatusBadge tone={event.event_type === "ENTER" ? "success" : "neutral"} icon={event.event_type === "ENTER" ? LogIn : LogOut}>
            {event.event_type === "ENTER" ? "Entrada" : "Salida"}
          </StatusBadge>
          <span className={styles.eventDetail}>{eventLabel(event)}</span>
        </span>
      ),
    },
    {
      key: "when",
      header: "Fecha",
      align: "right",
      render: (event) => new Date(event.occurred_at).toLocaleString("es-CO"),
    },
  ];

  const pendingDeleteGeofence = geofences.find((geofence) => geofence.id === pendingDeleteId) ?? null;

  return (
    <>
      <MetricGrid>
        <MetricCard icon={MapPinned} tone="info" label="Geocercas configuradas" value={geofences.length} />
      </MetricGrid>

      <Card padding="none">
        <div className={styles.cardHead}>
          <CardHeader icon={MapPinned} title="Zonas y última ubicación" />
        </div>
        <GeofenceMap geofences={geofences} location={lastLocation} />
      </Card>

      <p className={styles.disclosure}>
        Precisión aproximada (ver docs/sprint-13.md): las entradas y salidas se detectan cuando
        llega un nuevo reporte de ubicación, cada ~15 minutos, no en tiempo real.
      </p>

      <div className={styles.columns}>
        <Card padding="none">
          <div className={styles.cardHead}>
            <CardHeader icon={MapPinned} title="Mis geocercas" />
          </div>
          {state.kind === "loading" && (
            <div className={styles.emptyWrap}>
              <Spinner label="Cargando geocercas…" />
            </div>
          )}
          {state.kind === "error" && <p className={styles.error}>{state.message}</p>}
          {state.kind === "loaded" && geofences.length === 0 && (
            <div className={styles.emptyWrap}>
              <EmptyState icon={MapPinned} title="Todavía no hay geocercas para este dispositivo" />
            </div>
          )}
          {state.kind === "loaded" && geofences.length > 0 && (
            <DataTable columns={zoneColumns} rows={geofences} rowKey={(geofence) => geofence.id} />
          )}

          <div className={styles.historyToggle}>
            <Button size="sm" onClick={toggleEvents}>
              {eventsState.kind === "loaded" || eventsState.kind === "loading"
                ? "Ocultar historial de entradas/salidas"
                : "Ver historial de entradas/salidas"}
            </Button>
          </div>

          {eventsState.kind === "loading" && (
            <div className={styles.emptyWrap}>
              <Spinner label="Cargando historial…" />
            </div>
          )}
          {eventsState.kind === "error" && <p className={styles.error}>{eventsState.message}</p>}
          {eventsState.kind === "loaded" && eventsState.events.length === 0 && (
            <p className={styles.historyEmpty}>Todavía no se detectó ninguna entrada o salida.</p>
          )}
          {eventsState.kind === "loaded" && eventsState.events.length > 0 && (
            <DataTable columns={eventColumns} rows={eventsState.events} rowKey={(event) => event.id} />
          )}
        </Card>

        <Card>
          <CardHeader
            icon={editingId ? Pencil : Plus}
            title={editingId ? "Editar geocerca" : "Crear geocerca"}
            actions={
              editingId && (
                <Button size="sm" variant="ghost" icon={X} onClick={resetForm}>
                  Cancelar
                </Button>
              )
            }
          />
          <form className={styles.form} onSubmit={handleSubmit}>
            <Field label="Nombre">
              {(id) => (
                <Input
                  id={id}
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="Casa, Colegio…"
                  maxLength={100}
                />
              )}
            </Field>
            <div className={styles.grid2}>
              <Field label="Latitud">
                {(id) => (
                  <Input
                    id={id}
                    type="number"
                    step="any"
                    value={latitude}
                    onChange={(event) => setLatitude(event.target.value)}
                    placeholder="4.6109"
                  />
                )}
              </Field>
              <Field label="Longitud">
                {(id) => (
                  <Input
                    id={id}
                    type="number"
                    step="any"
                    value={longitude}
                    onChange={(event) => setLongitude(event.target.value)}
                    placeholder="-74.0817"
                  />
                )}
              </Field>
            </div>
            <Field label="Radio (metros)">
              {(id) => (
                <Input
                  id={id}
                  type="number"
                  min={1}
                  value={radiusMeters}
                  onChange={(event) => setRadiusMeters(event.target.value)}
                />
              )}
            </Field>

            <Button type="button" variant="secondary" icon={LocateFixed} onClick={usePrefillFromLastKnownLocation} loading={prefilling}>
              Usar última ubicación conocida
            </Button>

            {formError && <p className={styles.error}>{formError}</p>}

            <Button type="submit" variant="primary" loading={submitting}>
              {editingId ? "Guardar cambios" : "Crear geocerca"}
            </Button>
          </form>
        </Card>
      </div>

      <ConfirmDialog
        open={pendingDeleteId !== null}
        title="¿Eliminar esta geocerca?"
        description={pendingDeleteGeofence ? `Se eliminará "${pendingDeleteGeofence.name}" y dejará de generar entradas/salidas.` : undefined}
        confirmLabel="Eliminar"
        busy={deleting}
        onConfirm={confirmDelete}
        onCancel={() => setPendingDeleteId(null)}
      />
    </>
  );
}
