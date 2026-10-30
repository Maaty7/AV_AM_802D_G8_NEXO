from django.contrib.auth import authenticate
from django.utils import timezone
from rest_framework import serializers

from .models import CodigoVerificacion, Usuario
from .verificacion import hash_codigo


class RegistroSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, min_length=8)

    class Meta:
        model = Usuario
        fields = ["email", "password", "nombre"]

    def validate_email(self, email):
        email = Usuario.objects.normalize_email(email)
        if Usuario.objects.filter(email__iexact=email).exists():
            raise serializers.ValidationError("Ya existe una cuenta con ese email.")
        return email

    def create(self, datos_validados):
        return Usuario.objects.create_user(**datos_validados)


class LoginSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True)

    def validate(self, datos):
        usuario = authenticate(
            request=self.context.get("request"),
            email=datos["email"],
            password=datos["password"],
        )
        if usuario is None:
            raise serializers.ValidationError("Email o contraseña incorrectos.")
        datos["usuario"] = usuario
        return datos


class VerificarCodigoSerializer(serializers.Serializer):
    desafio = serializers.UUIDField()
    codigo = serializers.CharField(min_length=6, max_length=6)

    def validate(self, datos):
        try:
            desafio_obj = CodigoVerificacion.objects.select_related("usuario").get(desafio=datos["desafio"])
        except CodigoVerificacion.DoesNotExist:
            raise serializers.ValidationError("Desafío inválido.")

        if desafio_obj.usado:
            raise serializers.ValidationError("Este código ya fue utilizado. Solicita uno nuevo.")
        if desafio_obj.esta_vencido():
            raise serializers.ValidationError("El código venció. Solicita uno nuevo.")
        if desafio_obj.intentos_agotados():
            raise serializers.ValidationError("Demasiados intentos. Solicita un código nuevo.")

        if hash_codigo(datos["codigo"]) != desafio_obj.codigo_hash:
            desafio_obj.intentos += 1
            desafio_obj.save(update_fields=["intentos"])
            raise serializers.ValidationError("Código incorrecto.")

        datos["desafio_obj"] = desafio_obj
        return datos


class ReenviarCodigoSerializer(serializers.Serializer):
    desafio = serializers.UUIDField()

    def validate(self, datos):
        try:
            desafio_obj = CodigoVerificacion.objects.select_related("usuario").get(desafio=datos["desafio"])
        except CodigoVerificacion.DoesNotExist:
            raise serializers.ValidationError("Desafío inválido.")

        if desafio_obj.usado:
            raise serializers.ValidationError("Este desafío ya se completó. Inicia sesión de nuevo.")

        segundos_desde_envio = (timezone.now() - desafio_obj.ultimo_envio_en).total_seconds()
        if segundos_desde_envio < CodigoVerificacion.REENVIO_MINIMO_SEGUNDOS:
            espera = int(CodigoVerificacion.REENVIO_MINIMO_SEGUNDOS - segundos_desde_envio)
            raise serializers.ValidationError(f"Espera {espera} segundos antes de solicitar otro código.")

        datos["desafio_obj"] = desafio_obj
        return datos
