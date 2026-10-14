from typing import Optional

from fastapi import APIRouter, Depends, Query, Response
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.errors import AppError
from app.dependencies import get_current_member, is_admin
from app.models import Member, Vehicle
from app.models.enums import VehicleType
from app.schemas.vehicle import VehicleIn, VehicleOut

router = APIRouter(prefix="/vehicles", tags=["Vehicles"])


def _get_or_404(db: Session, vehicle_id: int) -> Vehicle:
    vehicle = db.get(Vehicle, vehicle_id)
    if vehicle is None:
        raise AppError(404, "El vehículo no existe", "VEHICLE_NOT_FOUND")
    return vehicle


def _plate_taken(db: Session, plate: str, except_id: int | None = None) -> bool:
    query = select(Vehicle.id).where(Vehicle.plate == plate)
    if except_id is not None:
        query = query.where(Vehicle.id != except_id)
    return db.scalar(query) is not None


def _save(db: Session) -> None:
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise AppError(409, "Ya existe un vehículo con esa placa", "PLATE_EXISTS")


@router.get("", response_model=list[VehicleOut])
def list_vehicles(
    brand: Optional[str] = Query(None, description="Marca (búsqueda parcial)"),
    type: Optional[VehicleType] = Query(None),
    member_id: Optional[int] = Query(None, description="Solo el administrador puede filtrar por dueño"),
    db: Session = Depends(get_db),
    current: Member = Depends(get_current_member),
):
    query = select(Vehicle).order_by(Vehicle.brand, Vehicle.model)
    if is_admin(current):
        if member_id is not None:
            query = query.where(Vehicle.member_id == member_id)
    else:
        if member_id is not None and member_id != current.id:
            raise AppError(403, "Solo puedes ver tus propios vehículos", "FORBIDDEN")
        query = query.where(Vehicle.member_id == current.id)
    if brand:
        query = query.where(Vehicle.brand.ilike(f"%{brand.strip()}%"))
    if type:
        query = query.where(Vehicle.type == type)
    return db.scalars(query).all()


@router.get("/{vehicle_id}", response_model=VehicleOut)
def get_vehicle(vehicle_id: int, db: Session = Depends(get_db), current: Member = Depends(get_current_member)):
    vehicle = _get_or_404(db, vehicle_id)
    if not is_admin(current) and vehicle.member_id != current.id:
        raise AppError(403, "Este vehículo no es tuyo", "FORBIDDEN")
    return vehicle


@router.post("", response_model=VehicleOut, status_code=201)
def create_vehicle(data: VehicleIn, db: Session = Depends(get_db), current: Member = Depends(get_current_member)):
    if _plate_taken(db, data.plate):
        raise AppError(409, "Ya existe un vehículo con esa placa", "PLATE_EXISTS")
    vehicle = Vehicle(member_id=current.id, **data.model_dump())  # el dueño sale del token
    db.add(vehicle)
    _save(db)
    db.refresh(vehicle)
    return vehicle


@router.put("/{vehicle_id}", response_model=VehicleOut)
def update_vehicle(
    vehicle_id: int,
    data: VehicleIn,
    db: Session = Depends(get_db),
    current: Member = Depends(get_current_member),
):
    vehicle = _get_or_404(db, vehicle_id)
    if vehicle.member_id != current.id:
        raise AppError(403, "Solo el dueño puede editar este vehículo", "FORBIDDEN")
    if _plate_taken(db, data.plate, except_id=vehicle.id):
        raise AppError(409, "Ya existe un vehículo con esa placa", "PLATE_EXISTS")
    for field, value in data.model_dump().items():
        setattr(vehicle, field, value)
    _save(db)
    db.refresh(vehicle)
    return vehicle


@router.delete("/{vehicle_id}", status_code=204)
def delete_vehicle(vehicle_id: int, db: Session = Depends(get_db), current: Member = Depends(get_current_member)):
    vehicle = _get_or_404(db, vehicle_id)
    if vehicle.member_id != current.id:
        raise AppError(403, "Solo el dueño puede eliminar este vehículo", "FORBIDDEN")
    db.delete(vehicle)
    db.commit()
    return Response(status_code=204)
