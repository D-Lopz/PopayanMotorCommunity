"""Dependencias de seguridad: quién es el usuario actual y qué rol tiene."""
import jwt
from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.errors import AppError
from app.core.security import decode_access_token
from app.models import Member
from app.models.enums import Role

bearer_scheme = HTTPBearer(auto_error=False)


def get_current_member(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> Member:
    if credentials is None:
        raise AppError(401, "Debes iniciar sesión", "NOT_AUTHENTICATED")
    try:
        payload = decode_access_token(credentials.credentials)
        member_id = int(payload["sub"])
    except (jwt.PyJWTError, KeyError, ValueError):
        raise AppError(401, "Sesión inválida o vencida", "INVALID_TOKEN")
    member = db.get(Member, member_id)
    if member is None:
        raise AppError(401, "El usuario de la sesión ya no existe", "INVALID_TOKEN")
    return member


def require_admin(member: Member = Depends(get_current_member)) -> Member:
    if member.role != Role.ADMINISTRATOR:
        raise AppError(403, "Solo el administrador puede hacer esto", "FORBIDDEN")
    return member


def is_admin(member: Member) -> bool:
    return member.role == Role.ADMINISTRATOR
