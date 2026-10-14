"""Hora de Colombia (UTC-5, sin horario de verano).

MySQL guarda fechas sin zona horaria, así que el backend decide qué hora
es "ahora" en Colombia para comparar con la fecha/hora de los eventos.
"""
from datetime import datetime, timedelta, timezone

COLOMBIA_TZ = timezone(timedelta(hours=-5), "America/Bogota")


def now_colombia() -> datetime:
    """Fecha y hora actual de Colombia, como datetime 'ingenuo' (sin tzinfo)."""
    return datetime.now(COLOMBIA_TZ).replace(tzinfo=None)
