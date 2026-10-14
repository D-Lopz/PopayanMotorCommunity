import re
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.core.timeutils import now_colombia
from app.models.enums import VehicleType

PLATE_RE = re.compile(r"^[A-Z0-9]{5,7}$")


class VehicleIn(BaseModel):
    brand: str = Field(min_length=1, max_length=50)
    model: str = Field(min_length=1, max_length=50)
    year: int
    type: VehicleType
    plate: str
    description: Optional[str] = Field(default=None, max_length=1000)

    @field_validator("brand", "model")
    @classmethod
    def strip_text(cls, v: str) -> str:
        v = v.strip()
        if not v:
            raise ValueError("No puede estar vacío")
        return v

    @field_validator("year")
    @classmethod
    def check_year(cls, v: int) -> int:
        max_year = now_colombia().year + 1
        if v < 1900 or v > max_year:
            raise ValueError(f"El año debe estar entre 1900 y {max_year}")
        return v

    @field_validator("plate")
    @classmethod
    def normalize_plate(cls, v: str) -> str:
        v = v.strip().upper().replace(" ", "").replace("-", "")
        if not PLATE_RE.match(v):
            raise ValueError("La placa debe tener entre 5 y 7 letras o números (ej: ABC123)")
        return v


class VehicleOut(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    member_id: int
    brand: str
    model: str
    year: int
    type: VehicleType
    plate: str
    description: Optional[str]
