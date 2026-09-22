package cl.duoc.nexo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scheduleId: Int,
    val nombreJornada: String,
    val fecha: String,
    val horaInicio: String,
    val horaTermino: String,
    val minutosTotales: Long,
    val minutosEducacion: Long,
    val minutosComunicacion: Long,
    val minutosEntretenimiento: Long,
    val minutosJuegos: Long,
    val minutosOtros: Long,
    val generadoEn: Long
)
