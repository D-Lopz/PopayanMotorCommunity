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
