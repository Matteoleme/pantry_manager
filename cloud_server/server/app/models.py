from sqlalchemy import ForeignKey, Integer, Numeric, String, Boolean, ForeignKeyConstraint, DateTime
from sqlalchemy.orm import Mapped, mapped_column, relationship
from decimal import Decimal
from datetime import datetime
from .database import Base


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    username: Mapped[str] = mapped_column(String(100), nullable=False, unique=True, index=True)
    password: Mapped[str] = mapped_column(String(255), nullable=False)
    token_share: Mapped[str] = mapped_column(String(260), nullable=False)
    own_token_share: Mapped[str] = mapped_column(String(260), nullable=False)
    fcm_token: Mapped[str | None] = mapped_column(String(512), nullable=True)
    local: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    session_version: Mapped[int] = mapped_column(Integer, nullable=False, default=0)

    pantries: Mapped[list["Pantry"]] = relationship(
        back_populates="creator_user",
        cascade="all, delete-orphan",
    )

    share_requests: Mapped[list["PantryShareRequest"]] = relationship(
        back_populates="requesting_user",
    )


class Pantry(Base):
    __tablename__ = "pantry"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    creator: Mapped[int] = mapped_column(
        Integer,
        ForeignKey("users.id"),
        nullable=False,
        index=True,
    )
    token_share: Mapped[str] = mapped_column(
        String(260),
        nullable=False,
        unique=True,
    )
    kcal_threshold: Mapped[int] = mapped_column(
        Integer,
        nullable=False,
        default=0,
    )

    ###### get the associated list of products
    products: Mapped[list["Product"]] = relationship(
        back_populates="pantry",
    )
    '''
    products: Mapped[list["Product"]] = relationship(
        primaryjoin="Pantry.token_share == foreign(Product.token_share)",
        viewonly=True,
    )
    '''

    creator_user: Mapped[User] = relationship(
        back_populates="pantries",
    )

    share_requests: Mapped[list["PantryShareRequest"]] = relationship(
        back_populates="pantry",
        cascade="all, delete-orphan",
    )

class PantryShareRequest(Base):
    __tablename__ = "pantry_share_requests"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    pantry_id: Mapped[int] = mapped_column(Integer, ForeignKey("pantry.id"), nullable=False, index=True)
    requesting_user_id: Mapped[int] = mapped_column(Integer, ForeignKey("users.id"), nullable=False,index=True)
    status: Mapped[str] = mapped_column(
        String(20),
        nullable=False,
        default="pending",
    )

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=datetime.utcnow,
    )

    pantry: Mapped[Pantry] = relationship(
        back_populates="share_requests",
    )

    requesting_user: Mapped[User] = relationship(
        back_populates="share_requests",
    )

class Category(Base):
    __tablename__ = "categories"

    name: Mapped[str] = mapped_column(String(100), primary_key=True)
    token_share: Mapped[str] = mapped_column(
        String(260),
        ForeignKey("pantry.token_share"),
        primary_key=True,
    )
    active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)


class Product(Base):
    __tablename__ = "products"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    EAN: Mapped[str] = mapped_column(String(15), nullable=True)
    unit: Mapped[str] = mapped_column(String(10), nullable=False)
    quantity: Mapped[Decimal] = mapped_column(Numeric(12,3), nullable=False, default=0.0)
    category: Mapped[str] = mapped_column(String(50), nullable=False)
    kcal: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    token_share: Mapped[str] = mapped_column(
        String(260),
        ForeignKey("pantry.token_share"),
        nullable=False,
    )
    active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)

    __table_args__ = (
        ForeignKeyConstraint(
            ["category", "token_share"],
            ["categories.name", "categories.token_share"],
        ),
    )

    pantry: Mapped[Pantry] = relationship(
        back_populates="products"
    )


class Event(Base):
    __tablename__ = "events"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    token_share: Mapped[str] = mapped_column(
        String(260),
        ForeignKey("pantry.token_share"),
        nullable=False,
    )
    product_id: Mapped[int] = mapped_column(
        Integer,
        ForeignKey("products.id"),
        nullable=False,
    )
    event_date: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    kcal: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    quantity: Mapped[Decimal] = mapped_column(Numeric(12,3), nullable=False)
    unit: Mapped[str] = mapped_column(String(10), nullable=False)
    category: Mapped[str] = mapped_column(String(50), nullable=False)
