from django.urls import path

from .views import InformeListaCreaView

urlpatterns = [
    path("", InformeListaCreaView.as_view(), name="informes"),
]
