import re
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.core.timeutils import now_colombia
from app.models.enums import VehicleType

PLATE_RE = re.compile(r"^[A-Z0-9]{5,7}$")

# Catálogo único de marcas: Android lo descarga de GET /api/vehicles/brands, así no hay otra lista que sincronizar.
BRANDS = (
    "Alfa Romeo", "Audi", "BMW", "BYD", "Chery", "Chevrolet", "Citroën", "Dodge", "Ferrari", "Fiat",
    "Ford", "Honda", "Hyundai", "JAC", "Jaguar", "Jeep", "Kia", "Lamborghini", "Land Rover", "Lexus",
    "Mazda", "Mercedes-Benz", "Mini", "Mitsubishi", "Nissan", "Peugeot", "Porsche", "RAM", "Renault",
    "Subaru", "Suzuki", "Tesla", "Toyota", "Volkswagen", "Volvo",
)
_BRANDS_BY_KEY = {b.casefold(): b for b in BRANDS}
# Opción del selector de Android para escribir otra marca; nunca se guarda como marca.
OTHER_BRAND = "Otra"
# Marca personalizada: 2 a 30 letras, números, espacios y . & ' - (al menos una letra).
CUSTOM_BRAND_RE = re.compile(r"^(?=.*[A-Za-zÀ-ÖØ-öø-ÿ])[A-Za-zÀ-ÖØ-öø-ÿ0-9 .&'-]{2,30}$")


def normalize_brand(value: str) -> str:
    """Del catálogo → su nombre oficial ("mazda" → "Mazda"); si no, marca personalizada validada."""
    v = " ".join(value.split())
    official = _BRANDS_BY_KEY.get(v.casefold())
    if official:
        return official
    if v.casefold() == OTHER_BRAND.casefold():
        raise ValueError("Escribe el nombre de la marca")
    if not CUSTOM_BRAND_RE.match(v):
        raise ValueError("La marca debe tener entre 2 y 30 letras o números (se permiten espacios y . & ' -)")
    return v


class VehicleIn(BaseModel):
    brand: str = Field(min_length=1, max_length=50)
    model: str = Field(min_length=1, max_length=50)
    year: int
    type: VehicleType
    plate: str
    description: Optional[str] = Field(default=None, max_length=1000)
    is_primary: bool = False

    @field_validator("brand")
    @classmethod
    def check_brand(cls, v: str) -> str:
        return normalize_brand(v)

    @field_validator("model")
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
    is_primary: bool
