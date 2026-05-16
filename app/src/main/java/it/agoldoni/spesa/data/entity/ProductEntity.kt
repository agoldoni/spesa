package it.agoldoni.spesa.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["nameKey"], unique = true),
        Index("departmentId")
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameKey: String,
    val addedAt: Long,
    val updatedAt: Long = System.currentTimeMillis(),
    val departmentId: String? = null
)
