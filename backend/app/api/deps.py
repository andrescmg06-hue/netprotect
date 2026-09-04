from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.security import InvalidAccessTokenError, decode_access_token
from app.db.session import get_db
from app.models import User

_bearer_scheme = HTTPBearer(auto_error=False)


async def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(_bearer_scheme),
    db: AsyncSession = Depends(get_db),
) -> User:
    if credentials is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, detail="not_authenticated")

    try:
        user_id = decode_access_token(credentials.credentials)
    except InvalidAccessTokenError as exc:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, detail="invalid_access_token") from exc

    user = await db.get(User, user_id)
    if user is None or not user.is_active:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, detail="user_not_found_or_inactive")

    return user
