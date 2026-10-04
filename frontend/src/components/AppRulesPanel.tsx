"use client";

import { Ban, Clock, History, ShieldCheck, Trash2 } from "lucide-react";
import { useCallback, useEffect, useId, useMemo, useState } from "react";

import { RuleTypeFields, type RuleTypeFieldsValue, DEFAULT_RULE_TYPE_FIELDS, validateRuleTypeFields } from "@/components/RuleTypeFields";
import { AppIcon, Button, EmptyState, Field, Input, Spinner, StatusBadge } from "@/components/ui";
import {
  ApiError,
  type AppRule,
  type AppRuleEvent,
  type DeviceApplication,
  type RuleType,
  type UpsertAppRuleInput,
  deleteAppRule,
  listAppRules,
  listDeviceApplications,
  listRuleEvents,
  upsertAppRule,
} from "@/lib/apiClient";
import { describeRule, ruleTypeLabel, ruleTypeTone } from "@/lib/ruleFormatting";
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

type RuleFilter = "ALL" | RuleType;

const FILTERS: { value: RuleFilter; label: string }[] = [
  { value: "ALL", label: "Todas" },
  { value: "BLOCK", label: "Bloquear" },
  { value: "ALLOW", label: "Permitir" },
  { value: "DAILY_LIMIT", label: "Límite diario" },
  { value: "WEEKLY_LIMIT", label: "Límite semanal" },
  { value: "SCHEDULE", label: "Horario" },
];

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/** A readable name for a package when the device's inventory has one that says more than the
 * package itself; otherwise null, so the row shows the package exactly once (the e2e relies on it). */
function readableLabel(labels: Map<string, string>, packageName: string): string | null {
  const label = labels.get(packageName)?.trim();
  return label && label !== packageName ? label : null;
}

/** Empty state drawing: a quiet shield with two of the things a rule can do beside it. */
function RulesIllustration() {
  return (
    <span className={styles.illustration} aria-hidden="true">
      <ShieldCheck className={styles.illusShield} size={64} strokeWidth={1.1} />
      <Ban className={styles.illusBlock} size={20} strokeWidth={1.5} />
      <Clock className={styles.illusLimit} size={20} strokeWidth={1.5} />
    </span>
  );
}

/** Sprint 24: split out of DeviceRulesPanel — this half is per-app rule CRUD plus the blocks
 * this device already applied; device-level settings (default policy, school mode) moved to
 * DevicePolicyPanel. Same endpoints, same upsert-by-package semantics as before.
 *
 * Sprint 35: the rule-type fields (type + minutes/schedule/days) move to the shared
 * RuleTypeFields, since DeviceCategoriesPanel needs the exact same form.
 *
 * Sprint 56: a control centre instead of four metric cards over a table — the rules are rows
 * filtered by their real type, the form sits beside them (below on a phone). The device's own
 * inventory (read-only, same endpoint as "Apps del dispositivo") gives each row a readable name
 * and the package field a list of installed apps to pick from; if it fails, rows show the package.
 */
export function AppRulesPanel({ accessToken, deviceId }: { accessToken: string; deviceId: string }) {
  const [rulesState, setRulesState] = useState<RulesState>({ kind: "loading" });
  const [reloadToken, setReloadToken] = useState(0);
  const [eventsState, setEventsState] = useState<EventsState>({ kind: "idle" });
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [filter, setFilter] = useState<RuleFilter>("ALL");
  const [applications, setApplications] = useState<DeviceApplication[]>([]);
  const appListId = useId();

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

  // Names only: a failure here leaves the rules readable by package, so it is deliberately silent.
  useEffect(() => {
    let cancelled = false;
    listDeviceApplications(accessToken, deviceId)
      .then(({ applications: apps }) => {
        if (!cancelled) setApplications(apps);
      })
      .catch(() => {
        if (!cancelled) setApplications([]);
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId]);

  const loadRules = useCallback(() => {
    setRulesState({ kind: "loading" });
    setReloadToken((current) => current + 1);
  }, []);

  // Sprint 18: stay live while this screen is open — another tutor session or the device's own
  // enforcement can change these rules at any time.
  useDeviceRulesRealtime(accessToken, deviceId, loadRules);

  const rules = useMemo(() => (rulesState.kind === "loaded" ? rulesState.rules : []), [rulesState]);
  const counts = useMemo(() => {
    const byType = new Map<RuleFilter, number>([["ALL", rules.length]]);
    for (const rule of rules) byType.set(rule.rule_type, (byType.get(rule.rule_type) ?? 0) + 1);
    return byType;
  }, [rules]);
  const visibleRules = useMemo(
    () => (filter === "ALL" ? rules : rules.filter((rule) => rule.rule_type === filter)),
    [rules, filter]
  );
  const labels = useMemo(
    () => new Map(applications.map((app) => [app.package_name, app.app_label])),
    [applications]
  );
  const pickableApps = useMemo(
    () =>
      applications
        .filter((app) => !app.uninstalled_at)
        .sort((a, b) => a.app_label.localeCompare(b.app_label, "es")),
    [applications]
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

  const historyOpen = eventsState.kind === "loaded" || eventsState.kind === "loading";

  return (
    <div className={styles.layout}>
      <section className={styles.rulesColumn} aria-labelledby="app-rules-title">
        <header className={styles.head}>
          <h2 id="app-rules-title" className={styles.title}>
            Reglas activas
          </h2>
          <p className={styles.lead}>
            Cada regla decide qué pasa con una app concreta. Lo que no tiene regla sigue el modo por
            defecto de la política del dispositivo.
          </p>
        </header>

        {rulesState.kind === "loaded" && rules.length > 0 && (
          <div className={styles.filters} role="group" aria-label="Filtrar por tipo de regla">
            {FILTERS.map((option) => {
              const count = counts.get(option.value) ?? 0;
              const active = filter === option.value;
              return (
                <button
                  key={option.value}
                  type="button"
                  aria-pressed={active}
                  className={active ? `${styles.filter} ${styles.filterActive}` : styles.filter}
                  onClick={() => setFilter(option.value)}
                >
                  {option.value !== "ALL" && (
                    <span className={`${styles.marker} ${styles[ruleTypeTone(option.value)]}`} aria-hidden="true" />
                  )}
                  {option.label}
                  <span className={styles.filterCount}>{count}</span>
                </button>
              );
            })}
          </div>
        )}

        {rulesState.kind === "loading" && (
          <div className={styles.stateWrap}>
            <Spinner label="Cargando reglas…" />
          </div>
        )}
        {rulesState.kind === "error" && <p className={styles.error}>{rulesState.message}</p>}
        {rulesState.kind === "loaded" && rules.length === 0 && (
          <EmptyState
            icon={ShieldCheck}
            illustration={<RulesIllustration />}
            title="Este dispositivo todavía no tiene reglas"
            description="Crea la primera con el formulario: elige la app y decide si se bloquea, se permite, tiene un límite o un horario. Mientras tanto se aplica el modo por defecto."
          />
        )}
        {rulesState.kind === "loaded" && rules.length > 0 && visibleRules.length === 0 && (
          <p className={styles.quiet}>No hay reglas de este tipo.</p>
        )}
        {rulesState.kind === "loaded" && visibleRules.length > 0 && (
          <ul className={styles.list}>
            {visibleRules.map((rule) => {
              const label = readableLabel(labels, rule.package_name);
              return (
                <li key={rule.id} className={styles.row}>
                  <AppIcon label={label ?? rule.package_name} seed={rule.package_name} />
                  <span className={styles.identity}>
                    {label ? (
                      <>
                        <span className={styles.appName}>{label}</span>
                        <span className={styles.appPackage}>{rule.package_name}</span>
                      </>
                    ) : (
                      <span className={styles.appName}>{rule.package_name}</span>
                    )}
                  </span>
                  <span className={styles.ruleInfo}>
                    <StatusBadge tone={ruleTypeTone(rule.rule_type)} dot>
                      {ruleTypeLabel(rule.rule_type)}
                    </StatusBadge>
                    <span className={styles.ruleDetail}>{describeRule(rule)}</span>
                  </span>
                  <span className={styles.rowAction}>
                    <Button
                      size="sm"
                      variant="ghost"
                      icon={Trash2}
                      aria-label={`Eliminar la regla de ${label ?? rule.package_name}`}
                      onClick={() => handleDelete(rule.id)}
                    >
                      Eliminar
                    </Button>
                  </span>
                </li>
              );
            })}
          </ul>
        )}

        <div className={styles.history}>
          <Button size="sm" variant="ghost" icon={History} onClick={toggleEvents} aria-expanded={historyOpen}>
            {historyOpen ? "Ocultar bloqueos aplicados" : "Ver bloqueos aplicados"}
          </Button>

          {eventsState.kind === "loading" && (
            <div className={styles.stateWrap}>
              <Spinner label="Cargando historial…" />
            </div>
          )}
          {eventsState.kind === "error" && <p className={styles.error}>{eventsState.message}</p>}
          {eventsState.kind === "loaded" && eventsState.events.length === 0 && (
            <p className={styles.quiet}>Todavía no se aplicó ningún bloqueo en este dispositivo.</p>
          )}
          {eventsState.kind === "loaded" && eventsState.events.length > 0 && (
            <ol className={styles.events}>
              {eventsState.events.map((event) => (
                <li key={event.id} className={styles.event}>
                  <time className={styles.eventTime} dateTime={event.occurred_at}>
                    {new Date(event.occurred_at).toLocaleString("es-CO")}
                  </time>
                  <span className={styles.eventApp}>
                    {readableLabel(labels, event.package_name) ?? event.package_name}
                  </span>
                  <span className={styles.eventType}>{ruleTypeLabel(event.rule_type_applied)}</span>
                </li>
              ))}
            </ol>
          )}
        </div>
      </section>

      <section className={styles.formColumn} aria-labelledby="app-rule-form-title">
        <h2 id="app-rule-form-title" className={styles.title}>
          Nueva regla
        </h2>
        <p className={styles.lead}>Si la app ya tiene una regla, esta la reemplaza.</p>
        <form className={styles.form} onSubmit={handleSubmit}>
          <Field label="Aplicación (paquete)" hint="Ej. com.instagram.android">
            {(id) => (
              <Input
                id={id}
                value={packageName}
                onChange={(event) => setPackageName(event.target.value)}
                placeholder="com.instagram.android"
                maxLength={255}
                list={pickableApps.length > 0 ? appListId : undefined}
                autoComplete="off"
              />
            )}
          </Field>
          {pickableApps.length > 0 && (
            <datalist id={appListId}>
              {pickableApps.map((app) => (
                <option key={app.package_name} value={app.package_name} label={app.app_label} />
              ))}
            </datalist>
          )}

          <RuleTypeFields value={fields} onChange={updateFields} />

          {formError && (
            <p className={styles.error} role="alert">
              {formError}
            </p>
          )}

          <Button type="submit" variant="primary" loading={submitting} fullWidth>
            Guardar regla
          </Button>
        </form>
      </section>
    </div>
  );
}
