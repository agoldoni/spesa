package it.agoldoni.spesa

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import it.agoldoni.spesa.data.repository.SpesaRepository
import it.agoldoni.spesa.data.entity.MemberEntity
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            repository.ensureSeedMembers(DEFAULT_MEMBERS)
        }
        syncSource.start()
    }

    companion object {
        // ARGB as Long (0xAARRGGBB). Pre-seeded household members.
        private val DEFAULT_MEMBERS = listOf(
            MemberEntity(id = "m", name = "M", colorArgb = 0xFF1D9E75),
            MemberEntity(id = "l", name = "L", colorArgb = 0xFF1976D2)
        )
    }
}
