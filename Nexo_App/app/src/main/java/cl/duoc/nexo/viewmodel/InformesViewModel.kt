package cl.duoc.nexo.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.entities.ReportEntity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class InformesViewModel(application: Application) : AndroidViewModel(application) {

    private val reportDao = NexoDatabase.getInstance(application).reportDao()

    var informes by mutableStateOf<List<ReportEntity>>(emptyList())
        private set

    init {
        reportDao.observarTodos()
            .onEach { lista -> informes = lista }
            .launchIn(viewModelScope)
    }
}
