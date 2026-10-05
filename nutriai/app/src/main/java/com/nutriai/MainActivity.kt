package com.nutriai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nutriai.core.ui.theme.NutriAiTheme
import com.nutriai.navigation.NutriAiNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // El modo de tema elegido por el usuario (DataStore) se conectará en la Fase 2.
            NutriAiTheme {
                NutriAiNavHost()
            }
        }
    }
}
