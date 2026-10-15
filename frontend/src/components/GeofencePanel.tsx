"use client";

import { ArrowLeftRight, History, LocateFixed, LogIn, LogOut, MapPin, Pencil, Trash2, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import { type GeofenceDraft, GeofenceMap, formatCoordinates, formatDistance } from "@/components/GeofenceMap";
import { Button, ConfirmDialog, EmptyState, Field, Input, Spinner } from "@/components/ui";
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
  | { kind: "loaded"; events: GeofenceEvent[]; loadedAt: number }
  | { kind: "error"; message: string };

/** The API accepts any radius above 0 and up to 100 000 m. The slider is logarithmic (10 m to
 * 100 km, one decade per quarter) because the useful values (a house, a school) live in the first
 * quarter of a linear 0–100 000 range; the number field next to it takes any exact value. */
const MAX_RADIUS_METERS = 100_000;
const MIN_SLIDER_METERS = 10;
const SLIDER_STEPS = 1000;
const SLIDER_DECADES = Math.log10(MAX_RADIUS_METERS / MIN_SLIDER_METERS);
const RADIUS_TICKS = ["10 m", "100 m", "1 km", "10 km", "100 km"];

function sliderToMeters(position: number): number {
  const raw = MIN_SLIDER_METERS * 10 ** ((position / SLIDER_STEPS) * SLIDER_DECADES);
  const step = raw < 100 ? 5 : raw < 1000 ? 10 : raw < 10_000 ? 100 : 1000;
  return Math.min(MAX_RADIUS_METERS, Math.max(MIN_SLIDER_METERS, Math.round(raw / step) * step));
}

function metersToSlider(meters: number): number {
  const clamped = Math.min(MAX_RADIUS_METERS, Math.max(MIN_SLIDER_METERS, meters));
  return Math.round((Math.log10(clamped / MIN_SLIDER_METERS) / SLIDER_DECADES) * SLIDER_STEPS);
}

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function eventLabel(event: GeofenceEvent): string {
  return event.event_type === "ENTER" ? `Entró a ${event.geofence_name}` : `Salió de ${event.geofence_name}`;
}

function formatEventDate(iso: string): string {
  return new Date(iso).toLocaleString("es-CO", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" });
}

function prefersReducedMotion(): boolean {
  return typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

/** Sprint 14 (logic), Sprint 36 (design), Sprint 57 (map-centric composition). No Android/GMS
 * Geofencing API — the backend detects ENTER/EXIT from consecutive location reports (see
 * docs/sprint-14.md), so creating a zone needs no map click handler: the form follows the flow
 * ubicación → radio → nombre, with "usar última ubicación conocida" to prefill it, and the
 * schematic beside it draws the unsaved zone live. The map is a hand-made SVG schematic (no
 * Leaflet/OpenStreetMap, no coordinates of a minor sent to a tile server). Omitted on purpose
 * (docs/redesign/fase-0-informe.md §5): address search, an "only exits" type (the backend always
 * records both), deltas and route lines. */
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

  const formRef = useRef<HTMLDivElement>(null);

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

  // Read once per device for the map's "última ubicación conocida" marker. Every read of this
  // endpoint is audited (LOCATION_VIEWED, Sprint 47), so it no longer re-runs after each save:
  // creating or editing a zone doesn't move the device.
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
  }, [accessToken, deviceId]);

  const reload = useCallback(() => {
    setState({ kind: "loading" });
    setReloadToken((current) => current + 1);
  }, []);

  const geofences = useMemo(() => (state.kind === "loaded" ? state.geofences : []), [state]);
  const lastLocation = locationState.kind === "loaded" ? locationState.report : null;

  // The unsaved zone, drawn live on the schematic as soon as the coordinates are valid.
  const draft = useMemo<GeofenceDraft | null>(() => {
    if (latitude.trim() === "" || longitude.trim() === "") return null;
    const lat = Number(latitude);
    const lon = Number(longitude);
    if (!Number.isFinite(lat) || lat < -90 || lat > 90 || !Number.isFinite(lon) || lon < -180 || lon > 180) {
      return null;
    }
    const radius = Number(radiusMeters);
    return {
      latitude: lat,
      longitude: lon,
      radius: Number.isFinite(radius) && radius > 0 ? Math.min(radius, MAX_RADIUS_METERS) : 0,
      name: name.trim(),
    };
  }, [latitude, longitude, radiusMeters, name]);

  const parsedRadius = Number(radiusMeters);
  const sliderPosition = metersToSlider(Number.isFinite(parsedRadius) && parsedRadius > 0 ? parsedRadius : 300);

  const todayCounts = useMemo(() => {
    if (eventsState.kind !== "loaded") return null;
    const today = new Date(eventsState.loadedAt).toDateString();
    return eventsState.events.reduce(
      (counts, event) => {
        if (new Date(event.occurred_at).toDateString() !== today) return counts;
        return event.event_type === "ENTER"
          ? { ...counts, enter: counts.enter + 1 }
          : { ...counts, exit: counts.exit + 1 };
      },
      { enter: 0, exit: 0 }
    );
  }, [eventsState]);

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
    // On a phone the form sits above the list, out of view: bring it back.
    formRef.current?.scrollIntoView({ behavior: prefersReducedMotion() ? "auto" : "smooth", block: "start" });
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
      .then(({ events }) => setEventsState({ kind: "loaded", events, loadedAt: Date.now() }))
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
    const radius = Number(radiusMeters);
    if (latitude.trim() === "" || !Number.isFinite(parsedLatitude) || parsedLatitude < -90 || parsedLatitude > 90) {
      setFormError("La latitud debe ser un número entre -90 y 90.");
      return;
    }
    if (longitude.trim() === "" || !Number.isFinite(parsedLongitude) || parsedLongitude < -180 || parsedLongitude > 180) {
      setFormError("La longitud debe ser un número entre -180 y 180.");
      return;
    }
    if (!Number.isFinite(radius) || radius <= 0) {
      setFormError("El radio debe ser un número de metros mayor que 0.");
      return;
    }
    if (radius > MAX_RADIUS_METERS) {
      setFormError("El radio no puede superar 100 000 metros.");
      return;
    }

    const input: UpsertGeofenceInput = {
      name: name.trim(),
      latitude: parsedLatitude,
      longitude: parsedLongitude,
      radius_meters: radius,
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

  const pendingDeleteGeofence = geofences.find((geofence) => geofence.id === pendingDeleteId) ?? null;
  const editingGeofence = geofences.find((geofence) => geofence.id === editingId) ?? null;
  const eventsOpen = eventsState.kind === "loaded" || eventsState.kind === "loading";

  return (
    <>
      <section className={styles.workspace} aria-label="Mapa y formulario de geocercas">
        <div className={styles.mapCol}>
          <GeofenceMap
            geofences={geofences}
            location={lastLocation}
            draft={draft}
            ghostId={editingId}
            emptyHint="Escribe unas coordenadas o usa la última ubicación conocida: la zona aparecerá aquí a escala."
          />
        </div>

        <div ref={formRef} className={styles.formCol}>
          <header className={styles.formHead}>
            <div>
              <p className={`eyebrow ${styles.eyebrow}`}>{editingId ? "Editando zona" : "Nueva zona"}</p>
              <h2 className={styles.formTitle}>
                {editingGeofence ? `Editar «${editingGeofence.name}»` : editingId ? "Editar geocerca" : "Crear geocerca"}
              </h2>
            </div>
            {editingId && (
              <Button size="sm" variant="ghost" icon={X} onClick={resetForm}>
                Cancelar
              </Button>
            )}
          </header>

          <form className={styles.form} onSubmit={handleSubmit} noValidate>
            <fieldset className={styles.step}>
              <legend className={styles.stepLegend}>
                <span className={styles.stepNumber}>01</span>
                Ubicación
              </legend>
              <div className={styles.grid2}>
                <Field label="Latitud">
                  {(id) => (
                    <Input
                      id={id}
                      type="number"
                      step="any"
                      inputMode="decimal"
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
                      inputMode="decimal"
                      value={longitude}
                      onChange={(event) => setLongitude(event.target.value)}
                      placeholder="-74.0817"
                    />
                  )}
                </Field>
              </div>
              <Button
                type="button"
                size="sm"
                variant="ghost"
                icon={LocateFixed}
                onClick={usePrefillFromLastKnownLocation}
                loading={prefilling}
                className={styles.prefill}
              >
                Usar última ubicación conocida
              </Button>
            </fieldset>

            <fieldset className={styles.step}>
              <legend className={styles.stepLegend}>
                <span className={styles.stepNumber}>02</span>
                Radio
              </legend>
              <div className={styles.radiusRow}>
                <input
                  type="range"
                  className={styles.slider}
                  min={0}
                  max={SLIDER_STEPS}
                  step={1}
                  value={sliderPosition}
                  onChange={(event) => setRadiusMeters(String(sliderToMeters(Number(event.target.value))))}
                  aria-label="Radio de la zona (escala logarítmica)"
                  aria-valuetext={
                    Number.isFinite(parsedRadius) && parsedRadius > 0 ? `${formatDistance(parsedRadius)}` : "Sin radio"
                  }
                />
                <span className={styles.radiusField}>
                  <Input
                    type="number"
                    min={1}
                    max={MAX_RADIUS_METERS}
                    inputMode="numeric"
                    value={radiusMeters}
                    onChange={(event) => setRadiusMeters(event.target.value)}
                    aria-label="Radio exacto en metros"
                    className={styles.radiusInput}
                  />
                  <span className={styles.unit} aria-hidden="true">
                    m
                  </span>
                </span>
              </div>
              <div className={styles.ticks} aria-hidden="true">
                {RADIUS_TICKS.map((tick) => (
                  <span key={tick}>{tick}</span>
                ))}
              </div>
            </fieldset>

            <fieldset className={styles.step}>
              <legend className={styles.stepLegend}>
                <span className={styles.stepNumber}>03</span>
                Nombre
              </legend>
              <Input
                value={name}
                onChange={(event) => setName(event.target.value)}
                placeholder="Casa, Colegio, Entrenamiento…"
                maxLength={100}
                aria-label="Nombre de la zona"
              />
            </fieldset>

            <div className={styles.notice}>
              <ArrowLeftRight size={18} strokeWidth={1.75} aria-hidden="true" className={styles.noticeIcon} />
              <p>
                <strong>Avisa al entrar y al salir.</strong> Se comprueba con cada reporte de ubicación, cada
                ~15 minutos: no es tiempo real.
              </p>
            </div>

            {formError && (
              <p className={styles.formError} role="alert">
                {formError}
              </p>
            )}

            <Button type="submit" variant="primary" loading={submitting} fullWidth>
              {editingId ? "Guardar cambios" : "Crear geocerca"}
            </Button>
          </form>
        </div>
      </section>

      <section className={styles.zones} aria-labelledby="geofence-list-title">
        <header className={styles.sectionHead}>
          <div>
            <h2 id="geofence-list-title" className={styles.sectionTitle}>
              Mis geocercas
            </h2>
            <p className={styles.sectionMeta}>
              {state.kind === "loaded"
                ? geofences.length === 1
                  ? "1 zona en este dispositivo"
                  : `${geofences.length} zonas en este dispositivo`
                : "Zonas configuradas en este dispositivo"}
            </p>
          </div>
          <Button size="sm" icon={History} onClick={toggleEvents} aria-expanded={eventsOpen}>
            {eventsOpen ? "Ocultar historial de entradas/salidas" : "Ver historial de entradas/salidas"}
          </Button>
        </header>

        {state.kind === "loading" && (
          <div className={styles.pad}>
            <Spinner label="Cargando geocercas…" />
          </div>
        )}
        {state.kind === "error" && (
          <p className={styles.error} role="alert">
            {state.message}
          </p>
        )}
        {state.kind === "loaded" && geofences.length === 0 && (
          <EmptyState
            icon={MapPin}
            illustration={
              <span className={styles.emptyArt} aria-hidden="true">
                <span className={styles.emptyRingOuter} />
                <span className={styles.emptyRingInner} />
                <MapPin size={22} strokeWidth={1.75} className={styles.emptyPin} />
              </span>
            }
            title="Todavía no hay geocercas para este dispositivo"
            description="Dibuja la primera arriba: unas coordenadas, un radio y un nombre como Casa o Colegio."
          />
        )}
        {state.kind === "loaded" && geofences.length > 0 && (
          <ol className={styles.zoneList}>
            {geofences.map((geofence, index) => (
              <li
                key={geofence.id}
                className={geofence.id === editingId ? `${styles.zoneRow} ${styles.zoneRowActive}` : styles.zoneRow}
              >
                <span className={styles.zoneIndex}>{String(index + 1).padStart(2, "0")}</span>
                <span className={styles.zoneMain}>
                  <span className={styles.zoneName}>{geofence.name}</span>
                  <span className={styles.zoneCoords}>
                    {formatCoordinates(geofence.latitude, geofence.longitude)}
                  </span>
                </span>
                <span className={styles.zoneRadius}>
                  <span className={styles.zoneRadiusValue}>{formatDistance(geofence.radius_meters)}</span>
                  <span className={styles.zoneRadiusLabel}>radio</span>
                </span>
                <span className={styles.rowActions}>
                  <Button size="sm" variant="ghost" icon={Pencil} onClick={() => startEditing(geofence)}>
                    Editar
                  </Button>
                  <Button size="sm" variant="ghost" icon={Trash2} onClick={() => setPendingDeleteId(geofence.id)}>
                    Eliminar
                  </Button>
                </span>
              </li>
            ))}
          </ol>
        )}

        {eventsState.kind !== "idle" && (
          <div className={styles.events}>
            <div className={styles.eventsHead}>
              <h3 className={styles.eventsTitle}>Entradas y salidas</h3>
              {todayCounts && (
                <p className={styles.sectionMeta}>
                  Hoy: {todayCounts.enter} {todayCounts.enter === 1 ? "entrada" : "entradas"} · {todayCounts.exit}{" "}
                  {todayCounts.exit === 1 ? "salida" : "salidas"}
                </p>
              )}
            </div>
            {eventsState.kind === "loading" && <Spinner label="Cargando historial…" />}
            {eventsState.kind === "error" && (
              <p className={styles.eventsError} role="alert">
                {eventsState.message}
              </p>
            )}
            {eventsState.kind === "loaded" && eventsState.events.length === 0 && (
              <p className={styles.eventsEmpty}>Todavía no se detectó ninguna entrada o salida.</p>
            )}
            {eventsState.kind === "loaded" && eventsState.events.length > 0 && (
              <ul className={styles.eventList}>
                {eventsState.events.map((event) => {
                  const isEnter = event.event_type === "ENTER";
                  const Icon = isEnter ? LogIn : LogOut;
                  return (
                    <li key={event.id} className={styles.eventRow}>
                      <span className={isEnter ? `${styles.eventIcon} ${styles.enter}` : styles.eventIcon}>
                        <Icon size={15} strokeWidth={2} aria-hidden="true" />
                      </span>
                      <span className={styles.eventText}>{eventLabel(event)}</span>
                      <time className={styles.eventTime} dateTime={event.occurred_at}>
                        {formatEventDate(event.occurred_at)}
                      </time>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        )}
      </section>

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
