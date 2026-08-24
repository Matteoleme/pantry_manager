from pydantic import BaseModel, ConfigDict, Field
from datetime import datetime


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

#################################### PANTRY ########################
class PantryCreate(BaseModel):
    #creator: int
    #token_share: str = Field(min_length=1, max_length=260)
    kcal_threshold: int = Field(default=0, ge=0)

class PantryResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    creator: int
    token_share: str
    kcal_threshold: int

#################################### PANTRY SHARING ########################
class PantryShareRequestCreate(BaseModel):
    token_share: str = Field(min_length=1, max_length= 260)

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
    token_share: str

#################################### PRODUCTS ########################
class ProductCreate(BaseModel):
    name: str = Field(min_length=1, max_length=100,)
    EAN: str | None = Field(default=None, max_length=15,)
    unit: str = Field(min_length=1, max_length=10,)
    quantity: int = Field(default=0,)
    category: str = Field(min_length=1, max_length=50,)
    #token_share: str = Field(min_length=1, max_length=260,)


class ProductResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    EAN: str | None
    unit: str
    quantity: int
    category: str
    token_share: str

#################################### EVENTS ########################
class EventCreate(BaseModel):
    #token_share: str = Field(min_length=1, max_length=260)
    product_id: int
    event_date: datetime
    kcal: int = Field(default=0)
    quantity: int
    unit: str = Field(min_length=1, max_length=10)

class EventResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    token_share: str
    product_id: int
    event_date: datetime
    kcal: int
    quantity: int
    unit: str
