package cl.duoc.nexo.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import cl.duoc.nexo.data.local.dao.ParentSettingsDao
import cl.duoc.nexo.data.local.dao.ReportDao
import cl.duoc.nexo.data.local.dao.ScheduleDao
import cl.duoc.nexo.data.local.dao.SupervisedAppDao
import cl.duoc.nexo.data.local.entities.ParentSettingsEntity
import cl.duoc.nexo.data.local.entities.ReportEntity
import cl.duoc.nexo.data.local.entities.ScheduleEntity
import cl.duoc.nexo.data.local.entities.SupervisedAppEntity

@Database(
    entities = [
        ParentSettingsEntity::class,
        ScheduleEntity::class,
        ReportEntity::class,
        SupervisedAppEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class NexoDatabase : RoomDatabase() {

    abstract fun parentSettingsDao(): ParentSettingsDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun reportDao(): ReportDao
    abstract fun supervisedAppDao(): SupervisedAppDao

    companion object {
        @Volatile
        private var INSTANCE: NexoDatabase? = null

        fun getInstance(context: Context): NexoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    NexoDatabase::class.java,
                    "nexo_database"
                )
                    // MVP académico: sin migraciones formales todavía: si cambia
                    // el esquema, se recrea la base en vez de crashear.
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instancia
                instancia
            }
        }
    }
}