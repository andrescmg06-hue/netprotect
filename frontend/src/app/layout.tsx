import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

import { AuthProvider } from "@/contexts/AuthContext";

// Self-hosted at build time by next/font: the browser never contacts Google Fonts, so the
// Sprint 21 CSP (font-src 'self') needs no change.
const inter = Inter({ subsets: ["latin"], variable: "--font-inter", display: "swap" });

export const metadata: Metadata = {
  title: "NetProtect · Panel del tutor",
  description: "Plataforma de seguridad digital y control parental"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="es" className={inter.variable}>
      <body>
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
