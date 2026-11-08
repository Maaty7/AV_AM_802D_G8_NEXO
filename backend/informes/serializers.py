import logging

from rest_framework import serializers

from dispositivos.models import Dispositivo

from .correo import enviar_resumen_informe_por_correo, es_informe_reciente
from .models import AppUso, Informe

logger = logging.getLogger(__name__)


class AppUsoSerializer(serializers.ModelSerializer):
    class Meta:
        model = AppUso
        fields = ["nombre", "categoria", "minutos"]


class InformeEntradaSerializer(serializers.ModelSerializer):
    """Lo que la app manda en POST /api/informes/ — incluye el dispositivo de origen y la lista de apps."""

    dispositivo_uuid = serializers.CharField(write_only=True, max_length=64)
    dispositivo_nombre = serializers.CharField(write_only=True, max_length=150)
    apps = AppUsoSerializer(many=True, required=False, default=list)

    class Meta:
        model = Informe
        fields = [
            "dispositivo_uuid",
            "dispositivo_nombre",
            "nombre_jornada",
            "fecha",
            "hora_inicio",
            "hora_termino",
            "dias",
            "minutos_totales",
            "minutos_educacion",
            "minutos_comunicacion",
            "minutos_redes_sociales",
            "minutos_entretenimiento",
            "minutos_juegos",
            "minutos_otros",
            "generado_en",
            "apps",
        ]

    def create(self, datos_validados):
        apoderado = self.context["request"].user
        uuid = datos_validados.pop("dispositivo_uuid")
        nombre_dispositivo = datos_validados.pop("dispositivo_nombre")
        apps_datos = datos_validados.pop("apps", [])

        dispositivo, creado = Dispositivo.objects.get_or_create(
            apoderado=apoderado, uuid=uuid, defaults={"nombre": nombre_dispositivo}
        )
        if not creado and dispositivo.nombre != nombre_dispositivo:
            dispositivo.nombre = nombre_dispositivo
            dispositivo.save(update_fields=["nombre"])

        informe, _ = Informe.objects.update_or_create(
            dispositivo=dispositivo,
            nombre_jornada=datos_validados["nombre_jornada"],
            fecha=datos_validados["fecha"],
            defaults=datos_validados,
        )
        # Reemplaza por completo la lista de apps — "se actualiza, no se duplica".
        informe.apps.all().delete()
        AppUso.objects.bulk_create(AppUso(informe=informe, **app) for app in apps_datos)

        # Correo de resumen solo si: (a) todavía no se envió con éxito antes
        # — si el envío falla, correo_enviado queda en False y el próximo
        # re-post (reintento normal de subida) tiene otra oportunidad — y
        # (b) la jornada es de hoy o de ayer, para no mandar un correo por
        # cada informe si alguna vez se sincroniza de golpe todo un
        # historial viejo. Un informe antiguo queda con correo_enviado=False
        # para siempre, a propósito: nunca se reintenta solo por ser viejo.
        if not informe.correo_enviado and es_informe_reciente(informe.fecha):
            try:
                enviar_resumen_informe_por_correo(informe, apps_datos)
                informe.correo_enviado = True
                informe.save(update_fields=["correo_enviado"])
            except Exception:
                logger.exception("No se pudo enviar el correo de resumen del informe %s", informe.id)

        return informe


class InformeSalidaSerializer(serializers.ModelSerializer):
    """Lo que devuelve GET /api/informes/ — incluye los datos del dispositivo y la lista de apps."""

    apps = AppUsoSerializer(many=True, read_only=True)
    dispositivo_uuid = serializers.CharField(source="dispositivo.uuid", read_only=True)
    dispositivo_nombre = serializers.CharField(source="dispositivo.nombre", read_only=True)

    class Meta:
        model = Informe
        fields = [
            "id",
            "dispositivo_uuid",
            "dispositivo_nombre",
            "nombre_jornada",
            "fecha",
            "hora_inicio",
            "hora_termino",
            "dias",
            "minutos_totales",
            "minutos_educacion",
            "minutos_comunicacion",
            "minutos_redes_sociales",
            "minutos_entretenimiento",
            "minutos_juegos",
            "minutos_otros",
            "generado_en",
            "apps",
            "creado_en",
            "actualizado_en",
        ]
