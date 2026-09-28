package cl.duoc.nexo.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.duoc.nexo.data.local.dao.ExcepcionJornadaDao
import cl.duoc.nexo.data.local.dao.ParentSettingsDao
import cl.duoc.nexo.data.local.dao.ReportDao
import cl.duoc.nexo.data.local.dao.ScheduleDao
import cl.duoc.nexo.data.local.dao.SupervisedAppDao
import cl.duoc.nexo.data.local.entities.ExcepcionJornadaEntity
import cl.duoc.nexo.data.local.entities.ParentSettingsEntity
import cl.duoc.nexo.data.local.entities.ReportEntity
import cl.duoc.nexo.data.local.entities.ScheduleEntity
import cl.duoc.nexo.data.local.entities.SupervisedAppEntity

@Database(
    entities = [
        ParentSettingsEntity::class,
        ScheduleEntity::class,
        ReportEntity::class,
        SupervisedAppEntity::class,
        ExcepcionJornadaEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class NexoDatabase : RoomDatabase() {

    abstract fun parentSettingsDao(): ParentSettingsDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun reportDao(): ReportDao
    abstract fun supervisedAppDao(): SupervisedAppDao
    abstract fun excepcionJornadaDao(): ExcepcionJornadaDao

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

        // La versión 6 agrega los días configurados de la jornada al informe
        // guardado — el PDF rediseñado de informes los necesita mostrar.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE reports ADD COLUMN dias TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        // La versión 7 agrega el detalle por app (serializado como texto) al
        // informe guardado — el PDF necesita la sección "Aplicaciones
        // utilizadas" y solo existe la app real en el momento en que
        // JornadaReportWorker calcula el informe, no después.
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE reports ADD COLUMN appsUsadas TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        // La versión 8 agrega la tabla de excepciones manuales (vacaciones,
        // paros, etc.) que el WorkManager revisa antes de generar un informe.
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS excepciones_jornada (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "scheduleId INTEGER NOT NULL, " +
                        "fechaInicio TEXT NOT NULL, " +
                        "fechaTermino TEXT NOT NULL, " +
                        "motivo TEXT NOT NULL)"
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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build()
                INSTANCE = instancia
                instancia
            }
        }
    }
}