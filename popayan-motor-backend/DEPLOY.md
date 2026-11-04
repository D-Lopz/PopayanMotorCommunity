# Despliegue en Azure for Students

Arquitectura: APK Android → HTTPS → App Service **F1** (Linux, Python) → TLS → Azure Database for MySQL Flexible **B1ms**.
Costo esperado: 0 USD (sin tarjeta no hay cobro posible). Revisar el saldo en https://www.microsoftazuresponsorships.com/ a las 48–72 h de crear el MySQL y luego cada semana.

> Nunca ejecutar `sql/schema.sql` contra una base con datos: hace `DROP DATABASE`.

## 1. Antes de empezar

1. Aplicar en la base local las migraciones pendientes de `sql/migrations` (solo con autorización).
2. Generar valores **nuevos** para producción; nada de lo usado en local o visible en el historial de git:
   - `SECRET_KEY`: `python -c "import secrets; print(secrets.token_urlsafe(48))"`
   - contraseña del administrador de MySQL y del usuario de la app
   - contraseña del administrador de la app

## 2. Recursos en el portal

Un solo grupo de recursos (`rg-popayan-motor`) y la misma región para todo.

**Azure Database for MySQL – Flexible Server**

| Campo | Valor |
|---|---|
| Tipo de carga | Development |
| Compute | Burstable · **Standard_B1ms** |
| Almacenamiento | 20 GiB · autogrow **desactivado** · IOPS pre-provisionadas |
| High availability | Desactivada |
| Backup | 7 días · Locally redundant |
| Versión | 8.4 si aparece; si no, 8.0 |
| Autenticación | Solo MySQL (usuario admin nuevo) |
| Red | Public access (allowed IP addresses) · **sin** "Allow public access from Azure services" |
| No activar | Defender, réplicas, Log Analytics, claves gestionadas por el cliente |

**App Service (Web App)**

| Campo | Valor |
|---|---|
| Publicar | Code · Linux · Python 3.13 (o 3.12) |
| Plan | Nuevo, **Free F1** (el portal suele preseleccionar uno de pago) |
| Base de datos | No usar el asistente "Web App + Database" |
| Application Insights | No |
| Después de crear | HTTPS Only: On · TLS mínimo 1.2 · FTP desactivado |

No comprar dominios, certificados ni nada del Marketplace.

## 3. Firewall de MySQL

En la Web App → *Properties* → copiar **Additional Outbound IP Addresses**. En MySQL → *Networking* → agregar una regla por cada IP.
Para la importación, agregar temporalmente la IP de tu PC y **quitarla** al terminar.

## 4. Base de datos

```bash
# 1. Copia de la base local (sin DROP DATABASE)
mysqldump -u root -p --single-transaction --set-gtid-purged=OFF popayan_motor > popayan_motor.sql

# 2. Crear la base vacía en Azure e importar
mysql -h <servidor>.mysql.database.azure.com -u <admin> -p --ssl-mode=REQUIRED \
  -e "CREATE DATABASE popayan_motor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
mysql -h <servidor>.mysql.database.azure.com -u <admin> -p --ssl-mode=REQUIRED popayan_motor < popayan_motor.sql
```

Usuario propio para la app (solo datos, sin permisos de estructura):

```sql
CREATE USER 'pmc_app'@'%' IDENTIFIED BY '<contraseña nueva>';
GRANT SELECT, INSERT, UPDATE, DELETE ON popayan_motor.* TO 'pmc_app'@'%';
```

El archivo `popayan_motor.sql` contiene datos personales: guardarlo fuera del repositorio.

## 5. Configuración de la Web App

*Settings → Environment variables*:

| Variable | Valor |
|---|---|
| `DATABASE_URL` | `mysql+pymysql://pmc_app:<contraseña>@<servidor>.mysql.database.azure.com:3306/popayan_motor` |
| `DB_SSL_CA` | `/etc/ssl/certs/ca-certificates.crt` |
| `SECRET_KEY` | la nueva |
| `ACCESS_TOKEN_EXPIRE_MINUTES` | `120` |
| `SCM_DO_BUILD_DURING_DEPLOYMENT` | `1` |

Si la contraseña tiene `@ : / ? #`, codificarla en la URL (por ejemplo `@` → `%40`).

*Settings → Configuration → Startup Command*:

```bash
python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --proxy-headers --forwarded-allow-ips="*"
```

## 6. Publicar el backend

Desde la carpeta del backend, empaquetar solo el código y las dependencias; nunca `venv` ni `.env`.
Se usa `tar` (incluido en Windows) porque `Compress-Archive` de PowerShell 5.1 puede guardar rutas con `\`, que Linux no entiende:

```bash
tar -a -c -f ../backend.zip --exclude __pycache__ app requirements.txt
```

Subir `backend.zip` arrastrándolo a `https://<app>.scm.azurewebsites.net/ZipDeployUI`. Azure instala `requirements.txt` y arranca la app.
Comprobar: `https://<app>.azurewebsites.net/docs`.

Crear el administrador apuntando el `.env` local a Azure (con tu IP en el firewall) y ejecutando `python -m scripts.create_admin`.

## 7. APK de Android

En `PopayanMotorCommunity/local.properties` (no se sube a git):

```properties
api.releaseUrl=https://<app>.azurewebsites.net/api/
release.storeFile=C:/Users/<usuario>/keystores/popayan-motor.jks
release.storePassword=...
release.keyAlias=popayan-motor
release.keyPassword=...
```

El keystore se crea una sola vez (fuera del repositorio) y se guarda con copia de seguridad; sin él no se pueden publicar actualizaciones:

```bash
keytool -genkeypair -v -keystore C:/Users/<usuario>/keystores/popayan-motor.jks -alias popayan-motor -keyalg RSA -keysize 2048 -validity 10000
```

Generar el APK: `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.
El build de release solo acepta `https`; el de debug sigue usando `api.baseUrl` o el emulador.

## 8. Operación

- El plan F1 duerme la app: abrir `/docs` un par de minutos antes de cada demo.
- Límite F1: 60 min de CPU al día. Si aparece "QuotaExceeded", se recupera al día siguiente.
- Si cambian las IPs de salida de la Web App, actualizar el firewall de MySQL.
- Guardar un `mysqldump` periódico en tu PC: al acabar el crédito o los 12 meses, Azure da de baja los recursos.
