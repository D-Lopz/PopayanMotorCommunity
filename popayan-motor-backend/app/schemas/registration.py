from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field

from app.schemas.event import EventSummary
from app.schemas.member import MemberSummary


class RegistrationIn(BaseModel):
    event_id: int
    vehicle_id: int
    companions: int = Field(default=0, ge=0, le=10, description="No consumen cupo")


class RegistrationVehicleIn(BaseModel):
    vehicle_id: int


class VehicleSummary(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    brand: str
    model: str
    plate: str


class RegistrationOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    member_id: int
    event_id: int
    vehicle_id: Optional[int]
    companions: int
    code: str
    code_expires_at: datetime  # propiedad calculada en el modelo, desde la fecha del evento
    registration_date: datetime
    event: EventSummary
    member: MemberSummary
    vehicle: Optional[VehicleSummary]
