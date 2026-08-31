from app.cache.redis_client import _encode_command


def test_encode_ping_command_uses_resp_format() -> None:
    assert _encode_command("PING") == b"*1\r\n$4\r\nPING\r\n"


def test_encode_auth_command_does_not_add_plaintext_outside_payload() -> None:
    command = _encode_command("AUTH", "secret")
    assert command.startswith(b"*2\r\n$4\r\nAUTH\r\n$6\r\n")
    assert command.endswith(b"secret\r\n")
