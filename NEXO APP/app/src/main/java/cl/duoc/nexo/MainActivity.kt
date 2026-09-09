package cl.duoc.nexo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import cl.duoc.nexo.ui.theme.NEXOTheme
import cl.duoc.nexo.ui.usagetest.UsageTestScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NEXOTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Pantalla temporal: prueba de factibilidad de UsageStatsManager
                    // (prioridad técnica #1). Se reemplazará por la navegación real
                    // (splash -> configuración -> horarios) una vez validada.
                    UsageTestScreen(modifier = Modifier.fillMaxSize().padding(innerPadding))
                }
            }
        }
    }
}
