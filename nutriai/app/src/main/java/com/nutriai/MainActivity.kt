package com.nutriai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutriai.core.ui.theme.NutriAiTheme
import com.nutriai.feature.root.RootState
import com.nutriai.feature.root.RootViewModel
import com.nutriai.navigation.NutriAiNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val rootViewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // La pantalla de inicio se mantiene mientras se carga el catálogo y el perfil activo.
        splash.setKeepOnScreenCondition { rootViewModel.state.value is RootState.Loading }
        enableEdgeToEdge()
        setContent {
            val state by rootViewModel.state.collectAsStateWithLifecycle()
            val ready = state as? RootState.Ready ?: return@setContent
            NutriAiTheme(themeMode = ready.themeMode) {
                NutriAiNavHost(hasProfile = ready.hasProfile)
            }
        }
    }
}
