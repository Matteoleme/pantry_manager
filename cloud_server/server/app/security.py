import hashlib
from pwdlib import PasswordHash
from datetime import datetime, timedelta, timezone
from jose import JWTError, jwt

from .config import (
    JWT_ALGORITHM,
    JWT_EXPIRE_MINUTES,
    JWT_SECRET_KEY,
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
def create_access_token(user_id: int) -> str:
    expire = datetime.now(timezone.utc) + timedelta(
        minutes=JWT_EXPIRE_MINUTES
    )

    payload = {
        "sub": str(user_id),
        "exp": expire,
    }

    return jwt.encode(
        payload,
        JWT_SECRET_KEY,
        algorithm=JWT_ALGORITHM,
    )


def decode_access_token(token: str) -> int:
    try:
        payload = jwt.decode(
            token,
            JWT_SECRET_KEY,
            algorithms=[JWT_ALGORITHM],
        )

        subject = payload.get("sub")

        if subject is None:
            raise ValueError("Missing subject")

        return int(subject)

    except (JWTError, ValueError):
        raise ValueError("Invalid access token")
