package cl.duoc.nexo.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 5,
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

        // La versión 4 agregó la columna temaOscuro a parent_settings. Antes
        // ese cambio se aplicó con fallbackToDestructiveMigration(), lo que
        // borró los datos locales de los usuarios (jornadas incluidas). No
        // debe volver a pasar: de ahora en adelante cada cambio de esquema
        // necesita su propia Migration explícita como esta.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE parent_settings ADD COLUMN temaOscuro " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        // La versión 5 agrega "Redes Sociales" como categoría propia (antes
        // vivía dentro de Entretenimiento), así que reports necesita su
        // propia columna de minutos para esa categoría.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE reports ADD COLUMN minutosRedesSociales " +
                        "INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): NexoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    NexoDatabase::class.java,
                    "nexo_database"
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instancia
                instancia
            }
        }
    }
}