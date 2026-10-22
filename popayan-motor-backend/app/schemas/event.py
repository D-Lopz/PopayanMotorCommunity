from datetime import date, time
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.models.enums import EventCategory


class EventIn(BaseModel):
    name: str = Field(min_length=3, max_length=150)
    description: Optional[str] = Field(default=None, max_length=2000)
    date: date
    end_date: Optional[date] = Field(default=None, description="Último día si dura varios días")
    time: time
    location: str = Field(min_length=2, max_length=200)
    max_capacity: int = Field(gt=0, le=100000)
    category: EventCategory
    price: int = Field(default=0, ge=0, le=100_000_000, description="Aporte en COP, 0 = gratis")
    distance_km: Optional[float] = Field(default=None, gt=0, le=10000)

    @model_validator(mode="after")
    def check_end_date(self):
        if self.end_date is not None and self.end_date < self.date:
            raise ValueError("La fecha de fin no puede ser anterior a la de inicio")
        return self

    @field_validator("name", "location")
    @classmethod
    def strip_text(cls, v: str) -> str:
        v = v.strip()
        if not v:
            raise ValueError("No puede estar vacío")
        return v


class EventOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    name: str
    description: Optional[str]
    date: date
    end_date: Optional[date]
    time: time
    location: str
    max_capacity: int
    category: EventCategory
    price: int
    distance_km: Optional[float]
    registered_count: int = 0
    attendees_count: int = 0  # inscritos + acompañantes
    finished: bool = False  # True desde las 00:01 del día siguiente al último día


class EventSummary(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    name: str
    date: date
    end_date: Optional[date]
    time: time
    location: str
    distance_km: Optional[float]
