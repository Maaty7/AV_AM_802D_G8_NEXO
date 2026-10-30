package cl.duoc.nexo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.duoc.nexo.data.local.entities.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    /** Devuelve el id autogenerado de la fila insertada — lo usa SubirInformeScheduler para encolar su sincronización. */
    @Insert
    suspend fun insertar(report: ReportEntity): Long

    @Query("SELECT * FROM reports ORDER BY generadoEn DESC")
    fun observarTodos(): Flow<List<ReportEntity>>

    /** Para "Sincronizar ahora": sube todos los informes guardados de una vez, sin observar cambios. */
    @Query("SELECT * FROM reports ORDER BY generadoEn DESC")
    suspend fun obtenerTodos(): List<ReportEntity>

    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Int): ReportEntity?

    /** Para no duplicar el informe de una jornada si el Worker se re-ejecuta para el mismo día. */
    @Query("SELECT COUNT(*) FROM reports WHERE scheduleId = :scheduleId AND fecha = :fecha")
    suspend fun contarPorJornadaYFecha(scheduleId: Int, fecha: String): Int
}
