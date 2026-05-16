package it.agoldoni.spesa.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "departments")
data class DepartmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
