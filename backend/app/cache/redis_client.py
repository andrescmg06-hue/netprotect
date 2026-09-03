import asyncio
import ssl
from urllib.parse import unquote, urlparse

from app.core.config import settings


class RedisHealthError(RuntimeError):
    """Raised when the configured Redis service cannot answer a health probe."""


def _encode_command(*parts: str) -> bytes:
    encoded = [part.encode("utf-8") for part in parts]
    chunks = [f"*{len(encoded)}\r\n".encode("ascii")]
    for item in encoded:
        chunks.append(f"${len(item)}\r\n".encode("ascii"))
        chunks.append(item + b"\r\n")
    return b"".join(chunks)


async def _read_simple_response(reader: asyncio.StreamReader) -> str:
    line = await asyncio.wait_for(reader.readline(), timeout=3)
    if not line:
        raise RedisHealthError("redis_connection_closed")

    prefix, payload = line[:1], line[1:].rstrip(b"\r\n")
    if prefix == b"+":
        return payload.decode("utf-8", errors="replace")
    if prefix == b"-":
        raise RedisHealthError("redis_command_failed")
    raise RedisHealthError("redis_unexpected_response")


async def ping_redis() -> None:
    parsed = urlparse(settings.redis_url)
    if parsed.scheme not in {"redis", "rediss"}:
        raise RedisHealthError("redis_invalid_scheme")

    host = parsed.hostname or "localhost"
    port = parsed.port or 6379
    tls_context: ssl.SSLContext | bool | None = None
    if parsed.scheme == "rediss":
        tls_context = ssl.create_default_context()

    try:
        reader, writer = await asyncio.wait_for(
            asyncio.open_connection(host=host, port=port, ssl=tls_context),
            timeout=3,
        )
    except (TimeoutError, OSError) as exc:
        raise RedisHealthError("redis_unavailable") from exc

    try:
        password = unquote(parsed.password) if parsed.password else None
        username = unquote(parsed.username) if parsed.username else None
        if password:
            auth_command = (
                _encode_command("AUTH", username, password)
                if username
                else _encode_command("AUTH", password)
            )
            writer.write(auth_command)
            await writer.drain()
            if await _read_simple_response(reader) != "OK":
                raise RedisHealthError("redis_auth_failed")

        writer.write(_encode_command("PING"))
        await writer.drain()
        if await _read_simple_response(reader) != "PONG":
            raise RedisHealthError("redis_unavailable")
    finally:
        writer.close()
        await writer.wait_closed()


async def close_redis() -> None:
    # Sprint 1 uses short-lived readiness probes and keeps no Redis connection pool.
    return None
