package it.agoldoni.spesa.sync

import android.util.Log
import com.google.gson.Gson
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import it.agoldoni.spesa.data.AppDatabase
import it.agoldoni.spesa.data.entity.DepartmentEntity
import it.agoldoni.spesa.data.entity.FavoriteEntity
import it.agoldoni.spesa.data.entity.ListItemEntity
import it.agoldoni.spesa.data.entity.MemberEntity
import it.agoldoni.spesa.data.entity.ProductEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttSyncSource @Inject constructor(
    private val db: AppDatabase,
    private val config: MqttConfig
) : SyncSource {

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var client: Mqtt3AsyncClient? = null
    @Volatile private var connected = false

    override fun start() = reconnectIfNeeded()

    override fun stop() {
        scope.launch { disconnectInternal() }
    }

    override fun reconnectIfNeeded() {
        scope.launch {
            if (config.isConfigured && !connected) {
                connectInternal()
            } else if (!config.isConfigured && connected) {
                disconnectInternal()
            }
        }
    }

    override fun isConnected(): Boolean = connected

    private fun connectInternal() {
        if (connected && client != null) return

        try {
            val builder = MqttClient.builder()
                .useMqttVersion3()
                .identifier("spesa-${UUID.randomUUID().toString().substring(0, 8)}")
                .serverHost(config.brokerUrl)
                .serverPort(config.port)
            if (config.useTls) builder.sslWithDefaultConfig()

            val c = builder.buildAsync()

            val username = config.username
            val password = config.password
            if (username.isNotEmpty()) {
                c.connectWith()
                    .cleanSession(false)
                    .simpleAuth()
                    .username(username)
                    .password(password.toByteArray(StandardCharsets.UTF_8))
                    .applySimpleAuth()
                    .send()
                    .join()
            } else {
                c.connectWith()
                    .cleanSession(false)
                    .send()
                    .join()
            }

            client = c
            connected = true
            Log.i(TAG, "MQTT connected")

            subscribeToTopics()
            scope.launch { publishAll() }
        } catch (e: Exception) {
            Log.e(TAG, "MQTT connection failed", e)
            connected = false
            client = null
        }
    }

    private fun disconnectInternal() {
        client?.let {
            try {
                it.disconnect().join()
            } catch (e: Exception) {
                Log.e(TAG, "MQTT disconnect error", e)
            }
        }
        connected = false
        client = null
    }

    private fun subscribeToTopics() {
        val c = client ?: return
        val gid = config.groupId
        KINDS.forEach { kind ->
            c.subscribeWith()
                .topicFilter("sync/$gid/$kind/#")
                .callback { handleIncoming(it, kind) }
                .send()
                .whenComplete { _, throwable ->
                    if (throwable != null) Log.e(TAG, "Subscribe $kind failed", throwable)
                    else Log.i(TAG, "Subscribed to $kind")
                }
        }
    }

    private fun handleIncoming(publish: Mqtt3Publish, kind: String) {
        scope.launch {
            try {
                val topic = publish.topic.toString()
                val id = topic.substringAfterLast('/')
                val payload = publish.payloadAsBytes
                if (payload.isEmpty()) {
                    when (kind) {
                        KIND_LIST_ITEMS -> db.listItemDao().deleteById(id)
                        KIND_FAVORITES -> db.favoriteDao().deleteById(id)
                        KIND_DEPARTMENTS -> db.departmentDao().deleteById(id)
                    }
                    return@launch
                }
                val json = String(payload, StandardCharsets.UTF_8)
                when (kind) {
                    KIND_MEMBERS -> handleMember(json)
                    KIND_PRODUCTS -> handleProduct(json)
                    KIND_LIST_ITEMS -> handleListItem(json)
                    KIND_FAVORITES -> handleFavorite(json)
                    KIND_DEPARTMENTS -> handleDepartment(json)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling incoming message", e)
            }
        }
    }

    private suspend fun handleMember(json: String) {
        val remote = gson.fromJson(json, MemberEntity::class.java)
        val local = db.memberDao().getById(remote.id)
        if (local == null || remote.updatedAt > local.updatedAt) db.memberDao().upsert(remote)
    }

    private suspend fun handleProduct(json: String) {
        val remote = gson.fromJson(json, ProductEntity::class.java)
        val local = db.productDao().getById(remote.id)
        if (local == null || remote.updatedAt > local.updatedAt) db.productDao().upsert(remote)
    }

    private suspend fun handleListItem(json: String) {
        val remote = gson.fromJson(json, ListItemEntity::class.java)
        if (db.productDao().getById(remote.productId) == null) {
            Log.w(TAG, "Skipping list_item ${remote.id}: product missing")
            return
        }
        val local = db.listItemDao().getById(remote.id)
        if (local == null || remote.updatedAt > local.updatedAt) db.listItemDao().upsert(remote)
    }

    private suspend fun handleFavorite(json: String) {
        val remote = gson.fromJson(json, FavoriteEntity::class.java)
        if (db.productDao().getById(remote.productId) == null) {
            Log.w(TAG, "Skipping favorite ${remote.id}: product missing")
            return
        }
        val local = db.favoriteDao().getById(remote.id)
        if (local == null || remote.updatedAt > local.updatedAt) db.favoriteDao().upsert(remote)
    }

    private suspend fun handleDepartment(json: String) {
        val remote = gson.fromJson(json, DepartmentEntity::class.java)
        val local = db.departmentDao().getById(remote.id)
        if (local == null || remote.updatedAt > local.updatedAt) db.departmentDao().upsert(remote)
    }

    private suspend fun publishAll() {
        if (!connected || client == null) return
        try {
            db.memberDao().getAll().forEach { publishEntity(KIND_MEMBERS, it.id, it) }
            db.productDao().getAll().forEach { publishEntity(KIND_PRODUCTS, it.id, it) }
            db.listItemDao().getAll().forEach { publishEntity(KIND_LIST_ITEMS, it.id, it) }
            db.favoriteDao().getAll().forEach { publishEntity(KIND_FAVORITES, it.id, it) }
            db.departmentDao().getAll().forEach { publishEntity(KIND_DEPARTMENTS, it.id, it) }
            Log.i(TAG, "Full sync published")
        } catch (e: Exception) {
            Log.e(TAG, "Full sync failed", e)
        }
    }

    override suspend fun pushMember(member: MemberEntity) =
        publishEntity(KIND_MEMBERS, member.id, member)

    override suspend fun pushProduct(product: ProductEntity) =
        publishEntity(KIND_PRODUCTS, product.id, product)

    override suspend fun pushListItem(item: ListItemEntity) =
        publishEntity(KIND_LIST_ITEMS, item.id, item)

    override suspend fun pushFavorite(favorite: FavoriteEntity) =
        publishEntity(KIND_FAVORITES, favorite.id, favorite)

    override suspend fun pushDepartment(department: DepartmentEntity) =
        publishEntity(KIND_DEPARTMENTS, department.id, department)

    override suspend fun deleteListItem(id: String) = publishDelete(KIND_LIST_ITEMS, id)
    override suspend fun deleteFavorite(id: String) = publishDelete(KIND_FAVORITES, id)
    override suspend fun deleteDepartment(id: String) = publishDelete(KIND_DEPARTMENTS, id)

    private fun publishEntity(kind: String, id: String, entity: Any) {
        val c = client ?: return
        if (!connected) return
        try {
            val topic = "sync/${config.groupId}/$kind/$id"
            val json = gson.toJson(entity)
            c.publishWith()
                .topic(topic)
                .payload(json.toByteArray(StandardCharsets.UTF_8))
                .retain(true)
                .send()
        } catch (e: Exception) {
            Log.e(TAG, "Publish $kind/$id failed", e)
        }
    }

    private fun publishDelete(kind: String, id: String) {
        val c = client ?: return
        if (!connected) return
        try {
            val topic = "sync/${config.groupId}/$kind/$id"
            c.publishWith()
                .topic(topic)
                .payload(ByteArray(0))
                .retain(true)
                .send()
        } catch (e: Exception) {
            Log.e(TAG, "Publish delete $kind/$id failed", e)
        }
    }

    companion object {
        private const val TAG = "MqttSyncSource"
        private const val KIND_MEMBERS = "members"
        private const val KIND_PRODUCTS = "products"
        private const val KIND_LIST_ITEMS = "list_items"
        private const val KIND_FAVORITES = "favorites"
        private const val KIND_DEPARTMENTS = "departments"
        private val KINDS = listOf(KIND_MEMBERS, KIND_PRODUCTS, KIND_LIST_ITEMS, KIND_FAVORITES, KIND_DEPARTMENTS)
    }
}
