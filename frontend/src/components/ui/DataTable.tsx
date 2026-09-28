import type { ReactNode } from "react";

import styles from "./DataTable.module.css";

export type Column<T> = {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  align?: "left" | "right";
  /** The identifying column (a name, a package). Its label is hidden in the mobile card layout
   * because it already reads as the card's own title there. At most one per table. */
  primary?: boolean;
};

/** A real `<table>` (screen readers and browsers get the semantics for free) that turns into a
 * stack of cards below 720px: `data-label` on each `<td>` becomes its heading, `text-align: right`
 * columns are honored as columns are meant to be, not what a phone can offer, so nothing there
 * needs the label repeated in a table cell 400px wide.
 */
export function DataTable<T>({
  columns,
  rows,
  rowKey,
  onRowClick,
}: {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  onRowClick?: (row: T) => void;
}) {
  return (
    <div className={styles.wrapper}>
      <table className={styles.table}>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} className={column.align === "right" ? styles.right : undefined}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const key = rowKey(row);
            return (
              <tr
                key={key}
                className={onRowClick ? styles.clickable : undefined}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
              >
                {columns.map((column) => (
                  <td
                    key={column.key}
                    data-label={column.primary ? undefined : column.header}
                    className={[
                      column.align === "right" ? styles.right : "",
                      column.primary ? styles.primaryCell : "",
                    ]
                      .filter(Boolean)
                      .join(" ")}
                  >
                    {column.render(row)}
                  </td>
                ))}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
