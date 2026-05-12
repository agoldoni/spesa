package it.agoldoni.spesa.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorArgb: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
