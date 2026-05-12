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

    /**
     * Ensures a [MemberEntity] exists for the given username. The member id is the
     * trimmed username (stable identity). The displayed [MemberEntity.name] is the
     * trimmed alias when non-blank, otherwise the username itself. Color is derived
     * deterministically from the username, so renames don't shift the hue.
     * If the existing member's name no longer matches the desired one (e.g. alias
     * was added or changed) the entity is updated and re-synced. Returns null for
     * blank usernames.
     */
    suspend fun ensureCurrentUserMember(username: String, alias: String = ""): MemberEntity? {
        val u = username.trim()
        if (u.isEmpty()) return null
        val a = alias.trim()
        val displayName = if (a.isNotEmpty()) a else u

        val existing = db.memberDao().getById(u)
        if (existing != null && existing.name == displayName) return existing

        val color = existing?.colorArgb ?: colorForUsername(u)
        val member = MemberEntity(
            id = u,
            name = displayName,
            colorArgb = color,
            updatedAt = System.currentTimeMillis()
        )
        db.memberDao().upsert(member)
        mirror { sync.pushMember(member) }
        return member
    }

    /**
     * Same as [ensureCurrentUserMember] but launched on the repository's
     * application-scoped coroutine. Use this from short-lived contexts (e.g. an
     * Activity save handler) so the work survives the caller's lifecycle.
     */
    fun applyUserMemberConfig(
        username: String,
        alias: String,
        onActiveMember: (String) -> Unit = {}
    ) {
        scope.launch {
            ensureCurrentUserMember(username, alias)?.let { onActiveMember(it.id) }
        }
    }

    private fun colorForUsername(name: String): Long {
        val idx = Math.floorMod(name.hashCode(), USER_PALETTE.size)
        return USER_PALETTE[idx]
    }

    suspend fun addOrIncrementByName(rawName: String, memberId: String?) {
        val name = rawName.trim()
        if (name.isEmpty()) return
        val product = ensureProduct(name)
        addOrIncrement(product.id, memberId)
    }

    suspend fun addOrIncrement(productId: String, memberId: String?) {
        val now = System.currentTimeMillis()
        val existing = db.listItemDao().findByProduct(productId)
        if (existing != null) {
            val updated = existing.copy(quantity = existing.quantity + 1, updatedAt = now)
            db.listItemDao().upsert(updated)
            mirror { sync.pushListItem(updated) }
        } else {
            val item = ListItemEntity(
                id = UUID.randomUUID().toString(),
                productId = productId,
                quantity = 1,
                memberId = memberId,
                addedAt = now,
                updatedAt = now
            )
            db.listItemDao().upsert(item)
            mirror { sync.pushListItem(item) }
        }
    }

    suspend fun increment(itemId: String) {
        val item = db.listItemDao().getById(itemId) ?: return
        val updated = item.copy(quantity = item.quantity + 1, updatedAt = System.currentTimeMillis())
        db.listItemDao().upsert(updated)
        mirror { sync.pushListItem(updated) }
    }

    suspend fun decrement(itemId: String) {
        val item = db.listItemDao().getById(itemId) ?: return
        if (item.quantity <= 1) return
        val updated = item.copy(quantity = item.quantity - 1, updatedAt = System.currentTimeMillis())
        db.listItemDao().upsert(updated)
        mirror { sync.pushListItem(updated) }
    }

    suspend fun remove(itemId: String) {
        db.listItemDao().deleteById(itemId)
        mirror { sync.deleteListItem(itemId) }
    }

    suspend fun clearAll() {
        val ids = db.listItemDao().getAllIds()
        db.listItemDao().deleteAll()
        ids.forEach { id -> mirror { sync.deleteListItem(id) } }
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
                ordering = all.size,
                updatedAt = System.currentTimeMillis()
            )
            db.favoriteDao().upsert(fav)
            mirror { sync.pushFavorite(fav) }
        }
    }

    suspend fun reorderFavorites(orderedIds: List<String>) {
        db.favoriteDao().reorder(orderedIds)
        val now = System.currentTimeMillis()
        orderedIds.forEachIndexed { index, id ->
            val updated = db.favoriteDao().getById(id)?.copy(ordering = index, updatedAt = now)
                ?: return@forEachIndexed
            db.favoriteDao().upsert(updated)
            mirror { sync.pushFavorite(updated) }
        }
    }

    private suspend fun ensureProduct(name: String): ProductEntity {
        val key = name.lowercase(Locale.ROOT)
        db.productDao().findByKey(key)?.let { return it }
        val now = System.currentTimeMillis()
        val product = ProductEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            nameKey = key,
            addedAt = now,
            updatedAt = now
        )
        db.productDao().upsert(product)
        mirror { sync.pushProduct(product) }
        return product
    }

    private fun mirror(block: suspend () -> Unit) {
        scope.launch { runCatching { block() } }
    }

    private companion object {
        // ARGB longs (0xAARRGGBB). Saturated tones that read well on light/dark backgrounds.
        private val USER_PALETTE = listOf(
            0xFF1D9E75, 0xFF1976D2, 0xFFE65100, 0xFF7B1FA2,
            0xFF00838F, 0xFFC2185B, 0xFF558B2F, 0xFF455A64
        )
    }
}
