import { Nunito } from "next/font/google";

import "./tailwind.css";

// Self-hosted at build time (same reasoning as the root layout's Inter): the browser never
// contacts Google Fonts, so this doesn't need a CSP change.
const nunito = Nunito({
  subsets: ["latin"],
  weight: ["400", "600", "700", "800"],
  variable: "--font-nunito",
  display: "swap",
});

export default function LoginMockupLayout({ children }: { children: React.ReactNode }) {
  return <div className={`${nunito.variable} font-display`}>{children}</div>;
}
