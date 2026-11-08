from rest_framework import generics, status
from rest_framework.response import Response

from .models import Informe
from .serializers import InformeEntradaSerializer, InformeSalidaSerializer


class InformeListaCreaView(generics.ListCreateAPIView):
    """
    GET: informes del apoderado autenticado (vía dispositivo__apoderado),
    filtrables por ?fecha=yyyy-mm-dd.
    POST: crea o actualiza (idempotente por dispositivo+nombre_jornada+fecha).
    """

    def get_queryset(self):
        queryset = Informe.objects.filter(dispositivo__apoderado=self.request.user)
        fecha = self.request.query_params.get("fecha")
        if fecha:
            queryset = queryset.filter(fecha=fecha)
        return queryset

    def get_serializer_class(self):
        if self.request.method == "POST":
            return InformeEntradaSerializer
        return InformeSalidaSerializer

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        informe = serializer.save()
        return Response(InformeSalidaSerializer(informe).data, status=status.HTTP_201_CREATED)
