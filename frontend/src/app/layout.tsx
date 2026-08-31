import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "NetProtect",
  description: "Plataforma de seguridad digital y control parental"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="es">
      <body>{children}</body>
    </html>
  );
}
