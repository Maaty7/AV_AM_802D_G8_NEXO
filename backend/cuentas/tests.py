from datetime import timedelta

from django.core import mail
from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APITestCase

from .models import CodigoVerificacion, Usuario
from .verificacion import hash_codigo


def crear_desafio_directo(usuario, codigo="123456", vencido=False, usado=False, intentos=0):
    """Crea un CodigoVerificacion directo en la BD, sin pasar por el endpoint — para probar casos puntuales sin depender del correo real."""
    ahora = timezone.now()
    return CodigoVerificacion.objects.create(
        usuario=usuario,
        codigo_hash=hash_codigo(codigo),
        expira_en=ahora - timedelta(minutes=1) if vencido else ahora + timedelta(minutes=10),
        ultimo_envio_en=ahora,
        usado=usado,
        intentos=intentos,
    )


class RegistroLoginTests(APITestCase):
    def test_registro_crea_usuario_y_devuelve_un_desafio_no_un_token(self):
        respuesta = self.client.post(
            reverse("registro"),
            {"email": "apoderado@example.com", "password": "clave12345", "nombre": "Ana"},
        )

        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertIn("desafio", respuesta.data)
        self.assertNotIn("token", respuesta.data)
        self.assertTrue(Usuario.objects.filter(email="apoderado@example.com").exists())

    def test_registro_envia_un_correo_con_el_codigo(self):
        self.client.post(
            reverse("registro"),
            {"email": "apoderado@example.com", "password": "clave12345", "nombre": "Ana"},
        )
        self.assertEqual(len(mail.outbox), 1)
        self.assertIn("código", mail.outbox[0].subject.lower())
        self.assertEqual(mail.outbox[0].to, ["apoderado@example.com"])

    def test_registro_con_email_repetido_falla(self):
        Usuario.objects.create_user(email="apoderado@example.com", password="clave12345")

        respuesta = self.client.post(
            reverse("registro"),
            {"email": "apoderado@example.com", "password": "otraClave123"},
        )

        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_login_con_credenciales_correctas_devuelve_un_desafio_no_un_token(self):
        Usuario.objects.create_user(email="apoderado@example.com", password="clave12345")

        respuesta = self.client.post(
            reverse("login"),
            {"email": "apoderado@example.com", "password": "clave12345"},
        )

        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertIn("desafio", respuesta.data)
        self.assertNotIn("token", respuesta.data)

    def test_login_con_password_incorrecta_falla(self):
        Usuario.objects.create_user(email="apoderado@example.com", password="clave12345")

        respuesta = self.client.post(
            reverse("login"),
            {"email": "apoderado@example.com", "password": "otra_clave"},
        )

        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)


class VerificarCodigoTests(APITestCase):
    def setUp(self):
        self.usuario = Usuario.objects.create_user(email="apoderado@example.com", password="clave12345")

    def test_codigo_correcto_devuelve_token_y_marca_el_usuario_verificado(self):
        desafio = crear_desafio_directo(self.usuario, codigo="123456")

        respuesta = self.client.post(
            reverse("verificar-codigo"), {"desafio": str(desafio.desafio), "codigo": "123456"}
        )

        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertIn("token", respuesta.data)
        self.usuario.refresh_from_db()
        self.assertTrue(self.usuario.verificado)

    def test_codigo_incorrecto_falla_y_suma_un_intento(self):
        desafio = crear_desafio_directo(self.usuario, codigo="123456")

        respuesta = self.client.post(
            reverse("verificar-codigo"), {"desafio": str(desafio.desafio), "codigo": "000000"}
        )

        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        desafio.refresh_from_db()
        self.assertEqual(desafio.intentos, 1)

    def test_codigo_vencido_falla(self):
        desafio = crear_desafio_directo(self.usuario, codigo="123456", vencido=True)

        respuesta = self.client.post(
            reverse("verificar-codigo"), {"desafio": str(desafio.desafio), "codigo": "123456"}
        )

        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_cinco_intentos_agotados_bloquea_incluso_con_el_codigo_correcto(self):
        desafio = crear_desafio_directo(self.usuario, codigo="123456")

        for _ in range(CodigoVerificacion.MAX_INTENTOS):
            self.client.post(
                reverse("verificar-codigo"), {"desafio": str(desafio.desafio), "codigo": "000000"}
            )

        desafio.refresh_from_db()
        self.assertEqual(desafio.intentos, CodigoVerificacion.MAX_INTENTOS)

        respuesta = self.client.post(
            reverse("verificar-codigo"), {"desafio": str(desafio.desafio), "codigo": "123456"}
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_un_codigo_ya_usado_no_se_puede_volver_a_verificar(self):
        desafio = crear_desafio_directo(self.usuario, codigo="123456")

        primera = self.client.post(
            reverse("verificar-codigo"), {"desafio": str(desafio.desafio), "codigo": "123456"}
        )
        self.assertEqual(primera.status_code, status.HTTP_200_OK)

        segunda = self.client.post(
            reverse("verificar-codigo"), {"desafio": str(desafio.desafio), "codigo": "123456"}
        )
        self.assertEqual(segunda.status_code, status.HTTP_400_BAD_REQUEST)

    def test_desafio_inexistente_falla(self):
        respuesta = self.client.post(
            reverse("verificar-codigo"),
            {"desafio": "00000000-0000-0000-0000-000000000000", "codigo": "123456"},
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)


class ReenviarCodigoTests(APITestCase):
    def setUp(self):
        self.usuario = Usuario.objects.create_user(email="apoderado@example.com", password="clave12345")

    def test_reenviar_antes_de_60_segundos_falla(self):
        desafio = crear_desafio_directo(self.usuario, codigo="123456")

        respuesta = self.client.post(reverse("reenviar-codigo"), {"desafio": str(desafio.desafio)})

        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(len(mail.outbox), 0)

    def test_reenviar_despues_de_60_segundos_envia_un_correo_nuevo_y_resetea_intentos(self):
        desafio = crear_desafio_directo(self.usuario, codigo="123456", intentos=3)
        desafio.ultimo_envio_en = timezone.now() - timedelta(seconds=61)
        desafio.save(update_fields=["ultimo_envio_en"])

        respuesta = self.client.post(reverse("reenviar-codigo"), {"desafio": str(desafio.desafio)})

        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertEqual(len(mail.outbox), 1)
        desafio.refresh_from_db()
        self.assertEqual(desafio.intentos, 0)

    def test_reenviar_un_desafio_ya_usado_falla(self):
        desafio = crear_desafio_directo(self.usuario, codigo="123456", usado=True)
        desafio.ultimo_envio_en = timezone.now() - timedelta(seconds=61)
        desafio.save(update_fields=["ultimo_envio_en"])

        respuesta = self.client.post(reverse("reenviar-codigo"), {"desafio": str(desafio.desafio)})

        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)


class FlujoCompletoTests(APITestCase):
    """Registro -> verificar con el código real enviado por correo -> token funciona."""

    def test_flujo_completo_de_registro_y_verificacion(self):
        respuesta_registro = self.client.post(
            reverse("registro"),
            {"email": "apoderado@example.com", "password": "clave12345", "nombre": "Ana"},
        )
        desafio = respuesta_registro.data["desafio"]

        # El código real nunca se devuelve en la respuesta — se extrae del cuerpo del correo (locmem en tests).
        cuerpo_correo = mail.outbox[0].body
        codigo = next(palabra for palabra in cuerpo_correo.split() if palabra.isdigit() and len(palabra) == 6)

        respuesta_verificar = self.client.post(
            reverse("verificar-codigo"), {"desafio": desafio, "codigo": codigo}
        )

        self.assertEqual(respuesta_verificar.status_code, status.HTTP_200_OK)
        self.assertIn("token", respuesta_verificar.data)
