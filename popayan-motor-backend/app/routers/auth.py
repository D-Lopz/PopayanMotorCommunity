from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.errors import AppError
from app.core.security import create_access_token, hash_password, verify_password
from app.models import Member
from app.models.enums import Role
from app.schemas.member import LoginIn, MemberOut, RegisterIn, TokenOut

router = APIRouter(prefix="/auth", tags=["Auth"])


@router.post("/register", response_model=MemberOut, status_code=201)
def register(data: RegisterIn, db: Session = Depends(get_db)):
    """Crea una cuenta. El rol SIEMPRE es MEMBER: nadie puede registrarse como admin."""
    email = data.email.lower()
    if db.scalar(select(Member.id).where(Member.email == email)):
        raise AppError(409, "Ya existe una cuenta con ese correo", "EMAIL_EXISTS")
    member = Member(
        name=data.name,
        email=email,
        password_hash=hash_password(data.password),
        phone=data.phone,
        role=Role.MEMBER,
    )
    db.add(member)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise AppError(409, "Ya existe una cuenta con ese correo", "EMAIL_EXISTS")
    db.refresh(member)
    return member


@router.post("/login", response_model=TokenOut)
def login(data: LoginIn, db: Session = Depends(get_db)):
    member = db.scalar(select(Member).where(Member.email == data.email.lower()))
    # Mismo mensaje si falla el correo o la clave: no revela qué correos existen.
    if member is None or not verify_password(data.password, member.password_hash):
        raise AppError(401, "Correo o contraseña incorrectos", "INVALID_CREDENTIALS")
    token = create_access_token(member.id, member.role.value)
    return TokenOut(access_token=token, member=MemberOut.model_validate(member))
