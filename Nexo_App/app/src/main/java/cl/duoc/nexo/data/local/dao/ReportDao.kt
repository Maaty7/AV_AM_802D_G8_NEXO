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

    /**
     * Informe más reciente de la MISMA jornada (mismo [scheduleId]) con fecha
     * anterior a [fecha] — para la sección "Comparación con la jornada
     * anterior" del PDF. `fecha` es texto "yyyy-MM-dd", así que el orden
     * lexicográfico ya coincide con el orden cronológico.
     *
     * Sin `suspend`: la usa [cl.duoc.nexo.pdf.InformePdfGenerator], que ya
     * hace E/S de disco de forma síncrona en el hilo desde el que se lo
     * llama (igual que el resto de esa clase).
     */
    @Query("SELECT * FROM reports WHERE scheduleId = :scheduleId AND fecha < :fecha ORDER BY fecha DESC LIMIT 1")
    fun obtenerAnteriorDeLaMismaJornada(scheduleId: Int, fecha: String): ReportEntity?
}
