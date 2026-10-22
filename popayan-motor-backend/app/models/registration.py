from datetime import datetime

from sqlalchemy import CheckConstraint, DateTime, ForeignKey, SmallInteger, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.core.timeutils import now_colombia


class Registration(Base):
    __tablename__ = "registration"

    __table_args__ = (
        UniqueConstraint("member_id", "event_id", name="uq_registration_member_event"),
        CheckConstraint("companions BETWEEN 0 AND 10", name="ck_registration_companions"),
    )

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    member_id: Mapped[int] = mapped_column(ForeignKey("member.id", ondelete="CASCADE"), nullable=False)
    event_id: Mapped[int] = mapped_column(ForeignKey("event.id", ondelete="RESTRICT"), nullable=False)
    # Obligatorio al inscribirse; queda NULL solo si se borra el vehículo después de terminado el evento.
    vehicle_id: Mapped[int | None] = mapped_column(ForeignKey("vehicle.id", ondelete="SET NULL"), nullable=True)
    companions: Mapped[int] = mapped_column(SmallInteger, nullable=False, default=0)
    code: Mapped[str] = mapped_column(String(10), nullable=False, unique=True)
    registration_date: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=now_colombia)

    member = relationship("Member", back_populates="registrations")
    event = relationship("Event", back_populates="registrations")
    vehicle = relationship("Vehicle", passive_deletes=True)

    @property
    def code_expires_at(self) -> datetime:
        return self.event.ends_at
