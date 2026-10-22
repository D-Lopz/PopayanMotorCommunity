"""Lógica de eventos: cupos calculados, fechas y borrado seguro."""
from datetime import datetime

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.errors import AppError
from app.core.timeutils import now_colombia
from app.models import Event, Registration
from app.schemas.event import EventIn, EventOut


def event_start(event: Event) -> datetime:
    return datetime.combine(event.date, event.time)


def is_past(event: Event) -> bool:
    """Regla 3: un evento es pasado si su hora de inicio ya ocurrió (hora Colombia)."""
    return event_start(event) <= now_colombia()


def is_finished(event: Event) -> bool:
    return event.ends_at <= now_colombia()


def count_registrations(db: Session, event_id: int) -> int:
    return db.scalar(select(func.count(Registration.id)).where(Registration.event_id == event_id)) or 0


def to_out(db: Session, events: list[Event]) -> list[EventOut]:
    """Convierte eventos a esquema de salida agregando registered_count (una sola consulta)."""
    counts: dict[int, tuple[int, int]] = {}
    if events:
        rows = db.execute(
            select(Registration.event_id, func.count(Registration.id), func.sum(Registration.companions))
            .where(Registration.event_id.in_([e.id for e in events]))
            .group_by(Registration.event_id)
        ).all()
        counts = {event_id: (total, int(companions or 0)) for event_id, total, companions in rows}
    result = []
    for e in events:
        out = EventOut.model_validate(e)
        registered, companions = counts.get(e.id, (0, 0))
        out.registered_count = registered
        out.attendees_count = registered + companions  # los acompañantes no consumen cupo
        out.finished = is_finished(e)
        result.append(out)
    return result


def get_event_or_404(db: Session, event_id: int) -> Event:
    event = db.get(Event, event_id)
    if event is None:
        raise AppError(404, "El evento no existe", "EVENT_NOT_FOUND")
    return event


def _check_future(data: EventIn) -> None:
    if datetime.combine(data.date, data.time) <= now_colombia():
        raise AppError(422, "La fecha y hora del evento debe ser futura", "INVALID_EVENT_DATE")


def create_event(db: Session, data: EventIn) -> Event:
    _check_future(data)
    event = Event(**data.model_dump())
    db.add(event)
    db.commit()
    db.refresh(event)
    return event


def update_event(db: Session, event: Event, data: EventIn) -> Event:
    if (data.date, data.time) != (event.date, event.time):
        _check_future(data)  # Solo se valida si el administrador cambia la fecha u hora
    registered = count_registrations(db, event.id)
    if data.max_capacity < registered:
        raise AppError(
            409,
            f"La capacidad no puede ser menor a los {registered} inscritos actuales",
            "CAPACITY_BELOW_REGISTERED",
        )
    for field, value in data.model_dump().items():
        setattr(event, field, value)
    db.commit()
    db.refresh(event)
    return event


def delete_event(db: Session, event: Event) -> None:
    if count_registrations(db, event.id) > 0:
        raise AppError(
            409, "No se puede eliminar un evento que ya tiene inscripciones", "EVENT_HAS_REGISTRATIONS"
        )
    db.delete(event)
    db.commit()
