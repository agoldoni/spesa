package it.agoldoni.spesa.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import it.agoldoni.spesa.data.ThemePreferenceStore
import it.agoldoni.spesa.ui.shopping.ShoppingScreen
import it.agoldoni.spesa.ui.theme.SpesaTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeStore: ThemePreferenceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by themeStore.observeIsDark().collectAsState(initial = themeStore.get())
            SpesaTheme(isDark = isDark) {
                ShoppingScreen(
                    isDark = isDark,
                    onToggleTheme = themeStore::toggle
                )
            }
        }
    }
}
