# Pantry Manager — Cloud Backend

- FastAPI Python API
- PostgreSQL database
- SQLAlchemy ORM
- Docker Compose

## Database schema

### users

| Column | Type | Constraints |
-------------------------------
| id | integer | primary key, auto-increment |
| name | varchar(255) | not null |
| username | varchar(100) | not null, unique |
| password | varchar(255) | not null |
| token_share | varchar(255) | not null |

### pantry

| Column | Type | Constraints |
-------------------------------
| id | integer | primary key, auto-increment |
| creator | integer | not null, foreign key → users.id |
| token_share | varchar(255) | not null, unique |

`creator` is indexed as part of the foreign-key relationship.

## Run
```bash
docker compose up --build
```

The API will be available at:

- http://localhost:8000
- http://localhost:8000/docs

## Current API

- `GET /health`
- `POST /users`
- `GET /users/{user_id}`
- `POST /pantries`

