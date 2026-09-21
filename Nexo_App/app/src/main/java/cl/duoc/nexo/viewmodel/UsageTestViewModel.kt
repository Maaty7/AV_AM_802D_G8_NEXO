package cl.duoc.nexo.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import cl.duoc.nexo.domain.AppUsageInfo
import cl.duoc.nexo.monitoring.UsageStatsRepository

class UsageTestViewModel(application: Application) : AndroidViewModel(application) {

    var apps by mutableStateOf<List<AppUsageInfo>>(emptyList())
        private set

    fun cargarUsoDeHoy() {
        apps = UsageStatsRepository.obtenerUsoDeHoy(getApplication())
    }
}