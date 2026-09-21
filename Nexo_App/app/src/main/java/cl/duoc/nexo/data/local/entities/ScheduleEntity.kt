package cl.duoc.nexo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val horaInicio: String,
    val horaTermino: String,
    val dias: String,
    val activo: Boolean = true
)