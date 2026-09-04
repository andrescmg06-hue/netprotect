from datetime import UTC, datetime

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.deps import get_current_user
from app.core.config import settings
from app.core.security import (
    create_access_token,
    generate_refresh_token,
    hash_refresh_token,
    refresh_token_expiry,
)
from app.db.session import get_db
from app.models import User, UserSession
from app.schemas.auth import (
    CurrentUserResponse,
    GoogleLoginRequest,
    LogoutRequest,
    RefreshRequest,
    TokenPairResponse,
)
from app.services.audit import record_audit_event
from app.services.google_auth import InvalidGoogleTokenError, verify_google_id_token

router = APIRouter(prefix="/auth", tags=["auth"])


def _client_ip(request: Request) -> str | None:
    return request.client.host if request.client else None


async def _get_or_create_user(db: AsyncSession, *, google_sub: str, email: str,
                               display_name: str | None, avatar_url: str | None) -> User:
    result = await db.execute(select(User).where(User.google_sub == google_sub))
    user = result.scalar_one_or_none()

    if user is not None:
        user.email = email
        user.display_name = display_name
        user.avatar_url = avatar_url
        return user

    user = User(
        email=email,
        google_sub=google_sub,
        display_name=display_name,
        avatar_url=avatar_url,
    )
    db.add(user)
    await db.flush()
    return user


async def _issue_token_pair(db: AsyncSession, user: User) -> TokenPairResponse:
    access_token, _ = create_access_token(user.id)
    refresh_token = generate_refresh_token()

    db.add(
        UserSession(
            user_id=user.id,
            refresh_token_hash=hash_refresh_token(refresh_token),
            expires_at=refresh_token_expiry(),
        )
    )

    return TokenPairResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        expires_in=settings.access_token_ttl_minutes * 60,
    )


@router.post("/google", response_model=TokenPairResponse)
async def login_with_google(
    payload: GoogleLoginRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
) -> TokenPairResponse:
    try:
        identity = verify_google_id_token(payload.id_token)
    except InvalidGoogleTokenError as exc:
        raise HTTPException(
            status.HTTP_401_UNAUTHORIZED, detail="invalid_google_token"
        ) from exc

    user = await _get_or_create_user(
        db,
        google_sub=identity.google_sub,
        email=identity.email,
        display_name=identity.display_name,
        avatar_url=identity.avatar_url,
    )

    tokens = await _issue_token_pair(db, user)

    await record_audit_event(
        db, actor_user_id=user.id, action="LOGIN", ip_address=_client_ip(request)
    )
    await db.commit()

    return tokens


@router.post("/refresh", response_model=TokenPairResponse)
async def refresh_tokens(
    payload: RefreshRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
) -> TokenPairResponse:
    token_hash = hash_refresh_token(payload.refresh_token)
    result = await db.execute(
        select(UserSession).where(UserSession.refresh_token_hash == token_hash)
    )
    session_row = result.scalar_one_or_none()

    now = datetime.now(UTC)
    if (
        session_row is None
        or session_row.revoked_at is not None
        or session_row.expires_at <= now
    ):
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, detail="invalid_refresh_token")

    user = await db.get(User, session_row.user_id)
    if user is None or not user.is_active:
        raise HTTPException(
            status.HTTP_401_UNAUTHORIZED, detail="user_not_found_or_inactive"
        )

    session_row.revoked_at = now
    tokens = await _issue_token_pair(db, user)

    await record_audit_event(
        db, actor_user_id=user.id, action="TOKEN_REFRESH", ip_address=_client_ip(request)
    )
    await db.commit()

    return tokens


@router.post("/logout", status_code=status.HTTP_204_NO_CONTENT)
async def logout(
    payload: LogoutRequest,
    request: Request,
    db: AsyncSession = Depends(get_db),
) -> None:
    token_hash = hash_refresh_token(payload.refresh_token)
    result = await db.execute(
        select(UserSession).where(UserSession.refresh_token_hash == token_hash)
    )
    session_row = result.scalar_one_or_none()

    if session_row is not None and session_row.revoked_at is None:
        session_row.revoked_at = datetime.now(UTC)
        await record_audit_event(
            db,
            actor_user_id=session_row.user_id,
            action="LOGOUT",
            ip_address=_client_ip(request),
        )

    await db.commit()


@router.get("/me", response_model=CurrentUserResponse)
async def get_me(current_user: User = Depends(get_current_user)) -> CurrentUserResponse:
    return CurrentUserResponse(
        id=current_user.id,
        email=current_user.email,
        display_name=current_user.display_name,
        avatar_url=current_user.avatar_url,
    )
