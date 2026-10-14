from datetime import datetime

from pydantic import BaseModel, ConfigDict

from app.schemas.event import EventSummary
from app.schemas.member import MemberSummary


class RegistrationIn(BaseModel):
    event_id: int


class RegistrationOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    member_id: int
    event_id: int
    registration_date: datetime
    event: EventSummary
    member: MemberSummary
