import uuid
from datetime import datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.crypto import decrypt_coordinate
from app.core.geo import haversine_distance_meters
from app.models.geofence import ENTER, EXIT, Geofence, GeofenceEvent
from app.models.location import DeviceLocationReport


async def evaluate_geofence_transitions(
    db: AsyncSession,
    device_id: uuid.UUID,
    previous_report: DeviceLocationReport | None,
    new_latitude: float,
    new_longitude: float,
    occurred_at: datetime,
) -> list[GeofenceEvent]:
    """Detects ENTER/EXIT by comparing the device's previous known point against its new one for
    every geofence it has — no Android Geofencing API involved (see docs/sprint-14.md). Called
    inline from POST /devices/{id}/location, right after that endpoint's own purge and before its
    insert, same "evaluate at write time, no scheduler" pattern already used for retention.

    previous_report is None on a device's first-ever report (or its first report after every
    prior one aged out of the retention window): there is no earlier point to compare against, so
    no transition can be established — this silently sets the baseline rather than firing a
    spurious ENTER for a geofence the device already happened to be inside.
    """
    if previous_report is None:
        return []

    geofences = (
        await db.execute(select(Geofence).where(Geofence.device_id == device_id))
    ).scalars().all()
    if not geofences:
        return []

    previous_latitude = decrypt_coordinate(previous_report.latitude_ciphertext)
    previous_longitude = decrypt_coordinate(previous_report.longitude_ciphertext)

    events: list[GeofenceEvent] = []
    for geofence in geofences:
        center_latitude = decrypt_coordinate(geofence.latitude_ciphertext)
        center_longitude = decrypt_coordinate(geofence.longitude_ciphertext)

        was_inside = (
            haversine_distance_meters(
                previous_latitude, previous_longitude, center_latitude, center_longitude
            )
            <= geofence.radius_meters
        )
        is_inside = (
            haversine_distance_meters(
                new_latitude, new_longitude, center_latitude, center_longitude
            )
            <= geofence.radius_meters
        )

        if was_inside == is_inside:
            continue

        event = GeofenceEvent(
            device_id=device_id,
            geofence_id=geofence.id,
            geofence_name=geofence.name,
            event_type=ENTER if is_inside else EXIT,
            occurred_at=occurred_at,
        )
        db.add(event)
        events.append(event)

    return events
