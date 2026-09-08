import math

EARTH_RADIUS_METERS = 6_371_000.0


def haversine_distance_meters(
    lat1: float, lon1: float, lat2: float, lon2: float
) -> float:
    """Great-circle distance between two coordinates — the geofence containment check
    (Sprint 14) needs this instead of flat Euclidean distance because degrees of longitude
    shrink toward the poles; at Colombia's latitude the error would already be small, but the
    formula costs nothing extra and doesn't bake in an assumption that stops being true if this
    project is ever used somewhere else.
    """
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)

    a = (
        math.sin(delta_phi / 2) ** 2
        + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
    )
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return EARTH_RADIUS_METERS * c
