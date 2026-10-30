from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Usuario


class RegistroLoginTests(APITestCase):
    def test_registro_crea_usuario_y_devuelve_token(self):
        respuesta = self.client.post(
            reverse("registro"),
            {"email": "apoderado@example.com", "password": "clave12345", "nombre": "Ana"},
        )

        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)
        self.assertIn("token", respuesta.data)
        self.assertTrue(Usuario.objects.filter(email="apoderado@example.com").exists())

    def test_registro_con_email_repetido_falla(self):
        Usuario.objects.create_user(email="apoderado@example.com", password="clave12345")

        respuesta = self.client.post(
            reverse("registro"),
            {"email": "apoderado@example.com", "password": "otraClave123"},
        )

        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_login_con_credenciales_correctas_devuelve_token(self):
        Usuario.objects.create_user(email="apoderado@example.com", password="clave12345")

        respuesta = self.client.post(
            reverse("login"),
            {"email": "apoderado@example.com", "password": "clave12345"},
        )

        self.assertEqual(respuesta.status_code, status.HTTP_200_OK)
        self.assertIn("token", respuesta.data)

    def test_login_con_password_incorrecta_falla(self):
        Usuario.objects.create_user(email="apoderado@example.com", password="clave12345")

        respuesta = self.client.post(
            reverse("login"),
            {"email": "apoderado@example.com", "password": "otra_clave"},
        )

        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)
