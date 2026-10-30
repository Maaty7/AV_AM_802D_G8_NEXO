from django.contrib import admin
from django.contrib.auth.admin import UserAdmin

from .forms import UsuarioChangeForm, UsuarioCreationForm
from .models import Usuario


@admin.register(Usuario)
class UsuarioAdmin(UserAdmin):
    add_form = UsuarioCreationForm
    form = UsuarioChangeForm
    model = Usuario

    ordering = ["email"]
    list_display = ["email", "nombre", "is_staff", "is_active", "date_joined"]
    search_fields = ["email", "nombre"]
    fieldsets = (
        (None, {"fields": ("email", "password")}),
        ("Datos personales", {"fields": ("nombre",)}),
        ("Permisos", {"fields": ("is_active", "is_staff", "is_superuser", "groups", "user_permissions")}),
        ("Fechas", {"fields": ("last_login", "date_joined")}),
    )
    add_fieldsets = (
        (None, {"classes": ("wide",), "fields": ("email", "password1", "password2")}),
    )
    readonly_fields = ["date_joined", "last_login"]
