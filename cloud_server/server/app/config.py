import os


DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql+psycopg://pantry:pantry@postgres:5432/pantry",
)


###### JSON Web Token - config #######
JWT_SECRET_KEY = os.getenv(
    "JWT_SECRET_KEY",
    "Pantry123!",
)

JWT_ALGORITHM = "HS256"

JWT_EXPIRE_MINUTES = int(
    os.getenv(
        "JWT_EXPIRE_MINUTES",
        "60",
    )
)
