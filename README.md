# NEXO

NEXO es una app de supervisión digital parental por jornadas: el apoderado
configura bloques de tiempo (ej. el horario escolar) y ve cuánto tiempo y en
qué apps usó el teléfono un menor durante cada uno, por categoría. Usa
únicamente los datos de uso que Android permite consultar de forma oficial
— nunca lee contenido privado (mensajes, contraseñas, capturas de pantalla,
navegación, ubicación).

Proyecto Capstone de Ingeniería en Informática (DuocUC).

## Equipo

| Integrante | Rol |
|---|---|
| Matías Esquerra | UI |
| Cristopher Paredes | Monitoreo Android |
| Martín Muñoz | Base de datos y backend |
| Cristóbal Monsalves | Seguridad e informes |

## Funcionalidades

- **Jornadas**: bloques de horario configurables (nombre, hora de inicio y
  término, días de la semana), incluyendo jornadas nocturnas que cruzan
  medianoche.
- **Recreos y descansos**: pausas dentro de una jornada — el uso de apps en
  esos tramos no cuenta en el total ni en las categorías.
- **Días sin clases y feriados**: excepciones manuales (vacaciones, paros) y
  los feriados de Chile se revisan antes de generar cada informe automático.
- **Estadísticas**: dashboard con tiempo total, distribución por categoría
  (Educación, Comunicación, Redes Sociales, Entretenimiento, Juegos, Otros),
  apps más usadas y uso a lo largo de la jornada.
- **Informes automáticos y PDF**: se generan solos al terminar cada jornada
  (WorkManager), con historial, vista previa y descarga en PDF.
- **PIN con hash**: protege las acciones sensibles de Configuración; se
  guarda solo como hash SHA-256, nunca en texto plano, y se vuelve a pedir
  si Android mata el proceso en segundo plano.
- **Tema claro/oscuro**: preferencia persistente, elegida por el apoderado.
- **Vinculación de cuenta y sincronización**: opcional — si el apoderado
  vincula una cuenta, los informes se suben a un backend propio; si no
  vincula ninguna, la app funciona igual que siempre y nunca usa la red.
- **Verificación en dos pasos por correo**: al registrarse o iniciar sesión
  en el backend, se envía un código de 6 dígitos por correo (vence a los 10
  minutos, máximo 5 intentos) antes de entregar la sesión.
- **Correo de resumen del informe**: al sincronizar un informe de una
  jornada reciente (de hoy o de ayer), el apoderado recibe un correo con el
  resumen (tiempo total, por categoría y apps más usadas); los informes más
  antiguos no generan correo, para no saturar la bandeja al sincronizar un
  historial completo.

## Stack y arquitectura

**App Android** — Kotlin + Jetpack Compose, arquitectura MVVM, Room
(persistencia local), WorkManager (informes automáticos y sincronización en
segundo plano), `UsageStatsManager` (API oficial de Android para
estadísticas de uso), Navigation Compose.

**Backend** — Django + Django REST Framework + PostgreSQL, autenticación
por token, configuración por variables de entorno.

### Estructura del repositorio

```
AV_AM_802D_G8_NEXO/
├── Nexo_App/       App Android (abrir esta carpeta en Android Studio)
├── backend/        Backend Django REST + PostgreSQL
└── README.md       Este archivo
```

### Estructura de la app (`Nexo_App/app/src/main/java/cl/duoc/nexo/`)

```
ui/            Pantallas Compose, organizadas por sección
               (onboarding, security, permissions, home, schedule,
               stats, settings, reports, navigation, theme)
viewmodel/     Un ViewModel por pantalla (MVVM)
data/local/    Entidades y DAOs de Room + NexoDatabase + SharedPreferences
data/remote/   Cliente HTTP hacia el backend (HttpURLConnection + org.json)
domain/        Modelos y lógica pura de dominio (sin dependencias de Android)
monitoring/    Acceso a UsageStatsManager y clasificación de apps
security/      Estado de sesión (verificación de PIN en memoria)
work/          WorkManager: informe automático y subida al backend
pdf/           Generación de los informes en PDF
utils/         Utilidades (hash del PIN, feriados de Chile)
```

## Cómo instalar y correr la app

1. Abre la carpeta `Nexo_App` (no la raíz del repo) en Android Studio.
2. Requisitos: minSdk 26 (Android 8.0+). **Se recomienda un dispositivo
   físico**, no un emulador — el registro de eventos de uso real de
   `UsageStatsManager` no siempre se comporta igual en un emulador.
3. Compilar/instalar como cualquier proyecto Android (`Run` en Android
   Studio, o desde `Nexo_App`: `./gradlew installDebug` con el teléfono
   conectado).
4. La primera vez, el propio onboarding de la app guía al apoderado a
   activar el permiso especial **Usage Access** en Ajustes del sistema —
   no se puede pedir con el diálogo estándar de permisos de Android.
5. Recomendado (no exigido por la app): en Ajustes del sistema → Apps →
   NEXO → Batería, elegir "Sin restricciones", para que WorkManager genere
   los informes automáticos puntualmente incluso con la app en segundo
   plano por mucho tiempo.
6. Solo si vas a probar la sincronización con el backend: agrega en
   `Nexo_App/local.properties` (archivo local, no se sube a git) la IP de
   tu computador en la red donde corre el backend:
   ```properties
   nexo.backend.ip=TU_IP_LOCAL
   ```
   Sin esta clave, la build de debug usa `127.0.0.1` por defecto. Más
   detalles en `backend/README.md`.

## Cómo levantar el backend

Instrucciones completas, con cada comando exacto, en
[`backend/README.md`](backend/README.md). Resumen:

```powershell
cd backend
py -3.13 -m venv venv
.\venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env   # luego edita .env con tu contraseña real de PostgreSQL
python manage.py migrate
python manage.py createsuperuser
python manage.py runserver 0.0.0.0:8000   # 0.0.0.0 para que el teléfono pueda conectarse
```

Para que el teléfono (en la misma red Wi-Fi/Ethernet) alcance el servidor,
hay que abrir el puerto 8000 en el firewall de Windows **solo para redes
privadas** (comando y detalles en `backend/README.md`), y la red del
computador debe estar configurada como "Privada", no "Pública".

### Script para retomar el trabajo (`scripts/iniciar-nexo.ps1`)

Después de la primera instalación, para no repetir los pasos anteriores
cada vez que se vuelve a trabajar en el proyecto, hay un script que deja
todo listo con un solo comando (desde la raíz del repo):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\iniciar-nexo.ps1
```

Hace lo siguiente, en orden: revisa que el servicio de PostgreSQL esté
corriendo (y lo intenta iniciar si no, pidiendo permiso de administrador
si hace falta); compara la IP local actual contra `nexo.backend.ip` en
`Nexo_App/local.properties` y `DJANGO_ALLOWED_HOSTS` en `backend/.env`,
avisando si cambiaron (si cambió la IP del computador, hay que actualizar
ambos archivos a mano y reinstalar la app con `.\gradlew installDebug`);
corre `python manage.py migrate`; y levanta `runserver 0.0.0.0:8000` en
una ventana nueva de PowerShell (si detecta que el puerto 8000 ya está en
uso, no abre una segunda). El script no contiene contraseñas ni IPs fijas.

## Cómo correr los tests

**App** (desde `Nexo_App`):
```powershell
./gradlew testDebugUnitTest
./gradlew lintDebug
```

**Backend** (con el venv activado, desde `backend`):
```powershell
python manage.py test
```
Corren contra una base de datos de prueba temporal — el rol de PostgreSQL
necesita el privilegio `CREATEDB` (ver `backend/README.md`).

## Privacidad y seguridad

**Qué datos usa NEXO:** solo lo que expone `UsageStatsManager`, la API
oficial de Android para estadísticas de uso — qué apps estuvieron en primer
plano, por cuánto tiempo, y su categoría (derivada del paquete de la app,
nunca de su contenido).

**Qué NO lee nunca:** el contenido de mensajes, notificaciones, contraseñas,
capturas de pantalla, historial de navegación, ubicación, contactos, ni
nada que requiera permisos de accesibilidad o captura de pantalla.

**PIN:** se guarda solo como hash SHA-256 en la base de datos local — nunca
en texto plano, y nunca sale del teléfono (tampoco se envía al backend). Si
Android mata el proceso de la app en segundo plano y se reabre desde
Recientes, vuelve a pedir el PIN antes de mostrar cualquier pantalla.

**Backend y sincronización (opcionales):** si el apoderado no vincula una
cuenta, la app no hace ninguna llamada de red. Si vincula una, el token de
sesión se guarda en preferencias locales del teléfono — excluidas
explícitamente de los respaldos automáticos de Android (igual que la base
de datos local), así que ni la base de datos ni el token viajan en un
backup en la nube o transferencia de equipo. El tráfico sin cifrar (HTTP)
solo está permitido en la build de **debug**, y únicamente hacia la IP
local configurada en `local.properties` — la build de **release** solo
admite HTTPS.

## Limitaciones conocidas y trabajo futuro

- El backend corre solo en red local, pensado para desarrollo/pruebas — no
  hay despliegue en la nube ni HTTPS real.
- Recuperación de PIN olvidado.
- Analítica avanzada: tendencias históricas, índice de cumplimiento,
  alertas.
- Modelo Premium: sin cobro implementado todavía.
- Si el teléfono tiene instalada una versión muy antigua de la app, puede
  ser necesario reinstalarla (desinstalar y volver a instalar) en vez de
  solo actualizarla, por cambios de esquema de la base de datos local que
  quedaron muy atrás.
