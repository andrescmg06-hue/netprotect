import path from "node:path";
import type { NextConfig } from "next";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8000";
// The realtime channel (Sprint 18) opens a WebSocket against the same origin as the API, so
// connect-src has to name both schemes of that one origin.
const apiWebSocketUrl = apiBaseUrl.replace(/^http/, "ws");

// script-src/style-src still need 'unsafe-inline': Next.js App Router injects inline bootstrap
// scripts and styles, and removing that would take a nonce plumbed through middleware. Left as a
// deliberate, documented limit of this sprint (docs/sprint-21.md), not an oversight.
const contentSecurityPolicy = [
  "default-src 'self'",
  "script-src 'self' 'unsafe-inline'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: https://maps.gstatic.com https://maps.googleapis.com",
  "font-src 'self'",
  `connect-src 'self' ${apiBaseUrl} ${apiWebSocketUrl}`,
  // The tutor's location view (Sprint 13) embeds Google Maps in an iframe.
  "frame-src https://www.google.com",
  "frame-ancestors 'none'",
  "base-uri 'self'",
  "form-action 'self'",
  "object-src 'none'",
].join("; ");

const nextConfig: NextConfig = {
  output: "standalone",
  reactStrictMode: true,
  poweredByHeader: false,
  turbopack: {
    root: path.join(__dirname),
  },
  async headers() {
    return [
      {
        source: "/(.*)",
        headers: [
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "X-Frame-Options", value: "DENY" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
          { key: "Content-Security-Policy", value: contentSecurityPolicy },
          // Ignored by browsers over plain HTTP, so it is safe to send in every environment;
          // it only starts mattering once a real TLS terminator exists (Paso 25).
          { key: "Strict-Transport-Security", value: "max-age=63072000; includeSubDomains" }
        ]
      }
    ];
  }
};

export default nextConfig;
