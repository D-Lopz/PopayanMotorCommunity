import re
from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator

from app.models.enums import Role

PHONE_RE = re.compile(r"^\+?\d{7,15}$")


def _clean_phone(value: Optional[str]) -> Optional[str]:
    if value is None or value.strip() == "":
        return None
    value = value.strip().replace(" ", "")
    if not PHONE_RE.match(value):
        raise ValueError("El teléfono debe tener entre 7 y 15 dígitos")
    return value


class RegisterIn(BaseModel):
    name: str = Field(min_length=2, max_length=100)
    email: EmailStr
    password: str = Field(min_length=8, max_length=64)
    phone: Optional[str] = None

    @field_validator("name")
    @classmethod
    def strip_name(cls, v: str) -> str:
        v = v.strip()
        if len(v) < 2:
            raise ValueError("El nombre es muy corto")
        return v

    @field_validator("phone")
    @classmethod
    def check_phone(cls, v):
        return _clean_phone(v)


class LoginIn(BaseModel):
    email: EmailStr
    password: str = Field(min_length=1, max_length=64)


class MemberUpdate(BaseModel):
    """Un miembro solo puede editar nombre y teléfono (decisión D15)."""

    name: str = Field(min_length=2, max_length=100)
    phone: Optional[str] = None

    @field_validator("name")
    @classmethod
    def strip_name(cls, v: str) -> str:
        v = v.strip()
        if len(v) < 2:
            raise ValueError("El nombre es muy corto")
        return v

    @field_validator("phone")
    @classmethod
    def check_phone(cls, v):
        return _clean_phone(v)


class MemberOut(BaseModel):
    """Nunca incluye password_hash."""

    model_config = ConfigDict(from_attributes=True)
    id: int
    name: str
    email: str
    phone: Optional[str]
    join_date: datetime
    role: Role


class MemberSummary(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: int
    name: str
    email: str


class TokenOut(BaseModel):
    access_token: str
    token_type: str = "bearer"
    member: MemberOut
