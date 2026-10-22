"""Fixtures: por defecto usa SQLite en memoria; con TEST_DATABASE_URL usa MySQL de pruebas."""
import itertools
import os
from datetime import date, timedelta

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, event
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.core.database import Base, get_db
from app.core.security import hash_password
from app.core.timeutils import now_colombia
from app.main import app
from app.models import Member
from app.models.enums import Role

TEST_URL = os.getenv("TEST_DATABASE_URL", "sqlite://")


@pytest.fixture()
def db_session():
    kwargs = {"connect_args": {"check_same_thread": False}, "poolclass": StaticPool} if TEST_URL.startswith("sqlite") else {}
    engine = create_engine(TEST_URL, **kwargs)
    if TEST_URL.startswith("sqlite"):
        @event.listens_for(engine, "connect")
        def _fk_on(dbapi_conn, _):  # SQLite ignora las claves foráneas si no se activan
            dbapi_conn.execute("PRAGMA foreign_keys=ON")
    Base.metadata.drop_all(engine)
    Base.metadata.create_all(engine)
    Session = sessionmaker(bind=engine, autoflush=False)
    session = Session()
    yield session
    session.close()
    Base.metadata.drop_all(engine)
    engine.dispose()


@pytest.fixture()
def client(db_session):
    def override():
        yield db_session

    app.dependency_overrides[get_db] = override
    yield TestClient(app)
    app.dependency_overrides.clear()


def _make_user(db, name, email, role):
    db.add(Member(name=name, email=email, password_hash=hash_password("Clave12345"), role=role))
    db.commit()


def _login(client, email):
    r = client.post("/api/auth/login", json={"email": email, "password": "Clave12345"})
    assert r.status_code == 200, r.text
    return {"Authorization": f"Bearer {r.json()['access_token']}"}


@pytest.fixture()
def admin(client, db_session):
    _make_user(db_session, "Admin", "admin@mail.com", Role.ADMINISTRATOR)
    return _login(client, "admin@mail.com")


@pytest.fixture()
def member(client, db_session):
    _make_user(db_session, "Laura", "laura@mail.com", Role.MEMBER)
    return _login(client, "laura@mail.com")


@pytest.fixture()
def member2(client, db_session):
    _make_user(db_session, "Pedro", "pedro@mail.com", Role.MEMBER)
    return _login(client, "pedro@mail.com")


@pytest.fixture()
def make_event(client, admin):
    def _make(**overrides):
        body = {
            "name": "Meet de Noviembre",
            "description": "Encuentro mensual",
            "date": str(date.today() + timedelta(days=30)),
            "time": "16:00",
            "location": "Parque Caldas",
            "max_capacity": 30,
            "category": "Meet",
        }
        body.update(overrides)
        r = client.post("/api/events", json=body, headers=admin)
        assert r.status_code == 201, r.text
        return r.json()

    return _make


def vehicle_body(**overrides):
    body = {"brand": "BMW", "model": "M3", "year": 2018, "type": "Deportivo", "plate": "ABC123", "description": "Stage 1"}
    body.update(overrides)
    return body


_plates = itertools.count(100)


@pytest.fixture()
def make_vehicle(client):
    def _make(headers, **overrides):
        r = client.post("/api/vehicles", json=vehicle_body(plate=f"AAA{next(_plates)}", **overrides), headers=headers)
        assert r.status_code == 201, r.text
        return r.json()

    return _make


@pytest.fixture()
def register(client, make_vehicle):
    """Inscribe al miembro; si no se da vehicle_id, le crea un vehículo."""
    def _register(headers, event_id, **extra):
        if "vehicle_id" not in extra:
            extra["vehicle_id"] = make_vehicle(headers)["id"]
        return client.post("/api/registrations", json={"event_id": event_id, **extra}, headers=headers)

    return _register
