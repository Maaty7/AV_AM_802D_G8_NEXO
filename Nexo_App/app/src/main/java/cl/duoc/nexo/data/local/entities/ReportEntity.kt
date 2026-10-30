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
    val dias: String = "",
    val minutosTotales: Long,
    val minutosEducacion: Long,
    val minutosComunicacion: Long,
    val minutosRedesSociales: Long = 0L,
    val minutosEntretenimiento: Long,
    val minutosJuegos: Long,
    val minutosOtros: Long,
    /**
     * Detalle por app usada en la jornada, serializado como texto simple
     * (una app por línea: "nombre|categoria|minutos") para no necesitar una
     * tabla hija — solo se usa para pintar el PDF, nunca se consulta en SQL.
     */
    val appsUsadas: String = "",
    /**
     * Columnas agregadas por MIGRATION_8_9 junto con la tabla pausas_jornada,
     * pero sin uso: el uso en pausas se descarta del cálculo, no se guarda.
     * Se mantienen declaradas acá únicamente porque ya existen físicamente en
     * instalaciones que corrieron esa migración — removerlas del código haría
     * que Room las vea como "columnas inesperadas" en reports y falle la
     * validación post-migración ("Migration didn't properly handle: reports").
     */
    val minutosPausas: Long = 0L,
    val appsUsadasPausas: String = "",
    val generadoEn: Long
)
