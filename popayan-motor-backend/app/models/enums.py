"""Valores permitidos (se guardan como ENUM en MySQL)."""
from enum import Enum


class Role(str, Enum):
    MEMBER = "MEMBER"
    ADMINISTRATOR = "ADMINISTRATOR"


class VehicleType(str, Enum):
    MODIFICADO = "Modificado"
    DEPORTIVO = "Deportivo"
    CLASICO = "Clásico"
    EXCLUSIVO = "Exclusivo"
    SONIDO = "Sonido"
    OFFROAD = "Off-road"
    TUNING = "Tuning"
    OTRO = "Otro"


class EventCategory(str, Enum):
    MEET = "Meet"
    EXHIBICION = "Exhibición"
    RUTA = "Ruta"
    COMPETENCIA = "Competencia"
    EVENTO_SONIDO = "Evento de sonido"
    REUNION = "Reunión"
    OTRO = "Otro"
