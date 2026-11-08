from django.conf import settings
from django.db import models


class Dispositivo(models.Model):
    """
    Un teléfono vinculado a un apoderado. [uuid] lo genera/envía la app (no es
    el id de Django) — identifica al dispositivo. NO es único globalmente a
    propósito: solo es único POR apoderado (ver Meta.constraints), para que
    dos apoderados distintos nunca compartan ni mezclen datos aunque alguna
    vez coincidiera el mismo uuid entre ambos.
    """

    apoderado = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="dispositivos"
    )
    uuid = models.CharField(max_length=64)
    nombre = models.CharField(max_length=150)
    fecha_vinculo = models.DateTimeField(auto_now_add=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=["apoderado", "uuid"], name="dispositivo_unico_por_apoderado")
        ]
        ordering = ["-fecha_vinculo"]

    def __str__(self):
        return f"{self.nombre} ({self.apoderado.email})"
