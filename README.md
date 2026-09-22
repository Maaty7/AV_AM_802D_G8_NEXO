# NEXO

NEXO es una app Android de supervisión digital parental. Permite a un apoderado configurar "jornadas" de tiempo (por ejemplo, el horario escolar) y ver un informe claro de qué aplicaciones usó un menor, durante cuánto tiempo y en qué categoría — usando únicamente mecanismos oficiales de Android, nunca contenido privado (mensajes, contraseñas, capturas de pantalla).

Proyecto Capstone de Ingeniería en Informática (DuocUC).

## Stack técnico

- Kotlin + Jetpack Compose (UI declarativa)
- Arquitectura MVVM (ViewModel por pantalla)
- Room (persistencia local)
- WorkManager (informe automático al terminar cada jornada)
- `UsageStatsManager` (API oficial de Android para estadísticas de uso)
- Navigation Compose

## Cómo compilar y correr el proyecto

1. Abre la carpeta `Nexo_App` (no la raíz del repo) en Android Studio.
2. Requisitos: minSdk 26 (Android 8.0+).
3. **Se recomienda un dispositivo físico**, no un emulador, para probar `UsageStatsManager` — el registro de eventos de uso real no siempre se comporta igual en un emulador.
4. Compilar/correr como cualquier proyecto Android (`Run` en Android Studio, o `./gradlew :app:assembleDebug` desde `Nexo_App`).

## Permiso especial requerido

NEXO necesita el permiso especial **Usage Access** (`android.permission.PACKAGE_USAGE_STATS`) para leer estadísticas de uso de otras apps. Este permiso **no se puede pedir con el diálogo estándar de permisos** — el propio flujo de onboarding de la app (pantalla de Permisos) guía al apoderado a activarlo manualmente en Ajustes del sistema la primera vez que se abre NEXO.

## Estructura de carpetas (alto nivel)

```
app/src/main/java/cl/duoc/nexo/
├── ui/            Pantallas Compose, organizadas por sección
│                   (onboarding, security, permissions, home, schedule,
│                   stats, settings, reports, navigation, theme)
├── viewmodel/      Un ViewModel por pantalla (MVVM)
├── data/local/     Entidades y DAOs de Room + NexoDatabase
├── domain/         Modelos de dominio puros
├── monitoring/     Acceso a UsageStatsManager y clasificación de apps
├── work/           WorkManager (informe automático al terminar la jornada)
└── utils/          Utilidades (hash del PIN, etc.)
```

## Estado del proyecto

MVP local funcional: onboarding, PIN, jornadas configurables, dashboard de estadísticas, informes automáticos con historial y descarga/compartir, control por PIN de las acciones sensibles. No hay backend — está fuera de alcance hasta que el MVP local esté completo y validado.
