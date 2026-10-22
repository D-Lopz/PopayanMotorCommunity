# Popayán Motor Community — Backend

API REST para la aplicación móvil **Popayán Motor Community**, desarrollada como proyecto académico de la asignatura Desarrollo Móvil de la Corporación Universitaria Autónoma del Cauca.

## 1. Tecnologías

- **Lenguaje:** Python 3.11 o superior.
- **Backend:** FastAPI.
- **Base de datos:** MySQL 8 o MariaDB.
- **Autenticación:** JWT.
- **Seguridad de contraseñas:** hash bcrypt.
- **Validación de datos:** Pydantic.
- **Pruebas automatizadas:** pytest.
- **Pruebas de API:** Postman.

## 2. Funcionalidades

- Registro e inicio de sesión de usuarios.
- Autenticación mediante tokens JWT.
- Control de acceso según roles y permisos.
- Gestión de miembros.
- Registro y administración de vehículos.
- Consulta, creación, edición y eliminación de eventos.
- Inscripción y cancelación de participación en eventos.
- Búsqueda y filtrado de información.
- Validación de datos y manejo de errores HTTP.

## 3. Instalación y configuración

### Requisitos previos

- Python 3.11 o superior.
- MySQL 8 o MariaDB.
- Git.
- Postman para las pruebas de la API.

### Preparación del entorno

Desde la carpeta del backend, ejecutar los siguientes comandos en Windows:

```bash
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
```

Crear el archivo `.env` a partir de `.env.example` y configurar las variables de entorno necesarias, incluyendo la conexión a la base de datos y la clave secreta para la autenticación.

No se deben publicar credenciales, contraseñas ni claves secretas en el repositorio.

## 4. Base de datos

La estructura de la base de datos se encuentra en:

`sql/schema.sql`

Ejecutar el script desde MySQL Workbench para crear la base de datos `popayan_motor` y sus tablas. **El script borra y recrea la base completa** (`DROP DATABASE IF EXISTS`), así que después hay que volver a crear el administrador con `python -m scripts.create_admin`. La versión 1.0 entregada está en el tag git `v1.0`.

Las entidades principales son:

- **Member:** información de los miembros.
- **Vehicle:** vehículos asociados a los miembros.
- **Event:** información de los eventos automotrices.
- **Registration:** inscripciones de los miembros a los eventos, con el vehículo con el que asisten, el número de acompañantes y un código único `PMC-XXXXXX`.

Cambios de la v1.1: `event.end_date` (último día si el evento dura varios días), `event.price` (aporte en COP, solo informativo), `event.distance_km`, `vehicle.is_primary`, y `registration.vehicle_id`, `companions` y `code`.

Las relaciones, restricciones y reglas de integridad se encuentran definidas en el esquema SQL.

## 5. Ejecución del backend

Desde la carpeta del backend y con el entorno virtual activado, iniciar el servidor:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

La documentación interactiva de la API estará disponible en:

- **Swagger UI:** http://localhost:8000/docs
- **OpenAPI:** http://localhost:8000/openapi.json

Para consumir la API desde Android:

- **Emulador de Android:** `http://10.0.2.2:8000/api/`
- **Dispositivo físico:** `http://IP_DEL_PC:8000/api/`

En un dispositivo físico, el computador y el teléfono deben estar conectados a una red que permita la comunicación entre ambos. La dirección IP debe corresponder a la dirección local del computador.

## 6. Endpoints de la API

La API documenta 21 operaciones principales, agrupadas en los siguientes módulos. Los métodos, parámetros y esquemas definitivos deben corresponder a las rutas implementadas y a la documentación generada por FastAPI.

### 6.1. Autenticación

| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| POST | `/api/auth/register` | Registrar un miembro | Público |
| POST | `/api/auth/login` | Iniciar sesión y obtener un JWT | Público |

### 6.2. Miembros

| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| GET | `/api/members?search=` | Consultar y buscar miembros | Administrador |
| GET | `/api/members/{id}` | Consultar un miembro | Administrador o propietario |
| PUT | `/api/members/{id}` | Actualizar un miembro | Administrador o propietario |
| DELETE | `/api/members/{id}` | Eliminar un miembro | Administrador |

### 6.3. Vehículos

| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| GET | `/api/vehicles?brand=&type=&member_id=` | Consultar y filtrar vehículos | Miembro: propios; administrador: todos |
| POST | `/api/vehicles` | Registrar un vehículo | Usuario autenticado |
| GET | `/api/vehicles/{id}` | Consultar un vehículo | Propietario o administrador |
| PUT | `/api/vehicles/{id}` | Actualizar un vehículo | Propietario |
| DELETE | `/api/vehicles/{id}` | Eliminar un vehículo (409 si tiene inscripciones a eventos sin terminar) | Propietario |

El primer vehículo de cada miembro queda como principal (`is_primary`). Al marcar otro con `"is_primary": true`, el anterior se desmarca. Si se borra el principal, pasa a serlo el más antiguo.

### 6.4. Eventos

| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| GET | `/api/events?category=&date=&search=` | Consultar y filtrar eventos | Usuario autenticado |
| GET | `/api/events/{id}` | Consultar el detalle de un evento | Usuario autenticado |
| POST | `/api/events` | Crear un evento | Administrador |
| PUT | `/api/events/{id}` | Actualizar un evento | Administrador |
| DELETE | `/api/events/{id}` | Eliminar un evento | Administrador |

Cada evento devuelve, además de sus campos, `registered_count` (cupos ocupados), `attendees_count` (inscritos + acompañantes) y `finished` (`true` desde las 00:01 del día siguiente a `end_date`, o a `date` si no tiene fin; es lo que mueve el evento a la pestaña "Pasados").

### 6.5. Inscripciones

| Método | Endpoint | Descripción | Acceso |
|---|---|---|---|
| GET | `/api/registrations?event_id=&member_id=` | Consultar inscripciones | Miembro: propias; administrador: todas |
| POST | `/api/registrations` | Inscribir al miembro autenticado en un evento | Usuario autenticado |
| GET | `/api/registrations/{id}` | Consultar una inscripción | Propietario o administrador |
| PUT | `/api/registrations/{id}` | Cambiar el vehículo de la inscripción (solo antes de que termine el evento) | Propietario |
| DELETE | `/api/registrations/{id}` | Cancelar una inscripción | Propietario o administrador |

Para crear una inscripción se envía el evento, un vehículo del propio garaje y los acompañantes (0 a 10, opcional):

```json
{
  "event_id": 1,
  "vehicle_id": 4,
  "companions": 2
}
```

La respuesta incluye `code` (`PMC-` + 6 caracteres sin O/0/I/1) y `code_expires_at`, que coincide con el momento en que el evento pasa a "Pasados".

Los identificadores y campos requeridos deben corresponder a los esquemas implementados en la API.

### 6.6. Respuestas y errores

Las respuestas HTTP deben reflejar el resultado de cada operación. Entre los códigos utilizados se encuentran:

- `200 OK`: solicitud procesada correctamente.
- `201 Created`: recurso creado.
- `204 No Content`: operación completada sin contenido de respuesta.
- `401 Unauthorized`: falta autenticación válida.
- `403 Forbidden`: permisos insuficientes.
- `404 Not Found`: recurso inexistente.
- `409 Conflict`: conflicto con una regla de negocio.
- `422 Unprocessable Entity`: datos de entrada no válidos.

El formato de error documentado es:

```json
{
  "detail": "Mensaje descriptivo del error",
  "code": "CODIGO_ERROR"
}
```

Los errores de validación pueden incluir información adicional sobre los campos que presentan problemas.

## 7. Reglas de negocio

La aplicación contempla las siguientes reglas:

1. **Capacidad de los eventos:** el número de inscripciones no puede superar la capacidad máxima establecida. Cada inscripción ocupa un cupo; los acompañantes no ocupan cupo, solo suman al total de asistentes.
2. **Inscripciones duplicadas:** un miembro no puede inscribirse más de una vez al mismo evento.
3. **Eventos pasados:** no se permiten inscripciones cuando la fecha y hora de inicio del evento ya han transcurrido.
4. **Validación del servidor:** los datos deben validarse antes de crear o actualizar recursos.
5. **Eliminación de eventos:** no se permite eliminar un evento que tenga inscripciones asociadas.
6. **Vehículo de la inscripción:** debe pertenecer al miembro. Un vehículo con inscripciones a eventos sin terminar no se puede eliminar; primero se cambia el vehículo de esas inscripciones o se cancelan. Las inscripciones de eventos terminados no lo impiden: conservan el historial sin vehículo.
7. **Permisos:** las operaciones administrativas requieren el rol correspondiente y las operaciones sobre recursos personales deben respetar la autorización del propietario.

Las validaciones deben estar implementadas en el backend y respaldadas por las restricciones correspondientes de la base de datos cuando aplique.

## 8. Pruebas de la API con Postman

La colección de solicitudes se encuentra en:

`postman/PopayanMotor.postman_collection.json`

La colección contiene 61 peticiones y se genera con `python postman/build_collection.py`.

### Configuración

1. Iniciar el backend y verificar que la base de datos esté disponible.
2. Abrir Postman.
3. Importar el archivo `postman/PopayanMotor.postman_collection.json`.
4. Configurar las variables de la colección que sean necesarias, incluidas `admin_email` y `admin_password` si están definidas en ella.
5. Confirmar que la URL base apunte al servidor local.
6. Ejecutar las solicitudes en el orden requerido mediante **Collection Runner**.

La cuenta administrativa debe existir antes de ejecutar las pruebas que dependan de permisos de administrador.

### Cobertura esperada

La colección debe permitir comprobar, según las solicitudes incluidas:

- Registro e inicio de sesión.
- Obtención y utilización del token JWT.
- Acceso permitido y denegado según roles.
- Operaciones CRUD de miembros.
- Operaciones CRUD de vehículos.
- Consulta, filtrado y administración de eventos.
- Creación y cancelación de inscripciones.
- Validaciones de datos.
- Respuestas ante recursos inexistentes.
- Conflictos por duplicados y restricciones de negocio.
- Protección de rutas que requieren autenticación.

### Resultados de las pruebas

Los resultados deben registrarse después de ejecutar la colección en Postman.

| Elemento | Resultado que se debe verificar |
|---|---|
| Colección de Postman | Cantidad real de solicitudes incluidas |
| Solicitudes ejecutadas | Cantidad de solicitudes completadas |
| Pruebas exitosas | Cantidad de aserciones aprobadas |
| Pruebas fallidas | Cantidad de aserciones fallidas |
| Casos pendientes | Solicitudes o validaciones no ejecutadas |

No se debe afirmar que todas las solicitudes o validaciones pasaron sin comprobar los resultados de la ejecución.

## 9. Pruebas automatizadas

El proyecto incluye pruebas automatizadas con `pytest`.

Para ejecutar las pruebas:

```bash
pytest -q
```

La suite tiene 30 pruebas automatizadas que corren por defecto con SQLite en memoria.

Para ejecutar pruebas contra MySQL, si el proyecto tiene configurado el soporte correspondiente:

```bash
TEST_DATABASE_URL=mysql+pymysql://USUARIO:CONTRASEÑA@localhost/popayan_test pytest -q
```

Antes de ejecutar este comando, crear y configurar la base de datos de pruebas según los requisitos del proyecto. Se recomienda utilizar credenciales de prueba y no incluirlas en el repositorio.

### Prueba de concurrencia

El proyecto también documenta un script para comprobar inscripciones simultáneas:

```bash
python tests/concurrency_check.py
```

La ejecución debe realizarse con el servidor y la base de datos configurados según los requisitos del script.

Los resultados de `pytest`, las pruebas contra MySQL y la verificación de concurrencia deben documentarse a partir de las ejecuciones reales.

## 10. Autenticación y seguridad

El inicio de sesión devuelve un token JWT que se utiliza para acceder a las rutas protegidas.

Las solicitudes autenticadas deben incluir la cabecera:

```http
Authorization: Bearer <token>
```

Las contraseñas deben almacenarse mediante hash bcrypt y nunca en texto plano. Las claves secretas y los datos de conexión deben configurarse mediante variables de entorno.

El acceso a los recursos debe verificarse en el backend, sin depender exclusivamente de las restricciones de la interfaz Android.

## 11. Estructura del proyecto

La estructura documentada del backend es:

```text
backend/
├── app/
│   ├── main.py
│   ├── ...
├── sql/
│   └── schema.sql
├── scripts/
│   └── create_admin.py
├── tests/
│   └── ...
├── postman/
│   └── PopayanMotor.postman_collection.json
├── requirements.txt
├── .env.example
└── README.md
```

Esta estructura es orientativa. Los nombres y archivos definitivos deben coincidir con los que existan realmente en el repositorio.

## 12. Documentación adicional

- **Swagger UI:** `http://localhost:8000/docs`
- **Esquema OpenAPI:** `http://localhost:8000/openapi.json`
- **Esquema de la base de datos:** `sql/schema.sql`
- **Colección de pruebas:** `postman/PopayanMotor.postman_collection.json`

La documentación de Swagger permite consultar las rutas implementadas, sus parámetros, los esquemas de entrada y salida y las respuestas declaradas.

---

**Proyecto:** Popayán Motor Community  
**Asignatura:** Desarrollo Móvil  
**Institución:** Corporación Universitaria Autónoma del Cauca  
**Tecnologías principales:** Kotlin, Android, FastAPI y MySQL