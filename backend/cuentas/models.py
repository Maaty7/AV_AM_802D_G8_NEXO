from django.contrib.auth.base_user import AbstractBaseUser, BaseUserManager
from django.contrib.auth.models import PermissionsMixin
from django.db import models


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
        return self.create_user(email, password, **campos_extra)


class Usuario(AbstractBaseUser, PermissionsMixin):
    """El apoderado. Login por email + contraseña — el PIN de la app nunca llega acá."""

    email = models.EmailField(unique=True)
    nombre = models.CharField(max_length=150, blank=True)
    is_staff = models.BooleanField(default=False)
    is_active = models.BooleanField(default=True)
    date_joined = models.DateTimeField(auto_now_add=True)

    objects = UsuarioManager()

    USERNAME_FIELD = "email"
    REQUIRED_FIELDS = []

    def __str__(self):
        return self.email
