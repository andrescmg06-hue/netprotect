import { ApiError } from "@/lib/apiClient";

/** The backend's own message when the request reached it (an `ApiError` carries its `detail`),
 * otherwise the caller's Spanish fallback, so a network failure never shows a raw browser error.
 * Sprint 55: one copy for the redesigned views instead of the 13 per-panel duplicates
 * (docs/redesign/fase-0-informe.md §3); the other panels move to it in their own sprint. */
export function describeError(error: unknown, fallback: string): string {
  return error instanceof ApiError ? error.message : fallback;
}
