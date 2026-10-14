from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.core.timeutils import now_colombia


class Registration(Base):
    __tablename__ = "registration"
    
    __table_args__ = (UniqueConstraint("member_id", "event_id", name="uq_registration_member_event"),)

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    member_id: Mapped[int] = mapped_column(ForeignKey("member.id", ondelete="CASCADE"), nullable=False)
    event_id: Mapped[int] = mapped_column(ForeignKey("event.id", ondelete="RESTRICT"), nullable=False)
    registration_date: Mapped[datetime] = mapped_column(DateTime, nullable=False, default=now_colombia)

    member = relationship("Member", back_populates="registrations")
    event = relationship("Event", back_populates="registrations")
