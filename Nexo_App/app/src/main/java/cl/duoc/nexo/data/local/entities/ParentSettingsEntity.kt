package cl.duoc.nexo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parent_settings")
data class ParentSettingsEntity(
    @PrimaryKey val id: Int = 1, // Solo habrá un registro (config del apoderado)
    val nombreApoderado: String,
    val correoApoderado: String,
    val pinHash: String
)