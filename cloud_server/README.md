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
new user  (DELETED)

- `POST /pantries`  
new pantry

- `GET /pantry`  
retrieve pantry

- `POST /pantry-share-requests`  
create request to join a pantry
<!-- { "username": "alice11" } -->  


- `POST /pantry-share-requests/{request_id}/approve`
- `POST /pantry-share-requests/{request_id}/reject`  
approve/reject pantry join request

- `POST /auth/register`
- `POST /auth/login`  
register and login user  

- `GET /categories`  TODO<!---->
list all categories  

- `POST /categories`  
create new category
<!-- { "name": "breakfast" } -->  


- `POST /products`  
create new product  

- TODO
<!-- GET get pantry  !! (attach all products in list) -->
<!-- POST new product -->
GET list categories
<!--POST new categories -->
POST update product by diff quantity
POST eat(update products and events)(list of [product_id, quantity]) 
POST change_psw
POST change pantry to local (exit shared pantry)
GET get pending pantry share requests (owner of pantry)
GET daily statistics from events

- TODO 
<!-- modify product/event quantity integer -> float -->
add local field to user for changing pantry
<!-- modify request share based by username of creator -->
modify jwt expiration to unlimited
add standard categories by default on pantry creation (meat, drinks, vegetables, fish, others)

send notification on reaching kcal_threshold
send notification on request share pantry
