package it.agoldoni.spesa.data.repository

import it.agoldoni.spesa.data.AppDatabase
import it.agoldoni.spesa.data.entity.FavoriteEntity
import it.agoldoni.spesa.data.entity.ListItemEntity
import it.agoldoni.spesa.data.entity.MemberEntity
import it.agoldoni.spesa.data.entity.ProductEntity
import it.agoldoni.spesa.data.relation.FavoriteWithProduct
import it.agoldoni.spesa.data.relation.ListItemWithDetails
import it.agoldoni.spesa.sync.SyncSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpesaRepository @Inject constructor(
    private val db: AppDatabase,
    private val sync: SyncSource
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observeMembers(): Flow<List<MemberEntity>> = db.memberDao().observeAll()
    fun observeListItems(): Flow<List<ListItemWithDetails>> = db.listItemDao().observeAllWithDetails()
    fun observeFavorites(): Flow<List<FavoriteWithProduct>> = db.favoriteDao().observeAllWithProduct()
    fun observeItemCount(): Flow<Int> = db.listItemDao().observeItemCount()
    fun observeTotalQuantity(): Flow<Int> = db.listItemDao().observeTotalQuantity()
    fun observeSuggestions(prefix: String): Flow<List<ProductEntity>> =
        db.productDao().observeSuggestions(prefix.lowercase(Locale.ROOT))

    suspend fun ensureSeedMembers(seed: List<MemberEntity>) {
        db.memberDao().insertIfMissing(seed)
        seed.forEach { mirror { sync.pushMember(it) } }
    }

    /** Adds a product by name, or increments quantity if already in the list. */
    suspend fun addOrIncrementByName(rawName: String, memberId: String?) {
        val name = rawName.trim()
        if (name.isEmpty()) return
        val product = ensureProduct(name)
        addOrIncrement(product.id, memberId)
    }

    suspend fun addOrIncrement(productId: String, memberId: String?) {
        val existing = db.listItemDao().findByProduct(productId)
        if (existing != null) {
            val updated = existing.copy(quantity = existing.quantity + 1)
            db.listItemDao().upsert(updated)
            mirror { sync.pushListItem(updated) }
        } else {
            val item = ListItemEntity(
                id = UUID.randomUUID().toString(),
                productId = productId,
                quantity = 1,
                memberId = memberId,
                addedAt = System.currentTimeMillis()
            )
            db.listItemDao().upsert(item)
            mirror { sync.pushListItem(item) }
        }
    }

    suspend fun increment(itemId: String) {
        val item = db.listItemDao().getById(itemId) ?: return
        val updated = item.copy(quantity = item.quantity + 1)
        db.listItemDao().upsert(updated)
        mirror { sync.pushListItem(updated) }
    }

    suspend fun decrement(itemId: String) {
        val item = db.listItemDao().getById(itemId) ?: return
        if (item.quantity <= 1) return
        val updated = item.copy(quantity = item.quantity - 1)
        db.listItemDao().upsert(updated)
        mirror { sync.pushListItem(updated) }
    }

    suspend fun remove(itemId: String) {
        db.listItemDao().deleteById(itemId)
        mirror { sync.deleteListItem(itemId) }
    }

    suspend fun clearAll() {
        db.listItemDao().deleteAll()
        mirror { sync.clearListItems() }
    }

    suspend fun toggleFavorite(productId: String) {
        val all = db.favoriteDao().getAll()
        val existing = all.firstOrNull { it.productId == productId }
        if (existing != null) {
            db.favoriteDao().deleteById(existing.id)
            mirror { sync.deleteFavorite(existing.id) }
        } else {
            val fav = FavoriteEntity(
                id = UUID.randomUUID().toString(),
                productId = productId,
                ordering = all.size
            )
            db.favoriteDao().upsert(fav)
            mirror { sync.pushFavorite(fav) }
        }
    }

    suspend fun reorderFavorites(orderedIds: List<String>) {
        db.favoriteDao().reorder(orderedIds)
        mirror { sync.pushFavoriteOrder(orderedIds) }
    }

    private suspend fun ensureProduct(name: String): ProductEntity {
        val key = name.lowercase(Locale.ROOT)
        db.productDao().findByKey(key)?.let { return it }
        val product = ProductEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            nameKey = key,
            addedAt = System.currentTimeMillis()
        )
        db.productDao().upsert(product)
        mirror { sync.pushProduct(product) }
        return product
    }

    private fun mirror(block: suspend () -> Unit) {
        scope.launch { runCatching { block() } }
    }
}
