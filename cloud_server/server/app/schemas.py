from pydantic import BaseModel, ConfigDict, Field, field_validator
from decimal import Decimal
from datetime import datetime, date

######################## LOGIN AUTH with JWT ########################
class LoginRequest(BaseModel):
    username: str
    password: str


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"

class RefreshTokenRequest(BaseModel):
    refresh_token: str

######################## CHANGE PASSWORD ########################
class ChangePasswordRequest(BaseModel):
    oldPassword: str = Field(min_length=1)
    newPassword: str = Field(min_length=8)


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

    @field_validator("name", "username")
    @classmethod
    def cannot_contain_colon(cls, value: str) -> str:
        if ":" in value:
            raise ValueError("Name and username cannot contain ':'")

        return value

class UserResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    username: str
    local: bool
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

class ProductQuantityUpdate(BaseModel):
    quantity: Decimal

    @field_validator("quantity")
    @classmethod
    def quantity_non_zero(cls, value: Decimal):
        if value == 0:
            raise ValueError("Quantity relative change cannot be zero")
        return value
    
#################################### PANTRY ########################
class PantryCreate(BaseModel):
    #creator: int
    #token_share: str = Field(min_length=1, max_length=260)
    kcal_threshold: int = Field(default=0, ge=0)

class PantryResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    creator: str
    #token_share: str
    kcal_threshold: int

    #list of products in the response
    #products: list[ProductResponse] = Field(default_factory=list)

class PantryThresholdModify(BaseModel):
    kcal_threshold: int = Field(default=0, ge=0)

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

class PantryShareRequestResponseInfo(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    requester_id: int
    requester_username: str
    requester_name: str
    status: str
    created_at: datetime

class PantryShareRequestListResponse(BaseModel):
    requests: list[PantryShareRequestResponseInfo]

#################################### CATEGORIES ########################
class CategoryCreate(BaseModel):
    name: str = Field(min_length=1, max_length=100,)
    #token_share: str = Field(min_length=1, max_length=260,)


class CategoryResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    name: str
    #token_share: str

#################################### EVENTS ########################
class EventProduct(BaseModel):
    product_id: int
    quantity: Decimal

class EventCreate(BaseModel):
    products: list[EventProduct] = Field(min_length=1)

########################### STATISTICS with EVENTS ###################
class CategoryKcalPercentage(BaseModel):
    category: str
    kcal: Decimal
    percentage: Decimal


class DayStatsResponse(BaseModel):
    date: date
    total_kcal: Decimal
    threshold: int
    categories: list[CategoryKcalPercentage]


class DailyKcalStats(BaseModel):
    date: date
    kcal: Decimal


class MonthStatsResponse(BaseModel):
    start_date: date
    end_date: date
    threshold: int
    days: list[DailyKcalStats]
