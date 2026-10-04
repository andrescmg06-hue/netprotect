import type { Metadata } from "next";
import { Inter, Newsreader } from "next/font/google";
import "./globals.css";

import { AuthProvider } from "@/contexts/AuthContext";

// Self-hosted at build time by next/font: the browser never contacts Google Fonts, so the
// Sprint 21 CSP (font-src 'self') needs no change.
const inter = Inter({ subsets: ["latin"], variable: "--font-inter", display: "swap" });

// The serif voice of titles (h1–h4, see globals.css). Newsreader replaced Playfair Display after three
// independent 1x renders showed Playfair's hairline strokes (the crossbar of the "e") vanishing, so
// titles read "Gcoccrcas". Decision D1 in docs/redesign/fase-0-informe.md stays open for the designer;
// swapping the face again means editing this constant and `--font-serif` in globals.css. Only the
// 400/500/600 weights and the italic used by `.quote` are loaded; 500 is the login title's weight
// (the mockup sits between 400 and 600) and the browser only downloads the weights a page uses.
const serif = Newsreader({
  subsets: ["latin"],
  variable: "--font-serif-face",
  display: "swap",
  weight: ["400", "500", "600"],
  style: ["normal", "italic"]
});

export const metadata: Metadata = {
  title: "NetProtect · Panel del tutor",
  description: "Plataforma de seguridad digital y control parental"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="es" className={`${inter.variable} ${serif.variable}`}>
      <body>
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
