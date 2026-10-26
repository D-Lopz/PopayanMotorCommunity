from datetime import datetime

from sqlalchemy import DateTime, Enum, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.core.timeutils import now_colombia
from app.models.enums import Role


class Member(Base):
    __tablename__ = "member"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    email: Mapped[str] = mapped_column(String(255), nullable=False, unique=True)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    phone: Mapped[str | None] = mapped_column(String(20), nullable=True)
    city: Mapped[str | None] = mapped_column(String(100), nullable=True)  # migración 001
    join_date: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=now_colombia)
    role: Mapped[Role] = mapped_column(
        Enum(Role, values_callable=lambda e: [m.value for m in e], name="role"),
        nullable=False,
        default=Role.MEMBER,
    )

    # Al borrar un miembro se borran sus vehículos e inscripciones (decisión D6).
    vehicles = relationship("Vehicle", back_populates="member", cascade="all, delete-orphan", passive_deletes=True)
    registrations = relationship("Registration", back_populates="member", cascade="all, delete-orphan", passive_deletes=True)
