import re
from typing import Optional

from fastapi import APIRouter, Depends, Query, Response
from sqlalchemy import select
from sqlalchemy.orm import Session, joinedload

from app.core.database import get_db
from app.core.errors import AppError
from app.dependencies import get_current_member, is_admin, require_admin
from app.models import Member, Registration
from app.schemas.registration import RegistrationCheckOut, RegistrationIn, RegistrationOut, RegistrationVehicleIn
from app.services import event_service, registration_service

router = APIRouter(prefix="/registrations", tags=["Registrations"])

CODE_RE = re.compile(r"^PMC-[A-Z0-9]{6}$")


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


# Declarada antes de "/{registration_id}": si no, "verify" se intentaría leer como id.
@router.get("/verify", response_model=RegistrationCheckOut)
def verify_registration(
    code: str = Query(..., description="Contenido del QR, ej. PMC-AB12CD"),
    event_id: int = Query(..., description="Evento en cuya entrada se valida"),
    db: Session = Depends(get_db),
    _: Member = Depends(require_admin),
):
    """Valida el QR en la entrada del evento. Solo consulta: no marca asistencia."""
    code = code.strip().upper()
    if not CODE_RE.match(code):
        raise AppError(422, "El código no tiene el formato PMC-XXXXXX", "INVALID_CODE")
    registration = db.scalar(
        select(Registration)
        .options(joinedload(Registration.event), joinedload(Registration.member), joinedload(Registration.vehicle))
        .where(Registration.code == code)
    )
    if registration is None:
        raise AppError(404, "La inscripción no existe", "REGISTRATION_NOT_FOUND")
    # No se dice a qué evento pertenece: solo que no es este.
    if registration.event_id != event_id:
        raise AppError(409, "Esta inscripción no corresponde a este evento", "WRONG_EVENT")
    if event_service.is_finished(registration.event):  # misma regla que code_expires_at
        raise AppError(409, "El evento ya terminó, el código venció", "EVENT_FINISHED")
    return RegistrationCheckOut(
        code=registration.code,
        event_name=registration.event.name,
        member_name=registration.member.name,
        companions=registration.companions,
        vehicle=registration.vehicle,
    )


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
    # Cancelar un evento ya empezado borraría el historial (y los "Km en rutas"); el admin sí puede.
    if not is_admin(current) and event_service.is_past(registration.event):
        raise AppError(409, "El evento ya empezó, no se puede cancelar la inscripción", "EVENT_STARTED")
    db.delete(registration)
    db.commit()
    return Response(status_code=204)
