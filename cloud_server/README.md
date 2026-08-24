# Pantry Manager — Cloud Backend

- FastAPI Python API
- PostgreSQL database
- SQLAlchemy ORM
- Docker Compose

## Run
```bash
docker compose up --build
```

The API will be available at:

- http://localhost:8000
- http://localhost:8000/docs

## Current API

get health status
- `GET /health`  
new user
- `POST /users`  
new pantry
- `POST /pantries`  
create request to join a pantry
- `POST /pantry-share-requests`  
approve/reject pantry join request
- `POST /pantry-share-requests/{request_id}/approve`
- `POST /pantry-share-requests/{request_id}/reject`  

- ``
