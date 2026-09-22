package cl.duoc.nexo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duoc.nexo.data.local.entities.SupervisedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupervisedAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(app: SupervisedAppEntity)

    @Query("SELECT * FROM supervised_apps")
    fun observarTodas(): Flow<List<SupervisedAppEntity>>

    @Query("SELECT packageName FROM supervised_apps WHERE supervisada = 0")
    suspend fun obtenerPackagesNoSupervisados(): List<String>
}
