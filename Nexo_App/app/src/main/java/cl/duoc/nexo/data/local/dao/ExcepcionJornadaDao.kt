package cl.duoc.nexo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import cl.duoc.nexo.data.local.entities.ExcepcionJornadaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExcepcionJornadaDao {

    @Insert
    suspend fun insertar(excepcion: ExcepcionJornadaEntity)

    @Delete
    suspend fun eliminar(excepcion: ExcepcionJornadaEntity)

    @Query("SELECT * FROM excepciones_jornada ORDER BY fechaInicio ASC")
    fun observarTodas(): Flow<List<ExcepcionJornadaEntity>>

    /** Excepciones que cubren [fecha] ("yyyy-MM-dd") y aplican a [scheduleId] (o a todas, scheduleId = 0). */
    @Query(
        "SELECT * FROM excepciones_jornada " +
            "WHERE (scheduleId = :scheduleId OR scheduleId = 0) " +
            "AND :fecha BETWEEN fechaInicio AND fechaTermino"
    )
    suspend fun obtenerVigentes(scheduleId: Int, fecha: String): List<ExcepcionJornadaEntity>
}
