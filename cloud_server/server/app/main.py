from fastapi import FastAPI, Depends, HTTPException, status
from sqlalchemy.orm import Session
from datetime import datetime

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
    EventResponse,
    PantryCreate,
    PantryResponse,
    PantryShareRequestCreate,
    PantryShareRequestResponse,
    UserCreate,
    UserResponse,
    DeviceTokenUpdate,
    CategoryCreate,
    CategoryResponse,
    ProductCreate,
    ProductResponse,
    TokenResponse,
    LoginRequest,

)
from .notifications import (
    send_pantry_share_notification,
    send_kcal_t_reached_notification,
)

#### security and user authentication
from .auth import get_current_user
from .security import (
    create_access_token,
    generate_initial_token_share,
    hash_password,
    verify_password,
)

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
    response_model=UserResponse,
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

    ###CREATE default categories
    for category_name in DEFAULT_CATEGORIES:
        category = Category(
            name=category_name,
            token_share=token_share,
        )

        db.add(category)

    db.commit()
    db.refresh(user)

    return user

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

    if not verify_password(
        payload.password,
        user.password,
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid username or password",
        )

    access_token = create_access_token(user.id)

    return TokenResponse(
        access_token=access_token,
        token_type="bearer",
    )

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

########## PANTRY Retrieve ##########
### recurrent function ###
def get_current_pantry(current_user: User, db: Session) -> Pantry:
    pantry = (
        db.query(Pantry)
        .filter(
            Pantry.token_share == current_user.token_share
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
    ### to retrieve all products: pantry.products

    return pantry

########## PANTRY JOIN REQUEST Creation ##########
@app.post(
    "/pantry-share-requests",
    response_model=PantryShareRequestResponse,
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
    '''
    TODO decomment
    if owner.fcm_token:
        try:
            send_pantry_share_notification(
                fcm_token=owner.fcm_token,
                request_id=request.id,
                requester_name=current_user.name,
            )
        except Exception as exc:
            print(f"failed to send FCM notification: {exc}")
    '''

    my_pantry = get_current_pantry(current_user, db)

    return my_pantry

########## PANTRY JOIN REQUEST Approve ##########
@app.post(
    "/pantry-share-requests/{request_id}/approve",
    response_model=PantryShareRequestResponse,
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
    my_pantry = get_current_pantry(current_user, db)

    return my_pantry

########## PANTRY JOIN REQUEST Reject ##########
@app.post(
    "/pantry-share-requests/{request_id}/reject",
    response_model=PantryShareRequestResponse,
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
    my_pantry = get_current_pantry(current_user, db)

    return my_pantry

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

    '''
    return {
        "status": "ok",
        "message": "Returned to your own pantry",
        "local": current_user.local,
    }
    '''
    my_pantry = get_current_pantry(current_user, db)
    return my_pantry

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
            Category.token_share == current_user.token_share
        )
        .order_by(Category.name)
        .all()
    )

    return categories

########## CATEGORY create ##########
@app.post(
    "/categories",
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

    category = Category(
        name=payload.name,
        token_share=current_user.token_share,
    )

    db.add(category)
    db.commit()
    db.refresh(category)

    categories = (
        db.query(Category)
        .filter(
            Category.token_share == current_user.token_share
        )
        .order_by(Category.name)
        .all()
    )
    
    return categories

########## PRODUCT create ##########
@app.post(
    "/products",
    response_model=ProductResponse,
)
def create_product(
    payload: ProductCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    category = (
        db.query(Category)
        .filter(
            Category.name == payload.category,
            Category.token_share == current_user.token_share,
        )
        .first()
    )

    if category is None:
        raise HTTPException(
            status_code=404,
            detail="Category not found",
        )

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
    #db.refresh(product)
    pantry = get_current_pantry(current_user, db)

    return pantry


########## EVENT create ##########
@app.post(
    "/eat",
    response_model=EventResponse,
)
def create_event(
    payload: EventCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    product = (
        db.query(Product)
        .filter(
            Product.id == payload.product_id,
            Product.token_share == current_user.token_share,
        )
        .first()
    )

    if product is None:
        raise HTTPException(
            status_code=404,
            detail="Product not found",
        )

    event = Event(
        token_share=current_user.token_share,
        product_id=payload.product_id,
        #category=payload.category,
        event_date=payload.event_date,
        kcal=payload.kcal,
        quantity=payload.quantity,
        unit=payload.unit,
    )

    actual_kcal = 0

    db.add(event)
    db.commit()
    db.refresh(event)

    pantry = get_current_pantry(current_user, db)

    #TODO decomment
    '''
    if actual_kcal >= pantry.kcal_threshold and pantry.kcal_threshold != 0:
        
        
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
                kcal_threshold=pantry.kcal_threshold,
            )
        except Exception as exc:
            print(f"failed to send FCM notification: {exc}")        
            
    '''
    return pantry

##########  ##########

########## USER Creation ########## !!!!!! deprecated
'''
@app.post(
    "/users",
    response_model=UserResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_user(payload: UserCreate, db: Session = Depends(get_db)):
    existing_user = (
        db.query(User)
        .filter(User.username == payload.username)
        .first()
    )

    if existing_user:
        raise HTTPException(
            status_code=409,
            detail="Username already exists",
        )

    #### initial token_share
    token_share = generate_initial_token_share(
        payload.name,
        payload.username,
        payload.password,
    )

    user = User(
        name=payload.name,
        username=payload.username,
        password=hash_password(payload.password),
        token_share=token_share,
    )

    db.add(user)
    db.commit()
    db.refresh(user)

    return user
'''

'''
@app.get("/users/{user_id}", response_model=UserResponse)
def get_user(user_id: int, db: Session = Depends(get_db)):
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found",
        )
    return user
'''

########## PANTRY Creation ########## !!!!!! deprecated
'''
@app.post(
    "/pantries",
    response_model=PantryResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_pantry(payload: PantryCreate, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    #user = db.get(User, user_id)

    if current_user is None:
        raise HTTPException(
            status_code=404,
            detail="User not found",
        )

    pantry = Pantry(
        creator=current_user.id,
        token_share=current_user.token_share,
        kcal_threshold=payload.kcal_threshold,
    )

    db.add(pantry)
    db.commit()
    db.refresh(pantry)

    return pantry
'''
