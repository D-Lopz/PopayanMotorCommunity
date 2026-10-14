from datetime import date as date_type
from datetime import time as time_type

from sqlalchemy import CheckConstraint, Date, Enum, String, Text, Time
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.models.enums import EventCategory


class Event(Base):
    __tablename__ = "event"
    __table_args__ = (CheckConstraint("max_capacity > 0", name="ck_event_capacity"),)

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(150), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    date: Mapped[date_type] = mapped_column(Date, nullable=False)
    time: Mapped[time_type] = mapped_column(Time, nullable=False)
    location: Mapped[str] = mapped_column(String(200), nullable=False)
    max_capacity: Mapped[int] = mapped_column(nullable=False)
    category: Mapped[EventCategory] = mapped_column(
        Enum(EventCategory, values_callable=lambda e: [m.value for m in e], name="event_category"),
        nullable=False,
    )

    # Sin cascade: un evento con inscripciones NO se puede borrar (decisión D6).
    registrations = relationship("Registration", back_populates="event")
