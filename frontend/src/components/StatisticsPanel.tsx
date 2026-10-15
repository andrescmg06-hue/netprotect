"use client";

import { ChartNoAxesColumn, RefreshCw } from "lucide-react";
import { type ReactNode, useEffect, useId, useMemo, useState } from "react";

import { AppIcon, BarList, Button, DonutChart, EmptyState, ProgressBar, SegmentedControl, Spinner } from "@/components/ui";
import {
  ApiError,
  type AppliedRuleType,
  type DeviceStatisticsResponse,
  type StatisticsPeriod,
  getDeviceStatistics,
} from "@/lib/apiClient";
import { CATEGORIES, CATEGORY_LABELS } from "@/lib/categoryFormatting";
import { CHART_NEUTRAL, chartColor } from "@/lib/chartColors";

import styles from "./StatisticsPanel.module.css";

// `key` ties a result to the request that produced it ("<period>:<reloadToken>"). While it differs
// from the current request a fetch is in flight: the panel shows a loading state instead of the
// "sin datos" it used to flash, without a synchronous setState in the effect.
type StatisticsState =
  | { kind: "loading" }
  | { kind: "loaded"; key: string; data: DeviceStatisticsResponse }
  | { kind: "error"; key: string; message: string };

const PERIOD_LABELS: Record<StatisticsPeriod, string> = {
  today: "Hoy",
  "7d": "7 días",
  "30d": "30 días",
};

const PERIOD_PHRASE: Record<StatisticsPeriod, string> = {
  today: "hoy",
  "7d": "en los últimos 7 días",
  "30d": "en los últimos 30 días",
};

/** Why the device blocked an app, in the tutor's words (the shared ruleTypeLabel names the rule,
 * which reads oddly as a reason: "Bloquear", "Sin aprobar"). */
const BLOCK_REASON: Record<AppliedRuleType, string> = {
  ALLOW: "Permitida",
  BLOCK: "App bloqueada por una regla",
  DAILY_LIMIT: "Límite diario alcanzado",
  WEEKLY_LIMIT: "Límite semanal alcanzado",
  SCHEDULE: "Fuera del horario permitido",
  CATEGORY: "Regla de su categoría",
  SCHOOL_MODE: "Horario escolar",
  DEFAULT_POLICY: "App sin aprobar",
};

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function formatDuration(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  if (hours > 0) return minutes > 0 ? `${hours} h ${minutes} min` : `${hours} h`;
  if (minutes > 0) return `${minutes} min`;
  return totalSeconds > 0 ? "< 1 min" : "0 min";
}

function formatMinutes(minutes: number): string {
  return formatDuration(minutes * 60);
}

function formatRate(rate: number | null): string {
  return rate === null ? "sin datos" : `${Math.round(rate * 100)} %`;
}

/** range_start/range_end are server UTC dates ("YYYY-MM-DD", Sprint 16): formatted in UTC so a
 * tutor west of Greenwich doesn't see the day before. */
function formatRange(start: string, end: string): string {
  const format = (value: string, withYear: boolean) =>
    new Date(`${value}T00:00:00Z`).toLocaleDateString("es-CO", {
      day: "numeric",
      month: "short",
      ...(withYear ? { year: "numeric" } : {}),
      timeZone: "UTC",
    });
  return start === end ? format(end, true) : `${format(start, false)} – ${format(end, true)}`;
}

function plural(count: number, one: string, many: string): string {
  return `${count} ${count === 1 ? one : many}`;
}

/** Sprint 16 (logic), Sprint 37 (charts), Sprint 58 (editorial recomposition): on-the-fly
 * aggregates over data that already existed — DeviceApplicationUsage (apps más usadas / por
 * categoría), AppRuleEvent (bloqueos) and DAILY_LIMIT rules (cumplimiento) — read through GET
 * /devices/{id}/statistics. Read as a page, not a dashboard: one figure (total use, the exact sum
 * of categories[] — top_apps stops at ten) and then four sections on a two-column rail, label on
 * the left, data on the right, separated by hairlines. The response has no per-day or per-hour
 * series and no previous period, so there are no time charts or comparisons. */
export function StatisticsPanel({
  accessToken,
  deviceId,
}: {
  accessToken: string;
  deviceId: string;
}) {
  const [period, setPeriod] = useState<StatisticsPeriod>("today");
  const [state, setState] = useState<StatisticsState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);
  const requestKey = `${period}:${reloadToken}`;

  useEffect(() => {
    let cancelled = false;
    const key = `${period}:${reloadToken}`;

    getDeviceStatistics(accessToken, deviceId, period)
      .then((data) => {
        if (!cancelled) {
          setState({ kind: "loaded", key, data });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setState({
            kind: "error",
            key,
            message: describeError(error, "No se pudieron cargar las estadísticas"),
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, period, reloadToken]);

  const settled = state.kind !== "loading" && state.key === requestKey;
  const data = state.kind === "loaded" && settled ? state.data : null;

  const reading = useMemo(() => {
    if (!data) return null;
    const totalSeconds = data.categories.reduce((sum, entry) => sum + entry.total_seconds, 0);
    const totalBlocks = data.blocks_by_reason.reduce((sum, entry) => sum + entry.count, 0);
    const categories = data.categories
      .filter((entry) => entry.total_seconds > 0)
      .sort((a, b) => b.total_seconds - a.total_seconds)
      .map((entry) => ({
        key: entry.category ?? "SIN_CATEGORIA",
        label: entry.category ? CATEGORY_LABELS[entry.category] : "Sin categoría",
        value: entry.total_seconds,
        // Stable colour per category (its catalogue position), whatever its rank this period.
        color: entry.category ? chartColor(CATEGORIES.indexOf(entry.category)) : CHART_NEUTRAL,
      }));
    const blocks = [...data.blocks_by_reason].sort((a, b) => b.count - a.count);
    const appLabels = Object.fromEntries(
      data.top_apps.map((entry) => [entry.package_name, entry.app_label ?? entry.package_name])
    );
    return { totalSeconds, totalBlocks, categories, blocks, appLabels };
  }, [data]);

  function reload() {
    setReloadToken((current) => current + 1);
  }

  const periodControl = (
    <div className={styles.toolbar}>
      <div className={styles.periodGroup}>
        <SegmentedControl
          label="Periodo"
          value={period}
          onChange={setPeriod}
          options={(Object.keys(PERIOD_LABELS) as StatisticsPeriod[]).map((value) => ({
            value,
            label: PERIOD_LABELS[value],
          }))}
        />
        {data && <span className={styles.range}>{formatRange(data.range_start, data.range_end)}</span>}
      </div>
      <Button size="sm" variant="ghost" icon={RefreshCw} loading={!settled && state.kind !== "loading"} onClick={reload}>
        Actualizar
      </Button>
    </div>
  );

  if (!settled) {
    return (
      <div className={styles.panel}>
        {periodControl}
        <div className={styles.status} aria-busy="true">
          <Spinner label="Cargando estadísticas…" />
        </div>
      </div>
    );
  }

  if (state.kind === "error") {
    return (
      <div className={styles.panel}>
        {periodControl}
        <div className={styles.status}>
          <p className={styles.error}>{state.message}</p>
          <Button size="sm" icon={RefreshCw} onClick={reload}>
            Reintentar
          </Button>
        </div>
      </div>
    );
  }

  if (!data || !reading) return null;

  const nothingYet =
    reading.totalSeconds === 0 && data.top_apps.length === 0 && reading.blocks.length === 0 && data.compliance.length === 0;

  if (nothingYet) {
    return (
      <div className={styles.panel}>
        {periodControl}
        <div className={styles.status}>
          <EmptyState
            icon={ChartNoAxesColumn}
            title={`Sin actividad registrada ${PERIOD_PHRASE[period]}`}
            description="Cuando el dispositivo reporte uso de apps o aplique una regla, aquí verás cuánto tiempo se usó, en qué y qué se bloqueó."
          />
        </div>
      </div>
    );
  }

  const topCategory = reading.categories[0];
  const topShare = topCategory && reading.totalSeconds > 0 ? Math.round((topCategory.value / reading.totalSeconds) * 100) : 0;

  return (
    <div className={styles.panel}>
      {periodControl}

      <section className={styles.lead} aria-label="Tiempo de uso">
        <p className={`eyebrow ${styles.leadLabel}`}>Tiempo de uso</p>
        <div className={styles.leadBody}>
          <p className={styles.figure}>{formatDuration(reading.totalSeconds)}</p>
          <p className={styles.sentence}>
            {topCategory
              ? `Usado ${PERIOD_PHRASE[period]}; la mayor parte en ${topCategory.label} (${formatDuration(topCategory.value)}).`
              : `Sin uso reportado ${PERIOD_PHRASE[period]}.`}{" "}
            {reading.totalBlocks === 0
              ? "No se aplicó ningún bloqueo."
              : `El dispositivo aplicó ${plural(reading.totalBlocks, "bloqueo", "bloqueos")}.`}
          </p>
        </div>
      </section>

      <RailSection
        title="Apps más usadas"
        description={
          data.top_apps.length > 0
            ? `${data.top_apps.length === 1 ? "La app" : `Las ${data.top_apps.length} apps`} con más tiempo en el periodo.`
            : undefined
        }
      >
        {data.top_apps.length === 0 ? (
          <p className={styles.quiet}>Sin uso de apps reportado en este periodo.</p>
        ) : (
          <BarList
            items={data.top_apps.map((entry) => ({
              key: entry.package_name,
              label: (
                <span className={styles.appCell}>
                  <AppIcon label={entry.app_label ?? entry.package_name} seed={entry.package_name} />
                  <span className={styles.appLabel}>{entry.app_label ?? entry.package_name}</span>
                </span>
              ),
              value: entry.total_seconds,
              color: "var(--color-navy)",
            }))}
            formatValue={formatDuration}
          />
        )}
      </RailSection>

      <RailSection title="Por categoría" description="Todo el tiempo de uso, según la categoría de cada app.">
        {reading.categories.length === 0 ? (
          <p className={styles.quiet}>Sin uso de apps reportado en este periodo.</p>
        ) : (
          <div className={styles.donut}>
            <DonutChart
              label="Uso por categoría"
              segments={reading.categories}
              centerValue={`${topShare} %`}
              centerLabel={topCategory?.label}
              formatValue={formatDuration}
            />
          </div>
        )}
      </RailSection>

      <RailSection title="Bloqueos" description="Cuántas veces el dispositivo cerró una app, según el motivo.">
        {reading.blocks.length === 0 ? (
          <p className={styles.quiet}>Ningún bloqueo en este periodo.</p>
        ) : (
          <dl className={styles.ledger}>
            {reading.blocks.map((entry) => (
              <div key={entry.rule_type_applied} className={styles.ledgerRow}>
                <dt>{BLOCK_REASON[entry.rule_type_applied]}</dt>
                <dd>{entry.count}</dd>
              </div>
            ))}
            <div className={`${styles.ledgerRow} ${styles.ledgerTotal}`}>
              <dt>Total</dt>
              <dd>{reading.totalBlocks}</dd>
            </div>
          </dl>
        )}
      </RailSection>

      <RailSection
        title="Límites diarios"
        description="Días en que se respetó cada límite diario. Un día sin uso reportado no cuenta ni a favor ni en contra."
      >
        {data.compliance.length === 0 ? (
          <p className={styles.quiet}>No hay límites diarios configurados.</p>
        ) : (
          <div className={styles.complianceList}>
            {data.compliance.map((entry) => (
              <ProgressBar
                key={`${entry.scope}-${entry.package_name ?? entry.category}`}
                label={
                  entry.scope === "APP"
                    ? (reading.appLabels[entry.package_name ?? ""] ?? entry.package_name ?? "App")
                    : entry.category
                      ? CATEGORY_LABELS[entry.category]
                      : "Sin categoría"
                }
                detail={
                  entry.compliance_rate === null
                    ? `Sin datos · límite ${formatMinutes(entry.daily_limit_minutes)} al día`
                    : `${formatRate(entry.compliance_rate)} · ${entry.days_compliant} de ${plural(
                        entry.days_evaluated,
                        "día",
                        "días"
                      )} · límite ${formatMinutes(entry.daily_limit_minutes)} al día`
                }
                value={entry.compliance_rate}
                tone={entry.compliance_rate === null ? "neutral" : entry.compliance_rate >= 0.8 ? "success" : "warning"}
              />
            ))}
          </div>
        )}
      </RailSection>

      <p className={styles.footnote}>Los periodos se cuentan en días completos según la hora UTC del servidor.</p>
    </div>
  );
}

function RailSection({ title, description, children }: { title: string; description?: string; children: ReactNode }) {
  const titleId = useId();
  return (
    <section className={styles.section} aria-labelledby={titleId}>
      <div className={styles.sectionHead}>
        <h2 id={titleId} className={styles.sectionTitle}>
          {title}
        </h2>
        {description && <p className={styles.sectionDescription}>{description}</p>}
      </div>
      <div className={styles.sectionBody}>{children}</div>
    </section>
  );
}
