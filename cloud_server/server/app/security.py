import hashlib
from pwdlib import PasswordHash
from datetime import datetime, timedelta, timezone
from jose import JWTError, jwt, ExpiredSignatureError
from fastapi import HTTPException, status

from .models import User

from .config import (
    JWT_ALGORITHM,
    JWT_EXPIRE_ACCESS_TOKEN_MINUTES,
    JWT_SECRET_KEY,
    JWT_EXPIRE_REFRESH_TOKEN_DAYS,
)


"""
from argon2 import PasswordHasher

pasword_hasher = PasswordHasher()

def hash_password(password: str) -> str:
    return pasword_hasher.hash(password)

def verify_password(password: str, password_hash: str) -> bool:
    try:
        pasword_hasher.verify(password_hash, password)
        return True
    except Exception:
        return False
""" 

########## create token_share ############
def generate_initial_token_share(name: str, username: str, password: str) -> str:
    #value = f"{name}:{username}:{password[:3]}"
    value = f"{name}:{username}"
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


########## password mngmt ##########
password_hash = PasswordHash.recommended()

def hash_password(password: str) -> str:
    return password_hash.hash(password)


def verify_password(password: str, hashed_password: str) -> bool:
    return password_hash.verify(password, hashed_password)
 
######### JWT token authentication ##########
### JWT contains user_id of authenticated user
### double access and refresh tokens system
def create_token(user: User, token_type: str, expires_delta: timedelta) -> str:
    expire = datetime.now(timezone.utc) + expires_delta
    payload = {
        "sub": str(user.id),
        "session_version": user.session_version,
        "type": token_type,
        "exp": expire,
    }
    return jwt.encode(
        payload,
        JWT_SECRET_KEY,
        algorithm=JWT_ALGORITHM,
    )

def create_access_token(user: User) -> str:
   
    return create_token(
        user,
        "access",
        timedelta(minutes=JWT_EXPIRE_ACCESS_TOKEN_MINUTES),
    )

def create_refresh_token(user: User) -> str:
   
    return create_token(
        user,
        "refresh",
        timedelta(days=JWT_EXPIRE_REFRESH_TOKEN_DAYS),
    )


def decode_access_token(token: str) -> dict:
    try:
        payload = jwt.decode(
            token,
            JWT_SECRET_KEY,
            algorithms=[JWT_ALGORITHM],
        )

        user_id = payload.get("sub")
        session_version = payload.get("session_version")
        token_type=payload.get("type")

        if user_id is None or session_version is None or token_type is None:
            raise ValueError("Invalid Token")

        if token_type != "access":
            raise ValueError("Not an Access Token")
        
        return {
            "user_id": int(user_id),
            "session_version": int(session_version),
        }
    
    except ExpiredSignatureError:
        ### alert your access token is expired ###
        raise ValueError("ACCESS_TOKEN_EXPIRED")
    
    except (JWTError, ValueError):
        raise ValueError("Invalid access token")

