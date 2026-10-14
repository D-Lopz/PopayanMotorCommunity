from sqlalchemy import CheckConstraint, Enum, ForeignKey, SmallInteger, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.models.enums import VehicleType


class Vehicle(Base):
    __tablename__ = "vehicle"
    __table_args__ = (CheckConstraint("year >= 1900", name="ck_vehicle_year"),)

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    member_id: Mapped[int] = mapped_column(ForeignKey("member.id", ondelete="CASCADE"), nullable=False)
    brand: Mapped[str] = mapped_column(String(50), nullable=False)
    model: Mapped[str] = mapped_column(String(50), nullable=False)
    year: Mapped[int] = mapped_column(SmallInteger, nullable=False)
    type: Mapped[VehicleType] = mapped_column(
        Enum(VehicleType, values_callable=lambda e: [m.value for m in e], name="vehicle_type"),
        nullable=False,
    )
    plate: Mapped[str] = mapped_column(String(10), nullable=False, unique=True)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)

    member = relationship("Member", back_populates="vehicles")
