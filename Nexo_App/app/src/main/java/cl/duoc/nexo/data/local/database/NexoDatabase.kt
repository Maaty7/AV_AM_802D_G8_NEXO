package cl.duoc.nexo.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cl.duoc.nexo.data.local.dao.ParentSettingsDao
import cl.duoc.nexo.data.local.dao.ScheduleDao
import cl.duoc.nexo.data.local.entities.ParentSettingsEntity
import cl.duoc.nexo.data.local.entities.ScheduleEntity

@Database(
    entities = [ParentSettingsEntity::class, ScheduleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NexoDatabase : RoomDatabase() {

    abstract fun parentSettingsDao(): ParentSettingsDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: NexoDatabase? = null

        fun getInstance(context: Context): NexoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    NexoDatabase::class.java,
                    "nexo_database"
                ).build()
                INSTANCE = instancia
                instancia
            }
        }
    }
}