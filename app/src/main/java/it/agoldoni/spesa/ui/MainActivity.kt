package it.agoldoni.spesa.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import it.agoldoni.spesa.ui.shopping.ShoppingScreen
import it.agoldoni.spesa.ui.theme.SpesaTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpesaTheme {
                ShoppingScreen()
            }
        }
    }
}
