from django.contrib import admin
from django.contrib.auth.admin import UserAdmin

from .forms import UsuarioChangeForm, UsuarioCreationForm
from .models import CodigoVerificacion, Usuario


@admin.register(Usuario)
class UsuarioAdmin(UserAdmin):
    add_form = UsuarioCreationForm
    form = UsuarioChangeForm
    model = Usuario

    ordering = ["email"]
    list_display = ["email", "nombre", "verificado", "is_staff", "is_active", "date_joined"]
    search_fields = ["email", "nombre"]
    fieldsets = (
        (None, {"fields": ("email", "password")}),
        ("Datos personales", {"fields": ("nombre",)}),
        ("Verificación", {"fields": ("verificado",)}),
        ("Permisos", {"fields": ("is_active", "is_staff", "is_superuser", "groups", "user_permissions")}),
        ("Fechas", {"fields": ("last_login", "date_joined")}),
    )
    add_fieldsets = (
        (None, {"classes": ("wide",), "fields": ("email", "password1", "password2")}),
    )
    readonly_fields = ["date_joined", "last_login"]


@admin.register(CodigoVerificacion)
class CodigoVerificacionAdmin(admin.ModelAdmin):
    # Nunca se muestra el código ni su hash acá — solo metadata para depurar.
    list_display = ["usuario", "desafio", "intentos", "usado", "expira_en", "creado_en"]
    list_filter = ["usado"]
    search_fields = ["usuario__email"]
    readonly_fields = ["usuario", "desafio", "codigo_hash", "intentos", "usado", "expira_en", "ultimo_envio_en", "creado_en"]
