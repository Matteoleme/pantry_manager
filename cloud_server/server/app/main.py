from fastapi import FastAPI, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy import func
from datetime import datetime, timezone, time, date, timedelta
from decimal import Decimal

from .database import Base, engine, get_db
from .models import (
    Event,
    Pantry,
    PantryShareRequest,
    User,
    Category,
    Product,
)
from .schemas import (
    EventCreate,
    EventProduct,
    CategoryKcalPercentage,
    DailyKcalStats,
    MonthStatsResponse,
    DayStatsResponse,
    PantryCreate,
    PantryResponse,
    PantryThresholdModify,
    PantryShareRequestCreate,
    PantryShareRequestResponse,
    PantryShareRequestResponseInfo,
    PantryShareRequestListResponse,
    UserCreate,
    UserResponse,
    UserUsername,
    DeviceTokenUpdate,
    CategoryCreate,
    CategoryResponse,
    ProductCreate,
    ProductResponse,
    ProductQuantityUpdate,
    TokenResponse,
    RefreshTokenRequest,
    LoginRequest,
    ChangePasswordRequest,

)
from .notifications import (
    send_pantry_share_notification,
    send_kcal_t_reached_notification,
    send_kcal_t_reached_notification_single,
)

#### security and user authentication
from .auth import get_current_user
from .security import (
    create_access_token,
    create_refresh_token,
    generate_initial_token_share,
    hash_password,
    verify_password,
)
from jose import JWTError, jwt
from .config import (
    JWT_ALGORITHM,
    JWT_EXPIRE_ACCESS_TOKEN_MINUTES,
    JWT_SECRET_KEY,
    JWT_EXPIRE_REFRESH_TOKEN_DAYS,
)

LOCKOUT_ATTEMPTS = 5
LOCKOUT_DURATION = timedelta(minutes=15)

#create DB
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Pantry Manager API",
    version="0.1.0",
)


@app.get("/health")
def health():
    return {"status": "ok"}

DEFAULT_CATEGORIES = [
    "Dairy",
    "Fruit",
    "Vegetables",
    "Meat",
    "Drinks",
    "Other",
]

########## USER registration (and new local pantry creation) ##########
@app.post(
    "/auth/register",
    #response_model=UserResponse,
    status_code=status.HTTP_201_CREATED,
)
def register(payload: UserCreate, db: Session = Depends(get_db)):
    existing_user = (
        db.query(User)
        .filter(User.username == payload.username)
        .first()
    )
    #check username if exists
    if existing_user:
        raise HTTPException(
            status_code=409,
            detail="Username already exists",
        )

    #check format username/name
    if ":" in payload.name or ":" in payload.username:
        raise HTTPException(
            status_code=400,
            detail="Username and Name cannot contain ':'",
        )

    ### generate token_share
    token_share = generate_initial_token_share(
        payload.name,
        payload.username,
        payload.password,
    )
    
    ### CREATE USER
    user = User(
        name=payload.name,
        username=payload.username,
        password=hash_password(payload.password),
        token_share=token_share,
        own_token_share = token_share,
    )
    db.add(user)

    db.flush()
    # this forces db to populate id and other fields without committing yet

    ###CREATE user PANTRY
    pantry = Pantry(
        creator = user.id,
        token_share = token_share,
        kcal_threshold = 0,
    )
    db.add(pantry)
    db.flush()

    ###CREATE default categories
    for category_name in DEFAULT_CATEGORIES:
        category = Category(
            name=category_name,
            token_share=token_share,
        )

        db.add(category)

    db.commit()
    db.refresh(user)

    return {
        "detail": "User created successfully",
    }

########## USER login ##########
@app.post(
    "/auth/login",
    response_model=TokenResponse,
)
def login(
    payload: LoginRequest,
    db: Session = Depends(get_db),
):
    user = (
        db.query(User)
        .filter(User.username == payload.username)
        .first()
    )

    if user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid username or password",
        )

    now = datetime.now(timezone.utc)
    #check if account locked
    if (
        user.login_attempt_n >= LOCKOUT_ATTEMPTS
        and user.login_date_last_attempt is not None
    ):
        lockout_until = user.login_date_last_attempt + LOCKOUT_DURATION
        if now < lockout_until:
            print("already locked")
            remaining_seconds = int((lockout_until - now).total_seconds())
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail={
                    "message": "Too many failed login attempts",
                    "retry_after_minutes": f"{(remaining_seconds/60):.2f}",
                },
                headers={"Retry-after": f"{(remaining_seconds/60):.2f}"},
            )
        #lockout expired
        user.login_attempt_n = 0
        user.login_date_last_attempt = None

    # check password
    if not verify_password(
        payload.password,
        user.password,
    ):
        user.login_attempt_n += 1
        user.login_date_last_attempt = now

        db.commit()

        if user.login_attempt_n >= LOCKOUT_ATTEMPTS:
            print("just locked")
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail={
                    "message": "Too many failed login attempts",
                    "retry_after_minutes": "30",
                },
                headers={"Retry-after": "30"},
            )
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid username or password",
        )

    #login successfull (reset counter)
    user.login_attempt_n = 0
    user.login_date_last_attempt = None
    db.commit()

    access_token = create_access_token(user)
    refresh_token = create_refresh_token(user)

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="bearer",
    )

########## USER refresh access_token ##########
@app.post("/auth/refresh")
def refresh(
    payload: RefreshTokenRequest,
    db: Session = Depends(get_db),
):
    try:
        decoded = jwt.decode(
            payload.refresh_token,
            JWT_SECRET_KEY,
            algorithms=JWT_ALGORITHM,
        )

        user_id = decoded.get("sub")
        session_version = decoded.get("session_version")
        token_type = decoded.get("type")

        if user_id is None or session_version is None or token_type is None:
            raise ValueError("Invalid refresh token")

    except(JWTError, ValueError, TypeError):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid refresh token",
        )

    user = db.get(User, int(user_id))
    if user is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User not found",
        )

    # Invalidates refresh tokens after logout/password change
    if user.session_version != int(session_version):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token has been revoked",
        )

    new_access_token = create_access_token(user)

    return {
        "access_token":new_access_token,
        "token_type":"bearer",
    }


########## USER logout ##########
@app.post(
    "/auth/logout",
    #response_model=TokenResponse,
)
def logout(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    current_user.session_version += 1
    db.commit()

    return {
        "status": "ok",
        "message": "Logged out successfully"
    }

########## USER change password ##########
@app.post("/auth/change_password")
def change_password(
    payload: ChangePasswordRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    # Verify current password
    if not verify_password(
        payload.oldPassword,
        current_user.password,
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Current password is incorrect",
        )

    # Prevent reusing the same password
    if verify_password(
        payload.newPassword,
        current_user.password,
    ):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="New password must be different from current password",
        )

    # Hash and save new password
    current_user.password = hash_password(
        payload.newPassword
    )
    current_user.session_version += 1

    db.commit()

    return {
        "status": "ok",
        "message": "Password changed successfully",
    }

########## USER DEVICE Registration (notifications with firebase) and TEST ##########
@app.put("/users/me/device")
def update_device_token(
    payload: DeviceTokenUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    current_user.fcm_token = payload.fcm_token

    db.commit()

    return {
        "status": "ok",
    }

@app.post("/test/fcm")
def test_fcm(
    current_user: User = Depends(get_current_user),
):
    if not current_user.fcm_token:
        raise HTTPException(
            status_code=400,
            detail="No FCM token registered",
        )

    response = send_pantry_share_notification(
        fcm_token=current_user.fcm_token,
        request_id=123,
        requester_name="Test User",
    )

    return {
        "message_id": response,
    }


########## USER retrieve user info ##########
@app.get(
    "/me",
    response_model=UserResponse,
)
def get_user_info(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):

    user = UserResponse(
        id = current_user.id,
        name = current_user.name,
        username = current_user.username,
        local = current_user.local,
    )
    return user


########## PANTRY Retrieve ##########
### recurrent function ###
def get_current_pantry(current_user: User, db: Session) -> Pantry:
    pantry = (
        db.query(Pantry)
        .filter(
            Pantry.token_share == current_user.token_share,
        )
        .first()
    )
    
    if pantry is None:
        raise HTTPException(
            status_code=404,
            detail="Pantry not found",
        )
    return pantry
### ---- ###
@app.get(
    "/pantry",
    response_model=PantryResponse,
)
def get_my_pantry(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    pantry = get_current_pantry(current_user, db)
    ### to retrieve all products: pantry.products+
    
    #retrieve username of pantry creator
    if current_user.local:
        creator_username = current_user.username
    else:
        creator_user = (
            db.query(User.username)
            .filter(User.id == pantry.creator)
            .first()
        )
        
        creator_username = creator_user.username

    all_users = (
        db.query(User.username)
        .filter(User.token_share == current_user.token_share)
    )
    my_pantry = PantryResponse(
        id = pantry.id,
        creator = creator_username,
        kcal_threshold = current_user.kcal_threshold,
        users = all_users,
    )
    return my_pantry

########## PANTRY threshold modify ##########
@app.post(
    "/update-threshold",
    response_model=PantryResponse,
)
def modify_pantry_kcal_threshold(payload: PantryThresholdModify, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):    
    
    pantry = get_current_pantry(current_user, db)

    if payload.kcal_threshold < 0:
        raise HTTPException(
            status_code=404,
            detail="Negative threshold",
        )
    current_user.kcal_threshold = payload.kcal_threshold
    db.commit()
    db.refresh(current_user)

    #retrieve username of pantry creator
    if current_user.local:
        creator_username = current_user.username
    else:
        creator_user = (
            db.query(User.username)
            .filter(User.id == pantry.creator)
            .first()
        )
    
        creator_username = creator_user.username

    all_users = (
        db.query(User.username)
        .filter(User.token_share == current_user.token_share)
    )

    my_pantry = PantryResponse(
        id = pantry.id,
        creator = creator_username,
        kcal_threshold = current_user.kcal_threshold,
        users = all_users,
    )

    return my_pantry


########## PANTRY JOIN REQUEST Creation ##########
@app.post(
    "/pantry-share-requests",
    #response_model=PantryShareRequestResponse,
    status_code=status.HTTP_201_CREATED,
)
def request_pantry_access(payload: PantryShareRequestCreate, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    #find pantry owner by username
    owner = (db.query(User).filter(User.username == payload.username).first())
    if owner is None:
        raise HTTPException(
            status_code=404,
            detail="User not found",
        )

    #do not insert your own username
    if owner.id == current_user.id:
        raise HTTPException(
            status_code=400,
            detail="You cannot request access to your own pantry",
        )

    #find pantry by owner
    pantry = (
        db.query(Pantry)
        .filter(Pantry.token_share == owner.token_share)
        .first()
    )

    if pantry is None:
        raise HTTPException(
            status_code=404,
            detail="Pantry not found, wrong creator username",
        )

    #check if i am already into the pantry
    if current_user.token_share == pantry.token_share:
        raise HTTPException(
            status_code=400,
            detail="you are already associated with this pantry",
        )

    #check for existing issued request
    existing_request = (
        db.query(PantryShareRequest)
        .filter(
            PantryShareRequest.pantry_id == pantry.id,
            PantryShareRequest.requesting_user_id == current_user.id,
            PantryShareRequest.status == "pending",
        )
        .first()
    )

    if existing_request:
        raise HTTPException(
            status_code=409,
            detail="A pending request already exists",
        )

    request = PantryShareRequest(
        pantry_id=pantry.id,
        requesting_user_id=current_user.id,
        status="pending",
        created_at=datetime.now(),
    )

    db.add(request)
    db.commit()
    db.refresh(request)

    ######################## fire notification to creator ############
    if owner.fcm_token:
        try:
            send_pantry_share_notification(
                fcm_token=owner.fcm_token,
                request_id=request.id,
                requester_name=current_user.name,
            )
        except Exception as exc:
            print(f"failed to send FCM notification: {exc}")

    #my_pantry = get_current_pantry(current_user, db)

    return {
        "status": "ok",
        "message": "request sent successfully"
    }

########## PANTRY retrieve PENDING share requests ##########
@app.get(
    "/retrieve-pantry-share-requests",
    response_model=PantryShareRequestListResponse,
)
def get_pending_share_requests(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    # you must be on your own pantry to see 
    if current_user.local == False:
        raise HTTPException(
            status_code=400,
            detail="You are not on your local pantry",
        )
    
    # The user must be the creator of the pantry
    pantry = (
        db.query(Pantry)
        .filter(
            Pantry.creator == current_user.id
        )
        .first()
    )

    if pantry is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="You are not the creator of a pantry",
        )

    # Retrieve pending requests
    requests = (
        db.query(
            PantryShareRequest,
            User,
        )
        .join(
            User,
            User.id == PantryShareRequest.requesting_user_id,
        )
        .filter(
            #PantryShareRequest.pantry_token_share == pantry.token_share,
            PantryShareRequest.pantry_id == pantry.id,
            PantryShareRequest.status == "pending",
        )
        .order_by(
            PantryShareRequest.created_at.asc()
        )
        .all()
    )

    result = []

    for request, requester in requests:
        result.append(
            PantryShareRequestResponseInfo(
                id=request.id,
                #requester_id=requester.id,
                requester_username=requester.username,
                requester_name=requester.name,
                status=request.status,
                created_at=request.created_at,
            )
        )

    return PantryShareRequestListResponse(
        requests=result
    )


########## PANTRY JOIN REQUEST Approve ##########
@app.post(
    "/pantry-share-requests/{request_id}/approve",
    #response_model=PantryShareRequestResponse,
)
def approve_pantry_request(request_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    request = db.get(
        PantryShareRequest,
        request_id,
    )

    if request is None:
        raise HTTPException(
            status_code=404,
            detail="Share request not found",
        )

    if request.status != "pending":
        raise HTTPException(
            status_code=400,
            detail="Request is no longer pending",
        )

    pantry = db.get(
        Pantry,
        request.pantry_id,
    )

    if pantry is None:
        raise HTTPException(
            status_code=404,
            detail="Pantry not found",
        )

    if pantry.creator != current_user.id:
        raise HTTPException(
            status_code=403,
            detail="Only the pantry creator can approve requests",
        )

    requesting_user = db.get(
        User,
        request.requesting_user_id,
    )
    

    if requesting_user is None:
        raise HTTPException(
            status_code=404,
            detail="Requesting user not found",
        )

    # ADD user to this pantry, update status of request, update 'local'field in user to False
    requesting_user.token_share = pantry.token_share
    requesting_user.local = False
    request.status = "accepted"

    db.commit()
    db.refresh(request)
    #my_pantry = get_current_pantry(current_user, db)

    return  {
        "status": "ok",
        "message": "request accepted successfully"
    }
    

########## PANTRY JOIN REQUEST Reject ##########
@app.post(
    "/pantry-share-requests/{request_id}/reject",
    #response_model=PantryShareRequestResponse,
)
def reject_pantry_request(request_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    request = db.get(
        PantryShareRequest,
        request_id,
    )

    if request is None:
        raise HTTPException(
            status_code=404,
            detail="Share request not found",
        )

    if request.status != "pending":
        raise HTTPException(
            status_code=400,
            detail="Request is no longer pending",
        )     

    pantry = db.get(
        Pantry,
        request.pantry_id,
    )

    if pantry is None:
        raise HTTPException(
            status_code=404,
            detail="Pantry not found",
        )

    if pantry.creator != current_user.id:
        raise HTTPException(
            status_code=403,
            detail="Only the pantry creator can reject requests",
        )
    # REJECT request
    request.status = "rejected"

    db.commit()
    db.refresh(request)
    #my_pantry = get_current_pantry(current_user, db)

    return {
        "status": "ok",
        "message": "request rejected successfully"
    }

########## PANTRY Leave shared pantry ##########
@app.post("/pantry/leave")
def leave_shared_pantry(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    # User is already in their own pantry.
    if current_user.local:
        raise HTTPException(
            status_code=400,
            detail="You are already in your own pantry",
        )

    # The original pantry must exist.
    if not current_user.own_token_share:
        raise HTTPException(
            status_code=500,
            detail="Original pantry information is missing",
        )

    # Restore the user's own pantry.
    current_user.token_share = current_user.own_token_share
    current_user.local = True

    db.commit()
    db.refresh(current_user)

    # = get_current_pantry(current_user, db)
    return {
        "status": "ok",
        "message": "Returned to your own pantry",
        "local": current_user.local,
    }

########## PANTRY Leave shared pantry ##########
@app.post("/pantry/remove/{username}")
def remove_user_from_pantry(
    username: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    # User isnot in their own pantry.
    if not current_user.local:
        raise HTTPException(
            status_code=400,
            detail="You are not in your own pantry",
        )
    if current_user.username == username:
        raise HTTPException(
            status_code=400,
            detail="This is your own pantry",
        )
    # Restore the user's own pantry.
    user = (
        db.query(User)
        .filter(
            User.username == username,
            User.token_share == current_user.token_share,
        )
        .first()
    )
    if user is None:
        raise HTTPException(
            status_code=404,
            detail="User not found",
        )

    user.token_share = user.own_token_share
    user.local = True

    db.commit()
    #db.refresh(current_user)

    # = get_current_pantry(current_user, db)
    return {
        "status": "ok",
        "message": f"{username} removed successfully",
    }

########## CATEGORIES retrieve ##########
@app.get(
    "/categories",
    response_model=list[CategoryResponse],
)
def get_categories(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    categories = (
        db.query(Category)
        .filter(
            Category.token_share == current_user.token_share,
            Category.active == True,
        )
        .order_by(Category.name)
        .all()
    )

    return categories

########## CATEGORY create ##########
@app.post(
    "/add_category",
    response_model=CategoryResponse,
)
def create_category(
    payload: CategoryCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    pantry = get_current_pantry(current_user, db)

    if pantry is None:
        raise HTTPException(
            status_code=404,
            detail="No pantry associated with this user",
        )

    #check if category already exists/inactive
    category_present = (
        db.query(Category)
        .filter(
            Category.name == payload.name,
            Category.token_share == current_user.token_share,
        )
        .first()
    )
    if category_present == None:

        category = Category(
            name=payload.name,
            token_share=current_user.token_share,
        )

        db.add(category)
        db.commit()
        db.refresh(category)

        '''
        categories = (
            db.query(Category)
            .filter(
                Category.token_share == current_user.token_share
            )
            .order_by(Category.name)
            .all()
        )
        '''
        
        return category
    else:
        if category_present.active == True:
            raise HTTPException(
                status_code=409,
                detail="Category already exists",
            )
        category_present.active = True
        db.commit()
        db.refresh(category_present)
        return category_present
        

########## CATEGORY delete by name ##########
@app.delete(
    "/delete_category/{category_name}",
    #response_model=CategoryResponse,
)
def delete_category(
    category_name: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    ''' never reached code
    pantry = get_current_pantry(current_user, db)

    if pantry is None:
        raise HTTPException(
            status_code=404,
            detail="No pantry associated with this user",
        )
    '''
    #search category by name and if it is active
    category = (
        db.query(Category)
        .filter(
            Category.name == category_name,
            Category.token_share == current_user.token_share,
            Category.active == True,
        )
        .first()
    )
    if category is None:
        raise HTTPException(
            status_code=404,
            detail="category not found",
        )

    #check if any product with such category exist
    products_with_cat = (
        db.query(Product.id)
        .filter(
            Product.category == category_name,
            Product.token_share == current_user.token_share,
            Product.active == True,
        )
        .first()
    )
    if products_with_cat:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Cannot delete category because products use it",
        )
    
    category.active = False
    db.commit()
    db.refresh(category)
    
    return {
        "status": "ok",
        "message": "category deleted successfully"
    }

########## PRODUCT create ##########
@app.post(
    "/add_product",
    response_model=ProductResponse,
)
def create_product(
    payload: ProductCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):

    # check quantity > 0
    if payload.quantity <= 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=(f"Incorrect quantity, select a positive quantity"),
        )

    # check category exist and active on db
    category = (
        db.query(Category)
        .filter(
            Category.name == payload.category,
            Category.token_share == current_user.token_share,
            Category.active == True,
        )
        .first()
    )
    if category is None:
        raise HTTPException(
            status_code=404,
            detail="Category not found",
        )

    ### check EAN
    if payload.EAN == None or payload.EAN == "":         
        #NULL EAN Behavior
        product = Product(
            name=payload.name,
            EAN=payload.EAN,
            unit=payload.unit,
            quantity=payload.quantity,
            category=payload.category,
            kcal=payload.kcal,
            token_share=current_user.token_share,
        )
        
        db.add(product)
        db.commit()
        db.refresh(product)
        return product

    else:
        # EAN NOT NULL
        #check if i have a product with same EAN
        product_with_EAN_present = (
            db.query(Product)
            .filter(
                Product.EAN == payload.EAN, 
                Product.token_share ==current_user.token_share,
                Product.active == True,
            )
            .first()
        )
        
        if product_with_EAN_present :
            if product_with_EAN_present.kcal != payload.kcal or product_with_EAN_present.name != payload.name:
                raise HTTPException(
                status_code=409,
                detail="(PRODUCTS with same EAN MISMATCH)",
            )
            new_quantity = (
                product_with_EAN_present.quantity + payload.quantity
            )
        
            # Update quantity
            product_with_EAN_present.quantity = new_quantity
            db.commit()
            db.refresh(product_with_EAN_present)
            return product_with_EAN_present
        
        else:
            #new product from scratch
            product = Product(
                name=payload.name,
                EAN=payload.EAN,
                unit=payload.unit,
                quantity=payload.quantity,
                category=payload.category,
                kcal=payload.kcal,
                token_share=current_user.token_share,
            )
            db.add(product)
            db.commit()
            db.refresh(product)
            return product

########## PRODUCT delete by id ##########
@app.delete(
    "/delete_product/{product_id}",
    #response_model=CategoryResponse,
)
def delete_product(
    product_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    ''' never reached code
    pantry = get_current_pantry(current_user, db)

    if pantry is None:
        raise HTTPException(
            status_code=404,
            detail="No pantry associated with this user",
        )
    '''
    #check if product exists
    product = (
        db.query(Product)
        .filter(
            Product.id == product_id,
            Product.token_share == current_user.token_share,
            Product.active == True,
        )
        .first()
    )
    if product is None:
        raise HTTPException(
            status_code=404,
            detail="Product not found in current pantry",
        )

    product.active = False

    db.commit()
    
    return {
        "status": "ok",
        "message": "product deleted successfully"
    }

########## PRODUCT update quantity ##########
@app.post(
    "/products/{product_id}/quantity",
    response_model=ProductResponse,
)
def update_product_quantity(
    product_id: int,
    payload: ProductQuantityUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    '''
    pantry = get_current_pantry(
        current_user,
        db,
    )
    '''

    # Find product in the currently selected pantry
    product = (
        db.query(Product)
        .filter(
            Product.id == product_id,
            Product.token_share == current_user.token_share,
            Product.active == True,
        )
        .first()
    )

    if product is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Product not found in current pantry",
        )

    # Calculate new quantity
    new_quantity = (
        product.quantity + payload.quantity
    )

    # Quantity cannot become negative
    if new_quantity < 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=(
                f"Insufficient quantity. "
                f"Current quantity: {product.quantity}, "
                f"requested change: {payload.quantity}"
            ),
        )

    # Update
    product.quantity = new_quantity

    db.commit()
    '''
    # Refresh pantry so the response contains the updated product list
    db.refresh(pantry)
    '''
    db.refresh(product)

    return product

########## PRODUCT retrieve product by id ##########
@app.get(
    "/products/{product_id}",
    response_model=ProductResponse,
)
def get_product_by_id(
    product_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    product = (
        db.query(Product)
        .filter(
            Product.id == product_id,
            Product.token_share == current_user.token_share,
            Product.active == True,
        )
        .first()
    )
    if product is None:
        raise HTTPException(
            status_code=404,
            detail="Product not found",
        )

    return product
    '''
    return ProductResponse(
        id=product.id,
        name=product.name,
        EAN=product.EAN,
        unit=product.unit,
        quantity=product.quantity,
        category=product.category,
        kcal=product.kcal,
    )'''

########## PRODUCT retrieve all products ##########
@app.get(
    "/all_products",
    response_model=list[ProductResponse],
)
def get_all_products(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    
    products = (
        db.query(Product)
        .filter(
            Product.token_share == current_user.token_share,
            Product.active == True,
        )
        .all()
    )
    
    return products

########## EVENT create ##########
def calculate_kcal(unit: str, kcal: int, quantity: Decimal) -> Decimal:
    unit = unit.lower()
    if unit == "unit":
        return kcal*quantity
    if unit == "kg" or unit == "l":
        return kcal*quantity*Decimal("10")
    raise ValueError(f"Unsupported product unit: {unit}")


@app.post(
    "/eat",
)
def create_event(
    payload: EventCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):

    pantry = get_current_pantry(current_user, db)
    event_date = datetime.now(timezone.utc)

    for selected_product in payload.products:
        requested_quantity = selected_product.quantity
        product = (
            db.query(Product)
            .filter(
                Product.id == selected_product.product_id,
                Product.token_share == current_user.token_share,
                Product.active == True,
            )
            .first()
        )
        if product is None:
            raise HTTPException(
                status_code=404,
                detail="Product not found",
            )
        if product.quantity < requested_quantity:
            raise HTTPException(
                status_code=400,
                detail=f"Insufficient quantity for product: {product.name}",
            )

        #calculate kcal for product
        try:
            event_kcal = calculate_kcal(product.unit, product.kcal, requested_quantity)
        except ValueError as exc:
            raise HTTPException(
                status_code=400,
                detail=str(exc),
            )

        #decrement product quantity in db
        product.quantity -= requested_quantity

        #create event eat
        event = Event(
            token_share=current_user.token_share,
            product_id=product.id,
            creator_id=current_user.id,
            event_date=event_date,
            kcal=event_kcal,
            quantity=requested_quantity,
            unit=product.unit,
            category=product.category,
        )
        db.add(event)

    #commit all events
    db.commit()
    #db.refresh(pantry)

    today = datetime.now(timezone.utc)
    #calculate kcal for today
    start = datetime.combine(
        today,
        time.min,
        tzinfo=timezone.utc,
    )
    
    end = datetime.combine(
        today,
        time.max,
        tzinfo=timezone.utc,
    )
    
    actual_kcal = (
        db.query(func.coalesce(func.sum(Event.kcal), 0))
        .filter(
            #Event.token_share == current_user.token_share,
            Event.event_date >= start,
            Event.event_date <= end,
            Event.creator_id == current_user.id,
        )
        .scalar()
    )

    if actual_kcal >= current_user.kcal_threshold and current_user.kcal_threshold != 0:

        if current_user.fcm_token:
            try:
                send_kcal_t_reached_notification_single(
                    current_user.fcm_token, 
                    actual_kcal, 
                    current_user.kcal_threshold,
                )
            except Exception as exc:
                print(f"failed to send FCM notification: {exc}")

        ''' 
        ###### SUPPORT PANTRY BASED kcal_threshold  
        pantry_users = (
            db.query(User)
            .filter(
                User.token_share == current_user.token_share
            )
        )
        fcm_tokens = []
        for pantry_user in pantry_users:
            if pantry_user.fcm_token:
                fcm_tokens.append(pantry_user.fcm_token)
        try:
            send_kcal_t_reached_notification(
                fcm_tokens=pantry_user.fcm_token,
                actual_kcal=actual_kcal,
                kcal_threshold=current_user.kcal_threshold,
            )
        except Exception as exc:
            print(f"failed to send FCM notification: {exc}")   
        '''
        return {
            "status":"ok",
            "message": "event created successfully",
            "kcal_threshold": f"exceeded: {actual_kcal} / {current_user.kcal_threshold}"
        }     
            
    
    return {
        "status":"ok",
        "message": "event created successfully",
        "kcal_threshold": "not_exceeded"
    }

########## STATISTICS on kcal ##########

### DAILY with categories ###
@app.get(
    "/stats/day",
    response_model=DayStatsResponse,
)
def get_day_stats(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    pantry = get_current_pantry(
        current_user,
        db,
    )

    # --------------------------------------------------------
    # Current day
    # --------------------------------------------------------

    today = datetime.now(timezone.utc).date()

    start = datetime.combine(
        today,
        time.min,
        tzinfo=timezone.utc,
    )

    end = start + timedelta(days=1)

    # --------------------------------------------------------
    # Total kcal
    # --------------------------------------------------------

    total_kcal = (
        db.query(
            func.coalesce(
                func.sum(Event.kcal),
                0,
            )
        )
        .filter(
            #Event.token_share == pantry.token_share,
            Event.creator_id == current_user.id,
            Event.event_date >= start,
            Event.event_date < end,
        )
        .scalar()
    )

    total_kcal = Decimal(total_kcal)

    # --------------------------------------------------------
    # Kcal grouped by category
    # --------------------------------------------------------

    category_results = (
        db.query(
            Event.category,
            func.sum(Event.kcal).label("kcal"),
        )
        .filter(
            #Event.token_share == pantry.token_share,
            Event.creator_id == current_user.id,
            Event.event_date >= start,
            Event.event_date < end,
        )
        .group_by(Event.category)
        .order_by(Event.category)
        .all()
    )

    categories = []

    for row in category_results:
        category_kcal = Decimal(row.kcal)

        if total_kcal > 0:
            percentage = (
                category_kcal
                / total_kcal
                * Decimal("100")
            )
        else:
            percentage = Decimal("0")

        categories.append(
            CategoryKcalPercentage(
                category=row.category,
                kcal=category_kcal,
                percentage=percentage.quantize(
                    Decimal("0.01")
                ),
            )
        )

    return DayStatsResponse(
        date=today,
        total_kcal=total_kcal,
        threshold=current_user.kcal_threshold,
        categories=categories,
    )

### MONTHLY ###
@app.get(
    "/stats/month",
    response_model=MonthStatsResponse,
)
def get_month_stats(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    pantry = get_current_pantry(
        current_user,
        db,
    )

    # --------------------------------------------------------
    # Last 30 calendar days, including today
    # --------------------------------------------------------

    today = datetime.now(timezone.utc).date()

    first_day = today - timedelta(days=29)

    start = datetime.combine(
        first_day,
        time.min,
        tzinfo=timezone.utc,
    )

    end = datetime.combine(
        today + timedelta(days=1),
        time.min,
        tzinfo=timezone.utc,
    )

    # --------------------------------------------------------
    # Retrieve kcal grouped by day
    # --------------------------------------------------------

    results = (
        db.query(
            func.date(Event.event_date).label("event_day"),
            func.sum(Event.kcal).label("kcal"),
        )
        .filter(
            #Event.token_share == pantry.token_share,
            Event.creator_id == current_user.id,
            Event.event_date >= start,
            Event.event_date < end,
        )
        .group_by(
            func.date(Event.event_date)
        )
        .order_by(
            func.date(Event.event_date)
        )
        .all()
    )

    # Convert database results to a dictionary.
    kcal_by_day = {
        row.event_day: Decimal(row.kcal)
        for row in results
    }

    # --------------------------------------------------------
    # Generate ALL 30 days
    # --------------------------------------------------------

    days = []

    current_day = first_day

    while current_day <= today:

        days.append(
            DailyKcalStats(
                date=current_day,
                kcal=kcal_by_day.get(
                    current_day,
                    Decimal("0"),
                ),
            )
        )

        current_day += timedelta(days=1)

    return MonthStatsResponse(
        start_date=first_day,
        end_date=today,
        threshold=current_user.kcal_threshold,
        days=days,
    )



