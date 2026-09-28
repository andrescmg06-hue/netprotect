"use client";

import { DashboardShell } from "@/components/DashboardShell";
import { LoginScreen } from "@/components/login/LoginScreen";
import { useAuth } from "@/contexts/AuthContext";

/** Auth gate: an authenticated tutor goes straight to DashboardShell; everyone else sees
 * `LoginScreen`. The pre-login screen no longer runs the Sprint 1 infrastructure check
 * (`/api/v1/health/ready`); the endpoint itself is unchanged in the backend.
 */
export default function Home() {
  const { status: authStatus, user, accessToken, signOut } = useAuth();

  if (authStatus === "authenticated" && user && accessToken) {
    return <DashboardShell accessToken={accessToken} user={user} onSignOut={() => void signOut()} />;
  }

  return <LoginScreen authStatus={authStatus === "loading" ? "loading" : "unauthenticated"} />;
}
