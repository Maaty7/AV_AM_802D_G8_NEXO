import uuid

from django.contrib.auth.base_user import AbstractBaseUser, BaseUserManager
from django.contrib.auth.models import PermissionsMixin
from django.db import models
from django.utils import timezone


class UsuarioManager(BaseUserManager):
    """Apoderados identificados por email — no hay username."""

    def create_user(self, email, password=None, **campos_extra):
        if not email:
            raise ValueError("El email es obligatorio.")
        email = self.normalize_email(email)
        usuario = self.model(email=email, **campos_extra)
        usuario.set_password(password)
        usuario.save(using=self._db)
        return usuario

    def create_superuser(self, email, password=None, **campos_extra):
        campos_extra.setdefault("is_staff", True)
        campos_extra.setdefault("is_superuser", True)
        campos_extra.setdefault("verificado", True)
        return self.create_user(email, password, **campos_extra)


class Usuario(AbstractBaseUser, PermissionsMixin):
    """El apoderado. Login por email + contraseña — el PIN de la app nunca llega acá."""

    email = models.EmailField(unique=True)
    nombre = models.CharField(max_length=150, blank=True)
    is_staff = models.BooleanField(default=False)
    is_active = models.BooleanField(default=True)
    # Distinto de is_active (que Django usa para permitir el login con
    # password): indica si completó al menos una vez la verificación por
    # correo. No bloquea nada por sí solo — registro y login siempre exigen
    # un código nuevo de todas formas — pero sirve para verlo en el admin.
    verificado = models.BooleanField(default=False)
    date_joined = models.DateTimeField(auto_now_add=True)

    objects = UsuarioManager()

    USERNAME_FIELD = "email"
    REQUIRED_FIELDS = []

    def __str__(self):
        return self.email


class CodigoVerificacion(models.Model):
    """
    Un código de 6 dígitos enviado por correo para completar un registro o
    login (2FA). Nunca se guarda en texto plano — solo su hash SHA-256
    ([codigo_hash], ver cuentas/verificacion.py). [desafio] es el id que ve
    el cliente; nunca el id numérico de esta fila ni el código mismo.
    """

    MAX_INTENTOS = 5
    VIGENCIA_MINUTOS = 10
    REENVIO_MINIMO_SEGUNDOS = 60

    usuario = models.ForeignKey(Usuario, on_delete=models.CASCADE, related_name="codigos_verificacion")
    desafio = models.UUIDField(default=uuid.uuid4, unique=True, editable=False)
    codigo_hash = models.CharField(max_length=64)
    intentos = models.PositiveSmallIntegerField(default=0)
    usado = models.BooleanField(default=False)
    expira_en = models.DateTimeField()
    ultimo_envio_en = models.DateTimeField()
    creado_en = models.DateTimeField(auto_now_add=True)

    def esta_vencido(self) -> bool:
        return timezone.now() > self.expira_en

    def intentos_agotados(self) -> bool:
        return self.intentos >= self.MAX_INTENTOS

    def __str__(self):
        return f"Código para {self.usuario.email} ({self.desafio})"
