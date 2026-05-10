package it.agoldoni.spesa.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import it.agoldoni.spesa.data.entity.FavoriteEntity
import it.agoldoni.spesa.data.relation.FavoriteWithProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query(
        """
        SELECT f.id AS favoriteId, f.productId AS productId,
               p.name AS productName, f.ordering AS ordering
        FROM favorites f
        INNER JOIN products p ON p.id = f.productId
        ORDER BY f.ordering ASC
        """
    )
    fun observeAllWithProduct(): Flow<List<FavoriteWithProduct>>

    @Query("SELECT * FROM favorites ORDER BY ordering ASC")
    suspend fun getAll(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE favorites SET ordering = :ordering WHERE id = :id")
    suspend fun updateOrdering(id: String, ordering: Int)

    @Transaction
    suspend fun reorder(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id ->
            updateOrdering(id, index)
        }
    }
}
