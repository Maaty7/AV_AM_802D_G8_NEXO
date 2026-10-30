# Backend mínimo de NEXO

API en Django + Django REST Framework + PostgreSQL. Recibe los informes de
jornada que hoy genera la app Android (lo mismo que guarda `ReportEntity.kt`
en Room), con cuentas de apoderado y autenticación por token.

**Fuera de alcance (a propósito):** correo, verificación en dos pasos,
recuperación de PIN, analítica, despliegue en la nube. El PIN de la app
**nunca** se envía al servidor.

## Requisitos

- Python 3.13 (el proyecto se desarrolló con `py -3.13`).
- PostgreSQL 17 corriendo localmente, con un usuario que tenga privilegio
  `CREATEDB` (Django lo necesita para crear/borrar la base `test_nexo_backend`
  temporal cuando corres los tests).

## Instalación (PowerShell, Windows)

```powershell
cd backend
py -3.13 -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

Si PostgreSQL todavía no está instalado:

```powershell
winget install --id PostgreSQL.PostgreSQL.17 -e
```

Crea el rol y la base del proyecto (te van a pedir la contraseña del
superusuario `postgres` que definiste al instalar):

```powershell
psql -U postgres -h localhost -c "CREATE ROLE nexo_user WITH LOGIN PASSWORD 'TU_CONTRASENA' CREATEDB;"
psql -U postgres -h localhost -c "CREATE DATABASE nexo_backend OWNER nexo_user;"
```

El `CREATEDB` en el rol es obligatorio — sin él, `python manage.py test` falla
porque no puede crear la base de pruebas temporal.

## Variables de entorno

Copia la plantilla y completa tu contraseña real (`.env` está en
`.gitignore`, nunca se sube a git):

```powershell
Copy-Item .env.example .env
```

Edita `backend\.env` y reemplaza `CAMBIAR_ESTA_CONTRASENA` en `DATABASE_URL`
por la contraseña real que le pusiste al rol `nexo_user`. `DJANGO_SECRET_KEY`
puedes dejarlo como está para desarrollo local, o generar uno propio:

```powershell
python -c "import secrets; print(secrets.token_urlsafe(50))"
```

Variables:
- `DJANGO_SECRET_KEY` — clave de Django. Nunca hardcodeada en el código.
- `DJANGO_DEBUG` — `True` en desarrollo, `False` en producción.
- `DJANGO_ALLOWED_HOSTS` — hosts separados por coma.
- `DATABASE_URL` — `postgres://usuario:contraseña@host:puerto/nombre_db`.

## Migraciones y servidor

```powershell
python manage.py migrate
python manage.py createsuperuser
python manage.py runserver
```

El panel de administración queda en `http://127.0.0.1:8000/admin/` — ahí se
ven Usuarios, Dispositivos e Informes (con sus apps usadas como inline).

## Tests

```powershell
python manage.py test
```

Corren contra una base `test_nexo_backend` temporal (la crea y la borra sola);
por eso el rol `nexo_user` necesita `CREATEDB`.

## Endpoints

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/api/cuentas/registro/` | No | `{email, password, nombre}` → crea el apoderado y devuelve `{token, email, nombre}` |
| POST | `/api/cuentas/login/` | No | `{email, password}` → `{token, email, nombre}` |
| POST | `/api/informes/` | Token | Crea o actualiza un informe de jornada (ver abajo) |
| GET | `/api/informes/` | Token | Informes del apoderado autenticado. Filtro opcional `?fecha=yyyy-mm-dd` |

Todas las rutas con "Auth: Token" requieren el header:
```
Authorization: Token <token recibido en registro o login>
```

### POST /api/informes/

El dispositivo se identifica con un UUID que genera la app (`dispositivo_uuid`)
más un nombre legible (`dispositivo_nombre`). Es idempotente: si ya existe un
informe con el mismo `dispositivo` + `nombre_jornada` + `fecha`, se actualiza
completo (incluida la lista de apps) en vez de crear uno nuevo.

```json
{
  "dispositivo_uuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "dispositivo_nombre": "Teléfono de Ana",
  "nombre_jornada": "Horario escolar",
  "fecha": "2026-10-20",
  "hora_inicio": "08:00:00",
  "hora_termino": "14:00:00",
  "dias": "LU, MA, MI, JU, VI",
  "minutos_totales": 90,
  "minutos_educacion": 60,
  "minutos_comunicacion": 10,
  "minutos_redes_sociales": 0,
  "minutos_entretenimiento": 20,
  "minutos_juegos": 0,
  "minutos_otros": 0,
  "generado_en": 1760000000000,
  "apps": [
    {"nombre": "Classroom", "categoria": "Educación", "minutos": 60},
    {"nombre": "WhatsApp", "categoria": "Comunicación", "minutos": 10},
    {"nombre": "YouTube", "categoria": "Entretenimiento", "minutos": 20}
  ]
}
```

`minutos` siempre debe ser >= 0, y `categoria` debe ser una de las 6 que usa
la app: Educación, Comunicación, Redes Sociales, Entretenimiento, Juegos, Otros.
`generado_en` es epoch-millis, igual que `ReportEntity.generadoEn` en la app.

Cada apoderado solo ve sus propios informes — el filtro es siempre por
`dispositivo__apoderado = request.user`, nunca por un id que venga del cliente.
