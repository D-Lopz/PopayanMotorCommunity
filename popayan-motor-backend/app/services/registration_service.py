"""Reglas de negocio de las inscripciones (Reglas 1, 2 y 3)."""
from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.errors import AppError
from app.models import Event, Member, Registration
from app.services import event_service


def register_member(db: Session, member: Member, event_id: int) -> Registration:
    # SELECT ... FOR UPDATE: bloquea la fila del evento hasta el commit, así dos
    # personas que se inscriben al mismo tiempo no pueden tomar el mismo último cupo.
    event = db.scalar(select(Event).where(Event.id == event_id).with_for_update())
    if event is None:
        raise AppError(404, "El evento no existe", "EVENT_NOT_FOUND")

    # Regla 3: no se inscribe a eventos pasados
    if event_service.is_past(event):
        raise AppError(409, "Este evento ya pasó, no admite inscripciones", "EVENT_PAST")

    # Regla 2: no se inscribe dos veces
    already = db.scalar(
        select(Registration.id).where(
            Registration.member_id == member.id, Registration.event_id == event_id
        )
    )
    if already is not None:
        raise AppError(409, "Ya estás inscrito en este evento", "ALREADY_REGISTERED")

    # Regla 1: no se supera la capacidad máxima.
    # El conteo es una lectura CON bloqueo (FOR UPDATE): una lectura normal usaría la "foto"
    # de datos tomada al inicio de la transacción (InnoDB, REPEATABLE READ) y no vería las
    # inscripciones que otros acaban de confirmar mientras esperábamos el bloqueo.
    taken = db.scalar(
        select(func.count(Registration.id)).where(Registration.event_id == event_id).with_for_update()
    )
    if taken >= event.max_capacity:
        raise AppError(409, "El evento ya alcanzó su capacidad máxima", "EVENT_FULL")

    registration = Registration(member_id=member.id, event_id=event_id)
    db.add(registration)
    try:
        db.commit()
    except IntegrityError:  # Respaldo: la restricción UNIQUE de MySQL
        db.rollback()
        raise AppError(409, "Ya estás inscrito en este evento", "ALREADY_REGISTERED")
    db.refresh(registration)
    return registration
