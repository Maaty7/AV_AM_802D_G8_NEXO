from django.conf import settings
from django.core.mail import send_mail
from django.template.loader import render_to_string

from .models import Informe

NOMBRES_CATEGORIAS = [
    ("Educación", "minutos_educacion"),
    ("Comunicación", "minutos_comunicacion"),
    ("Redes Sociales", "minutos_redes_sociales"),
    ("Entretenimiento", "minutos_entretenimiento"),
    ("Juegos", "minutos_juegos"),
    ("Otros", "minutos_otros"),
]


def enviar_resumen_informe_por_correo(informe: Informe, apps_datos: list[dict]) -> None:
    """
    Envía al apoderado un resumen de un informe NUEVO (ver
    InformeEntradaSerializer.create — solo se llama cuando correo_enviado
    todavía es False, independiente de si la fila se creó o se actualizó).
    """
    apoderado = informe.dispositivo.apoderado
    top_apps = sorted(apps_datos, key=lambda app: app["minutos"], reverse=True)[:5]
    categorias = [(nombre, getattr(informe, campo)) for nombre, campo in NOMBRES_CATEGORIAS]

    contexto = {
        "informe": informe,
        "apoderado": apoderado,
        "top_apps": top_apps,
        "categorias": categorias,
    }
    cuerpo_texto = render_to_string("informes/correo_informe.txt", contexto)
    cuerpo_html = render_to_string("informes/correo_informe.html", contexto)

    send_mail(
        subject=f"Informe de jornada — {informe.nombre_jornada} ({informe.fecha})",
        message=cuerpo_texto,
        from_email=settings.DEFAULT_FROM_EMAIL,
        recipient_list=[apoderado.email],
        html_message=cuerpo_html,
        fail_silently=False,
    )
