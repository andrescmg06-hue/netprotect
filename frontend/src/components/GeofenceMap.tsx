import { MapPin } from "lucide-react";

import type { Geofence, LocationReport } from "@/lib/apiClient";

import styles from "./GeofenceMap.module.css";

const METERS_PER_DEGREE_LAT = 111_320;
const VIEWBOX = 320;
const ZONE_COLORS = ["#1769ff", "#7a5af8", "#16b364", "#f79009", "#f04438", "#0e8a8a"];

type Meters = { x: number; y: number };

function toLocalMeters(lat: number, lon: number, originLat: number, originLon: number): Meters {
  const cosLat = Math.cos((originLat * Math.PI) / 180);
  return {
    x: (lon - originLon) * cosLat * METERS_PER_DEGREE_LAT,
    y: -(lat - originLat) * METERS_PER_DEGREE_LAT,
  };
}

/** Sprint 36. A hand-made SVG schematic, not a real map (see docs/planning/plan-frontend.md,
 * Sprint 36 decision): plots configured geofences and the device's last known location relative
 * to each other, to scale, using a flat local-meters projection (good enough at the block/city
 * scale these zones live at — no need for a real map projection). Nothing is sent to a tile
 * server; every coordinate stays on this page. */
export function GeofenceMap({ geofences, location }: { geofences: Geofence[]; location: LocationReport | null }) {
  const anchors = [
    ...geofences.map((g) => ({ lat: g.latitude, lon: g.longitude })),
    ...(location ? [{ lat: location.latitude, lon: location.longitude }] : []),
  ];

  if (anchors.length === 0) {
    return (
      <div className={styles.empty}>
        <MapPin size={20} aria-hidden="true" />
        <p>Crea una geocerca o espera la próxima ubicación reportada para ver el esquema aquí.</p>
      </div>
    );
  }

  const originLat = anchors.reduce((sum, a) => sum + a.lat, 0) / anchors.length;
  const originLon = anchors.reduce((sum, a) => sum + a.lon, 0) / anchors.length;

  const zonePoints = geofences.map((geofence) => ({
    geofence,
    ...toLocalMeters(geofence.latitude, geofence.longitude, originLat, originLon),
  }));
  const locationPoint = location ? toLocalMeters(location.latitude, location.longitude, originLat, originLon) : null;

  const maxRadius = geofences.length > 0 ? Math.max(...geofences.map((g) => g.radius_meters)) : 80;
  const xs = zonePoints.map((p) => p.x).concat(locationPoint ? [locationPoint.x] : [], [0]);
  const ys = zonePoints.map((p) => p.y).concat(locationPoint ? [locationPoint.y] : [], [0]);
  const span = Math.max(Math.max(...xs) - Math.min(...xs), Math.max(...ys) - Math.min(...ys));
  const halfExtent = span / 2 + maxRadius * 1.4 + 24;
  const scale = (VIEWBOX / 2 - 20) / halfExtent;

  function project(point: Meters) {
    return { x: VIEWBOX / 2 + point.x * scale, y: VIEWBOX / 2 + point.y * scale };
  }

  return (
    <div className={styles.wrap}>
      <svg
        viewBox={`0 0 ${VIEWBOX} ${VIEWBOX}`}
        className={styles.svg}
        role="img"
        aria-label="Esquema de las zonas configuradas y la última ubicación conocida del dispositivo"
      >
        {zonePoints.map((point, index) => {
          const { x, y } = project(point);
          const r = Math.max(point.geofence.radius_meters * scale, 6);
          const color = ZONE_COLORS[index % ZONE_COLORS.length];
          return (
            <g key={point.geofence.id}>
              <circle cx={x} cy={y} r={r} fill={color} fillOpacity={0.14} stroke={color} strokeWidth={1.5} />
              <circle cx={x} cy={y} r={3} fill={color} />
              <text x={x} y={Math.max(y - r - 8, 12)} textAnchor="middle" className={styles.zoneLabel}>
                {point.geofence.name}
              </text>
            </g>
          );
        })}

        {locationPoint &&
          (() => {
            const { x, y } = project(locationPoint);
            return (
              <g>
                <circle cx={x} cy={y} r={8} className={styles.locationRing} />
                <circle cx={x} cy={y} r={3.5} className={styles.locationDot} />
              </g>
            );
          })()}
      </svg>

      <div className={styles.legend}>
        {geofences.map((geofence, index) => (
          <span key={geofence.id} className={styles.legendItem}>
            <span className={styles.swatch} style={{ background: ZONE_COLORS[index % ZONE_COLORS.length] }} />
            {geofence.name}
          </span>
        ))}
        {location && (
          <span className={styles.legendItem}>
            <span className={styles.locationSwatch} />
            Última ubicación conocida
          </span>
        )}
      </div>

      <p className={styles.caption}>
        Esquema de posición relativa, no un mapa real: usa las coordenadas de cada fila en un mapa
        externo si necesitas ver calles o terreno.
      </p>
    </div>
  );
}
