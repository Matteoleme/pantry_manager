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

- `PUT /users/me/device`  
update device token FCM  

- `POST /test/fcm`  
test status of notification engine with FCM (send notification to yourself)  

- `POST /auth/register`
- `POST /auth/login`  
register / login user  

- `POST /auth/credentials`  
change password of user (must provide the old password besides being logged in)

- `GET /pantry`  
retrieve pantry

- `POST /pantry-share-requests`  
create request to join a pantry
<!--EX. { "username": "alice11" } -->  

- `GET /pantry-share-requests`  
list all pending join requests sent to your pantry  

- `POST /pantry/leave`  
get back to your own local pantry (exit shared pantry)  

- `POST /pantry-share-requests/{request_id}/approve`
- `POST /pantry-share-requests/{request_id}/reject`  
approve / reject pantry join request

- `GET /categories`  
list all categories in your current pantry 

- `POST /categories`  
create new category
<!--EX. { "name": "breakfast" } -->  

- `POST /products`  
create new product  

- `POST /products/{product_id}/quantity`  
update product quantity  
<!-- EX. {"quantity": -5} -->

- `POST /eat`  
eat a list of products and their quantities   
<!-- EX. {product_id, quantity},{product_id, quantity}... -->  

- `GET stats/day`  
retrieve the total kcal reached so far during the day (divided by category) and the threshold (if set)  

- `GET stats/month`  
retrieve the total kcal reached every day in the last 30 days and the threshold (if set)  

- `POST /update-threshold`  
update the kcal-threshold for your pantry 



- TODO
<!-- GET get pantry  !! (attach all products in list) -->
<!-- POST new product -->
<!-- GET list categories -->
<!--POST new categories -->
<!-- POST update product by diff quantity -->
POST delete product
POST modify product
?? POST delete category
?? GET events and threshold
<!-- POST modify kcal_threshold -->
<!-- POST eat(update products and events)(list of [product_id, quantity]) --> 
<!---- (NOT TO DO) POST edit event eat ---->
<!-- POST change_psw -->
<!-- POST change pantry to local (exit shared pantry) -->
<!-- GET get pending pantry share requests (only owner of pantry) -->
<!-- GET daily/monthly statistics from events -->

- TODO 
<!-- modify product/event quantity integer -> float -->
<!-- add "local" field to user for changing pantry -->
<!-- modify request share based by username of pantry creator -->
modify jwt expiration to unlimited
<!-- add standard categories by default on pantry creation (dairy, fruit, vegetables, meat, drinks, other) -->
<!-- only call pantry creation at user registration -->

<!-- send notification on reaching kcal_threshold (if != 0) -->
<!-- send notification on request share pantry -->
 
<!-- when doing something on main page, return updated pantry with list of products -->