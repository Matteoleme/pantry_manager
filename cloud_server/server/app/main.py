from fastapi import FastAPI, Depends, HTTPException, status
from sqlalchemy.orm import Session

from .database import Base, engine, get_db
from .models import (
    Event,
    Pantry,
    PantryShareRequest,
    User,
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
)
from .security import (
    generate_initial_token_share,
    hash_password,
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



########## USER Creation ##########
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
@app.post("/users", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
def create_user(payload: UserCreate, db: Session = Depends(get_db)):
    existing = db.query(User).filter(User.username == payload.username).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Username already exists",
        )

    user = User(
        name=payload.name,
        username=payload.username,
        password=hash_password(payload.password),
        token_share=payload.token_share,
    )

    db.add(user)
    db.commit()
    db.refresh(user)
    return user


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

########## PANTRY Creation ##########
@app.post(
    "/pantries",
    response_model=PantryResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_pantry(payload: PantryCreate, user_id: int, db: Session = Depends(get_db)):
    user = db.get(User, user_id)

    if user is None:
        raise HTTPException(
            status_code=404,
            detail="User not found",
        )

    pantry = Pantry(
        creator=user.id,
        token_share=user.token_share,
        kcal_threshold=payload.kcal_threshold,
    )

    db.add(pantry)
    db.commit()
    db.refresh(pantry)

    return pantry

'''
@app.post("/pantries", response_model=PantryResponse, status_code=status.HTTP_201_CREATED)
def create_pantry(payload: PantryCreate, db: Session = Depends(get_db)):
    creator = db.get(User, payload.creator)
    if creator is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Creator user not found",
        )

    pantry = Pantry(
        creator=payload.creator,
        token_share=payload.token_share,
    )

    db.add(pantry)
    db.commit()
    db.refresh(pantry)
    return pantry
'''

########## PANTRY JOIN REQUEST Creation ##########
@app.post(
    "/pantry-share-requests",
    response_model=PantryShareRequestResponse,
    status_code=status.HTTP_201_CREATED,
)
def request_pantry_access(payload: PantryShareRequestCreate, user_id: int, db: Session = Depends(get_db)):
    user = db.get(User, user_id)

    if user is None:
        raise HTTPException(
            status_code=404,
            detail="User not found",
        )

    pantry = (
        db.query(Pantry)
        .filter(Pantry.token_share == payload.token_share)
        .first()
    )

    if pantry is None:
        raise HTTPException(
            status_code=404,
            detail="Pantry not found",
        )

    if user.token_share == pantry.token_share:
        raise HTTPException(
            status_code=400,
            detail="User is already associated with this pantry",
        )

    existing_request = (
        db.query(PantryShareRequest)
        .filter(
            PantryShareRequest.pantry_id == pantry.id,
            PantryShareRequest.requesting_user_id == user.id,
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
        requesting_user_id=user.id,
        status="pending",
    )

    db.add(request)
    db.commit()
    db.refresh(request)

    return request

########## PANTRY JOIN REQUEST Approving ##########
@app.post(
    "/pantry-share-requests/{request_id}/approve",
    response_model=PantryShareRequestResponse,
)
def approve_pantry_request(request_id: int, creator_id: int, db: Session = Depends(get_db)):
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

    if pantry.creator != creator_id:
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

    # The actual membership change happens here.
    requesting_user.token_share = pantry.token_share

    request.status = "accepted"

    db.commit()
    db.refresh(request)

    return request

########## PANTRY JOIN REQUEST Rejecting ##########
@app.post(
    "/pantry-share-requests/{request_id}/reject",
    response_model=PantryShareRequestResponse,
)
def reject_pantry_request(request_id: int, creator_id: int, db: Session = Depends(get_db)):
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

    if pantry.creator != creator_id:
        raise HTTPException(
            status_code=403,
            detail="Only the pantry creator can reject requests",
        )

    request.status = "rejected"

    db.commit()
    db.refresh(request)

    return request

########## PANTRY  ##########

