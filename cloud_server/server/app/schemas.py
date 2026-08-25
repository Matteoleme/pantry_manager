from pydantic import BaseModel, ConfigDict, Field
from decimal import Decimal
from datetime import datetime

######################## LOGIN AUTH with JWT ########################
class LoginRequest(BaseModel):
    username: str
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"

######################## DEVICE Registration for notifications (firebase) ########################
class DeviceTokenUpdate(BaseModel):
    fcm_token: str = Field(
        min_length=1,
        max_length=512,
    )

#################################### USERS ########################
class UserCreate(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    username: str = Field(min_length=1, max_length=100)
    password: str = Field(min_length=8, max_length=255)
    #token_share: str = Field(min_length=1, max_length=260)

class UserResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    username: str
    #token_share: str

#################################### PRODUCTS ########################
class ProductCreate(BaseModel):
    name: str = Field(min_length=1, max_length=100,)
    EAN: str | None = Field(default=None, max_length=15,)
    unit: str = Field(min_length=1, max_length=10,)
    quantity: Decimal = Field(default=Decimal("0"),)
    category: str = Field(min_length=1, max_length=50,)
    kcal: int = Field(default=0,)
    #token_share: str = Field(min_length=1, max_length=260,)


class ProductResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    EAN: str | None
    unit: str
    quantity: Decimal
    category: str
    kcal: int
    #token_share: str
    
#################################### PANTRY ########################
class PantryCreate(BaseModel):
    #creator: int
    #token_share: str = Field(min_length=1, max_length=260)
    kcal_threshold: int = Field(default=0, ge=0)

class PantryResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    creator: int
    #token_share: str
    kcal_threshold: int

    #list of products in the response
    products: list[ProductResponse] = Field(default_factory=list)

#################################### PANTRY SHARING ########################
class PantryShareRequestCreate(BaseModel):
    #token_share: str = Field(min_length=1, max_length= 260)
    username: str = Field(min_length=1, max_length=100)

class PantryShareRequestResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    pantry_id: int
    requesting_user_id: int
    status: str
    created_at: datetime

#################################### CATEGORIES ########################
class CategoryCreate(BaseModel):
    name: str = Field(min_length=1, max_length=100,)
    #token_share: str = Field(min_length=1, max_length=260,)


class CategoryResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    name: str
    #token_share: str

#################################### EVENTS ########################
class EventCreate(BaseModel):
    #token_share: str = Field(min_length=1, max_length=260)
    product_id: int
    event_date: datetime
    kcal: int = Field(default=0)
    quantity: Decimal
    unit: str = Field(min_length=1, max_length=10)

class EventResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    token_share: str
    product_id: int
    event_date: datetime
    kcal: int
    quantity: Decimal
    unit: str
