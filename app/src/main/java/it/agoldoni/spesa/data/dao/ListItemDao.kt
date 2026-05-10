package it.agoldoni.spesa.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.agoldoni.spesa.data.entity.ListItemEntity
import it.agoldoni.spesa.data.relation.ListItemWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ListItemDao {
    @Query(
        """
        SELECT li.id AS itemId, li.productId AS productId, p.name AS productName,
               li.quantity AS quantity, li.addedAt AS addedAt,
               li.memberId AS memberId, m.name AS memberName, m.colorArgb AS memberColor
        FROM list_items li
        INNER JOIN products p ON p.id = li.productId
        LEFT JOIN members m ON m.id = li.memberId
        ORDER BY li.addedAt ASC
        """
    )
    fun observeAllWithDetails(): Flow<List<ListItemWithDetails>>

    @Query("SELECT * FROM list_items WHERE productId = :productId LIMIT 1")
    suspend fun findByProduct(productId: String): ListItemEntity?

    @Query("SELECT * FROM list_items WHERE id = :id")
    suspend fun getById(id: String): ListItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ListItemEntity)

    @Query("UPDATE list_items SET quantity = :qty WHERE id = :id")
    suspend fun updateQuantity(id: String, qty: Int)

    @Query("DELETE FROM list_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT id FROM list_items")
    suspend fun getAllIds(): List<String>

    @Query("DELETE FROM list_items")
    suspend fun deleteAll()

    @Query("SELECT IFNULL(SUM(quantity), 0) FROM list_items")
    fun observeTotalQuantity(): Flow<Int>

    @Query("SELECT COUNT(*) FROM list_items")
    fun observeItemCount(): Flow<Int>
}
