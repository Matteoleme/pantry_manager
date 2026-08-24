import hashlib
from pwdlib import PasswordHash

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
    value = f"{name}:{username}:{password[:3]}"
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


password_hash = PasswordHash.recommended()


def hash_password(password: str) -> str:
    return password_hash.hash(password)


def verify_password(password: str, hashed_password: str) -> bool:
    return password_hash.verify(password, hashed_password)
 
