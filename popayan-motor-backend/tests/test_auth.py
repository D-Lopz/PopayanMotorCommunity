from sqlalchemy import select

from app.models import Member

REG = {"name": "Laura Gómez", "email": "Laura@PopayanMotor.com", "password": "Clave12345", "phone": "3001234567"}


def test_register_ok_and_password_is_hashed(client, db_session):
    r = client.post("/api/auth/register", json = {**REG, "role": "ADMINISTRATOR"})
    assert r.status_code == 201
    body = r.json()
    assert body["role"] == "MEMBER"
    assert body["email"] == "laura@popayanmotor.com"
    assert "password" not in body and "password_hash" not in body
    stored = db_session.scalar(select(Member).where(Member.id == body["id"]))
    assert stored.password_hash != "Clave12345" and stored.password_hash.startswith("$2")


def test_register_duplicate_email(client):
    client.post("/api/auth/register", json=REG)
    r = client.post("/api/auth/register", json=REG)
    assert r.status_code == 409 and r.json()["code"] == "EMAIL_EXISTS"


def test_register_invalid_data(client):
    r = client.post("/api/auth/register", json={"name": "L", "email": "no-es-correo", "password": "123"})
    assert r.status_code == 422
    assert r.json()["code"] == "VALIDATION_ERROR"
    assert {e["field"] for e in r.json()["errors"]} >= {"name", "email", "password"}


def test_login_ok_and_wrong_password(client):
    client.post("/api/auth/register", json=REG)
    ok = client.post("/api/auth/login", json={"email": "laura@popayanmotor.com", "password": "Clave12345"})
    assert ok.status_code == 200 and ok.json()["token_type"] == "bearer"
    assert ok.json()["member"]["role"] == "MEMBER"
    bad = client.post("/api/auth/login", json={"email": "laura@popayanmotor.com", "password": "otra-clave"})
    assert bad.status_code == 401 and bad.json()["code"] == "INVALID_CREDENTIALS"
    unknown = client.post("/api/auth/login", json={"email": "nadie@mail.com", "password": "Clave12345"})
    assert unknown.status_code == 401


def test_protected_routes_need_token(client):
    assert client.get("/api/events").status_code == 401
    r = client.get("/api/events", headers={"Authorization": "Bearer basura"})
    assert r.status_code == 401 and r.json()["code"] == "INVALID_TOKEN"


def test_register_requires_domain(client):
    for email in ("laura@mail.com", "laura@popayanmotor.com.co", "laura@sub.popayanmotor.com"):
        r = client.post("/api/auth/register", json={**REG, "email": email})
        assert r.status_code == 422, email
        assert r.json()["errors"][0]["field"] == "email"


def test_login_works_for_old_accounts_outside_domain(client, admin):
    # El fixture admin usa admin@mail.com (creado directo en la BD): debe seguir entrando.
    assert admin["Authorization"].startswith("Bearer ")
