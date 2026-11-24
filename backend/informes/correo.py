import datetime

from django.conf import settings
from django.core.mail import EmailMultiAlternatives
from django.template.loader import render_to_string
from django.utils import timezone

from .models import Informe

NOMBRES_CATEGORIAS = [
    ("Educación", "minutos_educacion"),
    ("Comunicación", "minutos_comunicacion"),
    ("Redes Sociales", "minutos_redes_sociales"),
    ("Entretenimiento", "minutos_entretenimiento"),
    ("Juegos", "minutos_juegos"),
    ("Otros", "minutos_otros"),
]


def es_informe_reciente(fecha: datetime.date) -> bool:
    """
    Evita un envío masivo si alguna vez se sincronizan de golpe muchos
    informes viejos (ej. una instalación nueva subiendo todo su historial
    local): solo se considera "reciente" la jornada de hoy o de ayer.
    timezone.localtime() usa settings.TIME_ZONE (America/Santiago), no la
    hora del sistema operativo donde corra el proceso.
    """
    hoy = timezone.localtime(timezone.now()).date()
    return fecha in (hoy, hoy - datetime.timedelta(days=1))


def _nombre_archivo_pdf(informe: Informe) -> str:
    jornada = "".join(c if c.isalnum() else "_" for c in informe.nombre_jornada).strip("_") or "jornada"
    return f"Informe_NEXO_{jornada}_{informe.fecha}.pdf"


def enviar_resumen_informe_por_correo(
    informe: Informe, apps_datos: list[dict], pdf_bytes: bytes | None = None
) -> None:
    """
    Envía al apoderado un resumen de un informe NUEVO (ver
    InformeEntradaSerializer.create — solo se llama cuando correo_enviado
    todavía es False, independiente de si la fila se creó o se actualizó).
    Si [pdf_bytes] viene (el PDF que generó la propia app), se adjunta; si no,
    el correo se manda igual, solo que sin adjunto.
    """
    apoderado = informe.dispositivo.apoderado
    top_apps = sorted(apps_datos, key=lambda app: app["minutos"], reverse=True)[:5]
    categorias_crudas = [(nombre, getattr(informe, campo)) for nombre, campo in NOMBRES_CATEGORIAS]
    maximo = max((minutos for _, minutos in categorias_crudas), default=0) or 1
    categorias = [
        (nombre, minutos, round(minutos * 100 / maximo)) for nombre, minutos in categorias_crudas
    ]

    contexto = {
        "informe": informe,
        "apoderado": apoderado,
        "top_apps": top_apps,
        "categorias": categorias,
    }
    cuerpo_texto = render_to_string("informes/correo_informe.txt", contexto)
    cuerpo_html = render_to_string("informes/correo_informe.html", contexto)

    correo = EmailMultiAlternatives(
        subject=f"Informe de jornada — {informe.nombre_jornada} ({informe.fecha})",
        body=cuerpo_texto,
        from_email=settings.DEFAULT_FROM_EMAIL,
        to=[apoderado.email],
    )
    correo.attach_alternative(cuerpo_html, "text/html")
    if pdf_bytes:
        correo.attach(_nombre_archivo_pdf(informe), pdf_bytes, "application/pdf")
    correo.send(fail_silently=False)
