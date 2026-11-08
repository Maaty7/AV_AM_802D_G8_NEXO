from django.contrib import admin

from .models import Dispositivo


@admin.register(Dispositivo)
class DispositivoAdmin(admin.ModelAdmin):
    list_display = ["nombre", "uuid", "apoderado", "fecha_vinculo"]
    list_filter = ["fecha_vinculo"]
    search_fields = ["nombre", "uuid", "apoderado__email"]
