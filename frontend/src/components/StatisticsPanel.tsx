"use client";

import { AppWindow, Ban, ChartBar, ChartPie, Clock, Gauge, RefreshCw } from "lucide-react";
import { useEffect, useMemo, useState } from "react";

import {
  AppIcon,
  BarList,
  Button,
  ChartCard,
  DonutChart,
  MetricCard,
  MetricGrid,
  ProgressBar,
  SegmentedControl,
} from "@/components/ui";
import {
  ApiError,
  type DeviceStatisticsResponse,
  type StatisticsPeriod,
  getDeviceStatistics,
} from "@/lib/apiClient";
import { CATEGORIES, CATEGORY_LABELS } from "@/lib/categoryFormatting";
import { chartColor } from "@/lib/chartColors";
import { ruleTypeLabel } from "@/lib/ruleFormatting";

import styles from "./StatisticsPanel.module.css";

type StatisticsState =
  | { kind: "loading" }
  | { kind: "loaded"; data: DeviceStatisticsResponse }
  | { kind: "error"; message: string };

const PERIOD_LABELS: Record<StatisticsPeriod, string> = {
  today: "Hoy",
  "7d": "7 días",
  "30d": "30 días",
};

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

function formatDuration(totalSeconds: number): string {
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  if (hours > 0) return `${hours} h ${minutes} min`;
  if (minutes > 0) return `${minutes} min`;
  return "< 1 min";
}

function formatRate(rate: number | null): string {
  return rate === null ? "sin datos" : `${Math.round(rate * 100)}%`;
}

/** Sprint 16 (logic), Sprint 37 (design): on-the-fly aggregates over data that already existed —
 * DeviceApplicationUsage (apps más usadas / por categoría), AppRuleEvent (bloqueos) and
 * AppRule/CategoryRule DAILY_LIMIT rows (cumplimiento) — read through GET
 * /devices/{id}/statistics. Charts are ChartCard/DonutChart/BarList/ProgressBar (T4, hand-made
 * SVG, no library): the response is already aggregated lists per period, not a time series, so
 * these three shapes cover it without needing an axis or a real charting engine.
 */
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

  useEffect(() => {
    let cancelled = false;

    getDeviceStatistics(accessToken, deviceId, period)
      .then((data) => {
        if (!cancelled) {
          setState({ kind: "loaded", data });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setState({
            kind: "error",
            message: describeError(error, "No se pudieron cargar las estadísticas"),
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, period, reloadToken]);

  const data = state.kind === "loaded" ? state.data : null;

  const metrics = useMemo(() => {
    if (!data) return { totalSeconds: 0, appsWithUsage: 0, totalBlocks: 0, avgCompliance: null as number | null };
    const totalSeconds = data.categories.reduce((sum, entry) => sum + entry.total_seconds, 0);
    const totalBlocks = data.blocks_by_reason.reduce((sum, entry) => sum + entry.count, 0);
    const rates = data.compliance.map((entry) => entry.compliance_rate).filter((rate): rate is number => rate !== null);
    const avgCompliance = rates.length > 0 ? rates.reduce((sum, rate) => sum + rate, 0) / rates.length : null;
    return { totalSeconds, appsWithUsage: data.top_apps.length, totalBlocks, avgCompliance };
  }, [data]);

  const categorySegments = useMemo(
    () =>
      (data?.categories ?? []).map((entry) => {
        const index = entry.category ? CATEGORIES.indexOf(entry.category) : CATEGORIES.length;
        return {
          key: entry.category ?? "SIN_CATEGORIA",
          label: entry.category ? CATEGORY_LABELS[entry.category] : "Sin categoría",
          value: entry.total_seconds,
          color: chartColor(index),
        };
      }),
    [data]
  );

  return (
    <>
      <div className={styles.controls}>
        <SegmentedControl
          label="Periodo"
          value={period}
          onChange={setPeriod}
          options={(Object.keys(PERIOD_LABELS) as StatisticsPeriod[]).map((value) => ({
            value,
            label: PERIOD_LABELS[value],
          }))}
        />
        <Button size="sm" icon={RefreshCw} onClick={() => setReloadToken((current) => current + 1)}>
          Actualizar
        </Button>
      </div>

      {state.kind === "error" && <p className={styles.error}>{state.message}</p>}

      <MetricGrid>
        <MetricCard icon={Clock} tone="info" label="Tiempo total de uso" value={formatDuration(metrics.totalSeconds)} />
        <MetricCard icon={AppWindow} tone="purple" label="Apps con uso" value={metrics.appsWithUsage} />
        <MetricCard icon={Ban} tone="danger" label="Bloqueos" value={metrics.totalBlocks} />
        <MetricCard icon={Gauge} tone="success" label="Cumplimiento promedio" value={formatRate(metrics.avgCompliance)} />
      </MetricGrid>

      <div className={styles.columns}>
        <ChartCard
          icon={ChartPie}
          title="Uso por categoría"
          subtitle={PERIOD_LABELS[period]}
          isEmpty={data ? categorySegments.every((segment) => segment.value === 0) : true}
          emptyLabel="Sin datos de uso en este periodo"
        >
          <DonutChart
            label="Uso por categoría"
            segments={categorySegments}
            centerValue={formatDuration(metrics.totalSeconds)}
            centerLabel="total"
            formatValue={formatDuration}
          />
        </ChartCard>

        <ChartCard
          icon={ChartBar}
          title="Apps más usadas"
          subtitle={PERIOD_LABELS[period]}
          isEmpty={!data || data.top_apps.length === 0}
          emptyLabel="Sin datos de uso en este periodo"
        >
          <BarList
            items={(data?.top_apps ?? []).map((entry, index) => ({
              key: entry.package_name,
              label: (
                <span className={styles.appCell}>
                  <AppIcon label={entry.app_label ?? entry.package_name} seed={entry.package_name} />
                  <span className={styles.appLabel}>{entry.app_label ?? entry.package_name}</span>
                </span>
              ),
              value: entry.total_seconds,
              color: chartColor(index),
            }))}
            formatValue={formatDuration}
          />
        </ChartCard>
      </div>

      <div className={styles.columns}>
        <ChartCard
          icon={Ban}
          title="Bloqueos por tipo"
          subtitle={PERIOD_LABELS[period]}
          isEmpty={!data || data.blocks_by_reason.length === 0}
          emptyLabel="Ningún bloqueo registrado en este periodo"
        >
          <BarList
            items={(data?.blocks_by_reason ?? []).map((entry, index) => ({
              key: entry.rule_type_applied,
              label: ruleTypeLabel(entry.rule_type_applied),
              value: entry.count,
              color: chartColor(index),
            }))}
          />
        </ChartCard>

        <ChartCard
          icon={Gauge}
          title="Cumplimiento de límites diarios"
          isEmpty={!data || data.compliance.length === 0}
          emptyLabel="No hay reglas de límite diario configuradas"
        >
          <div className={styles.complianceList}>
            {(data?.compliance ?? []).map((entry) => (
              <ProgressBar
                key={`${entry.scope}-${entry.package_name ?? entry.category}`}
                label={entry.scope === "APP" ? entry.package_name : entry.category ? CATEGORY_LABELS[entry.category] : "Sin categoría"}
                detail={`${formatRate(entry.compliance_rate)} · ${entry.days_compliant}/${entry.days_evaluated} días (límite ${entry.daily_limit_minutes} min/día)`}
                value={entry.compliance_rate}
                tone={
                  entry.compliance_rate === null
                    ? "neutral"
                    : entry.compliance_rate >= 0.8
                      ? "success"
                      : entry.compliance_rate >= 0.5
                        ? "warning"
                        : "danger"
                }
              />
            ))}
          </div>
        </ChartCard>
      </div>
    </>
  );
}
