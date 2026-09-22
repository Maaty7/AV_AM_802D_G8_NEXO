package cl.duoc.nexo.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supervised_apps")
data class SupervisedAppEntity(
    @PrimaryKey val packageName: String,
    val supervisada: Boolean
)
