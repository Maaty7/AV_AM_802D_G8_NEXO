import logging
from datetime import timedelta

from django.utils import timezone
from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import CodigoVerificacion, Usuario
from .serializers import (
    LoginSerializer,
    ReenviarCodigoSerializer,
    RegistroSerializer,
    VerificarCodigoSerializer,
)
from .verificacion import enviar_codigo_por_correo, crear_desafio, generar_codigo_numerico, hash_codigo

logger = logging.getLogger(__name__)


def _emitir_desafio(usuario: Usuario) -> Response:
    """Crea un código nuevo, lo manda por correo, y devuelve solo el id del desafío (nunca un token)."""
    desafio_obj, codigo = crear_desafio(usuario)
    try:
        enviar_codigo_por_correo(usuario, codigo)
    except Exception:
        logger.exception("No se pudo enviar el código de verificación a %s", usuario.email)
        return Response(
            {"detail": "No se pudo enviar el código de verificación. Intenta de nuevo."},
            status=status.HTTP_503_SERVICE_UNAVAILABLE,
        )
    return Response({"desafio": str(desafio_obj.desafio)}, status=status.HTTP_200_OK)


class RegistroView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = RegistroSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        usuario = serializer.save()
        return _emitir_desafio(usuario)


class LoginView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = LoginSerializer(data=request.data, context={"request": request})
        serializer.is_valid(raise_exception=True)
        usuario = serializer.validated_data["usuario"]
        return _emitir_desafio(usuario)


class VerificarCodigoView(APIView):
    """Único lugar donde se entrega un token — solo tras un código correcto, vigente y no usado."""

    permission_classes = [AllowAny]

    def post(self, request):
        serializer = VerificarCodigoSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        desafio_obj = serializer.validated_data["desafio_obj"]

        desafio_obj.usado = True
        desafio_obj.save(update_fields=["usado"])

        usuario = desafio_obj.usuario
        if not usuario.verificado:
            usuario.verificado = True
            usuario.save(update_fields=["verificado"])

        token, _ = Token.objects.get_or_create(user=usuario)
        return Response({"token": token.key, "email": usuario.email, "nombre": usuario.nombre})


class ReenviarCodigoView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = ReenviarCodigoSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        desafio_obj = serializer.validated_data["desafio_obj"]

        nuevo_codigo = generar_codigo_numerico()
        ahora = timezone.now()
        desafio_obj.codigo_hash = hash_codigo(nuevo_codigo)
        desafio_obj.intentos = 0
        desafio_obj.expira_en = ahora + timedelta(minutes=CodigoVerificacion.VIGENCIA_MINUTOS)
        desafio_obj.ultimo_envio_en = ahora
        desafio_obj.save()

        try:
            enviar_codigo_por_correo(desafio_obj.usuario, nuevo_codigo)
        except Exception:
            logger.exception("No se pudo reenviar el código de verificación a %s", desafio_obj.usuario.email)
            return Response(
                {"detail": "No se pudo enviar el código de verificación. Intenta de nuevo."},
                status=status.HTTP_503_SERVICE_UNAVAILABLE,
            )

        return Response({"desafio": str(desafio_obj.desafio)})
