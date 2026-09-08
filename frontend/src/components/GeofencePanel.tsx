"use client";

import { useCallback, useEffect, useState } from "react";

import {
  ApiError,
  type Geofence,
  type GeofenceEvent,
  type UpsertGeofenceInput,
  createGeofence,
  deleteGeofence,
  getLatestLocation,
  listGeofenceEvents,
  listGeofences,
  updateGeofence,
} from "@/lib/apiClient";

type GeofencesState =
  | { kind: "loading" }
  | { kind: "loaded"; geofences: Geofence[] }
  | { kind: "error"; message: string };

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

/** Sprint 14. No Android/GMS Geofencing API — the backend detects ENTER/EXIT from consecutive
 * location reports (see docs/sprint-14.md), so creating a zone here needs no map click handler:
 * a plain numeric form plus "usar última ubicación conocida" to prefill it from what
 * DeviceLocationPanel already shows. Same web-only CRUD split as DeviceRulesPanel/
 * DeviceCategoriesPanel — Android's tutor screen only reads what's configured here.
 */
export function GeofencePanel({ accessToken, deviceId }: { accessToken: string; deviceId: string }) {
  const [state, setState] = useState<GeofencesState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);
  const [eventsState, setEventsState] = useState<EventsState>({ kind: "idle" });
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [prefilling, setPrefilling] = useState(false);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [latitude, setLatitude] = useState("");
  const [longitude, setLongitude] = useState("");
  const [radiusMeters, setRadiusMeters] = useState("300");

  // Same react-hooks/set-state-in-effect pattern as every other panel (see CLAUDE.md): setState
  // only runs inside a .then()/.catch() already chained in the effect body.
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

  const reload = useCallback(() => {
    setState({ kind: "loading" });
    setReloadToken((current) => current + 1);
  }, []);

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

  function handleDelete(geofenceId: string) {
    setFormError(null);
    deleteGeofence(accessToken, deviceId, geofenceId)
      .then(() => {
        if (editingId === geofenceId) resetForm();
        reload();
      })
      .catch((error) => setFormError(describeError(error, "No se pudo eliminar la geocerca")));
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

  return (
    <div className="rulesPanel">
      <div className="devicesPanelHeader">
        <strong>Geocercas</strong>
        <button type="button" onClick={reload}>
          Actualizar
        </button>
      </div>
      <p className="statusText">
        Precisión aproximada (ver docs/sprint-13.md): las entradas y salidas se detectan cuando
        llega un nuevo reporte de ubicación, cada ~15 minutos, no en tiempo real.
      </p>

      <form className="ruleForm" onSubmit={handleSubmit}>
        <input
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder="Nombre (Casa, Colegio…)"
          maxLength={100}
          aria-label="Nombre de la geocerca"
        />
        <input
          type="number"
          step="any"
          value={latitude}
          onChange={(event) => setLatitude(event.target.value)}
          placeholder="Latitud"
          aria-label="Latitud del centro"
        />
        <input
          type="number"
          step="any"
          value={longitude}
          onChange={(event) => setLongitude(event.target.value)}
          placeholder="Longitud"
          aria-label="Longitud del centro"
        />
        <input
          type="number"
          min={1}
          value={radiusMeters}
          onChange={(event) => setRadiusMeters(event.target.value)}
          placeholder="Radio (m)"
          aria-label="Radio en metros"
        />
        <button type="button" onClick={usePrefillFromLastKnownLocation} disabled={prefilling}>
          {prefilling ? "Buscando…" : "Usar última ubicación conocida"}
        </button>
        <button type="submit" disabled={submitting}>
          {submitting ? "Guardando…" : editingId ? "Guardar cambios" : "Crear geocerca"}
        </button>
        {editingId && (
          <button type="button" onClick={resetForm}>
            Cancelar edición
          </button>
        )}
      </form>

      {formError && <p className="authError">{formError}</p>}

      {state.kind === "loading" && <p className="statusText">Cargando geocercas…</p>}
      {state.kind === "error" && <p className="authError">{state.message}</p>}
      {state.kind === "loaded" && state.geofences.length === 0 && (
        <p className="statusText">Todavía no hay geocercas para este dispositivo.</p>
      )}
      {state.kind === "loaded" && state.geofences.length > 0 && (
        <ul className="appList">
          {state.geofences.map((geofence) => (
            <li key={geofence.id} className="appRow">
              <div>
                <div className="appLabel">{geofence.name}</div>
                <div className="appMeta">
                  Lat {geofence.latitude.toFixed(5)}, Lng {geofence.longitude.toFixed(5)} · radio{" "}
                  {Math.round(geofence.radius_meters)} m
                </div>
              </div>
              <div>
                <button type="button" onClick={() => startEditing(geofence)}>
                  Editar
                </button>
                <button type="button" className="dangerButton" onClick={() => handleDelete(geofence.id)}>
                  Eliminar
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      <button type="button" onClick={toggleEvents}>
        {eventsState.kind === "loaded" || eventsState.kind === "loading"
          ? "Ocultar historial de entradas/salidas"
          : "Ver historial de entradas/salidas"}
      </button>

      {eventsState.kind === "loading" && <p className="statusText">Cargando historial…</p>}
      {eventsState.kind === "error" && <p className="authError">{eventsState.message}</p>}
      {eventsState.kind === "loaded" && eventsState.events.length === 0 && (
        <p className="statusText">Todavía no se detectó ninguna entrada o salida.</p>
      )}
      {eventsState.kind === "loaded" && eventsState.events.length > 0 && (
        <ul className="appList">
          {eventsState.events.map((event) => (
            <li key={event.id} className="appRow">
              <div className="appLabel">{eventLabel(event)}</div>
              <span className="appUsage">{new Date(event.occurred_at).toLocaleString("es-CO")}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
