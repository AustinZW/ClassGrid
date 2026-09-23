package net.yeeren.classgrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import net.yeeren.classgrid.ui.ClassTableApp
import net.yeeren.classgrid.ui.theme.ClassTableTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClassTableTheme {
                val viewModel: MainViewModel = viewModel()
                ClassTableApp(viewModel)
            }
        }
    }
}
