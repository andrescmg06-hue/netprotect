import type { Metadata } from "next";
import { Inter, Playfair_Display } from "next/font/google";
import "./globals.css";

import { AuthProvider } from "@/contexts/AuthContext";

// Self-hosted at build time by next/font: the browser never contacts Google Fonts, so the
// Sprint 21 CSP (font-src 'self') needs no change.
const inter = Inter({ subsets: ["latin"], variable: "--font-inter", display: "swap" });

// The serif voice of titles (h1–h4, see globals.css). Playfair Display approximates the mockups'
// serif (decision D1 in docs/redesign/fase-0-informe.md is still open): only the 400/600 weights and
// the italic used by `.quote` are loaded, and swapping the face means editing this line only.
const playfair = Playfair_Display({
  subsets: ["latin"],
  variable: "--font-playfair",
  display: "swap",
  weight: ["400", "600"],
  style: ["normal", "italic"]
});

export const metadata: Metadata = {
  title: "NetProtect · Panel del tutor",
  description: "Plataforma de seguridad digital y control parental"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="es" className={`${inter.variable} ${playfair.variable}`}>
      <body>
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
