package cl.duoc.nexo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nexo.domain.AppUsageInfo
import cl.duoc.nexo.monitoring.UsageAccessPermission
import cl.duoc.nexo.monitoring.UsageStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado de la pantalla de prueba de factibilidad de UsageStatsManager.
 */
data class UsageTestUiState(
    val isPermissionGranted: Boolean = false,
    val isLoading: Boolean = false,
    val appUsageList: List<AppUsageInfo> = emptyList(),
    val hasQueried: Boolean = false
)

/**
 * ViewModel de la prueba de factibilidad (capa UI -> ViewModel -> monitoring,
 * siguiendo la misma arquitectura MVVM que usará el resto de la app).
 */
class UsageTestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UsageStatsRepository(application)

    private val _uiState = MutableStateFlow(UsageTestUiState())
    val uiState: StateFlow<UsageTestUiState> = _uiState.asStateFlow()

    /** Revisa si el permiso de Usage Access está otorgado (llamar en onResume). */
    fun refreshPermissionState() {
        val granted = UsageAccessPermission.isGranted(getApplication())
        _uiState.value = _uiState.value.copy(isPermissionGranted = granted)
    }

    /** Lleva al usuario a Ajustes para otorgar el acceso a datos de uso. */
    fun requestUsageAccess() {
        UsageAccessPermission.openSettings(getApplication())
    }

    /** Consulta UsageStatsManager y actualiza la lista de apps usadas hoy. */
    fun loadTodayUsage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val usage = withContext(Dispatchers.Default) { repository.getTodayUsage() }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                appUsageList = usage,
                hasQueried = true
            )
        }
    }
}
