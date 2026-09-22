package cl.duoc.nexo.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.duoc.nexo.data.local.entities.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Insert
    suspend fun insertar(schedule: ScheduleEntity)

    @Update
    suspend fun actualizar(schedule: ScheduleEntity)

    @Delete
    suspend fun eliminar(schedule: ScheduleEntity)

    @Query("SELECT * FROM schedules ORDER BY id ASC")
    fun observarTodas(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Int): ScheduleEntity?
}