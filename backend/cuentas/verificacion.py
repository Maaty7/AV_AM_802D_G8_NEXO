import hashlib
import secrets
from datetime import timedelta

from django.conf import settings
from django.core.mail import send_mail
from django.template.loader import render_to_string
from django.utils import timezone

from .models import CodigoVerificacion, Usuario


def generar_codigo_numerico() -> str:
    """6 dígitos, generados con secrets (aleatoriedad criptográfica, no random normal)."""
    return f"{secrets.randbelow(1_000_000):06d}"


def hash_codigo(codigo: str) -> str:
    return hashlib.sha256(codigo.encode("utf-8")).hexdigest()


def crear_desafio(usuario: Usuario) -> tuple[CodigoVerificacion, str]:
    """Crea un desafío nuevo con un código nuevo — nunca se guarda el código, solo su hash."""
    codigo = generar_codigo_numerico()
    ahora = timezone.now()
    desafio = CodigoVerificacion.objects.create(
        usuario=usuario,
        codigo_hash=hash_codigo(codigo),
        expira_en=ahora + timedelta(minutes=CodigoVerificacion.VIGENCIA_MINUTOS),
        ultimo_envio_en=ahora,
    )
    return desafio, codigo


def enviar_codigo_por_correo(usuario: Usuario, codigo: str) -> None:
    contexto = {"nombre": usuario.nombre, "codigo": codigo}
    cuerpo_texto = render_to_string("cuentas/correo_codigo.txt", contexto)
    cuerpo_html = render_to_string("cuentas/correo_codigo.html", contexto)
    send_mail(
        subject="Tu código de verificación de NEXO",
        message=cuerpo_texto,
        from_email=settings.DEFAULT_FROM_EMAIL,
        recipient_list=[usuario.email],
        html_message=cuerpo_html,
        fail_silently=False,
    )
