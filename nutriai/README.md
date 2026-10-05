# NutriAI (Android)

Asistente local para estimar y registrar calorías y macronutrientes. Las cifras que muestra la app
son **estimaciones**; no sustituye a un profesional de la salud.

## Estado

Todas las fases implementadas. **Cómo probarla:** ver [COMO_PROBAR.md](COMO_PROBAR.md).

| Área | Qué hay |
|---|---|
| Perfiles | Perfiles locales sin registro, cambio de perfil, datos separados por perfil |
| Cálculos | BMR (Mifflin-St Jeor / Harris-Benedict revisada), TDEE, objetivo con mínimo de seguridad, macros, objetivo manual |
| Comidas | Registro manual, platos compuestos (recetas de Costa Rica), porciones caseras, alimentos propios |
| IA | Foto (cámara o galería) → identificación → base USDA → revisión del usuario. MOCK visible si no hay backend |
| Historial | Día / semana / mes, detalle del día, estadísticas, peso con tendencia |
| Privacidad | Datos solo en el teléfono, sin copias en la nube, fotos temporales, borrado de historial/perfil/todo |
| Backend | Cloudflare Worker (`backend/`) que guarda la API key en el servidor |

### Pendiente / fuera de alcance de esta versión

- Unidades imperiales (lb, in): la preferencia existe en el modelo, pero la interfaz usa kg/cm.
- Textos solo en español (sin archivos de traducción todavía).
- Pruebas de interfaz en emulador real: hoy se ejecutan con Robolectric.
- APK firmado con clave propia para Google Play (hoy: clave de prueba).

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
