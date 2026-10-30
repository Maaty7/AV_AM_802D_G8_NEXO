from django.contrib.auth import authenticate
from rest_framework import serializers

from .models import Usuario


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
