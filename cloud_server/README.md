# Pantry Manager — Cloud Backend

- FastAPI Python API
- PostgreSQL database
- SQLAlchemy ORM
- Docker Compose

## Support  
Only single device and login per user is supported, doing login in one device invalidates other device's previous login.  
See /docs for the endpoint usage information  

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
``` return {"status":"ok"} ```  
<!--test ok-->

- `PUT /users/me/device`  
update device token FCM for notifications  
``` {} ```  

- `POST /test/fcm`  
test status of notification engine with FCM (send notification to yourself)  
``` {} ```  

- `POST /auth/register`  
``` {} ```  
<!-- -->

- `POST /auth/login`  
``` return { "access_token": "...", "refresh_token": "...", "token_type": "bearer"} ```  
<!-- test ok -->

- `POST /auth/logout`  
``` {} ```  

register / login / logout user  


- `POST /auth/refresh`  
refresh access token  
``` {} ```  

- `POST /auth/change_password`  
change password of user (must provide the old password besides being logged in)  
``` return {"status": "ok", "message": "Password changed successfully"} ```
``` return {"detail": "Invalid username or password"} ```  
<!-- test ok -->

- `GET /me`  
get user informations  
``` return {"id": 1, "name": "gianlu", "username": "gianlu", "local": true} ```  
<!-- test ok --> 

- `GET /pantry`  
retrieve pantry informations (no associated list of products)  
``` return {"id": ..., "creator": "...", "kcal_threshold": ...} ```  
<!-- test ok --> 

- `POST /update-threshold`  
update the kcal-threshold for your pantry  
``` return {"id": ..., "creator": "...", "kcal_threshold": ...} ```  
<!-- test ok --> 

- `POST /pantry-share-requests`  
create request to join a pantry
<!--EX. { "username": "alice11" } -->  
``` {} ```  

- `GET /retrieve-pantry-share-requests`  
list all pending join requests sent to your pantry  
``` {} ```  
<!-- test  -->

- `POST /pantry/leave`  
get back to your own local pantry (exit shared pantry)  
``` return {"detail": "You are already in your own pantry"} (400: bad request)```  
``` return {} ``` 
<!-- test  --> 

- `POST /pantry-share-requests/{request_id}/approve`
- `POST /pantry-share-requests/{request_id}/reject`  
approve / reject pantry join request  
``` {} ```  

- `GET /categories`  
list all categories in your current pantry  
``` return [{"name": "..."},{"name": "..."} ```  
<!-- test ok -->

- `POST /add_category`  
create new category
<!--EX. { "name": "breakfast" } -->  
``` {} ```  

- `DELETE /delete_category/{category_name}`  
delete category by name  
``` return {"status": "ok", "message": "category deleted successfully"} ```  
<!-- test ok -->

- `POST /add_product`  
create new product  
``` {} ```  

- `DELETE /delete_product/{product_id}`  
delete product by id  
``` {} ```  

- `POST /products/{product_id}/quantity`  
update product quantity  
<!-- EX. {"quantity": -5} -->  
``` {} ```  

- `GET /products/{product_id}`  
get product by id  
``` {} ```  

- `GET /all_products`  
list all products in your current pantry  
``` {} ```  

- `POST /eat`  
eat a list of products and their quantities   
<!-- EX. {product_id, quantity},{product_id, quantity}... -->
``` {} ```  

- `GET stats/day`  
retrieve the total kcal reached so far during the day (divided by category) and the threshold (if set)  
``` {} ```  

- `GET stats/month`  
retrieve the total kcal reached every day in the last 30 days and the threshold (if set)  
``` {} ```  

- TODO
<!-- GET get pantry  !! (attach all products in list) -->
<!-- POST new product -->
<!-- GET list categories -->
<!--POST new categories -->
<!-- POST update product by diff quantity -->
<!-- POST modify kcal_threshold -->
<!-- POST eat(update products and events)(list of [product_id, quantity]) --> 
<!-- POST change_psw -->
<!-- POST change pantry to local (exit shared pantry) -->
<!-- GET get pending pantry share requests (only owner of pantry) -->
<!-- GET daily/monthly statistics from events -->
<!-- POST delete product (sets product.active to false) -->
<!-- POST delete category (sets category.active to false) only if no product uses it -->
?? POST modify product
?? GET events and threshold
<!-- GET /me retrieve user informations(id, name, username) -->
<!-- GET /all_products retrieve list of products with no pantry info -->
<!-- GET product_by_id (id in querystring) -->
<!-- POST logout invalidates token jwt -->

- TODO 
<!-- modify product/event quantity integer -> float -->
<!-- add "local" field to user for changing pantry -->
<!-- modify request share based by username of pantry creator -->
<!-- modify add_product and add_category to return inserted item instead of pantry -->
<!-- modify get/pantry to retrieve only pantry informations (id, creator:username, kcal_threshold) -->
<!-- modify product_with_EAN in add_product check null-ean implementation -->
?? add creator_id to event and tailor statistics for each user
<!-- fix post/eat -->
fix delete category (gives conflicts when not)
<!-- fix retrieve share requests -->
<!-- fix username/name (':' not allowed) -->
<!-- modify jwt expiration to auto refresh allowing logout with stateless jwt -->
<!-- add standard categories by default on pantry creation (dairy, fruit, vegetables, meat, drinks, other) -->
<!-- only call pantry creation at user registration -->

<!-- send notification on reaching kcal_threshold (if != 0) -->
<!-- send notification on request share pantry -->
 
<!-- NO when doing something on main page, return updated pantry with list of products -->

<!-- check if needs Product.active.is_(True) -->