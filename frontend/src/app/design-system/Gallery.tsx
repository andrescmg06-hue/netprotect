"use client";

import {
  Ban,
  Bell,
  CalendarClock,
  Clock,
  LayoutGrid,
  Link2,
  Palette,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
  Smartphone,
  Trash2,
} from "lucide-react";
import { useState } from "react";

import {
  ALL_DAYS_MASK,
  Button,
  Card,
  CardHeader,
  ConfirmDialog,
  DayPicker,
  EmptyState,
  Field,
  Input,
  Logo,
  MetricCard,
  MetricGrid,
  PageHeader,
  SegmentedControl,
  Select,
  Spinner,
  StatusBadge,
  Switch,
  describeDays,
} from "@/components/ui";

import styles from "./Gallery.module.css";

/** Token swatches shown in the gallery: the surfaces, ink, brand and status colours of globals.css. */
const SWATCHES = [
  { token: "--color-bg" },
  { token: "--color-surface" },
  { token: "--color-surface-muted" },
  { token: "--color-border" },
  { token: "--color-border-strong" },
  { token: "--color-text" },
  { token: "--color-text-muted" },
  { token: "--color-text-subtle" },
  { token: "--color-navy-950" },
  { token: "--color-navy-700" },
  { token: "--color-primary" },
  { token: "--color-primary-text" },
  { token: "--color-primary-soft" },
  { token: "--color-success" },
  { token: "--color-warning" },
  { token: "--color-danger" },
  { token: "--color-purple" },
];

export function Gallery() {
  const [days, setDays] = useState(0b001_1111);
  const [period, setPeriod] = useState<"today" | "7d" | "30d">("7d");
  const [enabled, setEnabled] = useState(true);
  const [confirmOpen, setConfirmOpen] = useState(false);

  return (
    <main className={styles.page}>
      <div className={styles.logos}>
        <Logo height={48} />
        <Logo variant="shield" height={40} />
      </div>

      <PageHeader
        title="Sistema de diseño"
        description="Componentes base del Sprint 31, con los tokens del rediseño editorial (Sprint 53). Solo en desarrollo."
        breadcrumb="Sistema de diseño"
      />

      <PageHeader
        band
        title="Con banda fotográfica"
        description="Variante opt-in del encabezado: velo crema a la izquierda, título serif, frase y selector a la derecha."
        breadcrumb="Sistema de diseño"
        quote="Saber dónde están, para que lleguen más lejos."
        aside={<StatusBadge tone="success" dot>En línea</StatusBadge>}
      />

      <Card>
        <CardHeader icon={Palette} title="Tokens" subtitle="Superficies, texto, marca y estados. Cada muestra lee su variable de globals.css." />
        <div className={styles.swatches}>
          {SWATCHES.map((swatch) => (
            <div key={swatch.token} className={styles.swatch}>
              <span className={styles.chip} style={{ background: `var(${swatch.token})` }} />
              <span className={styles.swatchName}>{swatch.token}</span>
            </div>
          ))}
        </div>
        <div className={styles.type}>
          <span className="eyebrow">Etiqueta en mayúsculas</span>
          <h2 className={styles.typeTitle}>Título en serif, el tono editorial</h2>
          <p className="quote">Una frase editorial en serif cursiva.</p>
          <p>
            Texto de interfaz en sans: <span className="tabular">0123456789 · 4 h 12 min</span> con cifras alineadas.
          </p>
        </div>
      </Card>

      <MetricGrid>
        <MetricCard icon={ShieldCheck} tone="info" label="Reglas activas" value="12" hint="En 3 dispositivos" />
        <MetricCard icon={Ban} tone="danger" label="Apps bloqueadas" value="5" hint="Hoy" />
        <MetricCard icon={Clock} tone="warning" label="Con límite de tiempo" value="4" />
        <MetricCard icon={CalendarClock} tone="purple" label="Con horario" value="3" />
      </MetricGrid>

      <div className={styles.columns}>
        <Card>
          <CardHeader
            icon={LayoutGrid}
            title="Botones"
            subtitle="Primary, secondary, danger, ghost; dos tamaños; estado de carga."
          />
          <div className={styles.row}>
            <Button variant="primary" icon={Plus}>
              Crear regla
            </Button>
            <Button icon={RefreshCw}>Actualizar</Button>
            <Button variant="danger" icon={Trash2} onClick={() => setConfirmOpen(true)}>
              Desvincular
            </Button>
            <Button variant="ghost">Cancelar</Button>
          </div>
          <div className={styles.row}>
            <Button variant="primary" size="sm">
              Pequeño
            </Button>
            <Button size="sm" loading>
              Guardando…
            </Button>
            <Button variant="primary" disabled>
              Deshabilitado
            </Button>
          </div>
        </Card>

        <Card>
          <CardHeader icon={Bell} title="Estados" subtitle="Badges semánticos." />
          <div className={styles.row}>
            <StatusBadge tone="success" dot>
              En línea
            </StatusBadge>
            <StatusBadge tone="neutral" dot>
              Desconectado
            </StatusBadge>
            <StatusBadge tone="danger" icon={Ban}>
              Bloqueada
            </StatusBadge>
            <StatusBadge tone="success">Permitida</StatusBadge>
            <StatusBadge tone="warning" icon={Clock}>
              Límite diario
            </StatusBadge>
            <StatusBadge tone="purple" icon={CalendarClock}>
              Horario
            </StatusBadge>
            <StatusBadge tone="info">INFO</StatusBadge>
            <StatusBadge tone="danger">CRITICAL</StatusBadge>
          </div>
          <div className={styles.row}>
            <Spinner label="Cargando dispositivos…" />
          </div>
        </Card>
      </div>

      <div className={styles.columns}>
        <Card>
          <CardHeader icon={Plus} title="Formulario" subtitle="Campos, selector, días y switch." />
          <div className={styles.form}>
            <Field label="Aplicación (paquete)" hint="Ej. com.whatsapp">
              {(id) => <Input id={id} placeholder="com.instagram.android" />}
            </Field>
            <Field label="Tipo de regla">
              {(id) => (
                <Select id={id} defaultValue="BLOCK">
                  <option value="BLOCK">Bloquear</option>
                  <option value="ALLOW">Permitir</option>
                  <option value="DAILY_LIMIT">Límite diario</option>
                </Select>
              )}
            </Field>
            <div className={styles.twoCols}>
              <Field label="Hora de inicio">{(id) => <Input id={id} type="time" defaultValue="07:00" />}</Field>
              <Field label="Hora de fin" error="La hora de fin debe ser distinta.">
                {(id) => <Input id={id} type="time" defaultValue="07:00" />}
              </Field>
            </div>
            <Field label="Días" hint={describeDays(days)}>
              {() => <DayPicker value={days} onChange={setDays} />}
            </Field>
            <div className={styles.row}>
              <Switch checked={enabled} onChange={setEnabled} label="Horario escolar" />
              <span>Horario escolar {enabled ? "activado" : "desactivado"}</span>
            </div>
            <Button variant="secondary" size="sm" onClick={() => setDays(ALL_DAYS_MASK)}>
              Todos los días
            </Button>
          </div>
        </Card>

        <div className={styles.stack}>
          <Card>
            <CardHeader icon={Search} title="Búsqueda y periodo" />
            <Input icon={Search} placeholder="Buscar dispositivos, apps, reglas…" />
            <div className={styles.row}>
              <SegmentedControl
                label="Periodo"
                value={period}
                onChange={setPeriod}
                options={[
                  { value: "today", label: "Hoy" },
                  { value: "7d", label: "7 días" },
                  { value: "30d", label: "30 días" },
                ]}
              />
            </div>
          </Card>
          <Card padding="md">
            <EmptyState
              icon={Smartphone}
              title="Todavía no hay dispositivos vinculados"
              description="Genera un código de 6 dígitos desde Vinculación e ingrésalo en la app del dispositivo."
              action={
                <Button variant="primary" icon={Link2}>
                  Ir a Vinculación
                </Button>
              }
            />
          </Card>
          <Card padding="md">
            <EmptyState
              icon={ShieldCheck}
              illustration={<ShieldCheck size={72} strokeWidth={1} aria-hidden="true" />}
              title="Con ilustración opcional"
              description="La ilustración reemplaza al icono. Aquí es un trazo lineal de ejemplo, no un asset final."
            />
          </Card>
        </div>
      </div>

      <ConfirmDialog
        open={confirmOpen}
        title="¿Desvincular Samsung A54?"
        description="El dispositivo dejará de aplicar tus reglas y de reportar su estado."
        confirmLabel="Desvincular"
        onConfirm={() => setConfirmOpen(false)}
        onCancel={() => setConfirmOpen(false)}
      />
    </main>
  );
}
