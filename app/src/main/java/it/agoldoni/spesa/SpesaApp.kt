package it.agoldoni.spesa

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import it.agoldoni.spesa.data.ActiveMemberStore
import it.agoldoni.spesa.data.repository.SpesaRepository
import it.agoldoni.spesa.sync.MqttConfig
import it.agoldoni.spesa.sync.SyncSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SpesaApp : Application() {

    @Inject lateinit var repository: SpesaRepository
    @Inject lateinit var syncSource: SyncSource
    @Inject lateinit var mqttConfig: MqttConfig
    @Inject lateinit var activeMember: ActiveMemberStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            repository.ensureCurrentUserMember(mqttConfig.username, mqttConfig.alias)?.let {
                activeMember.set(it.id)
            }
        }
        syncSource.start()
    }
}
