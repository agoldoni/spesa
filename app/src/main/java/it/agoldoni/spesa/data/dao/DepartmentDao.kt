package it.agoldoni.spesa.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import it.agoldoni.spesa.data.entity.DepartmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DepartmentDao {

    @Query("SELECT * FROM departments WHERE deleted = 0 ORDER BY position ASC")
    fun observeAll(): Flow<List<DepartmentEntity>>

    @Query("SELECT * FROM departments ORDER BY position ASC")
    suspend fun getAll(): List<DepartmentEntity>

    @Query("SELECT * FROM departments WHERE id = :id")
    suspend fun getById(id: String): DepartmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DepartmentEntity)

    @Query("DELETE FROM departments WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE departments SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: String, position: Int)

    @Transaction
    suspend fun reorder(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id -> updatePosition(id, index) }
    }
}
