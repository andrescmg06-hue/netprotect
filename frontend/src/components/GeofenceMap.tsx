"use client";

import { MapPinOff } from "lucide-react";
import { useEffect, useRef, useState } from "react";

import type { Geofence, LocationReport } from "@/lib/apiClient";

import styles from "./GeofenceMap.module.css";

const METERS_PER_DEGREE_LAT = 111_320;
const PADDING = 56;
const MIN_SPAN_METERS = 300;
const TARGET_CELL_PX = 88;

/** A zone that is being typed in the form and has not been saved yet. */
export type GeofenceDraft = { latitude: number; longitude: number; radius: number; name: string };

type Meters = { x: number; y: number };
type Point = { x: number; y: number };

function toLocalMeters(lat: number, lon: number, originLat: number, originLon: number): Meters {
  const cosLat = Math.cos((originLat * Math.PI) / 180);
  return {
    x: (lon - originLon) * cosLat * METERS_PER_DEGREE_LAT,
    y: -(lat - originLat) * METERS_PER_DEGREE_LAT,
  };
}

/** 1, 2 or 5 times a power of ten: the grid cell and the scale bar read as round distances. */
function niceDistance(target: number): number {
  const exponent = Math.floor(Math.log10(target));
  const base = 10 ** exponent;
  const fraction = target / base;
  const nice = fraction < 1.5 ? 1 : fraction < 3.5 ? 2 : fraction < 7.5 ? 5 : 10;
  return nice * base;
}

export function formatDistance(meters: number): string {
  if (meters >= 1000) {
    return `${(meters / 1000).toLocaleString("es-CO", { maximumFractionDigits: 1 })} km`;
  }
  return `${Math.round(meters).toLocaleString("es-CO")} m`;
}

/** "4.71100, −74.07210": five decimals and a true minus sign (U+2212). The hyphen-minus of a
 * tabular-figures run renders with a gap after it ("- 74.07"); the minus glyph is as wide as a
 * digit and reads as one with its number. Display only, never parsed back. */
export function formatCoordinates(latitude: number, longitude: number): string {
  const part = (value: number) => value.toFixed(5).replace("-", "−");
  return `${part(latitude)}, ${part(longitude)}`;
}

function gridOffsets(origin: number, cell: number, extent: number): number[] {
  if (cell < 12) return [];
  const first = Math.ceil(-origin / cell);
  const last = Math.floor((extent - origin) / cell);
  const offsets: number[] = [];
  for (let k = first; k <= last; k += 1) offsets.push(Math.round(origin + k * cell) + 0.5);
  return offsets;
}

type Geometry = {
  zones: { geofence: Geofence; center: Point; r: number }[];
  draft: { center: Point; r: number; name: string; radius: number } | null;
  location: { center: Point; accuracyR: number } | null;
  cellMeters: number;
  cellPx: number;
  gridOrigin: Point;
};

function computeGeometry(
  geofences: Geofence[],
  location: LocationReport | null,
  draft: GeofenceDraft | null,
  showAccuracy: boolean,
  width: number,
  height: number
): Geometry | null {
  const anchors = [
    ...geofences.map((g) => ({ lat: g.latitude, lon: g.longitude })),
    ...(draft ? [{ lat: draft.latitude, lon: draft.longitude }] : []),
    ...(location ? [{ lat: location.latitude, lon: location.longitude }] : []),
  ];
  if (anchors.length === 0) return null;

  const originLat = anchors.reduce((sum, a) => sum + a.lat, 0) / anchors.length;
  const originLon = anchors.reduce((sum, a) => sum + a.lon, 0) / anchors.length;

  const zoneMeters = geofences.map((geofence) => ({
    geofence,
    ...toLocalMeters(geofence.latitude, geofence.longitude, originLat, originLon),
    r: geofence.radius_meters,
  }));
  const draftMeters = draft
    ? { ...toLocalMeters(draft.latitude, draft.longitude, originLat, originLon), r: draft.radius }
    : null;
  const locationMeters = location
    ? {
        ...toLocalMeters(location.latitude, location.longitude, originLat, originLon),
        r: showAccuracy ? location.accuracy_meters : 0,
      }
    : null;

  const circles = [
    ...zoneMeters,
    ...(draftMeters ? [draftMeters] : []),
    ...(locationMeters ? [locationMeters] : []),
  ];
  const minX = Math.min(...circles.map((c) => c.x - c.r));
  const maxX = Math.max(...circles.map((c) => c.x + c.r));
  const minY = Math.min(...circles.map((c) => c.y - c.r));
  const maxY = Math.max(...circles.map((c) => c.y + c.r));
  const spanX = Math.max(maxX - minX, MIN_SPAN_METERS);
  const spanY = Math.max(maxY - minY, MIN_SPAN_METERS);
  const centerX = (minX + maxX) / 2;
  const centerY = (minY + maxY) / 2;
  const scale = Math.min(Math.max(width - 2 * PADDING, 40) / spanX, Math.max(height - 2 * PADDING, 40) / spanY);

  const project = (m: Meters): Point => ({
    x: width / 2 + (m.x - centerX) * scale,
    y: height / 2 + (m.y - centerY) * scale,
  });

  const cellMeters = niceDistance(TARGET_CELL_PX / scale);

  return {
    zones: zoneMeters.map((z) => ({ geofence: z.geofence, center: project(z), r: Math.max(z.r * scale, 4) })),
    draft:
      draftMeters && draft
        ? { center: project(draftMeters), r: Math.max(draftMeters.r * scale, 4), name: draft.name, radius: draft.radius }
        : null,
    location: locationMeters
      ? { center: project(locationMeters), accuracyR: Math.max(locationMeters.r * scale, 10) }
      : null,
    cellMeters,
    cellPx: cellMeters * scale,
    gridOrigin: project({ x: 0, y: 0 }),
  };
}

/** Two-line label (name, radius) above a circle, or inside it when the circle fills the view. */
function labelPosition(center: Point, r: number, width: number): { x: number; y: number } {
  const x = Math.min(Math.max(center.x, 64), width - 64);
  // Baseline of the first line; the second sits 14px lower, still clear of the circle's top.
  const above = center.y - r - 22;
  return { x, y: above >= 16 ? above : center.y + 22 };
}

type Box = { left: number; right: number; top: number; bottom: number };

/** Rough box of an SVG text block (a name line plus an optional second line) from its character
 * count: enough to tell whether two labels would print over each other, not a text measurement. */
function textBox(x: number, baseline: number, widthChars: number, lines: 1 | 2): Box {
  const half = (widthChars * 6.8) / 2 + 4;
  return { left: x - half, right: x + half, top: baseline - 13, bottom: baseline + (lines === 2 ? 18 : 4) };
}

function overlaps(a: Box, b: Box): boolean {
  return a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top;
}

/** Where the device's own label goes: below its dot, else above it, else nowhere. A zone label can
 * land on the same spot (the device is inside that zone); the legend already names the marker, so
 * dropping the secondary label beats printing two texts over each other. */
function deviceLabelPosition(center: Point, text: string, width: number, taken: Box[]): { x: number; y: number } | null {
  const x = Math.min(Math.max(center.x, 64), width - 64);
  for (const y of [center.y + 24, center.y - 16]) {
    const box = textBox(x, y, text.length, 1);
    if (!taken.some((other) => overlaps(box, other))) return { x, y };
  }
  return null;
}

/** Sprint 36, recomposed in Sprint 57. A hand-made schematic, not a real map (see
 * docs/planning/plan-frontend.md, Sprint 36 decision, and .claude/rules/frontend.md): it plots the
 * configured zones, an unsaved draft and the device's last known location to scale on a flat
 * local-meters projection, over a grid whose cell is a round distance. Nothing is sent to a tile
 * server; every coordinate stays on this page. The SVG is measured (ResizeObserver) so one user
 * unit is one CSS pixel: labels stay crisp and the map can fill whatever height its section gives
 * it, instead of being a fixed square. */
export function GeofenceMap({
  geofences,
  location,
  draft = null,
  activeId = null,
  ghostId = null,
  variant = "zones",
  emptyHint = "Todavía no hay zonas ni una ubicación reportada para dibujar.",
}: {
  geofences: Geofence[];
  location: LocationReport | null;
  /** Unsaved zone typed in the form, drawn dashed in the accent colour. */
  draft?: GeofenceDraft | null;
  /** Zone to emphasise (the one that contains the device). */
  activeId?: string | null;
  /** Zone being edited: its saved outline is drawn faintly under the draft. */
  ghostId?: string | null;
  /** "tracking" makes the device the protagonist: accent dot plus its accuracy halo. */
  variant?: "zones" | "tracking";
  emptyHint?: string;
}) {
  const canvasRef = useRef<HTMLDivElement>(null);
  const [size, setSize] = useState({ width: 640, height: 420 });

  useEffect(() => {
    const element = canvasRef.current;
    if (!element || typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver((entries) => {
      const box = entries[0]?.contentRect;
      if (!box || box.width <= 0 || box.height <= 0) return;
      const next = { width: Math.round(box.width), height: Math.round(box.height) };
      setSize((current) => (current.width === next.width && current.height === next.height ? current : next));
    });
    observer.observe(element);
    return () => observer.disconnect();
  }, []);

  const { width, height } = size;
  const tracking = variant === "tracking";
  const geometry = computeGeometry(geofences, location, draft, tracking, width, height);

  const cellPx = geometry ? geometry.cellPx : TARGET_CELL_PX;
  const origin = geometry ? geometry.gridOrigin : { x: width / 2, y: height / 2 };
  const verticals = gridOffsets(origin.x, cellPx, width);
  const horizontals = gridOffsets(origin.y, cellPx, height);

  // Boxes of every zone/draft label, so the device label can step aside instead of overprinting.
  const labelBoxes: Box[] = geometry
    ? [
        ...geometry.zones
          .filter(({ geofence }) => geofence.id !== ghostId)
          .map(({ geofence, center, r }) => {
            const position = labelPosition(center, r, width);
            const meta = `radio ${formatDistance(geofence.radius_meters)}`;
            return textBox(position.x, position.y, Math.max(geofence.name.length, meta.length), 2);
          }),
        ...(geometry.draft
          ? [
              (() => {
                const position = labelPosition(geometry.draft.center, geometry.draft.r, width);
                return textBox(position.x, position.y, Math.max((geometry.draft.name || "Nueva zona").length, 14), 2);
              })(),
            ]
          : []),
      ]
    : [];
  const deviceText = tracking ? "Dispositivo" : "Última ubicación";
  const devicePosition = geometry?.location
    ? deviceLabelPosition(geometry.location.center, deviceText, width, labelBoxes)
    : null;

  const zoneCount = geofences.length;
  const ariaLabel = [
    "Esquema a escala",
    zoneCount === 1 ? "con 1 zona" : `con ${zoneCount} zonas`,
    location ? "y la última ubicación conocida del dispositivo" : "sin ubicación reportada",
  ].join(" ");

  return (
    <figure className={styles.map}>
      <div ref={canvasRef} className={styles.canvas}>
        <svg
          className={styles.svg}
          width={width}
          height={height}
          viewBox={`0 0 ${width} ${height}`}
          role="img"
          aria-label={ariaLabel}
        >
          <g className={styles.grid}>
            {verticals.map((x) => (
              <line key={`v${x}`} x1={x} y1={0} x2={x} y2={height} />
            ))}
            {horizontals.map((y) => (
              <line key={`h${y}`} x1={0} y1={y} x2={width} y2={y} />
            ))}
          </g>

          {geometry && (
            <>
              {geometry.zones.map(({ geofence, center, r }) => {
                const isGhost = geofence.id === ghostId;
                const isActive = geofence.id === activeId;
                const className = isGhost ? styles.ghost : isActive ? styles.zoneActive : styles.zone;
                return (
                  <g key={geofence.id}>
                    <circle cx={center.x} cy={center.y} r={r} className={className} />
                    {!isGhost && <circle cx={center.x} cy={center.y} r={2.5} className={styles.zoneCenter} />}
                  </g>
                );
              })}

              {geometry.draft && (
                <g>
                  <circle cx={geometry.draft.center.x} cy={geometry.draft.center.y} r={geometry.draft.r} className={styles.draft} />
                  <path
                    className={styles.crosshair}
                    d={`M ${geometry.draft.center.x - 7} ${geometry.draft.center.y} H ${geometry.draft.center.x + 7} M ${geometry.draft.center.x} ${geometry.draft.center.y - 7} V ${geometry.draft.center.y + 7}`}
                  />
                </g>
              )}

              {geometry.location &&
                (tracking ? (
                  <g>
                    <circle
                      cx={geometry.location.center.x}
                      cy={geometry.location.center.y}
                      r={geometry.location.accuracyR}
                      className={styles.halo}
                    />
                    <circle cx={geometry.location.center.x} cy={geometry.location.center.y} r={9} className={styles.deviceRing} />
                    <circle cx={geometry.location.center.x} cy={geometry.location.center.y} r={5.5} className={styles.deviceDot} />
                  </g>
                ) : (
                  <g>
                    <circle cx={geometry.location.center.x} cy={geometry.location.center.y} r={7} className={styles.lastRing} />
                    <circle cx={geometry.location.center.x} cy={geometry.location.center.y} r={3.5} className={styles.lastDot} />
                  </g>
                ))}

              {/* Labels last, so they sit above every circle. */}
              {geometry.zones
                .filter(({ geofence }) => geofence.id !== ghostId)
                .map(({ geofence, center, r }) => {
                  const position = labelPosition(center, r, width);
                  return (
                    <text key={`label-${geofence.id}`} x={position.x} y={position.y} textAnchor="middle" className={styles.label}>
                      <tspan className={geofence.id === activeId ? styles.labelNameActive : styles.labelName}>{geofence.name}</tspan>
                      <tspan x={position.x} dy={14} className={styles.labelMeta}>
                        radio {formatDistance(geofence.radius_meters)}
                      </tspan>
                    </text>
                  );
                })}

              {geometry.draft &&
                (() => {
                  const position = labelPosition(geometry.draft.center, geometry.draft.r, width);
                  return (
                    <text x={position.x} y={position.y} textAnchor="middle" className={styles.label}>
                      <tspan className={styles.draftName}>{geometry.draft.name || "Nueva zona"}</tspan>
                      <tspan x={position.x} dy={14} className={styles.labelMeta}>
                        {geometry.draft.radius > 0 ? `radio ${formatDistance(geometry.draft.radius)}` : "sin radio"}
                      </tspan>
                    </text>
                  );
                })()}

              {devicePosition && (
                <text
                  x={devicePosition.x}
                  y={devicePosition.y}
                  textAnchor="middle"
                  className={`${styles.label} ${styles.labelMeta}`}
                >
                  {deviceText}
                </text>
              )}

              <g className={styles.scale} transform={`translate(20 ${height - 22})`}>
                <path d={`M 0 -5 V 0 H ${geometry.cellPx} V -5`} />
                <text x={0} y={-10} className={styles.label}>
                  {formatDistance(geometry.cellMeters)}
                </text>
              </g>
            </>
          )}

          <g className={styles.north} transform={`translate(${width - 24} 26)`}>
            <path d="M 0 -12 L 5 2 L 0 -1 L -5 2 Z" />
            <text x={0} y={17} textAnchor="middle">
              N
            </text>
          </g>
        </svg>

        {!geometry && (
          <div className={styles.emptyNote}>
            <MapPinOff size={20} strokeWidth={1.75} aria-hidden="true" />
            <p>{emptyHint}</p>
          </div>
        )}
      </div>

      <figcaption className={styles.legend}>
        <span className={styles.legendItems}>
          {zoneCount > 0 && (
            <span className={styles.legendItem}>
              <span className={styles.swatchZone} aria-hidden="true" />
              Zona
            </span>
          )}
          {draft && (
            <span className={styles.legendItem}>
              <span className={styles.swatchDraft} aria-hidden="true" />
              Zona sin guardar
            </span>
          )}
          {location && (
            <span className={styles.legendItem}>
              <span className={tracking ? styles.swatchDevice : styles.swatchLast} aria-hidden="true" />
              {tracking ? "Dispositivo; el halo es la precisión aproximada" : "Última ubicación conocida"}
            </span>
          )}
          {geometry && <span className={styles.legendItem}>Cada cuadro mide {formatDistance(geometry.cellMeters)}</span>}
        </span>
        <span className={styles.caption}>Esquema sin calles: las coordenadas no salen de esta página.</span>
      </figcaption>
    </figure>
  );
}
