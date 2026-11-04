"""Conexión a MySQL con SQLAlchemy."""
from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from app.core.config import DATABASE_URL, DB_SSL_CA

# Con "ca", PyMySQL verifica el certificado y el nombre del servidor.
connect_args = {"ssl": {"ca": DB_SSL_CA}} if DB_SSL_CA else {}
engine = create_engine(DATABASE_URL, pool_pre_ping=True, connect_args=connect_args)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


class Base(DeclarativeBase):
    """Clase base de todas las tablas."""


def get_db():
    """Entrega una sesión de BD por petición y la cierra al terminar."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
