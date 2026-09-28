"use client";

import {
  Banknote,
  Clapperboard,
  Gamepad2,
  GraduationCap,
  Hammer,
  MessageCircle,
  Newspaper,
  Plus,
  ShieldAlert,
  ShoppingCart,
  Tags,
  Trash2,
  Users,
  type LucideIcon,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";

import { RuleTypeFields, type RuleTypeFieldsValue, DEFAULT_RULE_TYPE_FIELDS, validateRuleTypeFields } from "@/components/RuleTypeFields";
import {
  Button,
  Card,
  CardHeader,
  type Column,
  DataTable,
  EmptyState,
  Field,
  Input,
  Select,
  Spinner,
  StatusBadge,
} from "@/components/ui";
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
import { describeRule, ruleTypeLabel } from "@/lib/ruleFormatting";

import styles from "./DeviceCategoriesPanel.module.css";

type AssignmentsState =
  | { kind: "loading" }
  | { kind: "loaded"; assignments: CategoryAssignment[] }
  | { kind: "error"; message: string };

type CategoryRulesState =
  | { kind: "loading" }
  | { kind: "loaded"; categoryRules: CategoryRule[] }
  | { kind: "error"; message: string };

const CATEGORIES: Category[] = [
  "SOCIAL_MEDIA",
  "GAMES",
  "STREAMING",
  "EDUCATION",
  "PRODUCTIVITY",
  "COMMUNICATION",
  "NEWS",
  "SHOPPING",
  "FINANCE",
  "UTILITIES",
  "ADULT_CONTENT",
];

const CATEGORY_LABELS: Record<Category, string> = {
  SOCIAL_MEDIA: "Redes sociales",
  GAMES: "Juegos",
  STREAMING: "Streaming",
  EDUCATION: "Educación",
  PRODUCTIVITY: "Productividad",
  COMMUNICATION: "Comunicación",
  NEWS: "Noticias",
  SHOPPING: "Compras",
  FINANCE: "Finanzas",
  UTILITIES: "Utilidades",
  ADULT_CONTENT: "Contenido para adultos",
};

const CATEGORY_ICONS: Record<Category, LucideIcon> = {
  SOCIAL_MEDIA: Users,
  GAMES: Gamepad2,
  STREAMING: Clapperboard,
  EDUCATION: GraduationCap,
  PRODUCTIVITY: Hammer,
  COMMUNICATION: MessageCircle,
  NEWS: Newspaper,
  SHOPPING: ShoppingCart,
  FINANCE: Banknote,
  UTILITIES: Tags,
  ADULT_CONTENT: ShieldAlert,
};

function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}

/** Sprint 10/24. Sprint 35: the rule form (type + minutes/schedule/days) now comes from the
 * shared RuleTypeFields instead of a copy of AppRulesPanel's.
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
  const assignmentCountByCategory = useMemo(() => {
    const counts = new Map<Category, number>();
    for (const assignment of assignments) {
      counts.set(assignment.category, (counts.get(assignment.category) ?? 0) + 1);
    }
    return counts;
  }, [assignments]);
  const ruleByCategory = useMemo(() => {
    const map = new Map<Category, CategoryRule>();
    for (const rule of categoryRules) map.set(rule.category, rule);
    return map;
  }, [categoryRules]);

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

  const assignmentColumns: Column<CategoryAssignment>[] = [
    { key: "package", header: "Aplicación", primary: true, render: (a) => a.package_name },
    { key: "category", header: "Categoría", render: (a) => CATEGORY_LABELS[a.category] },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (a) => (
        <Button size="sm" variant="ghost" icon={Trash2} onClick={() => handleUnassign(a.id)}>
          Quitar
        </Button>
      ),
    },
  ];

  const ruleColumns: Column<CategoryRule>[] = [
    { key: "category", header: "Categoría", primary: true, render: (r) => CATEGORY_LABELS[r.category] },
    {
      key: "rule",
      header: "Regla",
      render: (r) => (
        <span className={styles.ruleCell}>
          <StatusBadge tone={r.rule_type === "BLOCK" ? "danger" : r.rule_type === "ALLOW" ? "success" : "warning"}>
            {ruleTypeLabel(r.rule_type)}
          </StatusBadge>
          <span className={styles.ruleDetail}>{describeRule(r)}</span>
        </span>
      ),
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (r) => (
        <Button size="sm" variant="ghost" icon={Trash2} onClick={() => handleDeleteCategoryRule(r.id)}>
          Eliminar
        </Button>
      ),
    },
  ];

  return (
    <>
      <div className={styles.categoryGrid}>
        {CATEGORIES.map((category) => {
          const Icon = CATEGORY_ICONS[category];
          const rule = ruleByCategory.get(category);
          const count = assignmentCountByCategory.get(category) ?? 0;
          return (
            <div className={styles.categoryCard} key={category}>
              <span className={styles.categoryIcon}>
                <Icon size={20} strokeWidth={1.8} aria-hidden="true" />
              </span>
              <strong className={styles.categoryName}>{CATEGORY_LABELS[category]}</strong>
              <span className={styles.categoryCount}>
                {count} {count === 1 ? "app" : "apps"}
              </span>
              {rule ? (
                <StatusBadge tone={rule.rule_type === "BLOCK" ? "danger" : rule.rule_type === "ALLOW" ? "success" : "warning"}>
                  {ruleTypeLabel(rule.rule_type)}
                </StatusBadge>
              ) : (
                <StatusBadge tone="neutral">Sin regla</StatusBadge>
              )}
            </div>
          );
        })}
      </div>

      <div className={styles.columns}>
        <Card padding="none">
          <div className={styles.cardHead}>
            <CardHeader icon={Tags} title="Apps asignadas a una categoría" />
          </div>
          <form className={styles.inlineForm} onSubmit={handleAssign}>
            <Input
              value={packageName}
              onChange={(event) => setPackageName(event.target.value)}
              placeholder="com.instagram.android"
              maxLength={255}
              aria-label="Paquete a categorizar"
            />
            <Select
              value={assignCategory}
              onChange={(event) => setAssignCategory(event.target.value as Category)}
              aria-label="Categoría"
            >
              {CATEGORIES.map((category) => (
                <option key={category} value={category}>
                  {CATEGORY_LABELS[category]}
                </option>
              ))}
            </Select>
            <Button type="submit" variant="primary" icon={Plus} loading={assigning}>
              Asignar
            </Button>
          </form>

          {assignmentsState.kind === "loading" && (
            <div className={styles.emptyWrap}>
              <Spinner label="Cargando categorías…" />
            </div>
          )}
          {assignmentsState.kind === "error" && <p className={styles.error}>{assignmentsState.message}</p>}
          {assignmentsState.kind === "loaded" && assignments.length === 0 && (
            <div className={styles.emptyWrap}>
              <EmptyState icon={Tags} title="Ninguna app tiene categoría asignada todavía" />
            </div>
          )}
          {assignmentsState.kind === "loaded" && assignments.length > 0 && (
            <DataTable columns={assignmentColumns} rows={assignments} rowKey={(a) => a.id} />
          )}
        </Card>

        <Card>
          <CardHeader icon={ShieldAlert} title="Regla por categoría" />
          <form className={styles.form} onSubmit={handleSetCategoryRule}>
            <Field label="Categoría">
              {(id) => (
                <Select id={id} value={ruleCategory} onChange={(event) => setRuleCategory(event.target.value as Category)}>
                  {CATEGORIES.map((category) => (
                    <option key={category} value={category}>
                      {CATEGORY_LABELS[category]}
                    </option>
                  ))}
                </Select>
              )}
            </Field>

            <RuleTypeFields value={fields} onChange={updateFields} />

            <Button type="submit" variant="primary" loading={savingRule}>
              Guardar regla de categoría
            </Button>
          </form>
        </Card>
      </div>

      {formError && <p className={styles.error}>{formError}</p>}

      {rulesState.kind === "loading" && (
        <Card>
          <Spinner label="Cargando reglas de categoría…" />
        </Card>
      )}
      {rulesState.kind === "error" && (
        <Card>
          <p className={styles.error}>{rulesState.message}</p>
        </Card>
      )}
      {rulesState.kind === "loaded" && categoryRules.length > 0 && (
        <Card padding="none">
          <div className={styles.cardHead}>
            <CardHeader title="Reglas por categoría activas" />
          </div>
          <DataTable columns={ruleColumns} rows={categoryRules} rowKey={(r) => r.id} />
        </Card>
      )}
    </>
  );
}
