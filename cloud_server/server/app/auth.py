from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer, HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from .database import get_db, SessionLocal
from .models import User
from .security import decode_access_token

bearer_scheme = HTTPBearer()

def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User:
    token = credentials.credentials

    try:
        token_data = decode_access_token(token)

    except ValueError as exc:
        if str(exc) == "ACCESS_TOKEN_EXPIRED":
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail={
                    "code":"ACCESS_TOKEN_EXPIRED",
                    "message":"Access token expired",
                },
                headers={
                    "WWW-Authenticate": "Bearer",
                },
            )
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication token",
            headers={
                "WWW-Authenticate": "Bearer",
            },
        )
    

    user = db.get(User, token_data["user_id"])

    if user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found",
            headers={
                "WWW-Authenticate": "Bearer",
            },
        )

    if user.session_version != token_data["session_version"]:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Session has been revoked",
            
        )
    
    return user

async def validate_access_token_on_websocket(token: str, session_version: int) -> bool:
    try:
        token_data = decode_access_token(token)
    except ValueError:
        return False

    #make sure session version in token is same as registered
    #if session_version != int(token_data["session_version"]):
    #    return False

    db = SessionLocal()
    try:
        user = db.get(User, token_data["user_id"])    
        if user is None or user.session_version != session_version:
            return False

        return True
    finally:
        db.close()
