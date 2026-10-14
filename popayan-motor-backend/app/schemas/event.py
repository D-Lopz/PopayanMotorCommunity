from datetime import date, time
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.models.enums import EventCategory


class EventIn(BaseModel):
    name: str = Field(min_length=3, max_length=150)
    description: Optional[str] = Field(default=None, max_length=2000)
    date: date
    time: time
    location: str = Field(min_length=2, max_length=200)
    max_capacity: int = Field(gt=0, le=100000)
    category: EventCategory

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
    time: time
    location: str
    max_capacity: int
    category: EventCategory
    registered_count: int = 0


class EventSummary(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    name: str
    date: date
    time: time
    location: str
