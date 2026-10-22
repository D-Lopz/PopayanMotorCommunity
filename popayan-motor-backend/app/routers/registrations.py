from typing import Optional

from fastapi import APIRouter, Depends, Query, Response
from sqlalchemy import select
from sqlalchemy.orm import Session, joinedload

from app.core.database import get_db
from app.core.errors import AppError
from app.dependencies import get_current_member, is_admin
from app.models import Member, Registration
from app.schemas.registration import RegistrationIn, RegistrationOut, RegistrationVehicleIn
from app.services import registration_service

router = APIRouter(prefix="/registrations", tags=["Registrations"])


def _get_or_404(db: Session, registration_id: int) -> Registration:
    registration = db.get(Registration, registration_id)
    if registration is None:
        raise AppError(404, "La inscripción no existe", "REGISTRATION_NOT_FOUND")
    return registration


@router.get("", response_model=list[RegistrationOut])
def list_registrations(
    event_id: Optional[int] = Query(None, description="Participantes de un evento (admin)"),
    member_id: Optional[int] = Query(None, description="Solo el administrador puede filtrar por miembro"),
    db: Session = Depends(get_db),
    current: Member = Depends(get_current_member),
):
    query = (
        select(Registration)
        .options(joinedload(Registration.event), joinedload(Registration.member), joinedload(Registration.vehicle))
        .order_by(Registration.registration_date.desc())
    )
    if is_admin(current):
        if member_id is not None:
            query = query.where(Registration.member_id == member_id)
        if event_id is not None:
            query = query.where(Registration.event_id == event_id)
    else:
        if member_id is not None and member_id != current.id:
            raise AppError(403, "Solo puedes ver tus propias inscripciones", "FORBIDDEN")
        query = query.where(Registration.member_id == current.id)
        if event_id is not None:
            query = query.where(Registration.event_id == event_id)
    return db.scalars(query).unique().all()


@router.get("/{registration_id}", response_model=RegistrationOut)
def get_registration(
    registration_id: int, db: Session = Depends(get_db), current: Member = Depends(get_current_member)
):
    registration = _get_or_404(db, registration_id)
    if not is_admin(current) and registration.member_id != current.id:
        raise AppError(403, "Esta inscripción no es tuya", "FORBIDDEN")
    return registration


@router.post("", response_model=RegistrationOut, status_code=201)
def create_registration(
    data: RegistrationIn, db: Session = Depends(get_db), current: Member = Depends(get_current_member)
):
    return registration_service.register_member(db, current, data)


@router.put("/{registration_id}", response_model=RegistrationOut)
def change_registration_vehicle(
    registration_id: int,
    data: RegistrationVehicleIn,
    db: Session = Depends(get_db),
    current: Member = Depends(get_current_member),
):
    """Cambia el vehículo con el que asiste (necesario para poder borrar el anterior)."""
    registration = _get_or_404(db, registration_id)
    return registration_service.change_vehicle(db, current, registration, data.vehicle_id)


@router.delete("/{registration_id}", status_code=204)
def delete_registration(
    registration_id: int, db: Session = Depends(get_db), current: Member = Depends(get_current_member)
):
    registration = _get_or_404(db, registration_id)
    if not is_admin(current) and registration.member_id != current.id:
        raise AppError(403, "Esta inscripción no es tuya", "FORBIDDEN")
    db.delete(registration)
    db.commit()
    return Response(status_code=204)
