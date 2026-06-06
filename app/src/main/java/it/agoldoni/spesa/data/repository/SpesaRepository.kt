package it.agoldoni.spesa.data.repository

import it.agoldoni.spesa.data.AppDatabase
import it.agoldoni.spesa.data.entity.DepartmentEntity
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
    fun observeDepartments(): Flow<List<DepartmentEntity>> = db.departmentDao().observeAll()

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
        when {
            existing != null && existing.deleted -> {
                // Revive a tombstoned item (the unique productId index forbids a new insert).
                val revived = existing.copy(
                    quantity = 1,
                    memberId = memberId,
                    addedAt = now,
                    updatedAt = now,
                    deleted = false
                )
                db.listItemDao().upsert(revived)
                mirror { sync.pushListItem(revived) }
            }
            existing != null -> {
                val updated = existing.copy(quantity = existing.quantity + 1, updatedAt = now)
                db.listItemDao().upsert(updated)
                mirror { sync.pushListItem(updated) }
            }
            else -> {
                val item = ListItemEntity(
                    // Identity is the productId (one list entry per product), so the
                    // same entry has the same id across devices and merges via LWW.
                    id = productId,
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
        val item = db.listItemDao().getById(itemId) ?: return
        val tombstone = item.copy(deleted = true, updatedAt = System.currentTimeMillis())
        db.listItemDao().upsert(tombstone)
        mirror { sync.pushListItem(tombstone) }
    }

    suspend fun clearAll() {
        val now = System.currentTimeMillis()
        db.listItemDao().getAll().filterNot { it.deleted }.forEach { item ->
            val tombstone = item.copy(deleted = true, updatedAt = now)
            db.listItemDao().upsert(tombstone)
            mirror { sync.pushListItem(tombstone) }
        }
    }

    suspend fun toggleFavorite(productId: String) {
        val now = System.currentTimeMillis()
        val all = db.favoriteDao().getAll()
        val existingLive = all.firstOrNull { it.productId == productId && !it.deleted }
        if (existingLive != null) {
            val tombstone = existingLive.copy(deleted = true, updatedAt = now)
            db.favoriteDao().upsert(tombstone)
            mirror { sync.pushFavorite(tombstone) }
        } else {
            val ordering = all.count { !it.deleted }
            // Reuse a tombstoned row when present (the unique productId index forbids a new insert).
            val existingTomb = all.firstOrNull { it.productId == productId }
            val fav = existingTomb?.copy(ordering = ordering, updatedAt = now, deleted = false)
                ?: FavoriteEntity(
                    // Identity is the productId (one favorite per product) for stable
                    // cross-device id and correct last-write-wins merge.
                    id = productId,
                    productId = productId,
                    ordering = ordering,
                    updatedAt = now
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

    // --- Departments ---

    suspend fun addDepartment(name: String) {
        val position = db.departmentDao().getAll().count { !it.deleted }
        val dept = DepartmentEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            position = position,
            updatedAt = System.currentTimeMillis()
        )
        db.departmentDao().upsert(dept)
        mirror { sync.pushDepartment(dept) }
    }

    suspend fun renameDepartment(id: String, newName: String) {
        val dept = db.departmentDao().getById(id) ?: return
        val updated = dept.copy(name = newName.trim(), updatedAt = System.currentTimeMillis())
        db.departmentDao().upsert(updated)
        mirror { sync.pushDepartment(updated) }
    }

    suspend fun deleteDepartment(id: String) {
        // Clear association on all products before deleting the department
        db.productDao().clearDepartment(id)
        val dept = db.departmentDao().getById(id) ?: return
        val tombstone = dept.copy(deleted = true, updatedAt = System.currentTimeMillis())
        db.departmentDao().upsert(tombstone)
        mirror { sync.pushDepartment(tombstone) }
    }

    suspend fun reorderDepartments(orderedIds: List<String>) {
        val now = System.currentTimeMillis()
        orderedIds.forEachIndexed { index, id ->
            val current = db.departmentDao().getById(id) ?: return@forEachIndexed
            val updated = current.copy(position = index, updatedAt = now)
            db.departmentDao().upsert(updated)
            mirror { sync.pushDepartment(updated) }
        }
    }

    suspend fun setProductDepartment(productId: String, departmentId: String?) {
        val product = db.productDao().getById(productId) ?: return
        val updated = product.copy(departmentId = departmentId, updatedAt = System.currentTimeMillis())
        db.productDao().update(updated)
        mirror { sync.pushProduct(updated) }
    }

    private suspend fun ensureProduct(name: String): ProductEntity {
        val key = name.lowercase(Locale.ROOT)
        db.productDao().findByKey(key)?.let { return it }
        val now = System.currentTimeMillis()
        val product = ProductEntity(
            // Deterministic id from the name: the same product resolves to the same
            // id on every device, so concurrent creation can't produce a nameKey
            // conflict (which previously cascade-deleted the linked list items).
            id = key,
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
