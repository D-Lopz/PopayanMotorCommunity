from datetime import date, time, timedelta

from app.models import Event
from app.models.enums import EventCategory


def test_register_ok_and_count(client, member, make_event, register):
    event = make_event()
    r = register(member, event["id"])
    assert r.status_code == 201
    assert r.json()["event"]["name"] == "Meet de Noviembre"
    assert client.get(f"/api/events/{event['id']}", headers=member).json()["registered_count"] == 1


def test_duplicate_registration_rejected(client, member, make_event, register):
    event = make_event()
    register(member, event["id"])
    r = register(member, event["id"])
    assert r.status_code == 409 and r.json()["code"] == "ALREADY_REGISTERED"


def test_capacity_limit(client, member, member2, make_event, register):
    event = make_event(max_capacity=1)
    assert register(member, event["id"]).status_code == 201
    r = register(member2, event["id"])
    assert r.status_code == 409 and r.json()["code"] == "EVENT_FULL"


def test_cancel_frees_a_spot(client, member, member2, make_event, register):
    event = make_event(max_capacity=1)
    reg = register(member, event["id"]).json()
    assert client.delete(f"/api/registrations/{reg['id']}", headers=member).status_code == 204
    assert register(member2, event["id"]).status_code == 201


def test_past_event_rejected(client, member, db_session, register):
    past = Event(name="Evento viejo", date=date.today() - timedelta(days=2), time=time(10, 0),
                 location="Parque", max_capacity=10, category=EventCategory.MEET)
    db_session.add(past)
    db_session.commit()
    r = register(member, past.id)
    assert r.status_code == 409 and r.json()["code"] == "EVENT_PAST"


def test_unknown_event_404(client, member, register):
    r = register(member, 9999)
    assert r.status_code == 404 and r.json()["code"] == "EVENT_NOT_FOUND"
    assert client.post("/api/registrations", json={}, headers=member).status_code == 422


def test_visibility_rules(client, admin, member, member2, make_event, register):
    event = make_event()
    reg = register(member, event["id"]).json()
    # el otro miembro no ve ni cancela la inscripción ajena, y su lista está vacía
    assert client.get(f"/api/registrations/{reg['id']}", headers=member2).status_code == 403
    assert client.delete(f"/api/registrations/{reg['id']}", headers=member2).status_code == 403
    assert client.get("/api/registrations", headers=member2).json() == []
    assert client.get(f"/api/registrations?member_id={reg['member_id']}", headers=member2).status_code == 403
    # el admin ve los participantes de un evento
    r = client.get(f"/api/registrations?event_id={event['id']}", headers=admin)
    assert len(r.json()) == 1 and r.json()[0]["member"]["name"] == "Laura"
    assert client.get("/api/registrations/9999", headers=admin).status_code == 404


def _event_in_db(db_session, days_ago, end_date=None):
    """Evento que empezó hace `days_ago` días (la API no deja crear eventos en el pasado)."""
    event = Event(name="Rodada", date=date.today() - timedelta(days=days_ago), end_date=end_date,
                  time=time(8, 0), location="Puracé", max_capacity=10, category=EventCategory.RUTA)
    db_session.add(event)
    db_session.commit()
    return event


def _register_in_db(db_session, client, headers, event, vehicle_id):
    from app.models import Registration
    member_id = client.get("/api/vehicles/%d" % vehicle_id, headers=headers).json()["member_id"]
    reg = Registration(member_id=member_id, event_id=event.id, vehicle_id=vehicle_id, code="PMC-TEST22")
    db_session.add(reg)
    db_session.commit()
    return reg


def test_companions_do_not_use_capacity(client, member, member2, make_event, register):
    event = make_event(max_capacity=1)
    r = register(member, event["id"], companions=5)
    assert r.status_code == 201 and r.json()["companions"] == 5
    ev = client.get(f"/api/events/{event['id']}", headers=member).json()
    assert ev["registered_count"] == 1 and ev["attendees_count"] == 6
    assert register(member2, event["id"]).json()["code"] == "EVENT_FULL"
    assert register(member2, event["id"], companions=11).status_code == 422


def test_vehicle_must_be_own(client, member, member2, make_event, make_vehicle, register):
    event = make_event()
    foreign = make_vehicle(member2)
    r = register(member, event["id"], vehicle_id=foreign["id"])
    assert r.status_code == 404 and r.json()["code"] == "VEHICLE_NOT_FOUND"
    assert client.post("/api/registrations", json={"event_id": event["id"]}, headers=member).status_code == 422


def test_code_format_and_expiry(client, member, member2, make_event, register):
    event = make_event(date=str(date.today() + timedelta(days=5)), end_date=str(date.today() + timedelta(days=6)))
    a = register(member, event["id"]).json()
    b = register(member2, event["id"]).json()
    alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    for code in (a["code"], b["code"]):
        assert code.startswith("PMC-") and len(code) == 10 and all(c in alphabet for c in code[4:])
    assert a["code"] != b["code"]
    # vence a las 00:01 del día siguiente al último día del evento
    assert a["code_expires_at"] == f"{date.today() + timedelta(days=7)}T00:01:00"


def test_end_date_controls_finished(client, admin, member, db_session):
    _event_in_db(db_session, days_ago=1)  # terminó ayer: hoy ya pasó las 00:01
    _event_in_db(db_session, days_ago=1, end_date=date.today())  # dura hasta hoy
    finished = [e["finished"] for e in client.get("/api/events", headers=member).json()]
    assert finished == [True, False]
    bad = {"name": "Mal", "date": str(date.today() + timedelta(days=5)), "end_date": str(date.today()),
           "time": "10:00", "location": "X", "max_capacity": 5, "category": "Meet"}
    assert client.post("/api/events", json=bad, headers=admin).status_code == 422


def test_change_vehicle_then_delete(client, member, member2, make_event, make_vehicle, register):
    event = make_event()
    first = make_vehicle(member)
    reg = register(member, event["id"], vehicle_id=first["id"]).json()
    r = client.delete(f"/api/vehicles/{first['id']}", headers=member)
    assert r.status_code == 409 and r.json()["code"] == "VEHICLE_HAS_REGISTRATIONS"
    assert "Cancela" in r.json()["detail"]  # no tiene otro carro

    second = make_vehicle(member)
    r = client.delete(f"/api/vehicles/{first['id']}", headers=member)
    assert r.status_code == 409 and "otro vehículo" in r.json()["detail"]

    assert client.put(f"/api/registrations/{reg['id']}", json={"vehicle_id": second["id"]}, headers=member2).status_code == 403
    r = client.put(f"/api/registrations/{reg['id']}", json={"vehicle_id": second["id"]}, headers=member)
    assert r.status_code == 200 and r.json()["vehicle"]["id"] == second["id"]
    assert client.delete(f"/api/vehicles/{first['id']}", headers=member).status_code == 204


def test_finished_registration_does_not_block_vehicle_delete(client, member, db_session, make_vehicle):
    vehicle = make_vehicle(member)
    old = _event_in_db(db_session, days_ago=3)
    reg = _register_in_db(db_session, client, member, old, vehicle["id"])
    r = client.put(f"/api/registrations/{reg.id}", json={"vehicle_id": vehicle["id"]}, headers=member)
    assert r.status_code == 409 and r.json()["code"] == "EVENT_FINISHED"
    assert client.delete(f"/api/vehicles/{vehicle['id']}", headers=member).status_code == 204
    r = client.get(f"/api/registrations/{reg.id}", headers=member).json()
    assert r["vehicle_id"] is None and r["vehicle"] is None
