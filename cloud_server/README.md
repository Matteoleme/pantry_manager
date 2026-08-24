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


- `GET /health`  
get health status  

- `POST /users`  
new user  
- `POST /pantries`  
new pantry

- `POST /pantry-share-requests`  
create request to join a pantry

- `POST /pantry-share-requests/{request_id}/approve`
- `POST /pantry-share-requests/{request_id}/reject`  
approve/reject pantry join request

- ``
