"use client";

import { Plus, Trash2, X } from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { RuleTypeFields, type RuleTypeFieldsValue, DEFAULT_RULE_TYPE_FIELDS, validateRuleTypeFields } from "@/components/RuleTypeFields";
import { Button, Field, Input, Select, Spinner, StatusBadge } from "@/components/ui";
import {
  ApiError,
  type Category,
  type CategoryAssignment,
  type CategoryRule,
  type UpsertCategoryRuleInput,
  deleteAppCategory,
  deleteCategoryRule,
  listAppCategories,
  listCategoryRules,
  upsertAppCategory,
  upsertCategoryRule,
} from "@/lib/apiClient";
import { CATEGORIES, CATEGORY_ICONS, CATEGORY_LABELS } from "@/lib/categoryFormatting";
import { describeRule, ruleTypeLabel, ruleTypeTone } from "@/lib/ruleFormatting";

import styles from "./DeviceCategoriesPanel.module.css";

type AssignmentsState =
  | { kind: "loading" }
  | { kind: "loaded"; assignments: CategoryAssignment[] }
  | { kind: "error"; message: string };

type CategoryRulesState =
  | { kind: "loading" }
  | { kind: "loaded"; categoryRules: CategoryRule[] }
  | { kind: "error"; message: string };

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/** Sprint 10/24. Sprint 35: the rule form (type + minutes/schedule/days) now comes from the
 * shared RuleTypeFields instead of a copy of AppRulesPanel's.
 *
 * Sprint 56: a collection with hierarchy instead of eleven identical tiles. Categories that hold
 * apps or carry a rule come first, larger, with their rule and their apps (each removable in
 * place); the rest are a quiet compact line. The two tables folded into that collection; the
 * assignment and category-rule forms and their handlers are unchanged.
 */
export function DeviceCategoriesPanel({
  accessToken,
  deviceId,
}: {
  accessToken: string;
  deviceId: string;
}) {
  const [assignmentsState, setAssignmentsState] = useState<AssignmentsState>({ kind: "loading" });
  const [assignmentsReloadToken, setAssignmentsReloadToken] = useState(0);
  const [rulesState, setRulesState] = useState<CategoryRulesState>({ kind: "loading" });
  const [rulesReloadToken, setRulesReloadToken] = useState(0);
  const [formError, setFormError] = useState<string | null>(null);

  const [packageName, setPackageName] = useState("");
  const [assignCategory, setAssignCategory] = useState<Category>(CATEGORIES[0]);
  const [assigning, setAssigning] = useState(false);

  const [ruleCategory, setRuleCategory] = useState<Category>(CATEGORIES[0]);
  const [fields, setFields] = useState<RuleTypeFieldsValue>(DEFAULT_RULE_TYPE_FIELDS);
  const updateFields = useCallback((patch: Partial<RuleTypeFieldsValue>) => {
    setFields((current) => ({ ...current, ...patch }));
  }, []);
  const [savingRule, setSavingRule] = useState(false);

  useEffect(() => {
    let cancelled = false;
    listAppCategories(accessToken, deviceId)
      .then(({ assignments }) => {
        if (!cancelled) setAssignmentsState({ kind: "loaded", assignments });
      })
      .catch((error) => {
        if (!cancelled) {
          setAssignmentsState({
            kind: "error",
            message: describeError(error, "No se pudieron cargar las categorías asignadas"),
          });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, assignmentsReloadToken]);

  useEffect(() => {
    let cancelled = false;
    listCategoryRules(accessToken, deviceId)
      .then(({ category_rules: categoryRules }) => {
        if (!cancelled) setRulesState({ kind: "loaded", categoryRules });
      })
      .catch((error) => {
        if (!cancelled) {
          setRulesState({
            kind: "error",
            message: describeError(error, "No se pudieron cargar las reglas de categoría"),
          });
        }
      });
    return () => {
      cancelled = true;
    };
  }, [accessToken, deviceId, rulesReloadToken]);

  const reloadAssignments = useCallback(() => {
    setAssignmentsState({ kind: "loading" });
    setAssignmentsReloadToken((current) => current + 1);
  }, []);

  const reloadRules = useCallback(() => {
    setRulesState({ kind: "loading" });
    setRulesReloadToken((current) => current + 1);
  }, []);

  const assignments = useMemo(
    () => (assignmentsState.kind === "loaded" ? assignmentsState.assignments : []),
    [assignmentsState]
  );
  const categoryRules = useMemo(
    () => (rulesState.kind === "loaded" ? rulesState.categoryRules : []),
    [rulesState]
  );
  const assignmentsByCategory = useMemo(() => {
    const byCategory = new Map<Category, CategoryAssignment[]>();
    for (const assignment of assignments) {
      byCategory.set(assignment.category, [...(byCategory.get(assignment.category) ?? []), assignment]);
    }
    return byCategory;
  }, [assignments]);
  const ruleByCategory = useMemo(() => {
    const map = new Map<Category, CategoryRule>();
    for (const rule of categoryRules) map.set(rule.category, rule);
    return map;
  }, [categoryRules]);

  // Hierarchy: categories in use first (most apps first, catalogue order otherwise), then the rest.
  const { inUse, unused } = useMemo(() => {
    const used = CATEGORIES.filter(
      (category) => (assignmentsByCategory.get(category)?.length ?? 0) > 0 || ruleByCategory.has(category)
    ).sort(
      (a, b) => (assignmentsByCategory.get(b)?.length ?? 0) - (assignmentsByCategory.get(a)?.length ?? 0)
    );
    return { inUse: used, unused: CATEGORIES.filter((category) => !used.includes(category)) };
  }, [assignmentsByCategory, ruleByCategory]);

  function handleAssign(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);
    if (!packageName.trim()) {
      setFormError("El nombre del paquete es obligatorio (por ejemplo: com.instagram.android).");
      return;
    }
    setAssigning(true);
    upsertAppCategory(accessToken, deviceId, packageName.trim(), assignCategory)
      .then(() => {
        setAssigning(false);
        setPackageName("");
        reloadAssignments();
      })
      .catch((error) => {
        setAssigning(false);
        setFormError(describeError(error, "No se pudo asignar la categoría"));
      });
  }

  function handleUnassign(assignmentId: string) {
    setFormError(null);
    deleteAppCategory(accessToken, deviceId, assignmentId)
      .then(() => reloadAssignments())
      .catch((error) => setFormError(describeError(error, "No se pudo quitar la categoría")));
  }

  function handleSetCategoryRule(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);

    const extras = validateRuleTypeFields(fields);
    if ("error" in extras) {
      setFormError(extras.error);
      return;
    }

    const input: UpsertCategoryRuleInput = { category: ruleCategory, rule_type: fields.ruleType, ...extras };

    setSavingRule(true);
    upsertCategoryRule(accessToken, deviceId, input)
      .then(() => {
        setSavingRule(false);
        reloadRules();
      })
      .catch((error) => {
        setSavingRule(false);
        setFormError(describeError(error, "No se pudo guardar la regla de categoría"));
      });
  }

  function handleDeleteCategoryRule(categoryRuleId: string) {
    setFormError(null);
    deleteCategoryRule(accessToken, deviceId, categoryRuleId)
      .then(() => reloadRules())
      .catch((error) => setFormError(describeError(error, "No se pudo eliminar la regla de categoría")));
  }

  const loading = assignmentsState.kind === "loading" || rulesState.kind === "loading";
  const categoryOptions = CATEGORIES.map((category) => (
    <option key={category} value={category}>
      {CATEGORY_LABELS[category]}
    </option>
  ));

  return (
    <div className={styles.page}>
      <section className={styles.collection} aria-labelledby="categories-title">
        <header className={styles.head}>
          <h2 id="categories-title" className={styles.title}>
            Categorías del dispositivo
          </h2>
          <p className={styles.lead}>
            Una regla de categoría se aplica a todas sus apps, salvo a las que ya tienen una regla
            propia en «Reglas por aplicación».
          </p>
        </header>

        {assignmentsState.kind === "error" && <p className={styles.error}>{assignmentsState.message}</p>}
        {rulesState.kind === "error" && <p className={styles.error}>{rulesState.message}</p>}

        {loading ? (
          <div className={styles.stateWrap}>
            <Spinner label="Cargando categorías…" />
          </div>
        ) : (
          <>
            {inUse.length > 0 ? (
              <ul className={styles.featured}>
                {inUse.map((category) => {
                  const Icon = CATEGORY_ICONS[category];
                  const rule = ruleByCategory.get(category);
                  const categoryApps = assignmentsByCategory.get(category) ?? [];
                  const count = categoryApps.length;
                  return (
                    <li key={category} className={styles.entry}>
                      <div className={styles.entryHead}>
                        <span className={styles.entryIcon}>
                          <Icon size={20} strokeWidth={1.6} aria-hidden="true" />
                        </span>
                        <h3 className={styles.entryName}>{CATEGORY_LABELS[category]}</h3>
                        <span className={styles.entryCount}>
                          <strong>{count}</strong> {count === 1 ? "app" : "apps"}
                        </span>
                      </div>

                      <div className={styles.entryRule}>
                        {rule ? (
                          <>
                            <span className={styles.ruleInfo}>
                              <StatusBadge tone={ruleTypeTone(rule.rule_type)} dot>
                                {ruleTypeLabel(rule.rule_type)}
                              </StatusBadge>
                              <span className={styles.ruleDetail}>{describeRule(rule)}</span>
                            </span>
                            <Button
                              size="sm"
                              variant="ghost"
                              icon={Trash2}
                              aria-label={`Eliminar la regla de ${CATEGORY_LABELS[category]}`}
                              onClick={() => handleDeleteCategoryRule(rule.id)}
                            >
                              Eliminar
                            </Button>
                          </>
                        ) : (
                          <span className={styles.noRule}>Sin regla propia: sus apps siguen el modo por defecto.</span>
                        )}
                      </div>

                      {count > 0 && (
                        <ul className={styles.apps} aria-label={`Apps en ${CATEGORY_LABELS[category]}`}>
                          {categoryApps.map((assignment) => (
                            <li key={assignment.id} className={styles.app}>
                              <span className={styles.appPackage}>{assignment.package_name}</span>
                              <button
                                type="button"
                                className={styles.removeApp}
                                aria-label={`Quitar ${assignment.package_name} de ${CATEGORY_LABELS[category]}`}
                                onClick={() => handleUnassign(assignment.id)}
                              >
                                <X size={14} strokeWidth={2} aria-hidden="true" />
                              </button>
                            </li>
                          ))}
                        </ul>
                      )}
                    </li>
                  );
                })}
              </ul>
            ) : (
              <p className={styles.quiet}>
                Todavía ninguna categoría tiene apps ni regla. Asigna una app o crea una regla abajo.
              </p>
            )}

            {unused.length > 0 && (
              <div className={styles.unused}>
                <h3 className={styles.unusedTitle}>Sin apps ni regla</h3>
                <ul className={styles.unusedList}>
                  {unused.map((category) => {
                    const Icon = CATEGORY_ICONS[category];
                    return (
                      <li key={category} className={styles.unusedItem}>
                        <Icon size={16} strokeWidth={1.6} aria-hidden="true" />
                        {CATEGORY_LABELS[category]}
                      </li>
                    );
                  })}
                </ul>
              </div>
            )}
          </>
        )}
      </section>

      {formError && (
        <p className={styles.error} role="alert">
          {formError}
        </p>
      )}

      <div className={styles.forms}>
        <section className={styles.formSection} aria-labelledby="categories-assign-title">
          <h2 id="categories-assign-title" className={styles.formTitle}>
            Asignar una app
          </h2>
          <p className={styles.lead}>Si la app ya tenía categoría, pasa a la nueva.</p>
          <form className={styles.form} onSubmit={handleAssign}>
            <Field label="Aplicación (paquete)">
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
            <Field label="Categoría">
              {(id) => (
                <Select
                  id={id}
                  value={assignCategory}
                  onChange={(event) => setAssignCategory(event.target.value as Category)}
                >
                  {categoryOptions}
                </Select>
              )}
            </Field>
            <Button type="submit" variant="secondary" icon={Plus} loading={assigning}>
              Asignar
            </Button>
          </form>
        </section>

        <section className={styles.formSection} aria-labelledby="categories-rule-title">
          <h2 id="categories-rule-title" className={styles.formTitle}>
            Regla por categoría
          </h2>
          <p className={styles.lead}>Si la categoría ya tiene una regla, esta la reemplaza.</p>
          <form className={styles.form} onSubmit={handleSetCategoryRule}>
            <Field label="Categoría">
              {(id) => (
                <Select id={id} value={ruleCategory} onChange={(event) => setRuleCategory(event.target.value as Category)}>
                  {categoryOptions}
                </Select>
              )}
            </Field>

            <RuleTypeFields value={fields} onChange={updateFields} />

            <Button type="submit" variant="primary" loading={savingRule}>
              Guardar regla de categoría
            </Button>
          </form>
        </section>
      </div>
    </div>
  );
}
