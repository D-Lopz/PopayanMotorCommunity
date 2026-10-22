"""Punto de entrada de la API"""
from fastapi import FastAPI

from app.core.errors import register_error_handlers
from app.routers import auth, events, members, registrations, vehicles

app = FastAPI(
    title="Popayán Motor Community API",
    description="API REST para la app Android de la comunidad automotriz de Popayán.",
    version="1.1.0",
)

register_error_handlers(app)

for router in (auth.router, members.router, vehicles.router, events.router, registrations.router):
    app.include_router(router, prefix="/api")
