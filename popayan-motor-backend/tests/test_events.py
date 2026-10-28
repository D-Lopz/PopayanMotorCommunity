from datetime import date, timedelta

from tests.conftest import vehicle_body  # noqa: F401


def test_admin_crud_event(client, admin, make_event):
    event = make_event()
    assert event["registered_count"] == 0
    r = client.get(f"/api/events/{event['id']}", headers=admin)
    assert r.status_code == 200 and r.json()["name"] == "Meet de Noviembre"

    upd = {**event, "name": "Meet renombrado", "max_capacity": 50}
    for k in ("id", "registered_count"):
        upd.pop(k)
    r = client.put(f"/api/events/{event['id']}", json=upd, headers=admin)
    assert r.status_code == 200 and r.json()["max_capacity"] == 50

    assert client.delete(f"/api/events/{event['id']}", headers=admin).status_code == 204
    assert client.get(f"/api/events/{event['id']}", headers=admin).status_code == 404


def test_member_can_view_event_detail(client, member, make_event):
    event = make_event(name="Ruta al Puracé", distance_km=86)
    r = client.get(f"/api/events/{event['id']}", headers=member)
    assert r.status_code == 200
    assert r.json()["name"] == "Ruta al Puracé" and r.json()["registered_count"] == 0
    assert client.get(f"/api/events/{event['id']}").status_code == 401  # sin sesión no
    r = client.get("/api/events/999999", headers=member)
    assert r.status_code == 404 and r.json()["code"] == "EVENT_NOT_FOUND"


def test_member_cannot_create_event(client, member):
    r = client.post("/api/events", headers=member, json={
        "name": "X evento", "date": str(date.today() + timedelta(days=5)), "time": "10:00",
        "location": "Lugar", "max_capacity": 10, "category": "Meet"})
    assert r.status_code == 403 and r.json()["code"] == "FORBIDDEN"


def test_event_validations(client, admin):
    base = {"name": "Evento", "date": str(date.today() + timedelta(days=5)), "time": "10:00",
            "location": "Lugar", "max_capacity": 10, "category": "Meet"}
    assert client.post("/api/events", json={**base, "max_capacity": 0}, headers=admin).status_code == 422
    assert client.post("/api/events", json={**base, "category": "Inventada"}, headers=admin).status_code == 422
    past = client.post("/api/events", json={**base, "date": str(date.today() - timedelta(days=1))}, headers=admin)
    assert past.status_code == 422 and past.json()["code"] == "INVALID_EVENT_DATE"


def test_filters_and_search(client, member, make_event):
    make_event(name="Meet Parque", category="Meet", location="Parque Caldas")
    make_event(name="Ruta al Puracé", category="Ruta", location="Puracé")
    r = client.get("/api/events?category=Meet", headers=member)
    assert [e["name"] for e in r.json()] == ["Meet Parque"]
    r = client.get("/api/events?search=purac", headers=member)
    assert [e["name"] for e in r.json()] == ["Ruta al Puracé"]
    r = client.get(f"/api/events?date={date.today() + timedelta(days=30)}", headers=member)
    assert len(r.json()) == 2
    assert client.get("/api/events?category=Inventada", headers=member).status_code == 422


def test_cannot_delete_event_with_registrations(client, admin, member, make_event, register):
    event = make_event()
    register(member, event["id"])
    r = client.delete(f"/api/events/{event['id']}", headers=admin)
    assert r.status_code == 409 and r.json()["code"] == "EVENT_HAS_REGISTRATIONS"


def test_cannot_reduce_capacity_below_registered(client, admin, member, member2, make_event, register):
    event = make_event(max_capacity=5)
    for h in (member, member2):
        register(h, event["id"])
    upd = {k: v for k, v in event.items() if k not in ("id", "registered_count")}
    r = client.put(f"/api/events/{event['id']}", json={**upd, "max_capacity": 1}, headers=admin)
    assert r.status_code == 409 and r.json()["code"] == "CAPACITY_BELOW_REGISTERED"
    assert client.put(f"/api/events/{event['id']}", json={**upd, "max_capacity": 2}, headers=admin).status_code == 200


def test_member_cannot_edit_or_delete_event(client, member, make_event):
    event = make_event()
    body = {"name": "Hackeado", "date": event["date"], "time": event["time"], "location": "Lugar",
            "max_capacity": 10, "category": "Meet"}
    for r in (client.put(f"/api/events/{event['id']}", json=body, headers=member),
              client.delete(f"/api/events/{event['id']}", headers=member)):
        assert r.status_code == 403 and r.json()["code"] == "FORBIDDEN"
    assert client.get(f"/api/events/{event['id']}", headers=member).json()["name"] == event["name"]  # sin cambios


# ── Autoría del evento (created_by): solo para el contador del admin, no da permisos ──

def _event_body(name="Meet"):
    return {"name": name, "date": str(date.today() + timedelta(days=10)), "time": "16:00",
            "location": "Parque Caldas", "max_capacity": 10, "category": "Meet"}


def _create(client, headers, name="Meet", **extra):
    r = client.post("/api/events", json={**_event_body(name), **extra}, headers=headers)
    assert r.status_code == 201, r.text
    return r.json()["id"]


def _my_id(client, headers):
    from app.core.security import decode_access_token
    return int(decode_access_token(headers["Authorization"].split()[1])["sub"])


def _count(client, headers, admin_id):
    return len(client.get(f"/api/events?created_by={admin_id}", headers=headers).json())


def test_creator_comes_from_token_not_body(client, admin, admin2, db_session):
    from app.models import Event
    a, b = _my_id(client, admin), _my_id(client, admin2)
    event_id = _create(client, admin, created_by=b)  # intento de suplantar: se ignora
    assert db_session.get(Event, event_id).created_by == a


def test_counter_is_independent_per_admin(client, admin, admin2, member):
    a, b = _my_id(client, admin), _my_id(client, admin2)
    for i in range(2):
        _create(client, admin, f"Evento A{i}")
    for i in range(3):
        _create(client, admin2, f"Evento B{i}")
    assert (_count(client, admin, a), _count(client, admin, b)) == (2, 3)
    _create(client, admin2, "Evento B3")
    assert (_count(client, admin2, a), _count(client, admin2, b)) == (2, 4)
    assert len(client.get("/api/events", headers=member).json()) == 6  # sin filtro: todos


def test_edit_keeps_creator_and_permissions_stay_global(client, admin, admin2, db_session):
    from app.models import Event
    a = _my_id(client, admin)
    event_id = _create(client, admin)
    # otro admin puede editarlo (permisos globales sin cambios) y el autor no cambia
    r = client.put(f"/api/events/{event_id}", json={**_event_body("Editado"), "created_by": 999}, headers=admin2)
    assert r.status_code == 200
    db_session.expire_all()
    assert db_session.get(Event, event_id).created_by == a
    assert "created_by" not in r.json()  # no se expone a la app


def test_unknown_creator_counts_for_nobody(client, admin, db_session):
    from datetime import time

    from app.models import Event
    from app.models.enums import EventCategory
    # como los eventos anteriores a la migración 003: sin autor
    db_session.add(Event(name="Evento viejo", date=date.today() + timedelta(days=3), time=time(9, 0),
                         location="Centro", max_capacity=5, category=EventCategory.MEET))
    db_session.commit()
    assert _count(client, admin, _my_id(client, admin)) == 0
    assert len(client.get("/api/events", headers=admin).json()) == 1  # sigue visible


def test_member_counter_still_counts_registrations(client, admin, member, make_event, register):
    event = make_event()
    register(member, event["id"])
    me = _my_id(client, member)
    assert len(client.get(f"/api/registrations?member_id={me}", headers=member).json()) == 1
    assert _count(client, member, me) == 0
