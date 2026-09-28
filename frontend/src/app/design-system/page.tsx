import { notFound } from "next/navigation";

import { Gallery } from "./Gallery";

/** Development-only catalogue of the ui/ components (Sprint 31), used to check each one in a real
 * browser before the views adopt them. A production build answers 404 here. */
export default function DesignSystemPage() {
  if (process.env.NODE_ENV === "production") {
    notFound();
  }
  return <Gallery />;
}
