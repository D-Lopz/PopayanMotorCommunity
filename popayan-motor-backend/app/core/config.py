"""Configuración: lee variables de entorno (archivo .env) en un solo lugar."""
import os

from dotenv import load_dotenv

load_dotenv()


def _required(name: str) -> str:
    # Sin valor por defecto: si falta en el servidor, la app no arranca en vez de usar una clave conocida.
    value = os.getenv(name)
    if not value:
        raise RuntimeError(f"Falta la variable de entorno {name} (ver .env.example)")
    return value


DATABASE_URL = _required("DATABASE_URL")
SECRET_KEY = _required("SECRET_KEY")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "120"))
