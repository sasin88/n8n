# NutriAI (Android)

Asistente local para estimar y registrar calorías y macronutrientes. Las cifras que muestra la app
son **estimaciones**; no sustituye a un profesional de la salud.

## Estado

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Arquitectura y proyecto Android | ✅ |
| 2–13 | UI, perfiles, Room, cálculos, registro, cámara, IA, historial, privacidad, tests, optimización, APK | Pendiente |

## Arquitectura

```
UI (Compose) → ViewModel → Domain (casos de uso + interfaces) → Repository → Data (Room / DataStore / remoto)
```

- `domain/`: módulo Kotlin puro (sin Android). Modelos, interfaces (`FoodAnalysisService`,
  `NutritionDataSource`, `EnergyFormula`, repositorios) y reglas de negocio. Se prueba en la JVM.
- `app/`: Android, Jetpack Compose, Hilt, Navigation Compose (rutas tipadas).

Flujo de análisis: reconocimiento IA → identificación → base nutricional → motor de cálculo →
confirmación del usuario → registro. La IA **solo** identifica alimentos y cantidades; los
nutrientes salen de la base nutricional con su fuente registrada.

### Decisiones

| Tema | Decisión |
|---|---|
| Datos nutricionales | USDA FoodData Central (subconjunto incluido para uso offline) |
| IA | Interfaz `FoodAnalysisService`; primero un MOCK claramente identificado, luego un backend propio |
| API keys | Nunca en la app: App → backend propio → proveedor de IA |
| Android mínimo | 8.0 (API 26) |
| Copias de seguridad | Desactivadas: los datos no salen del dispositivo |

## Compilar

Requiere JDK 17 y Android SDK (compileSdk 35).

```
./gradlew :domain:test :app:testDebugUnitTest :app:assembleDebug
```

El workflow `.github/workflows/nutriai-android.yml` ejecuta pruebas, lint y genera el APK de debug
como artefacto descargable.
