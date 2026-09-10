"use client";

import type { CurrentUser } from "@/lib/apiClient";

/** Sprint 24: the account card that used to sit directly in page.tsx becomes its own dashboard
 * section — no new data, same CurrentUser the AuthContext already holds after login.
 */
export function AccountPanel({ user, onSignOut }: { user: CurrentUser; onSignOut: () => void }) {
  return (
    <div className="devicesPanel">
      <div className="devicesPanelHeader">
        <strong>Perfil y sesión</strong>
      </div>
      <div className="authCard" style={{ border: "none", padding: 0, marginTop: 12 }}>
        <div className="userRow">
          {user.avatar_url && (
            // eslint-disable-next-line @next/next/no-img-element
            <img className="avatar" src={user.avatar_url} alt="" />
          )}
          <div>
            <div className="userName">{user.display_name ?? user.email}</div>
            <div className="userEmail">{user.email}</div>
          </div>
        </div>
        <button type="button" onClick={onSignOut}>
          Cerrar sesión
        </button>
      </div>
    </div>
  );
}
