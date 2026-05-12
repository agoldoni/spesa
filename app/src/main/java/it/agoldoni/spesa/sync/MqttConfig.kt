package it.agoldoni.spesa.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MqttConfig @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isEnabled: Boolean get() = prefs.getBoolean(KEY_ENABLED, false)
    val brokerUrl: String get() = prefs.getString(KEY_BROKER_URL, "") ?: ""
    val port: Int get() = prefs.getInt(KEY_PORT, 8883)
    val username: String get() = prefs.getString(KEY_USERNAME, "") ?: ""
    val password: String get() = prefs.getString(KEY_PASSWORD, "") ?: ""
    val groupId: String get() = prefs.getString(KEY_GROUP_ID, "") ?: ""
    val useTls: Boolean get() = prefs.getBoolean(KEY_USE_TLS, true)
    val alias: String get() = prefs.getString(KEY_ALIAS, "") ?: ""

    val isValid: Boolean get() = brokerUrl.isNotEmpty() && groupId.isNotEmpty()
    val isConfigured: Boolean get() = isEnabled && isValid

    fun save(
        enabled: Boolean,
        brokerUrl: String,
        port: Int,
        username: String,
        password: String,
        groupId: String,
        useTls: Boolean,
        alias: String
    ) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_BROKER_URL, brokerUrl)
            .putInt(KEY_PORT, port)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .putString(KEY_GROUP_ID, groupId)
            .putBoolean(KEY_USE_TLS, useTls)
            .putString(KEY_ALIAS, alias)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "mqtt_config"
        private const val KEY_ENABLED = "mqtt_enabled"
        private const val KEY_BROKER_URL = "mqtt_broker_url"
        private const val KEY_PORT = "mqtt_port"
        private const val KEY_USERNAME = "mqtt_username"
        private const val KEY_PASSWORD = "mqtt_password"
        private const val KEY_GROUP_ID = "mqtt_group_id"
        private const val KEY_USE_TLS = "mqtt_use_tls"
        private const val KEY_ALIAS = "mqtt_alias"
    }
}
