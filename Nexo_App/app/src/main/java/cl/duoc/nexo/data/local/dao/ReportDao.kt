package cl.duoc.nexo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.duoc.nexo.data.local.entities.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    @Insert
    suspend fun insertar(report: ReportEntity)

    @Query("SELECT * FROM reports ORDER BY generadoEn DESC")
    fun observarTodos(): Flow<List<ReportEntity>>

    /** Para no duplicar el informe de una jornada si el Worker se re-ejecuta para el mismo día. */
    @Query("SELECT COUNT(*) FROM reports WHERE scheduleId = :scheduleId AND fecha = :fecha")
    suspend fun contarPorJornadaYFecha(scheduleId: Int, fecha: String): Int
}
