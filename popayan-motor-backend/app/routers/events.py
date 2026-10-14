from datetime import date as date_type
from typing import Optional

from fastapi import APIRouter, Depends, Query, Response
from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.dependencies import get_current_member, require_admin
from app.models import Event, Member
from app.models.enums import EventCategory
from app.schemas.event import EventIn, EventOut
from app.services import event_service

router = APIRouter(prefix="/events", tags=["Events"])


@router.get("", response_model=list[EventOut])
def list_events(
    category: Optional[EventCategory] = Query(None, description="Ej: Meet"),
    date: Optional[date_type] = Query(None, description="Fecha exacta AAAA-MM-DD"),
    search: Optional[str] = Query(None, description="Busca en nombre, lugar o descripción"),
    db: Session = Depends(get_db),
    _: Member = Depends(get_current_member),
):
    query = select(Event).order_by(Event.date, Event.time)
    if category:
        query = query.where(Event.category == category)
    if date:
        query = query.where(Event.date == date)
    if search:
        like = f"%{search.strip()}%"
        query = query.where(
            or_(Event.name.ilike(like), Event.location.ilike(like), Event.description.ilike(like))
        )
    return event_service.to_out(db, list(db.scalars(query).all()))


@router.get("/{event_id}", response_model=EventOut)
def get_event(event_id: int, db: Session = Depends(get_db), _: Member = Depends(get_current_member)):
    event = event_service.get_event_or_404(db, event_id)
    return event_service.to_out(db, [event])[0]


@router.post("", response_model=EventOut, status_code=201)
def create_event(data: EventIn, db: Session = Depends(get_db), _: Member = Depends(require_admin)):
    event = event_service.create_event(db, data)
    return event_service.to_out(db, [event])[0]


@router.put("/{event_id}", response_model=EventOut)
def update_event(event_id: int, data: EventIn, db: Session = Depends(get_db), _: Member = Depends(require_admin)):
    event = event_service.get_event_or_404(db, event_id)
    event = event_service.update_event(db, event, data)
    return event_service.to_out(db, [event])[0]


@router.delete("/{event_id}", status_code=204)
def delete_event(event_id: int, db: Session = Depends(get_db), _: Member = Depends(require_admin)):
    event = event_service.get_event_or_404(db, event_id)
    event_service.delete_event(db, event)
    return Response(status_code=204)
