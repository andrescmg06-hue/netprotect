"""Sprint 25: the route-authorization sweep the plan called for back in Sprint 4
(`docs/planning/plan-desarrollo.md`, Paso 3 — "una prueba que recorre el router comprobando que
ninguna ruta funcional queda sin dependencia de autorización") but that no sprint ever wrote.

Every other authorization test in this suite (test_authorization_integration.py,
test_rules_integration.py's "stranger tutor" cases, etc.) checks specific endpoints by hand. This
one instead walks the *live* FastAPI router, so a new endpoint added later without
`Depends(get_current_user)` (or a dependency built on it, like `require_role`/
`require_tutor_of_device`) fails this test on its own, without anyone having to remember to add a
case for it.

No database or network needed — importing `app.main` only builds the router, it doesn't connect to
anything (same reason `test_health.py` already runs in the `backend` CI job without
infrastructure) — so this stays out of the `integration` marker and runs on every `pytest -q`.
"""

from fastapi.routing import APIRoute

from app.api.deps import get_current_user
from app.main import app

# Explicitly public by design, and why — anything not listed here must depend on
# get_current_user, directly or through another dependency. Writing this sweep is what surfaced
# the last two: neither was an oversight, but neither had ever been written down as a deliberate
# exception before now.
# - GET /: a static "service: ok" banner (app/main.py's `root()`), nothing about any account.
# - /health*: a health check must not depend on the same auth stack it might be reporting on
#   (same "don't couple the watchdog to the thing it watches" reasoning already applied to the
#   global rate limiter in Sprint 21 — see CLAUDE.md).
# - POST /auth/google, POST /auth/refresh: the two entry points into a session. Requiring a
#   token that doesn't exist yet to obtain one would be circular.
# - POST /auth/logout: authenticates by *possessing* the refresh token being revoked (hashed and
#   looked up directly, see auth.py's `logout`), not by a bearer access token — deliberately so,
#   since the whole point of logout is to work even after the access token has already expired.
#   The refresh token itself is the secret an attacker would need, exactly as much as an access
#   token would be; there is no weaker check here, just a different one.
# - GET /metrics (Sprint 26): Prometheus scrapes this without an access token, the same reasoning
#   as /health* above — and unlike /health* it is never reachable from the internet in the first
#   place regardless (see app/main.py's comment where it's registered): no published host port on
#   backend, plus infra/caddy/Caddyfile explicitly 404s this one path before its own
#   `reverse_proxy` line, so only Prometheus itself, on the `private` Docker network, can ever
#   reach it.
_PUBLIC_ROUTES: frozenset[tuple[str, str]] = frozenset(
    {
        ("GET", "/"),
        ("GET", "/api/v1/health"),
        ("GET", "/api/v1/health/db"),
        ("GET", "/api/v1/health/redis"),
        ("GET", "/api/v1/health/ready"),
        ("POST", "/api/v1/auth/google"),
        ("POST", "/api/v1/auth/refresh"),
        ("POST", "/api/v1/auth/logout"),
        ("GET", "/metrics"),
    }
)

# Methods Starlette/FastAPI add or answer automatically, not endpoints anyone wrote by hand.
_SKIPPED_METHODS = {"HEAD", "OPTIONS"}


def _iter_routes(app):  # noqa: ANN001, ANN201 -- yields (path, methods, dependant) triples
    """Walks the app's route tree, resolving each route to its *full* mounted path.

    FastAPI 0.141 stopped flattening `include_router()` calls into `app.routes` — a router
    included into another shows up there as an internal wrapper (holding the un-prefixed,
    un-merged route beneath it) rather than as the resolved `APIRoute` a naive walk of
    `app.routes` would expect. That wrapper exposes `effective_route_contexts()`, which is the
    supported way to get the fully-resolved path/methods/dependant for everything nested under
    it — duck-typed here (checking for the method, not importing the private class) so this
    keeps working if that internal name changes in a future FastAPI release. Anything that is
    already a plain `APIRoute` at the top level (this app has none today, but `app.routes` could
    grow one) is handled directly instead of assumed away.
    """
    for route in app.routes:
        if isinstance(route, APIRoute):
            yield route.path, route.methods, route.dependant
        elif hasattr(route, "effective_route_contexts"):
            for ctx in route.effective_route_contexts():
                yield ctx.path, ctx.methods, ctx.dependant
        # Anything else (FastAPI's own /docs, /redoc, /openapi.json, generated as plain
        # Starlette Routes) is documentation, not a domain resource — out of scope here.


def _depends_on_get_current_user(dependant) -> bool:  # noqa: ANN001 -- fastapi.dependencies.models.Dependant
    """Recurses through the dependency graph FastAPI already built for this route: a route using
    `Depends(require_role(...))` has `get_current_user` as a *sub*-dependency of `require_role`'s
    inner check function, not as a direct one, so a shallow check would miss it.
    """
    if dependant.call is get_current_user:
        return True
    return any(_depends_on_get_current_user(sub) for sub in dependant.dependencies)


def test_every_functional_http_route_requires_authentication() -> None:
    """WebSocket routes are deliberately out of scope, not silently exempted: they authenticate
    via their first frame instead of FastAPI's dependency injection (browsers cannot set custom
    headers on a WebSocket handshake — see realtime.py's `_authenticate` docstring), and already
    have their own dedicated coverage in test_realtime_integration.py (missing-token, invalid-
    token, device-not-found and re-validation-on-reconnect cases).
    """
    unprotected: list[tuple[str, str]] = []
    checked = 0

    for path, methods, dependant in _iter_routes(app):
        for method in methods - _SKIPPED_METHODS:
            checked += 1
            if (method, path) in _PUBLIC_ROUTES:
                continue
            if not _depends_on_get_current_user(dependant):
                unprotected.append((method, path))

    # A regression in the sweep itself (e.g. the route tree resolving to nothing because of an
    # import order change) must not silently pass as "found nothing unprotected".
    assert checked > 30, f"the sweep only found {checked} routes — is the route tree empty?"
    assert unprotected == []
