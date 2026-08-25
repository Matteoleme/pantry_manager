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


########## USER registration ##########
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

    if existing_user:
        raise HTTPException(
            status_code=409,
            detail="Username already exists",
        )

    token_share = generate_initial_token_share(
        payload.name,
        payload.username,
        payload.password,
    )
    own_token_share = token_share

    user = User(
        name=payload.name,
        username=payload.username,
        password=hash_password(payload.password),
        token_share=token_share,
        own_token_share = own_token_share,
    )

    db.add(user)
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

########## USER DEVICE Registration (notifications with firebase) ##########
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


########## PANTRY Creation ##########
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

########## PANTRY Retrieve ##########
@app.get(
    "/pantry",
    response_model=PantryResponse,
)
def get_my_pantry(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
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
            detail="No pantry associated with this user",
        )

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
    if owner.fcm_token:
        try:
            send_pantry_share_notification(
                fcm_token=owner.fcm_token,
                request_id=request.id,
                requester_name=current_user.name,
            )
        except Exception as exc:
            print(f"failed to send FCM notification: {exc}")

    return request

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

    return request

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

    return request

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

    return {
        "status": "ok",
        "message": "Returned to your own pantry",
        "local": current_user.local,
    }

########## PANTRY Categories retrieve ##########
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

########## PANTRY Category create ##########
@app.post(
    "/categories",
    response_model=CategoryResponse,
)
def create_category(
    payload: CategoryCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
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
            detail="No pantry associated with this user",
        )

    category = Category(
        name=payload.name,
        token_share=current_user.token_share,
    )

    db.add(category)
    db.commit()
    db.refresh(category)

    return category

########## PANTRY Product create ##########
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
    db.refresh(product)

    return product


########## Event create ##########
@app.post(
    "/events",
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

    db.add(event)
    db.commit()
    db.refresh(event)

    return event

##########  ##########



