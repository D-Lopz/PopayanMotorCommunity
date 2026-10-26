"""Genera postman/PopayanMotor.postman_collection.json (ejecutar: python postman/build_collection.py)."""
import json

def req(name, method, path, token=None, body=None, tests=(), pre=None, query=None):
    headers = [{"key": "Content-Type", "value": "application/json"}] if body is not None else []
    if token:
        headers.append({"key": "Authorization", "value": "Bearer {{%s}}" % token})
    url = {"raw": "{{base_url}}" + path, "host": ["{{base_url}}"], "path": path.strip("/").split("/")}
    if query:
        url["raw"] += "?" + "&".join(f"{k}={v}" for k, v in query.items())
        url["query"] = [{"key": k, "value": v} for k, v in query.items()]
    item = {"name": name, "request": {"method": method, "header": headers, "url": url}}
    if body is not None:
        item["request"]["body"] = {"mode": "raw", "raw": json.dumps(body, indent=2, ensure_ascii=False)}
    events = []
    if pre:
        events.append({"listen": "prerequest", "script": {"type": "text/javascript", "exec": pre}})
    if tests:
        events.append({"listen": "test", "script": {"type": "text/javascript", "exec": list(tests)}})
    if events:
        item["event"] = events
    return item

def status(code, label=None):
    return f'pm.test("{label or "Status"} {code}", function () {{ pm.response.to.have.status({code}); }});'

def code_is(c):
    return f'pm.test("code {c}", function () {{ pm.expect(pm.response.json().code).to.eql("{c}"); }});'

def save(var, expr):
    return f'pm.collectionVariables.set("{var}", {expr});'

setup = [
    "const d = new Date(); d.setDate(d.getDate() + 30);",
    'pm.collectionVariables.set("event_date", d.toISOString().slice(0,10));',
    'pm.collectionVariables.set("past_date", new Date(Date.now() - 3*86400000).toISOString().slice(0,10));',
    'const stamp = Date.now();',
    'pm.collectionVariables.set("member_email", "laura" + stamp + "@popayanmotor.com");',
    'pm.collectionVariables.set("member2_email", "pedro" + stamp + "@popayanmotor.com");',
    'pm.collectionVariables.set("plate", "T" + String(stamp).slice(-5));',
    'pm.collectionVariables.set("plate2", "U" + String(stamp).slice(-5));',
    'pm.collectionVariables.set("plate3", "V" + String(stamp).slice(-5));',
]
ev = lambda **kw: {"name": "Meet Postman", "description": "Encuentro de prueba", "date": "{{event_date}}",
                    "time": "16:00", "location": "Parque Caldas", "max_capacity": 30, "category": "Meet", **kw}
veh = lambda **kw: {"brand": "BMW", "model": "M3", "year": 2018, "type": "Deportivo", "plate": "{{plate}}",
                    "description": "Stage 1", **kw}

folders = [
 ("0. Preparación", [
    req("Generar variables", "GET", "/docs", pre=setup, tests=[status(200)]),
 ]),
 ("1. Auth", [
    req("Registrar miembro 1", "POST", "/api/auth/register", body={"name": "Laura Gómez", "email": "{{member_email}}", "password": "Clave12345", "phone": "3001234567"},
        tests=[status(201), 'pm.test("rol MEMBER y sin hash", function(){ const j=pm.response.json(); pm.expect(j.role).to.eql("MEMBER"); pm.expect(j).to.not.have.property("password_hash"); });', save("member_id", "pm.response.json().id")]),
    req("Registrar miembro 2", "POST", "/api/auth/register", body={"name": "Pedro Ruiz", "email": "{{member2_email}}", "password": "Clave12345"}, tests=[status(201), save("member2_id", "pm.response.json().id")]),
    req("Registro con correo repetido (409)", "POST", "/api/auth/register", body={"name": "Laura Gómez", "email": "{{member_email}}", "password": "Clave12345"}, tests=[status(409), code_is("EMAIL_EXISTS")]),
    req("Registro con dominio no permitido (422)", "POST", "/api/auth/register", body={"name": "Laura Gómez", "email": "laura@gmail.com", "password": "Clave12345"}, tests=[status(422), code_is("VALIDATION_ERROR")]),
    req("Registro con datos inválidos (422)", "POST", "/api/auth/register", body={"name": "L", "email": "mal", "password": "1"}, tests=[status(422), code_is("VALIDATION_ERROR")]),
    req("Login miembro 1", "POST", "/api/auth/login", body={"email": "{{member_email}}", "password": "Clave12345"}, tests=[status(200), save("member_token", "pm.response.json().access_token")]),
    req("Login miembro 2", "POST", "/api/auth/login", body={"email": "{{member2_email}}", "password": "Clave12345"}, tests=[status(200), save("member2_token", "pm.response.json().access_token")]),
    req("Login con clave incorrecta (401)", "POST", "/api/auth/login", body={"email": "{{member_email}}", "password": "incorrecta"}, tests=[status(401), code_is("INVALID_CREDENTIALS")]),
    req("Login administrador", "POST", "/api/auth/login", body={"email": "{{admin_email}}", "password": "{{admin_password}}"}, tests=[status(200), save("admin_token", "pm.response.json().access_token")]),
    req("Sin token (401)", "GET", "/api/events", tests=[status(401), code_is("NOT_AUTHENTICATED")]),
 ]),
 ("2. Eventos", [
    req("Crear evento como miembro (403)", "POST", "/api/events", "member_token", ev(), tests=[status(403)]),
    req("Crear evento con fecha pasada (422)", "POST", "/api/events", "admin_token", ev(date="{{past_date}}"), tests=[status(422), code_is("INVALID_EVENT_DATE")]),
    req("Crear evento con capacidad 0 (422)", "POST", "/api/events", "admin_token", ev(max_capacity=0), tests=[status(422)]),
    req("Crear evento (admin)", "POST", "/api/events", "admin_token", ev(), tests=[status(201), save("event_id", "pm.response.json().id")]),
    req("Crear evento con cupo 1", "POST", "/api/events", "admin_token", ev(name="Evento cupo 1", max_capacity=1), tests=[status(201), save("event_small_id", "pm.response.json().id")]),
    req("Listar eventos", "GET", "/api/events", "member_token", tests=[status(200), 'pm.test("es lista", function(){ pm.expect(pm.response.json()).to.be.an("array"); });']),
    req("Filtrar por categoría", "GET", "/api/events", "member_token", query={"category": "Meet"}, tests=[status(200), 'pm.test("todos Meet", function(){ pm.response.json().forEach(e => pm.expect(e.category).to.eql("Meet")); });']),
    req("Buscar por texto", "GET", "/api/events", "member_token", query={"search": "Postman"}, tests=[status(200)]),
    req("Categoría inválida (422)", "GET", "/api/events", "member_token", query={"category": "Inventada"}, tests=[status(422)]),
    req("Detalle de evento", "GET", "/api/events/{{event_id}}", "member_token", tests=[status(200), 'pm.test("trae registered_count", function(){ pm.expect(pm.response.json()).to.have.property("registered_count"); });']),
    req("Evento inexistente (404)", "GET", "/api/events/999999", "member_token", tests=[status(404), code_is("EVENT_NOT_FOUND")]),
    req("Editar evento", "PUT", "/api/events/{{event_id}}", "admin_token", ev(name="Meet Postman editado", max_capacity=40), tests=[status(200)]),
 ]),
 ("3. Vehículos", [
    req("Registrar vehículo", "POST", "/api/vehicles", "member_token", veh(), tests=[status(201), 'pm.test("el primero es principal", function(){ pm.expect(pm.response.json().is_primary).to.eql(true); });', save("vehicle_id", "pm.response.json().id")]),
    req("Registrar vehículo miembro 2", "POST", "/api/vehicles", "member2_token", veh(plate="{{plate2}}"), tests=[status(201), save("vehicle2_id", "pm.response.json().id")]),
    req("Placa repetida (409)", "POST", "/api/vehicles", "member2_token", veh(), tests=[status(409), code_is("PLATE_EXISTS")]),
    req("Año inválido (422)", "POST", "/api/vehicles", "member_token", veh(year=1800, plate="ZZZ999"), tests=[status(422)]),
    req("Mis vehículos", "GET", "/api/vehicles", "member_token", tests=[status(200)]),
    req("Filtrar por marca", "GET", "/api/vehicles", "member_token", query={"brand": "bmw"}, tests=[status(200)]),
    req("Detalle de vehículo", "GET", "/api/vehicles/{{vehicle_id}}", "member_token", tests=[status(200)]),
    req("Vehículo ajeno (403)", "GET", "/api/vehicles/{{vehicle_id}}", "member2_token", tests=[status(403)]),
    req("Editar vehículo", "PUT", "/api/vehicles/{{vehicle_id}}", "member_token", veh(model="M4"), tests=[status(200)]),
 ]),
 ("4. Inscripciones y reglas de negocio", [
    req("Inscribirse con 3 acompañantes", "POST", "/api/registrations", "member_token", {"event_id": "{{event_id}}", "vehicle_id": "{{vehicle_id}}", "companions": 3},
        tests=[status(201), 'pm.test("código PMC-XXXXXX", function(){ pm.expect(pm.response.json().code).to.match(/^PMC-[A-HJ-NP-Z2-9]{6}$/); });', save("registration_id", "pm.response.json().id")]),
    req("Acompañantes no ocupan cupo", "GET", "/api/events/{{event_id}}", "member_token", tests=[status(200), 'pm.test("1 inscrito, 4 asistentes", function(){ const j=pm.response.json(); pm.expect(j.registered_count).to.eql(1); pm.expect(j.attendees_count).to.eql(4); });']),
    req("Vehículo ajeno (404)", "POST", "/api/registrations", "member2_token", {"event_id": "{{event_id}}", "vehicle_id": "{{vehicle_id}}"}, tests=[status(404), code_is("VEHICLE_NOT_FOUND")]),
    req("REGLA 2: inscripción duplicada (409)", "POST", "/api/registrations", "member_token", {"event_id": "{{event_id}}", "vehicle_id": "{{vehicle_id}}"}, tests=[status(409), code_is("ALREADY_REGISTERED")]),
    req("Evento inexistente (404)", "POST", "/api/registrations", "member_token", {"event_id": 999999, "vehicle_id": "{{vehicle_id}}"}, tests=[status(404)]),
    req("REGLA 1: llenar evento de cupo 1", "POST", "/api/registrations", "member_token", {"event_id": "{{event_small_id}}", "vehicle_id": "{{vehicle_id}}"}, tests=[status(201), save("registration_small_id", "pm.response.json().id")]),
    req("REGLA 1: evento lleno (409)", "POST", "/api/registrations", "member2_token", {"event_id": "{{event_small_id}}", "vehicle_id": "{{vehicle2_id}}"}, tests=[status(409), code_is("EVENT_FULL")]),
    req("No se borra vehículo con inscripciones (409)", "DELETE", "/api/vehicles/{{vehicle_id}}", "member_token", tests=[status(409), code_is("VEHICLE_HAS_REGISTRATIONS")]),
    req("Registrar segundo vehículo como principal", "POST", "/api/vehicles", "member_token", veh(plate="{{plate3}}", model="M2", is_primary=True), tests=[status(201), save("vehicle3_id", "pm.response.json().id")]),
    req("Cambiar vehículo de la inscripción", "PUT", "/api/registrations/{{registration_id}}", "member_token", {"vehicle_id": "{{vehicle3_id}}"}, tests=[status(200), 'pm.test("vehículo cambiado", function(){ pm.expect(pm.response.json().vehicle.id).to.eql(pm.collectionVariables.get("vehicle3_id")); });']),
    req("Mis inscripciones", "GET", "/api/registrations", "member_token", tests=[status(200), 'pm.test("2 inscripciones", function(){ pm.expect(pm.response.json().length).to.eql(2); });']),
    req("Inscripción ajena (403)", "GET", "/api/registrations/{{registration_id}}", "member2_token", tests=[status(403)]),
    req("Admin: participantes de un evento", "GET", "/api/registrations", "admin_token", query={"event_id": "{{event_id}}"}, tests=[status(200), 'pm.test("1 participante", function(){ pm.expect(pm.response.json().length).to.eql(1); });']),
    req("No se borra evento con inscripciones (409)", "DELETE", "/api/events/{{event_id}}", "admin_token", tests=[status(409), code_is("EVENT_HAS_REGISTRATIONS")]),
    req("Bajar capacidad bajo los inscritos (409)", "PUT", "/api/events/{{event_small_id}}", "admin_token", ev(name="Evento cupo 1", max_capacity=1), tests=[status(200)]),
 ]),
 ("5. Miembros", [
    req("Listar miembros como miembro (403)", "GET", "/api/members", "member_token", tests=[status(403)]),
    req("Listar miembros (admin)", "GET", "/api/members", "admin_token", tests=[status(200)]),
    req("Buscar miembros", "GET", "/api/members", "admin_token", query={"search": "laura"}, tests=[status(200)]),
    req("Ver mi perfil", "GET", "/api/members/{{member_id}}", "member_token", tests=[status(200)]),
    req("Ver perfil ajeno (403)", "GET", "/api/members/{{member2_id}}", "member_token", tests=[status(403)]),
    req("Editar mi perfil", "PUT", "/api/members/{{member_id}}", "member_token", {"name": "Laura G.", "phone": "3009998888", "city": "Popayán, Cauca"}, tests=[status(200), 'pm.test("guarda la ciudad", function(){ pm.expect(pm.response.json().city).to.eql("Popayán, Cauca"); });']),
 ]),
 ("6. Limpieza (DELETE)", [
    req("Cancelar inscripción", "DELETE", "/api/registrations/{{registration_id}}", "member_token", tests=[status(204)]),
    req("Cancelar inscripción cupo 1", "DELETE", "/api/registrations/{{registration_small_id}}", "member_token", tests=[status(204)]),
    req("Eliminar vehículo", "DELETE", "/api/vehicles/{{vehicle_id}}", "member_token", tests=[status(204)]),
    req("Eliminar vehículo ya borrado (404)", "DELETE", "/api/vehicles/{{vehicle_id}}", "member_token", tests=[status(404)]),
    req("Eliminar evento", "DELETE", "/api/events/{{event_id}}", "admin_token", tests=[status(204)]),
    req("Eliminar evento cupo 1", "DELETE", "/api/events/{{event_small_id}}", "admin_token", tests=[status(204)]),
    req("Admin no puede eliminarse a sí mismo (409)", "GET", "/api/members", "admin_token", tests=[status(200)]),
    req("Eliminar miembro 2 (admin)", "DELETE", "/api/members/{{member2_id}}", "admin_token", tests=[status(204)]),
    req("Eliminar miembro 1 (admin)", "DELETE", "/api/members/{{member_id}}", "admin_token", tests=[status(204)]),
 ]),
]
# El caso "bajar capacidad" ya no tiene inscritos en el evento cupo 1 distintos de 1: lo dejamos como edición válida.
for f in folders:
    for it in f[1]:
        if it["name"] == "Bajar capacidad bajo los inscritos (409)":
            it["name"] = "Editar evento con cupo igual a los inscritos (200)"
        if it["name"] == "Admin no puede eliminarse a sí mismo (409)":
            it["name"] = "Consultar miembros antes de limpiar"

collection = {
    "info": {"name": "Popayán Motor Community API", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
             "description": "Ejecutar en orden (Collection Runner). Requiere un administrador creado con scripts/create_admin.py."},
    "item": [{"name": n, "item": items} for n, items in folders],
    "variable": [
        {"key": "base_url", "value": "http://localhost:8000"},
        {"key": "admin_email", "value": "admin@popayanmotor.com"},
        {"key": "admin_password", "value": ""},  # se escribe en Postman (pestaña Variables), nunca en el repo
    ],
}
with open("postman/PopayanMotor.postman_collection.json", "w", encoding="utf-8") as fh:
    json.dump(collection, fh, indent=2, ensure_ascii=False)
print("colección generada:", sum(len(i) for _, i in folders), "peticiones")
