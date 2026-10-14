from tests.conftest import vehicle_body


def test_vehicle_crud_and_ownership(client, member, member2):
    r = client.post("/api/vehicles", json=vehicle_body(plate="abc-123"), headers=member)
    assert r.status_code == 201 and r.json()["plate"] == "ABC123"
    vid = r.json()["id"]

    assert client.get(f"/api/vehicles/{vid}", headers=member2).status_code == 403
    assert client.put(f"/api/vehicles/{vid}", json=vehicle_body(), headers=member2).status_code == 403
    assert client.delete(f"/api/vehicles/{vid}", headers=member2).status_code == 403
    assert client.get("/api/vehicles", headers=member2).json() == []

    r = client.put(f"/api/vehicles/{vid}", json=vehicle_body(model="M4"), headers=member)
    assert r.status_code == 200 and r.json()["model"] == "M4"
    assert client.delete(f"/api/vehicles/{vid}", headers=member).status_code == 204
    assert client.get(f"/api/vehicles/{vid}", headers=member).status_code == 404


def test_vehicle_validations_and_duplicate_plate(client, member, member2):
    assert client.post("/api/vehicles", json=vehicle_body(year=1800), headers=member).status_code == 422
    assert client.post("/api/vehicles", json=vehicle_body(year=2099), headers=member).status_code == 422
    assert client.post("/api/vehicles", json=vehicle_body(type="Volador"), headers=member).status_code == 422
    assert client.post("/api/vehicles", json=vehicle_body(plate="!!"), headers=member).status_code == 422
    assert client.post("/api/vehicles", json=vehicle_body(), headers=member).status_code == 201
    r = client.post("/api/vehicles", json=vehicle_body(), headers=member2)
    assert r.status_code == 409 and r.json()["code"] == "PLATE_EXISTS"


def test_vehicle_filters(client, member, admin):
    client.post("/api/vehicles", json=vehicle_body(), headers=member)
    client.post("/api/vehicles", json=vehicle_body(brand="Toyota", model="Hilux", type="Off-road", plate="XYZ789"), headers=member)
    assert len(client.get("/api/vehicles?brand=bmw", headers=member).json()) == 1
    assert len(client.get("/api/vehicles?type=Off-road", headers=member).json()) == 1
    assert len(client.get("/api/vehicles", headers=admin).json()) == 2  # admin ve todos


def test_members_permissions(client, admin, member, member2):
    assert client.get("/api/members", headers=member).status_code == 403
    members = client.get("/api/members", headers=admin).json()
    assert len(members) == 3 and all("password_hash" not in m for m in members)
    laura = next(m for m in members if m["name"] == "Laura")
    pedro = next(m for m in members if m["name"] == "Pedro")

    assert client.get(f"/api/members/{laura['id']}", headers=member).status_code == 200
    assert client.get(f"/api/members/{pedro['id']}", headers=member).status_code == 403
    r = client.put(f"/api/members/{laura['id']}", json={"name": "Laura G.", "phone": "3009998888"}, headers=member)
    assert r.status_code == 200 and r.json()["name"] == "Laura G."
    assert client.put(f"/api/members/{pedro['id']}", json={"name": "Hack"}, headers=member).status_code == 403
    assert client.get("/api/members?search=pedro", headers=admin).json()[0]["name"] == "Pedro"


def test_delete_member_cascades_and_self_delete_blocked(client, admin, member, make_event):
    event = make_event()
    client.post("/api/vehicles", json=vehicle_body(), headers=member)
    client.post("/api/registrations", json={"event_id": event["id"]}, headers=member)
    members = client.get("/api/members", headers=admin).json()
    laura = next(m for m in members if m["name"] == "Laura")
    admin_row = next(m for m in members if m["name"] == "Admin")
    assert client.delete(f"/api/members/{admin_row['id']}", headers=admin).status_code == 409
    assert client.delete(f"/api/members/{laura['id']}", headers=admin).status_code == 204
    assert client.get("/api/vehicles", headers=admin).json() == []
    assert client.get("/api/registrations", headers=admin).json() == []
    assert client.get(f"/api/members/{laura['id']}", headers=admin).status_code == 404
