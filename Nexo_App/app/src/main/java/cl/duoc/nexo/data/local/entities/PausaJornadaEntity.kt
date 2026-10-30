package cl.duoc.nexo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un recreo/descanso dentro de una jornada ([scheduleId]): el uso de apps en
 * este rango no cuenta en el total ni en las categorías de la jornada, se
 * contabiliza aparte. Sin `@ForeignKey` hacia ScheduleEntity — mismo criterio
 * que [ExcepcionJornadaEntity], para no complicar la migración con una
 * restricción de integridad; al eliminar una jornada hay que borrar sus
 * pausas a mano (ver HorariosViewModel.eliminarJornada).
 */
@Entity(tableName = "pausas_jornada")
data class PausaJornadaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scheduleId: Int,
    val nombre: String,
    val horaInicio: String, // "HH:mm"
    val horaTermino: String // "HH:mm"
)
