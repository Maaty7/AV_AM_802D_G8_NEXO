<#
.SINOPSIS
    Deja el backend de NEXO listo para trabajar: PostgreSQL corriendo,
    IP de red revisada contra la configuracion local, migraciones
    aplicadas y el servidor de desarrollo escuchando en 0.0.0.0:8000.

.DESCRIPCION
    No contiene contraseñas ni IPs fijas: la IP se detecta en el momento
    y se compara contra Nexo_App/local.properties y backend/.env, que
    son archivos locales ignorados por git. Si PostgreSQL necesita
    permisos de administrador para iniciar, pedira aceptar un UAC.

    Ejecutar desde cualquier carpeta:
        powershell -ExecutionPolicy Bypass -File scripts\iniciar-nexo.ps1
#>

$raiz = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $raiz "backend"
$appAndroid = Join-Path $raiz "Nexo_App"

function Escribir-Paso($texto) { Write-Host "`n== $texto ==" -ForegroundColor Cyan }
function Escribir-Ok($texto) { Write-Host "  OK: $texto" -ForegroundColor Green }
function Escribir-Aviso($texto) { Write-Host "  AVISO: $texto" -ForegroundColor Yellow }
function Escribir-Error($texto) { Write-Host "  ERROR: $texto" -ForegroundColor Red }

# --- 1. Servicio de PostgreSQL ---------------------------------------------
Escribir-Paso "PostgreSQL"
try {
    $servicio = Get-Service -Name "postgresql-x64-*" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $servicio) {
        Escribir-Error "No se encontro el servicio de PostgreSQL (postgresql-x64-*). Instalalo o revisa el nombre del servicio."
    } elseif ($servicio.Status -eq "Running") {
        Escribir-Ok "$($servicio.Name) ya esta corriendo."
    } else {
        Escribir-Aviso "$($servicio.Name) esta detenido. Intentando iniciarlo..."
        try {
            Start-Service -Name $servicio.Name -ErrorAction Stop
            Escribir-Ok "$($servicio.Name) iniciado."
        } catch {
            Write-Host "  Se necesita una ventana elevada para iniciar el servicio. Acepta el cuadro de UAC que va a aparecer." -ForegroundColor Yellow
            Start-Process powershell -Verb RunAs -ArgumentList '-NoExit', '-Command', "Start-Service -Name '$($servicio.Name)'; Write-Host 'Servicio iniciado.' -ForegroundColor Green"
        }
    }
} catch {
    Escribir-Error $_.Exception.Message
}

# --- 2. IP de red local vs. configuracion ------------------------------------
Escribir-Paso "IP de red local"
try {
    $ipsLocales = (Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" }).IPAddress

    $propiedades = Join-Path $appAndroid "local.properties"
    $envBackend = Join-Path $backend ".env"
    $ipConfigurada = $null

    if (Test-Path $propiedades) {
        $linea = Get-Content $propiedades | Where-Object { $_ -match "^nexo\.backend\.ip=" } | Select-Object -First 1
        if ($linea) { $ipConfigurada = ($linea -split "=", 2)[1].Trim() }
    }

    if (-not $ipConfigurada) {
        Escribir-Aviso "No se encontro nexo.backend.ip en Nexo_App/local.properties."
    } elseif ($ipsLocales -contains $ipConfigurada) {
        Escribir-Ok "La IP configurada ($ipConfigurada) coincide con una IP local actual."
    } else {
        Escribir-Aviso "La IP configurada en local.properties ($ipConfigurada) NO coincide con ninguna IP local actual ($($ipsLocales -join ', '))."
        Escribir-Aviso "Actualiza nexo.backend.ip en Nexo_App/local.properties y DJANGO_ALLOWED_HOSTS en backend/.env, y reinstala la app con .\gradlew installDebug."
    }

    if ((Test-Path $envBackend) -and $ipConfigurada) {
        $lineaHosts = Get-Content $envBackend | Where-Object { $_ -match "^DJANGO_ALLOWED_HOSTS=" } | Select-Object -First 1
        if ($lineaHosts -and ($lineaHosts -notmatch [regex]::Escape($ipConfigurada))) {
            Escribir-Aviso "DJANGO_ALLOWED_HOSTS en backend/.env no incluye $ipConfigurada. Agregala para que el telefono pueda conectarse."
        }
    }
} catch {
    Escribir-Error $_.Exception.Message
}

# --- 3. Migraciones -----------------------------------------------------------
Escribir-Paso "Migraciones"
try {
    Push-Location $backend
    & ".\venv\Scripts\python.exe" manage.py migrate
    if ($LASTEXITCODE -ne 0) { throw "manage.py migrate termino con codigo $LASTEXITCODE" }
    Escribir-Ok "Migraciones aplicadas."
} catch {
    Escribir-Error $_.Exception.Message
} finally {
    Pop-Location -ErrorAction SilentlyContinue
}

# --- 4. Servidor de desarrollo --------------------------------------------------
Escribir-Paso "Servidor (runserver 0.0.0.0:8000)"
try {
    $enUso = Get-NetTCPConnection -LocalPort 8000 -State Listen -ErrorAction SilentlyContinue
    if ($enUso) {
        Escribir-Aviso "El puerto 8000 ya esta en uso (PID $($enUso.OwningProcess | Select-Object -First 1)). Asumo que el servidor ya esta corriendo."
    } else {
        Start-Process powershell -ArgumentList '-NoExit', '-Command', "cd '$backend'; .\venv\Scripts\Activate.ps1; python manage.py runserver 0.0.0.0:8000"
        Start-Sleep -Seconds 3
        $enUso = Get-NetTCPConnection -LocalPort 8000 -State Listen -ErrorAction SilentlyContinue
        if ($enUso) {
            Escribir-Ok "Servidor escuchando en el puerto 8000."
        } else {
            Escribir-Error "El servidor no quedo escuchando en el puerto 8000. Revisa la ventana nueva que se abrio."
        }
    }
} catch {
    Escribir-Error $_.Exception.Message
}

# --- 5. Verificacion ------------------------------------------------------------
Escribir-Paso "Verificacion"
try {
    $respuesta = Invoke-WebRequest -Uri "http://127.0.0.1:8000/admin/" -UseBasicParsing -ErrorAction Stop
    Escribir-Ok "http://127.0.0.1:8000/admin/ responde (codigo $($respuesta.StatusCode))."
} catch {
    Escribir-Error "No se pudo verificar http://127.0.0.1:8000/admin/ : $($_.Exception.Message)"
}

Write-Host "`nListo. Si la IP cambio, no olvides reinstalar la app con .\gradlew installDebug." -ForegroundColor Cyan
