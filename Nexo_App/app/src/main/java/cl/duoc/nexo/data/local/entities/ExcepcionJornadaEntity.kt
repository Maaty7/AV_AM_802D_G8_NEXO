package cl.duoc.nexo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un rango de fechas en que NO corresponde generar informe automático
 * (vacaciones, paro, día libre, etc.), sin importar si el día de la semana
 * y la jornada coinciden. [scheduleId] = 0 significa que aplica a todas
 * las jornadas del apoderado, no solo a una en particular.
 */
@Entity(tableName = "excepciones_jornada")
data class ExcepcionJornadaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scheduleId: Int = 0,
    val fechaInicio: String, // "yyyy-MM-dd"
    val fechaTermino: String, // "yyyy-MM-dd"
    val motivo: String
)
