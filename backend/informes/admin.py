from django.contrib import admin

from .models import AppUso, Informe


class AppUsoInline(admin.TabularInline):
    model = AppUso
    extra = 0


@admin.register(Informe)
class InformeAdmin(admin.ModelAdmin):
    list_display = ["nombre_jornada", "fecha", "dispositivo", "minutos_totales", "correo_enviado", "actualizado_en"]
    list_filter = ["fecha"]
    search_fields = ["nombre_jornada", "dispositivo__nombre", "dispositivo__apoderado__email"]
    inlines = [AppUsoInline]
