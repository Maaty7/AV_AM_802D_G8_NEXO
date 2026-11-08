from django.db import models

# Las mismas 6 categorías que usa cl.duoc.nexo.ui.theme.coloresPorCategoria en la app.
CATEGORIAS_VALIDAS = [
    "Educación",
    "Comunicación",
    "Redes Sociales",
    "Entretenimiento",
    "Juegos",
    "Otros",
]
CATEGORIA_CHOICES = [(categoria, categoria) for categoria in CATEGORIAS_VALIDAS]


class Informe(models.Model):
    """
    Espejo de ReportEntity.kt de la app (Room). appsUsadas, el texto serializado
    "nombre|categoria|minutos" que usa Room para no necesitar una tabla hija,
    se recibe como una lista estructurada y se guarda en AppUso — el backend
    no tiene la restricción de Room que motivó esa serialización.
    """

    dispositivo = models.ForeignKey(
        "dispositivos.Dispositivo", on_delete=models.CASCADE, related_name="informes"
    )
    nombre_jornada = models.CharField(max_length=150)
    fecha = models.DateField()
    hora_inicio = models.TimeField()
    hora_termino = models.TimeField()
    dias = models.CharField(max_length=50, blank=True, default="")

    minutos_totales = models.PositiveIntegerField()
    minutos_educacion = models.PositiveIntegerField(default=0)
    minutos_comunicacion = models.PositiveIntegerField(default=0)
    minutos_redes_sociales = models.PositiveIntegerField(default=0)
    minutos_entretenimiento = models.PositiveIntegerField(default=0)
    minutos_juegos = models.PositiveIntegerField(default=0)
    minutos_otros = models.PositiveIntegerField(default=0)

    # Epoch millis — igual que ReportEntity.generadoEn (System.currentTimeMillis()).
    generado_en = models.BigIntegerField()

    # Para no reenviar el correo de resumen en cada actualización idempotente
    # del mismo informe — se marca True recién cuando el envío tuvo éxito,
    # así que si falló, el próximo re-post (el reintento normal del Worker
    # de la app) tiene otra oportunidad de enviarlo.
    correo_enviado = models.BooleanField(default=False)

    creado_en = models.DateTimeField(auto_now_add=True)
    actualizado_en = models.DateTimeField(auto_now=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["dispositivo", "nombre_jornada", "fecha"],
                name="informe_unico_por_dispositivo_jornada_fecha",
            )
        ]
        ordering = ["-fecha", "-generado_en"]

    def __str__(self):
        return f"{self.nombre_jornada} — {self.fecha} ({self.dispositivo})"


class AppUso(models.Model):
    """Una fila de la lista de apps usadas durante una jornada, con su categoría y minutos."""

    informe = models.ForeignKey(Informe, on_delete=models.CASCADE, related_name="apps")
    nombre = models.CharField(max_length=150)
    categoria = models.CharField(max_length=30, choices=CATEGORIA_CHOICES)
    minutos = models.PositiveIntegerField()

    def __str__(self):
        return f"{self.nombre} ({self.minutos} min)"
