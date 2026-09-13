"""Sprint 26: a Prometheus /metrics endpoint, deliberately hand-rolled on prometheus_client
directly instead of prometheus-fastapi-instrumentator — see requirements.txt for why that package
crashes every request on this project's pinned FastAPI 0.141.

Cardinality is bounded without depending on FastAPI 0.141's internal route-composition shape at
all (the same shape test_route_authorization_sweep.py already had to duck-type around in Sprint
25): `request.scope["route"].path` is only the matched route's *local* pattern relative to
whichever sub-router declared it (verified empirically — a request to /api/v1/health showed up
labelled "/health", missing the /api/v1/ prefix, and a deeper-nested route would lose even more of
its path), not the full mounted path a human would recognize. `path_label()` instead starts from
`request.url.path` — the real, already-fully-prefixed path actually requested — and swaps each
matched path parameter's captured value back out for its `{name}` placeholder, so
`/api/v1/devices/<uuid>/rules` becomes `/api/v1/devices/{device_id}/rules` regardless of how many
routers it passed through. Anything that never matched a route at all (a 404, a scanner probing
random paths) has no path params to substitute and falls back to a fixed placeholder instead of
the raw path, which is the one case that would otherwise let a caller mint unbounded label values.

Every path parameter in this project is typed `uuid.UUID` today, so `str(value)` always appears in
`request.url.path` verbatim — but `path_label()` does not assume that stays true forever
(/code-review flagged the earlier version, which did): if a future route's param string form ever
diverges from its raw URL segment, the substitution below falls back to the same fixed placeholder
rather than silently leaking that segment as an unbounded label value.
"""

import time

from prometheus_client import CONTENT_TYPE_LATEST, Counter, Histogram, generate_latest
from starlette.requests import Request
from starlette.responses import Response

REQUEST_COUNT = Counter(
    "http_requests_total",
    "Total HTTP requests received.",
    ["method", "path", "status"],
)
REQUEST_LATENCY = Histogram(
    "http_request_duration_seconds",
    "HTTP request latency in seconds.",
    ["method", "path"],
)

_UNMATCHED_PATH_LABEL = "unmatched"


def path_label(request: Request) -> str:
    route = request.scope.get("route")
    if route is None or not hasattr(route, "path"):
        return _UNMATCHED_PATH_LABEL

    path = request.url.path
    for name, value in request.path_params.items():
        token = str(value)
        if token not in path:
            return _UNMATCHED_PATH_LABEL
        path = path.replace(token, f"{{{name}}}", 1)
    return path


async def metrics_middleware(request: Request, call_next):
    start = time.perf_counter()
    response = await call_next(request)
    duration = time.perf_counter() - start

    path = path_label(request)
    REQUEST_COUNT.labels(request.method, path, response.status_code).inc()
    REQUEST_LATENCY.labels(request.method, path).observe(duration)
    return response


async def metrics_endpoint() -> Response:
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)
