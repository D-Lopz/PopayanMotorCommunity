from datetime import date, time, timedelta

from app.models import Event
from app.models.enums import EventCategory


def test_register_ok_and_count(client, member, make_event):
    event = make_event()
    r = client.post("/api/registrations", json={"event_id": event["id"]}, headers=member)
    assert r.status_code == 201
    assert r.json()["event"]["name"] == "Meet de Noviembre"
    assert client.get(f"/api/events/{event['id']}", headers=member).json()["registered_count"] == 1


def test_duplicate_registration_rejected(client, member, make_event):
    event = make_event()
    client.post("/api/registrations", json={"event_id": event["id"]}, headers=member)
    r = client.post("/api/registrations", json={"event_id": event["id"]}, headers=member)
    assert r.status_code == 409 and r.json()["code"] == "ALREADY_REGISTERED"


def test_capacity_limit(client, member, member2, make_event):
    event = make_event(max_capacity=1)
    assert client.post("/api/registrations", json={"event_id": event["id"]}, headers=member).status_code == 201
    r = client.post("/api/registrations", json={"event_id": event["id"]}, headers=member2)
    assert r.status_code == 409 and r.json()["code"] == "EVENT_FULL"


def test_cancel_frees_a_spot(client, member, member2, make_event):
    event = make_event(max_capacity=1)
    reg = client.post("/api/registrations", json={"event_id": event["id"]}, headers=member).json()
    assert client.delete(f"/api/registrations/{reg['id']}", headers=member).status_code == 204
    assert client.post("/api/registrations", json={"event_id": event["id"]}, headers=member2).status_code == 201


def test_past_event_rejected(client, member, db_session):
    past = Event(name="Evento viejo", date=date.today() - timedelta(days=2), time=time(10, 0),
                 location="Parque", max_capacity=10, category=EventCategory.MEET)
    db_session.add(past)
    db_session.commit()
    r = client.post("/api/registrations", json={"event_id": past.id}, headers=member)
    assert r.status_code == 409 and r.json()["code"] == "EVENT_PAST"


def test_unknown_event_404(client, member):
    r = client.post("/api/registrations", json={"event_id": 9999}, headers=member)
    assert r.status_code == 404 and r.json()["code"] == "EVENT_NOT_FOUND"
    assert client.post("/api/registrations", json={}, headers=member).status_code == 422


def test_visibility_rules(client, admin, member, member2, make_event):
    event = make_event()
    reg = client.post("/api/registrations", json={"event_id": event["id"]}, headers=member).json()
    # el otro miembro no ve ni cancela la inscripción ajena, y su lista está vacía
    assert client.get(f"/api/registrations/{reg['id']}", headers=member2).status_code == 403
    assert client.delete(f"/api/registrations/{reg['id']}", headers=member2).status_code == 403
    assert client.get("/api/registrations", headers=member2).json() == []
    assert client.get(f"/api/registrations?member_id={reg['member_id']}", headers=member2).status_code == 403
    # el admin ve los participantes de un evento
    r = client.get(f"/api/registrations?event_id={event['id']}", headers=admin)
    assert len(r.json()) == 1 and r.json()[0]["member"]["name"] == "Laura"
    assert client.get("/api/registrations/9999", headers=admin).status_code == 404
