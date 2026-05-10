package it.agoldoni.spesa.sync

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import it.agoldoni.spesa.data.AppDatabase
import it.agoldoni.spesa.data.entity.FavoriteEntity
import it.agoldoni.spesa.data.entity.ListItemEntity
import it.agoldoni.spesa.data.entity.MemberEntity
import it.agoldoni.spesa.data.entity.ProductEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Realtime Database sync. Active only when google-services.json is provided
 * at build time and BuildConfig.FIREBASE_ENABLED is true.
 *
 * Schema (single shared "household" — multi-household support is a future step):
 *   /spesa/{household}/members/{memberId}
 *   /spesa/{household}/products/{productId}
 *   /spesa/{household}/list_items/{itemId}
 *   /spesa/{household}/favorites/{favoriteId}
 *   /spesa/{household}/favorite_order/{favoriteId} -> ordering int
 */
@Singleton
class FirebaseSyncSource @Inject constructor(
    private val db: AppDatabase
) : SyncSource {

    private val firebase by lazy { FirebaseDatabase.getInstance() }
    private val root: DatabaseReference by lazy { firebase.getReference("spesa/$HOUSEHOLD") }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val listeners = mutableListOf<Pair<DatabaseReference, ChildEventListener>>()

    override fun start() {
        attach("members") { snap, removed ->
            scope.launch {
                if (removed) return@launch
                val m = snap.toMember() ?: return@launch
                db.memberDao().upsert(m)
            }
        }
        attach("products") { snap, removed ->
            scope.launch {
                if (removed) return@launch
                val p = snap.toProduct() ?: return@launch
                db.productDao().upsert(p)
            }
        }
        attach("list_items") { snap, removed ->
            scope.launch {
                if (removed) {
                    snap.key?.let { db.listItemDao().deleteById(it) }
                    return@launch
                }
                val item = snap.toListItem() ?: return@launch
                db.listItemDao().upsert(item)
            }
        }
        attach("favorites") { snap, removed ->
            scope.launch {
                if (removed) {
                    snap.key?.let { db.favoriteDao().deleteById(it) }
                    return@launch
                }
                val f = snap.toFavorite() ?: return@launch
                db.favoriteDao().upsert(f)
            }
        }
    }

    override fun stop() {
        listeners.forEach { (ref, l) -> ref.removeEventListener(l) }
        listeners.clear()
    }

    override suspend fun pushMember(member: MemberEntity) {
        root.child("members").child(member.id).setValue(
            mapOf("name" to member.name, "colorArgb" to member.colorArgb)
        )
    }

    override suspend fun pushProduct(product: ProductEntity) {
        root.child("products").child(product.id).setValue(
            mapOf("name" to product.name, "nameKey" to product.nameKey, "addedAt" to product.addedAt)
        )
    }

    override suspend fun pushListItem(item: ListItemEntity) {
        root.child("list_items").child(item.id).setValue(
            mapOf(
                "productId" to item.productId,
                "quantity" to item.quantity,
                "memberId" to item.memberId,
                "addedAt" to item.addedAt
            )
        )
    }

    override suspend fun deleteListItem(id: String) {
        root.child("list_items").child(id).removeValue()
    }

    override suspend fun clearListItems() {
        root.child("list_items").removeValue()
    }

    override suspend fun pushFavorite(favorite: FavoriteEntity) {
        root.child("favorites").child(favorite.id).setValue(
            mapOf("productId" to favorite.productId, "ordering" to favorite.ordering)
        )
    }

    override suspend fun deleteFavorite(id: String) {
        root.child("favorites").child(id).removeValue()
    }

    override suspend fun pushFavoriteOrder(orderedIds: List<String>) {
        val updates = orderedIds.mapIndexed { i, id -> "favorites/$id/ordering" to i }.toMap()
        root.updateChildren(updates)
    }

    private fun attach(path: String, onChange: (DataSnapshot, Boolean) -> Unit) {
        val ref = root.child(path)
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) =
                onChange(snapshot, false)

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) =
                onChange(snapshot, false)

            override fun onChildRemoved(snapshot: DataSnapshot) = onChange(snapshot, true)
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onCancelled(error: DatabaseError) = Unit
        }
        ref.addChildEventListener(listener)
        listeners += ref to listener
    }

    private fun DataSnapshot.toMember(): MemberEntity? {
        val id = key ?: return null
        val name = child("name").getValue(String::class.java) ?: return null
        val color = child("colorArgb").getValue(Long::class.java) ?: return null
        return MemberEntity(id, name, color)
    }

    private fun DataSnapshot.toProduct(): ProductEntity? {
        val id = key ?: return null
        val name = child("name").getValue(String::class.java) ?: return null
        val nameKey = child("nameKey").getValue(String::class.java) ?: name.lowercase()
        val addedAt = child("addedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
        return ProductEntity(id, name, nameKey, addedAt)
    }

    private fun DataSnapshot.toListItem(): ListItemEntity? {
        val id = key ?: return null
        val productId = child("productId").getValue(String::class.java) ?: return null
        val quantity = child("quantity").getValue(Int::class.java) ?: 1
        val memberId = child("memberId").getValue(String::class.java)
        val addedAt = child("addedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
        return ListItemEntity(id, productId, quantity, memberId, addedAt)
    }

    private fun DataSnapshot.toFavorite(): FavoriteEntity? {
        val id = key ?: return null
        val productId = child("productId").getValue(String::class.java) ?: return null
        val ordering = child("ordering").getValue(Int::class.java) ?: 0
        return FavoriteEntity(id, productId, ordering)
    }

    companion object {
        private const val HOUSEHOLD = "default"
    }
}
