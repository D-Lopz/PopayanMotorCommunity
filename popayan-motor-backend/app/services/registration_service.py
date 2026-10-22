"""Reglas de negocio de las inscripciones (Reglas 1, 2 y 3)."""
import secrets

from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.errors import AppError
from app.models import Event, Member, Registration, Vehicle
from app.schemas.registration import RegistrationIn
from app.services import event_service

# Sin O/0 ni I/1 para que el código no se confunda al leerlo.
CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"


def _new_code(db: Session) -> str:
    while True:
        code = "PMC-" + "".join(secrets.choice(CODE_ALPHABET) for _ in range(6))
        if db.scalar(select(Registration.id).where(Registration.code == code)) is None:
            return code


def _own_vehicle(db: Session, member: Member, vehicle_id: int) -> Vehicle:
    vehicle = db.scalar(select(Vehicle).where(Vehicle.id == vehicle_id, Vehicle.member_id == member.id))
    if vehicle is None:
        raise AppError(404, "El vehículo no existe en tu garaje", "VEHICLE_NOT_FOUND")
    return vehicle


def register_member(db: Session, member: Member, data: RegistrationIn) -> Registration:
    event_id = data.event_id
    # SELECT ... FOR UPDATE: bloquea la fila del evento hasta el commit, así dos
    # personas que se inscriben al mismo tiempo no pueden tomar el mismo último cupo.
    event = db.scalar(select(Event).where(Event.id == event_id).with_for_update())
    if event is None:
        raise AppError(404, "El evento no existe", "EVENT_NOT_FOUND")

    # Regla 3: no se inscribe a eventos pasados
    if event_service.is_past(event):
        raise AppError(409, "Este evento ya pasó, no admite inscripciones", "EVENT_PAST")

    _own_vehicle(db, member, data.vehicle_id)

    # Regla 2: no se inscribe dos veces
    already = db.scalar(
        select(Registration.id).where(
            Registration.member_id == member.id, Registration.event_id == event_id
        )
    )
    if already is not None:
        raise AppError(409, "Ya estás inscrito en este evento", "ALREADY_REGISTERED")

    # Regla 1: no se supera la capacidad máxima. Cupo = inscripciones; los acompañantes no cuentan.
    # El conteo es una lectura CON bloqueo (FOR UPDATE): una lectura normal usaría la "foto"
    # de datos tomada al inicio de la transacción (InnoDB, REPEATABLE READ) y no vería las
    # inscripciones que otros acaban de confirmar mientras esperábamos el bloqueo.
    taken = db.scalar(
        select(func.count(Registration.id)).where(Registration.event_id == event_id).with_for_update()
    )
    if taken >= event.max_capacity:
        raise AppError(409, "El evento ya alcanzó su capacidad máxima", "EVENT_FULL")

    registration = Registration(
        member_id=member.id,
        event_id=event_id,
        vehicle_id=data.vehicle_id,
        companions=data.companions,
        code=_new_code(db),
    )
    db.add(registration)
    try:
        db.commit()
    except IntegrityError:  # Respaldo: la restricción UNIQUE de MySQL
        db.rollback()
        raise AppError(409, "Ya estás inscrito en este evento", "ALREADY_REGISTERED")
    db.refresh(registration)
    return registration


def change_vehicle(db: Session, member: Member, registration: Registration, vehicle_id: int) -> Registration:
    if registration.member_id != member.id:
        raise AppError(403, "Esta inscripción no es tuya", "FORBIDDEN")
    if event_service.is_finished(registration.event):
        raise AppError(409, "El evento ya terminó, no se puede cambiar el vehículo", "EVENT_FINISHED")
    registration.vehicle_id = _own_vehicle(db, member, vehicle_id).id
    db.commit()
    db.refresh(registration)
    return registration
