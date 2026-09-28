"use client";

import { Ban, CalendarClock, Clock, History, Plus, ShieldCheck, Trash2 } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { RuleTypeFields, type RuleTypeFieldsValue, DEFAULT_RULE_TYPE_FIELDS, validateRuleTypeFields } from "@/components/RuleTypeFields";
import {
  AppIcon,
  Button,
  Card,
  CardHeader,
  type Column,
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
  type AppRule,
  type AppRuleEvent,
  type UpsertAppRuleInput,
  deleteAppRule,
  listAppRules,
  listRuleEvents,
  upsertAppRule,
} from "@/lib/apiClient";
import { describeRule, ruleTypeLabel } from "@/lib/ruleFormatting";
import { useDeviceRulesRealtime } from "@/lib/useDeviceRulesRealtime";

import styles from "./AppRulesPanel.module.css";

type RulesState =
  | { kind: "loading" }
  | { kind: "loaded"; rules: AppRule[] }
  | { kind: "error"; message: string };

type EventsState =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "loaded"; events: AppRuleEvent[] }
  | { kind: "error"; message: string };

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/** Sprint 24: split out of DeviceRulesPanel — this half is per-app rule CRUD plus the blocks
 * this device already applied; device-level settings (default policy, school mode) moved to
 * DevicePolicyPanel. Same endpoints, same upsert-by-package semantics as before.
 *
 * Sprint 35: the rule-type fields (type + minutes/schedule/days) move to the shared
 * RuleTypeFields, since DeviceCategoriesPanel needs the exact same form.
 */
export function AppRulesPanel({ accessToken, deviceId }: { accessToken: string; deviceId: string }) {
  const [rulesState, setRulesState] = useState<RulesState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);
  const [eventsState, setEventsState] = useState<EventsState>({ kind: "idle" });
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [packageName, setPackageName] = useState("");
  const [fields, setFields] = useState<RuleTypeFieldsValue>(DEFAULT_RULE_TYPE_FIELDS);
  const updateFields = useCallback((patch: Partial<RuleTypeFieldsValue>) => {
    setFields((current) => ({ ...current, ...patch }));
  }, []);

  useEffect(() => {
    let cancelled = false;

    listAppRules(accessToken, deviceId)
      .then(({ rules }) => {
        if (!cancelled) {
          setRulesState({ kind: "loaded", rules });
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setRulesState({ kind: "error", message: describeError(error, "No se pudieron cargar las reglas") });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, reloadToken]);

  const loadRules = useCallback(() => {
    setRulesState({ kind: "loading" });
    setReloadToken((current) => current + 1);
  }, []);

  // Sprint 18: stay live while this screen is open — another tutor session or the device's own
  // enforcement can change these rules at any time.
  useDeviceRulesRealtime(accessToken, deviceId, loadRules);

  const rules = useMemo(() => (rulesState.kind === "loaded" ? rulesState.rules : []), [rulesState]);
  const metrics = useMemo(
    () => ({
      total: rules.length,
      blocked: rules.filter((rule) => rule.rule_type === "BLOCK").length,
      limited: rules.filter((rule) => rule.rule_type === "DAILY_LIMIT" || rule.rule_type === "WEEKLY_LIMIT").length,
      scheduled: rules.filter((rule) => rule.rule_type === "SCHEDULE").length,
    }),
    [rules]
  );

  function toggleEvents() {
    if (eventsState.kind === "loaded" || eventsState.kind === "loading") {
      setEventsState({ kind: "idle" });
      return;
    }
    setEventsState({ kind: "loading" });
    listRuleEvents(accessToken, deviceId)
      .then(({ events }) => setEventsState({ kind: "loaded", events }))
      .catch((error) =>
        setEventsState({ kind: "error", message: describeError(error, "No se pudo cargar el historial") })
      );
  }

  function handleDelete(ruleId: string) {
    setFormError(null);
    deleteAppRule(accessToken, deviceId, ruleId)
      .then(() => loadRules())
      .catch((error) => setFormError(describeError(error, "No se pudo eliminar la regla")));
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);

    if (!packageName.trim()) {
      setFormError("El nombre del paquete es obligatorio (por ejemplo: com.instagram.android).");
      return;
    }

    const extras = validateRuleTypeFields(fields);
    if ("error" in extras) {
      setFormError(extras.error);
      return;
    }

    const input: UpsertAppRuleInput = {
      package_name: packageName.trim(),
      rule_type: fields.ruleType,
      ...extras,
    };

    setSubmitting(true);
    upsertAppRule(accessToken, deviceId, input)
      .then(() => {
        setSubmitting(false);
        setPackageName("");
        loadRules();
      })
      .catch((error) => {
        setSubmitting(false);
        setFormError(describeError(error, "No se pudo guardar la regla"));
      });
  }

  const ruleColumns: Column<AppRule>[] = [
    {
      key: "app",
      header: "Aplicación",
      primary: true,
      render: (rule) => (
        <span className={styles.appCell}>
          <AppIcon label={rule.package_name} seed={rule.package_name} />
          <span className={styles.appPackage}>{rule.package_name}</span>
        </span>
      ),
    },
    {
      key: "rule",
      header: "Regla",
      render: (rule) => (
        <span className={styles.ruleCell}>
          <StatusBadge tone={rule.rule_type === "BLOCK" ? "danger" : rule.rule_type === "ALLOW" ? "success" : rule.rule_type === "SCHEDULE" ? "purple" : "warning"}>
            {ruleTypeLabel(rule.rule_type)}
          </StatusBadge>
          <span className={styles.ruleDetail}>{describeRule(rule)}</span>
        </span>
      ),
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (rule) => (
        <Button size="sm" variant="ghost" icon={Trash2} onClick={() => handleDelete(rule.id)}>
          Eliminar
        </Button>
      ),
    },
  ];

  const eventColumns: Column<AppRuleEvent>[] = [
    {
      key: "app",
      header: "Aplicación",
      primary: true,
      render: (event) => (
        <span className={styles.appCell}>
          <AppIcon label={event.package_name} seed={event.package_name} />
          <span className={styles.appPackage}>{event.package_name}</span>
        </span>
      ),
    },
    { key: "type", header: "Regla aplicada", render: (event) => ruleTypeLabel(event.rule_type_applied) },
    {
      key: "when",
      header: "Fecha",
      align: "right",
      render: (event) => new Date(event.occurred_at).toLocaleString("es-CO"),
    },
  ];

  return (
    <>
      <MetricGrid>
        <MetricCard icon={ShieldCheck} tone="info" label="Reglas activas" value={metrics.total} />
        <MetricCard icon={Ban} tone="danger" label="Apps bloqueadas" value={metrics.blocked} />
        <MetricCard icon={Clock} tone="warning" label="Con límite de tiempo" value={metrics.limited} />
        <MetricCard icon={CalendarClock} tone="purple" label="Con horario" value={metrics.scheduled} />
      </MetricGrid>

      <div className={styles.columns}>
        <Card padding="none">
          <div className={styles.cardHead}>
            <CardHeader icon={ShieldCheck} title="Reglas activas" />
          </div>
          {rulesState.kind === "loading" && (
            <div className={styles.emptyWrap}>
              <Spinner label="Cargando reglas…" />
            </div>
          )}
          {rulesState.kind === "error" && <p className={styles.error}>{rulesState.message}</p>}
          {rulesState.kind === "loaded" && rules.length === 0 && (
            <div className={styles.emptyWrap}>
              <EmptyState icon={ShieldCheck} title="Todavía no hay reglas para este dispositivo" />
            </div>
          )}
          {rulesState.kind === "loaded" && rules.length > 0 && (
            <DataTable columns={ruleColumns} rows={rules} rowKey={(rule) => rule.id} />
          )}

          <div className={styles.historyToggle}>
            <Button size="sm" icon={History} onClick={toggleEvents}>
              {eventsState.kind === "loaded" || eventsState.kind === "loading"
                ? "Ocultar historial de bloqueos"
                : "Ver historial de bloqueos"}
            </Button>
          </div>

          {eventsState.kind === "loading" && (
            <div className={styles.emptyWrap}>
              <Spinner label="Cargando historial…" />
            </div>
          )}
          {eventsState.kind === "error" && <p className={styles.error}>{eventsState.message}</p>}
          {eventsState.kind === "loaded" && eventsState.events.length === 0 && (
            <p className={styles.historyEmpty}>Todavía no se aplicó ningún bloqueo en este dispositivo.</p>
          )}
          {eventsState.kind === "loaded" && eventsState.events.length > 0 && (
            <DataTable columns={eventColumns} rows={eventsState.events} rowKey={(event) => event.id} />
          )}
        </Card>

        <Card>
          <CardHeader icon={Plus} title="Crear nueva regla" />
          <form className={styles.form} onSubmit={handleSubmit}>
            <Field label="Aplicación (paquete)" hint="Ej. com.instagram.android">
              {(id) => (
                <Input
                  id={id}
                  value={packageName}
                  onChange={(event) => setPackageName(event.target.value)}
                  placeholder="com.instagram.android"
                  maxLength={255}
                />
              )}
            </Field>

            <RuleTypeFields value={fields} onChange={updateFields} />

            {formError && <p className={styles.error}>{formError}</p>}

            <Button type="submit" variant="primary" loading={submitting}>
              Guardar regla
            </Button>
          </form>
        </Card>
      </div>
    </>
  );
}
