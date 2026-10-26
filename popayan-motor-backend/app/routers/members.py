from typing import Optional

from fastapi import APIRouter, Depends, Query, Response
from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.errors import AppError
from app.dependencies import get_current_member, is_admin, require_admin
from app.models import Member
from app.schemas.member import MemberOut, MemberUpdate

router = APIRouter(prefix="/members", tags=["Members"])


def _get_or_404(db: Session, member_id: int) -> Member:
    member = db.get(Member, member_id)
    if member is None:
        raise AppError(404, "El miembro no existe", "MEMBER_NOT_FOUND")
    return member


def _check_self_or_admin(current: Member, member_id: int) -> None:
    if not is_admin(current) and current.id != member_id:
        raise AppError(403, "No tienes permiso sobre este miembro", "FORBIDDEN")


@router.get("", response_model=list[MemberOut])
def list_members(
    search: Optional[str] = Query(None, description="Busca en nombre o correo"),
    db: Session = Depends(get_db),
    _: Member = Depends(require_admin),
):
    query = select(Member).order_by(Member.name)
    if search:
        like = f"%{search.strip()}%"
        query = query.where(or_(Member.name.ilike(like), Member.email.ilike(like)))
    return db.scalars(query).all()


@router.get("/{member_id}", response_model=MemberOut)
def get_member(member_id: int, db: Session = Depends(get_db), current: Member = Depends(get_current_member)):
    _check_self_or_admin(current, member_id)
    return _get_or_404(db, member_id)


@router.put("/{member_id}", response_model=MemberOut)
def update_member(
    member_id: int,
    data: MemberUpdate,
    db: Session = Depends(get_db),
    current: Member = Depends(get_current_member),
):
    _check_self_or_admin(current, member_id)
    member = _get_or_404(db, member_id)
    member.name = data.name
    member.phone = data.phone
    member.city = data.city
    db.commit()
    db.refresh(member)
    return member


@router.delete("/{member_id}", status_code=204)
def delete_member(member_id: int, db: Session = Depends(get_db), current: Member = Depends(require_admin)):
    member = _get_or_404(db, member_id)
    if member.id == current.id:
        raise AppError(409, "No puedes eliminar tu propia cuenta", "CANNOT_DELETE_SELF")
    db.delete(member)
    db.commit()
    return Response(status_code=204)
