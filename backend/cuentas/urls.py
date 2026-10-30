from django.urls import path

from .views import LoginView, ReenviarCodigoView, RegistroView, VerificarCodigoView

urlpatterns = [
    path("registro/", RegistroView.as_view(), name="registro"),
    path("login/", LoginView.as_view(), name="login"),
    path("verificar/", VerificarCodigoView.as_view(), name="verificar-codigo"),
    path("reenviar/", ReenviarCodigoView.as_view(), name="reenviar-codigo"),
]
