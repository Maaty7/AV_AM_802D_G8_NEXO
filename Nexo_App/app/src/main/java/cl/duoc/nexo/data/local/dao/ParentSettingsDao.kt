package cl.duoc.nexo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duoc.nexo.data.local.entities.ParentSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParentSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(settings: ParentSettingsEntity)

    @Query("SELECT * FROM parent_settings WHERE id = 1 LIMIT 1")
    suspend fun obtener(): ParentSettingsEntity?

    @Query("SELECT * FROM parent_settings WHERE id = 1 LIMIT 1")
    fun observar(): Flow<ParentSettingsEntity?>
}