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
``` return {"detail": "Username already exists"} (409)```  
``` return {"detail": "User created successfully"} ```  
``` return "detail": "Username and Name cannot contain ':'" ```  
<!-- test ok -->

- `POST /auth/login`  
``` return { "access_token": "...", "refresh_token": "...", "token_type": "bearer"} ```  
``` return {"detail": {"message": "Too many failed login attempts", "retry_after_minutes": "..."}} (429 TOO_MANY_REQUESTS) ```
<!-- test ok -->

- `POST /auth/logout`  
``` return {"status": "ok", "message": "Logged out successfully"} ```  
<!-- test ok -->
register / login (lockout for 15m after 5 attempts) / logout user  

- `POST /auth/refresh`  
refresh access token  
``` return {"access_token": "...", "token_type": "bearer"} ```  
<!-- test ok -->

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
``` return {"id": ..., "creator": "...", "kcal_threshold": ..., "users": [{"username": "..."}]} ```  
<!-- test ok --> 

- `POST /update-threshold`  
update the kcal-threshold for the user  
``` return {"id": ..., "creator": "...", "kcal_threshold": ..., "users": [{"username": "..."}]} ```   
<!-- test ok --> 

- `POST /pantry-share-requests`  
create request to join a pantry
<!--EX. { "username": "alice11" } -->  
``` return {"status": "ok", "message": "request sent successfully"} ```  
``` return {"detail": "A pending request already exists"}  ```  
``` return {"detail": "You cannot request access to your own pantry"}  ```  
<!-- test ok (NO NOTIFICATION) -->

- `GET /retrieve-pantry-share-requests`  
list all pending join requests sent to your pantry  
``` return {"detail"="You are not on your local pantry"} ```  
``` return {"requests": [{"id": ..., "requester_username": "...", "requester_name": "...", "status": "pending", "created_at": "..."}]} ```  
<!-- test ok -->

- `POST /pantry/leave`  
get back to your own local pantry (exit shared pantry)  
``` return {"detail": "You are already in your own pantry"} (400: bad request) ```  
``` return {"status": "ok", "message": "Returned to your own pantry", "local": true} ```
<!-- test ok --> 

- `POST /pantry/remove/{username}`  
remove user from your own pantry  
``` return {"detail":"You are not in your own pantry"} (400) ```  
``` return {"status": "ok", "message": "... removed successfully"} ```  
 ``` return {"detail": "User not found"} (404) ```
<!-- test ok -->

- `POST /pantry-share-requests/{request_id}/approve`  
``` return {"status": "ok", "message": "request accepted successfully"} ```  
``` return {"detail": "Request is no longer pending"} ```  
<!-- test ok --> 

- `POST /pantry-share-requests/{request_id}/reject`  
``` return {"status": "ok", "message": "request rejected successfully"} ```
``` return {"detail": "Request is no longer pending"} ```  
<!-- test ok --> 
approve / reject pantry join request  

- `GET /categories`  
list all categories in your current pantry  
``` return [{"name": "..."},{"name": "..."} ```  
<!-- test ok -->

- `POST /add_category`  
create new category
<!--EX. { "name": "breakfast" } -->  
``` return {"name": "..."} ```  
``` return {"detail": "Category already exists"} (409) ```  
<!-- test ok -->

- `DELETE /delete_category/{category_name}`  
delete category by name  
``` return {"status": "ok", "message": "category deleted successfully"} ```  
``` return {"detail": "Cannot delete category because products use it"} ```  
 <!-- test ok -->

- `POST /add_product`  
create new product  
``` return {"id": ..., "name": "...", "EAN": "...", "unit": "...", "quantity": "...", "category": "...", "kcal": ...} ```  
``` return {"detail": "Category not found"} ```  
``` return {"detail": "(PRODUCTS with same EAN MISMATCH)"} (409) ```  
<!-- test ok -->

- `DELETE /delete_product/{product_id}`  
delete product by id  
``` return {"status": "ok","message": "product deleted successfully"} ```  
<!-- test ok -->

- `POST /products/{product_id}/quantity`  
update product quantity  
<!-- EX. {"quantity": -5} -->  
``` return {"id": ..., "name": "...", "EAN": "...", "unit": "...", "quantity": "...", "category": "...", "kcal": ...} ``` 
``` return {"detail": "Insufficient quantity. Current quantity: ..., requested change: ..."} (400) ```  
<!-- test ok -->

- `GET /products/{product_id}`  
get product by id  
``` return {"id": ..., "name": "...", "EAN": "...", "unit": "...", "quantity": "...", "category": "...", "kcal": ...} ```  
``` return {"detail": "Product not found"} ```  
<!-- test ok -->

- `GET /all_products`  
list all products in your current pantry  
``` return [{"id": ..., "name": "...", "EAN": "...", "unit": "...", "quantity": "...", "category": "...", "kcal": ...}] ```  
<!-- test ok -->

- `POST /eat`  
eat a list of products and their quantities   
<!-- EX. {product_id, quantity},{product_id, quantity}... -->
``` return {"status": "ok", "message": "event created successfully", "kcal_threshold": "not_exceeded"} ```  
``` return {"status": "ok", "message": "event created successfully", "kcal_threshold": "exceeded: ... / ..."} ```  
``` return {"detail": "Insufficient quantity for product: ..."} (400) ``` 
<!-- test ok (NO NOTIFICATION) -->

- `GET stats/day`  
retrieve the total kcal reached so far during the day (divided by category) and the threshold (if set)  
``` return {"date": "YYYY-MM-DD", "total_kcal": "...", "threshold": ..., "categories": [{"category": "...", "kcal": "...", "percentage": "..."}]} ```  
<!-- test ok -->

- `GET stats/month`  
retrieve the total kcal reached every day in the last 30 days and the threshold (if set)  
``` return {"start_date": "...", "end_date": "...", "threshold": ..., "days": [{}, ..., { "date": "", "kcal": "..."}]} ```  
<!-- test ok -->


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
?? POST modify product (name, category)
?? GET events and threshold
<!-- POST remove from your pantry (by username) -->
<!-- GET /me retrieve user informations(id, name, username) -->
<!-- GET /all_products retrieve list of products with no pantry info -->
<!-- GET product_by_id (id in querystring) -->
<!-- POST logout invalidates token jwt -->

- TODO  
<!-- decomment TODO of notifications (notifications.py)  and fix initialization -->
<!-- add creator_id to event and kcal_threshold to users and tailor statistics for each user -->
<!-- add login attempt limit to 5 wrong attempts (modify users table with attempt_n, date_attempt) -->
<!-- modify pantry response to add list of usernames in pantry -->
<!-- modify product/event quantity integer -> float -->
<!-- add "local" field to user for changing pantry -->
<!-- modify request share based by username of pantry creator -->
<!-- modify add_product and add_category to return inserted item instead of pantry -->
<!-- modify get/pantry to retrieve only pantry informations (id, creator:username, kcal_threshold) -->
<!-- modify product_with_EAN in add_product check null-ean implementation -->
<!-- fix post/eat -->
<!-- fix delete category (gives conflicts when not) -->
<!-- fix retrieve share requests -->
<!-- fix username/name (':' not allowed) -->
<!-- modify jwt expiration to auto refresh allowing logout with stateless jwt -->
<!-- add standard categories by default on pantry creation (dairy, fruit, vegetables, meat, drinks, other) -->
<!-- only call pantry creation at user registration -->

<!-- send notification on reaching kcal_threshold (if != 0) -->
<!-- send notification on request share pantry -->

<!-- add live update websocket on product/category modifications -->
<!-- fix behavior on access_token change (logout/expiry)  -->
<!-- NO when doing something on main page, return updated pantry with list of products -->

<!-- check if needs Product.active.is_(True) -->