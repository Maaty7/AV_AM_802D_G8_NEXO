package cl.duoc.nexo
import cl.duoc.nexo.ui.splash.SplashScreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.nexo.ui.theme.NEXOTheme
import cl.duoc.nexo.ui.navigation.NexoNavHost
import cl.duoc.nexo.viewmodel.TemaViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // La preferencia de tema (claro/oscuro) la elige el apoderado desde
            // Configuración y se guarda en Room; no sigue el modo del sistema.
            val temaViewModel: TemaViewModel = viewModel()
            NEXOTheme(darkTheme = temaViewModel.temaOscuro) {
                NexoNavHost()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NEXOTheme {
        Greeting("Android")
    }
}