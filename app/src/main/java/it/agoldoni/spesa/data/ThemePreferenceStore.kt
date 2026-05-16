package it.agoldoni.spesa.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferenceStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("spesa_prefs", Context.MODE_PRIVATE)

    fun observeIsDark(): Flow<Boolean> = callbackFlow {
        trySend(get())
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY) trySend(get())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun get(): Boolean = prefs.getBoolean(KEY, false)

    fun toggle() {
        prefs.edit().putBoolean(KEY, !get()).apply()
    }

    companion object {
        private const val KEY = "dark_mode"
    }
}
