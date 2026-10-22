"""Prueba manual de la Regla 1 con peticiones simultáneas (requiere el servidor corriendo y un admin).
Uso:  python tests/concurrency_check.py  [admin_email] [admin_password]
Crea un evento de 3 cupos, inscribe 10 usuarios a la vez y debe terminar con 3 inscritos exactos."""
import sys
import threading
import time
from datetime import date, timedelta

import httpx

B = "http://localhost:8000/api"
email = sys.argv[1] if len(sys.argv) > 1 else "admin@popayanmotor.com"
password = sys.argv[2] if len(sys.argv) > 2 else "Admin12345"
stamp = str(int(time.time()))
adm = {"Authorization": "Bearer " + httpx.post(B + "/auth/login", json={"email": email, "password": password}).json()["access_token"]}
ev = httpx.post(B + "/events", headers=adm, json={"name": "Concurrencia", "date": str(date.today() + timedelta(days=9)),
                "time": "10:00", "location": "Prueba", "max_capacity": 3, "category": "Meet"}).json()
tokens = []
for i in range(10):
    httpx.post(B + "/auth/register", json={"name": f"User {i}", "email": f"u{i}{stamp}@mail.com", "password": "Clave12345"})
    t = httpx.post(B + "/auth/login", json={"email": f"u{i}{stamp}@mail.com", "password": "Clave12345"}).json()["access_token"]
    v = httpx.post(B + "/vehicles", headers={"Authorization": "Bearer " + t}, json={"brand": "Mazda", "model": "MX-5", "year": 2020,
                   "type": "Deportivo", "plate": f"C{stamp[-5:]}{i}"}).json()
    tokens.append((t, v["id"]))
results = []
def go(tv):
    t, vid = tv
    results.append(httpx.post(B + "/registrations", headers={"Authorization": "Bearer " + t}, json={"event_id": ev["id"], "vehicle_id": vid}).status_code)
threads = [threading.Thread(target=go, args=(t,)) for t in tokens]
[t.start() for t in threads]; [t.join() for t in threads]
final = httpx.get(f"{B}/events/{ev['id']}", headers=adm).json()["registered_count"]
print(f"201 creadas: {results.count(201)} | 409 rechazadas: {results.count(409)} | inscritos finales: {final} de 3")
print("RESULTADO:", "OK" if final == 3 and results.count(201) == 3 else "FALLA")
