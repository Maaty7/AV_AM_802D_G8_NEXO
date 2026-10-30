package cl.duoc.nexo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.duoc.nexo.data.local.entities.PausaJornadaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PausaJornadaDao {

    @Insert
    suspend fun insertar(pausa: PausaJornadaEntity)

    @Update
    suspend fun actualizar(pausa: PausaJornadaEntity)

    @Delete
    suspend fun eliminar(pausa: PausaJornadaEntity)

    @Query("SELECT * FROM pausas_jornada WHERE scheduleId = :scheduleId ORDER BY horaInicio ASC")
    fun observarPorJornada(scheduleId: Int): Flow<List<PausaJornadaEntity>>

    @Query("SELECT * FROM pausas_jornada WHERE scheduleId = :scheduleId ORDER BY horaInicio ASC")
    suspend fun obtenerPorJornada(scheduleId: Int): List<PausaJornadaEntity>

    /** Se usa al eliminar una jornada — no hay FK/cascada, hay que limpiar a mano. */
    @Query("DELETE FROM pausas_jornada WHERE scheduleId = :scheduleId")
    suspend fun eliminarPorJornada(scheduleId: Int)
}
