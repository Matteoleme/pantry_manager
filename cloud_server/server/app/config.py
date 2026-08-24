import os


DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql+psycopg://pantry:pantry@postgres:5432/pantry",
)
