from django.db import migrations


def marcar_usuarios_existentes_como_verificados(apps, schema_editor):
    """
    Usuarios creados ANTES de que existiera el campo `verificado` (y por lo
    tanto antes de que existiera el flujo de 2FA) no deberían quedar
    marcados como "no verificados" solo por una casualidad de fechas — se
    consideran verificados retroactivamente. Cualquier usuario creado
    DESPUÉS de esta migración sigue naciendo con verificado=False, como
    corresponde, hasta que complete su primer código.
    """
    Usuario = apps.get_model("cuentas", "Usuario")
    Usuario.objects.update(verificado=True)


def no_hacer_nada_al_revertir(apps, schema_editor):
    # No tiene sentido "desverificar" usuarios al revertir — no-op.
    pass


class Migration(migrations.Migration):

    dependencies = [
        ("cuentas", "0002_usuario_verificado_codigoverificacion"),
    ]

    operations = [
        migrations.RunPython(marcar_usuarios_existentes_como_verificados, no_hacer_nada_al_revertir),
    ]
