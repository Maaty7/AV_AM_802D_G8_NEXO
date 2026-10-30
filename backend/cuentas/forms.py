"""
Formularios mínimos para poder crear/editar Usuario desde el admin de Django
(AbstractBaseUser no trae los formularios por defecto que sí trae AbstractUser).
"""
from django.contrib.auth.forms import UserChangeForm, UserCreationForm

from .models import Usuario


class UsuarioCreationForm(UserCreationForm):
    class Meta:
        model = Usuario
        fields = ("email",)


class UsuarioChangeForm(UserChangeForm):
    class Meta:
        model = Usuario
        fields = ("email", "nombre", "is_active", "is_staff")
