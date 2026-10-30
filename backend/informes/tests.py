from django.urls import reverse
from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.test import APIClient, APITestCase

from cuentas.models import Usuario
from dispositivos.models import Dispositivo

from .models import Informe


def payload_informe(**overrides):
    base = {
        "dispositivo_uuid": "11111111-1111-1111-1111-111111111111",
        "dispositivo_nombre": "Teléfono de Ana",
        "nombre_jornada": "Horario escolar",
        "fecha": "2026-10-20",
        "hora_inicio": "08:00:00",
        "hora_termino": "14:00:00",
        "dias": "LU, MA, MI, JU, VI",
        "minutos_totales": 90,
        "minutos_educacion": 60,
        "minutos_comunicacion": 10,
        "minutos_redes_sociales": 0,
        "minutos_entretenimiento": 20,
        "minutos_juegos": 0,
        "minutos_otros": 0,
        "generado_en": 1760000000000,
        "apps": [
            {"nombre": "Classroom", "categoria": "Educación", "minutos": 60},
            {"nombre": "WhatsApp", "categoria": "Comunicación", "minutos": 10},
            {"nombre": "YouTube", "categoria": "Entretenimiento", "minutos": 20},
        ],
    }
    base.update(overrides)
    return base


def crear_apoderado_autenticado(email="ana@example.com"):
    apoderado = Usuario.objects.create_user(email=email, password="clave12345")
    token = Token.objects.create(user=apoderado)
    cliente = APIClient()
    cliente.credentials(HTTP_AUTHORIZATION=f"Token {token.key}")
    return apoderado, cliente


class InformesTests(APITestCase):
    def setUp(self):
        self.apoderado, self.client = crear_apoderado_autenticado("ana@example.com")
        self.url = reverse("informes")

    def test_subir_informe_autenticado_lo_crea(self):
        respuesta = self.client.post(self.url, payload_informe(), format="json")

        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)
        self.assertEqual(Informe.objects.count(), 1)
        self.assertEqual(Dispositivo.objects.count(), 1)
        self.assertEqual(Informe.objects.first().apps.count(), 3)

    def test_subir_informe_sin_token_falla(self):
        cliente_sin_token = APIClient()
        respuesta = cliente_sin_token.post(self.url, payload_informe(), format="json")
        self.assertEqual(respuesta.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_subir_informe_con_minutos_negativos_falla(self):
        respuesta = self.client.post(
            self.url, payload_informe(minutos_totales=-5), format="json"
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_subir_informe_con_categoria_invalida_falla(self):
        respuesta = self.client.post(
            self.url,
            payload_informe(apps=[{"nombre": "App rara", "categoria": "Deportes", "minutos": 5}]),
            format="json",
        )
        self.assertEqual(respuesta.status_code, status.HTTP_400_BAD_REQUEST)

    def test_subir_mismo_informe_dos_veces_actualiza_no_duplica(self):
        self.client.post(self.url, payload_informe(), format="json")

        respuesta = self.client.post(
            self.url,
            payload_informe(
                minutos_totales=120,
                apps=[{"nombre": "Classroom", "categoria": "Educación", "minutos": 120}],
            ),
            format="json",
        )

        self.assertEqual(respuesta.status_code, status.HTTP_201_CREATED)
        self.assertEqual(Informe.objects.count(), 1)
        informe = Informe.objects.first()
        self.assertEqual(informe.minutos_totales, 120)
        self.assertEqual(informe.apps.count(), 1)
        self.assertEqual(informe.apps.first().nombre, "Classroom")

    def test_listar_informes_filtra_por_apoderado_autenticado(self):
        self.client.post(self.url, payload_informe(), format="json")

        _, cliente_otro = crear_apoderado_autenticado("otro@example.com")
        cliente_otro.post(
            self.url,
            payload_informe(
                dispositivo_uuid="22222222-2222-2222-2222-222222222222",
                fecha="2026-10-21",
            ),
            format="json",
        )

        respuesta = self.client.get(self.url)
        self.assertEqual(len(respuesta.data), 1)

        respuesta_otro = cliente_otro.get(self.url)
        self.assertEqual(len(respuesta_otro.data), 1)

    def test_listar_informes_filtra_por_fecha(self):
        self.client.post(self.url, payload_informe(fecha="2026-10-20"), format="json")
        self.client.post(
            self.url,
            payload_informe(nombre_jornada="Fin de semana", fecha="2026-10-21"),
            format="json",
        )

        respuesta = self.client.get(self.url, {"fecha": "2026-10-20"})

        self.assertEqual(len(respuesta.data), 1)
        self.assertEqual(respuesta.data[0]["fecha"], "2026-10-20")

    def test_mismo_uuid_de_dispositivo_en_dos_apoderados_no_mezcla_datos(self):
        self.client.post(self.url, payload_informe(), format="json")

        otro_apoderado, cliente_otro = crear_apoderado_autenticado("otro@example.com")
        # Mismo dispositivo_uuid que usó self.apoderado, pero lo manda OTRO apoderado.
        cliente_otro.post(
            self.url,
            payload_informe(fecha="2026-10-22", nombre_jornada="Otro horario"),
            format="json",
        )

        # El mismo uuid existe dos veces, pero como filas de Dispositivo distintas.
        self.assertEqual(Dispositivo.objects.filter(uuid="11111111-1111-1111-1111-111111111111").count(), 2)
        self.assertEqual(Informe.objects.filter(dispositivo__apoderado=self.apoderado).count(), 1)
        self.assertEqual(Informe.objects.filter(dispositivo__apoderado=otro_apoderado).count(), 1)

        respuesta = self.client.get(self.url)
        self.assertEqual(len(respuesta.data), 1)
        self.assertEqual(respuesta.data[0]["nombre_jornada"], "Horario escolar")

        respuesta_otro = cliente_otro.get(self.url)
        self.assertEqual(len(respuesta_otro.data), 1)
        self.assertEqual(respuesta_otro.data[0]["nombre_jornada"], "Otro horario")
