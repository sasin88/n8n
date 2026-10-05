package com.nutriai.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nutriai.core.ui.components.AppearIn
import com.nutriai.core.ui.components.NoticeCard
import com.nutriai.core.ui.components.PrimaryButton

/** Introducción breve del primer uso. No pide registro online. */
@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Spacer(Modifier.height(32.dp))
            AppearIn(0) { Text("🥗", style = MaterialTheme.typography.displaySmall) }
            Spacer(Modifier.height(16.dp))
            AppearIn(1) { Text("Bienvenido a NutriAI", style = MaterialTheme.typography.displaySmall) }
            Spacer(Modifier.height(8.dp))
            AppearIn(2) {
                Text(
                    "Lleva el control de lo que comes de forma simple.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(32.dp))
            AppearIn(3) { Feature("📷", "Fotografía tu comida", "La app estima qué hay en el plato y cuánto aporta.") }
            AppearIn(4) { Feature("✍️", "Revisa y corrige", "Tú confirmas los alimentos y las cantidades.") }
            AppearIn(5) { Feature("🔒", "Tus datos se quedan aquí", "Sin cuentas ni contraseñas. Todo se guarda en este teléfono.") }
        }
        Column {
            NoticeCard(
                "Las calorías son estimaciones orientativas. NutriAI no sustituye a un médico, nutricionista o dietista.",
            )
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Crear tu perfil", onStart)
        }
    }
}

@Composable
private fun Feature(emoji: String, title: String, body: String) {
    Row(Modifier.padding(vertical = 10.dp)) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
