from datetime import date as date_type
from datetime import datetime, timedelta
from datetime import time as time_type

from sqlalchemy import CheckConstraint, Date, Enum, Float, String, Text, Time
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.models.enums import EventCategory


class Event(Base):
    __tablename__ = "event"
    __table_args__ = (
        CheckConstraint("max_capacity > 0", name="ck_event_capacity"),
        CheckConstraint("price >= 0", name="ck_event_price"),
        CheckConstraint("end_date IS NULL OR end_date >= date", name="ck_event_end_date"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(150), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    date: Mapped[date_type] = mapped_column(Date, nullable=False)
    # Último día del evento si dura varios días; vacío = termina el mismo día.
    end_date: Mapped[date_type | None] = mapped_column(Date, nullable=True)
    time: Mapped[time_type] = mapped_column(Time, nullable=False)
    location: Mapped[str] = mapped_column(String(200), nullable=False)
    max_capacity: Mapped[int] = mapped_column(nullable=False)
    category: Mapped[EventCategory] = mapped_column(
        Enum(EventCategory, values_callable=lambda e: [m.value for m in e], name="event_category"),
        nullable=False,
    )
    price: Mapped[int] = mapped_column(nullable=False, default=0)  # COP, solo informativo
    distance_km: Mapped[float | None] = mapped_column(Float, nullable=True)

    # Sin cascade: un evento con inscripciones NO se puede borrar (decisión D6).
    registrations = relationship("Registration", back_populates="event")

    @property
    def ends_at(self) -> datetime:
        """00:01 del día siguiente al último día: ahí vence el código y el evento pasa a "Pasados"."""
        last_day = self.end_date or self.date
        return datetime.combine(last_day + timedelta(days=1), time_type(0, 1))
