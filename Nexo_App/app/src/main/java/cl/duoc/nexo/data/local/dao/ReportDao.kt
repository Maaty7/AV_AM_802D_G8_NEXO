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
}
