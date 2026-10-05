package com.nutriai.core.ui.format

import com.nutriai.domain.model.ActivityLevel
import com.nutriai.domain.model.Goal
import com.nutriai.domain.model.MealType
import com.nutriai.domain.model.NutritionSource
import com.nutriai.domain.model.PortionUnit
import com.nutriai.domain.model.Sex
import com.nutriai.domain.model.ThemeMode
import com.nutriai.domain.result.AppError
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Textos en lenguaje humano para valores del dominio. */
object Labels {
    private val es = Locale("es")

    fun mealType(type: MealType) = when (type) {
        MealType.BREAKFAST -> "Desayuno"
        MealType.LUNCH -> "Almuerzo"
        MealType.DINNER -> "Cena"
        MealType.SNACK -> "Snacks"
    }

    fun mealEmoji(type: MealType) = when (type) {
        MealType.BREAKFAST -> "🌅"
        MealType.LUNCH -> "🍽️"
        MealType.DINNER -> "🌙"
        MealType.SNACK -> "🍎"
    }

    fun sex(sex: Sex) = when (sex) {
        Sex.MALE -> "Hombre"
        Sex.FEMALE -> "Mujer"
    }

    fun goal(goal: Goal) = when (goal) {
        Goal.LOSE_WEIGHT -> "Perder peso"
        Goal.MAINTAIN_WEIGHT -> "Mantener peso"
        Goal.GAIN_WEIGHT -> "Ganar peso"
        Goal.GAIN_MUSCLE -> "Ganar masa muscular"
    }

    fun activity(level: ActivityLevel) = when (level) {
        ActivityLevel.SEDENTARY -> "Poco o nada de ejercicio"
        ActivityLevel.LIGHT -> "Ejercicio ligero (1–3 días/semana)"
        ActivityLevel.MODERATE -> "Ejercicio moderado (3–5 días/semana)"
        ActivityLevel.ACTIVE -> "Mucho ejercicio (6–7 días/semana)"
        ActivityLevel.VERY_ACTIVE -> "Trabajo físico o entrenamiento intenso"
    }

    fun activityShort(level: ActivityLevel) = when (level) {
        ActivityLevel.SEDENTARY -> "Sedentario"
        ActivityLevel.LIGHT -> "Ligero"
        ActivityLevel.MODERATE -> "Moderado"
        ActivityLevel.ACTIVE -> "Activo"
        ActivityLevel.VERY_ACTIVE -> "Muy activo"
    }

    fun themeMode(mode: ThemeMode) = when (mode) {
        ThemeMode.SYSTEM -> "Automático"
        ThemeMode.LIGHT -> "Claro"
        ThemeMode.DARK -> "Oscuro"
    }

    fun unit(unit: PortionUnit, quantity: Double = 1.0): String {
        val plural = quantity != 1.0
        return when (unit) {
            PortionUnit.GRAM -> "g"
            PortionUnit.MILLILITER -> "ml"
            PortionUnit.UNIT -> if (plural) "unidades" else "unidad"
            PortionUnit.CUP -> if (plural) "tazas" else "taza"
            PortionUnit.TABLESPOON -> if (plural) "cucharadas" else "cucharada"
            PortionUnit.TEASPOON -> if (plural) "cucharaditas" else "cucharadita"
            PortionUnit.SERVING -> if (plural) "porciones" else "porción"
            PortionUnit.SLICE -> if (plural) "rebanadas" else "rebanada"
            PortionUnit.PIECE -> if (plural) "piezas" else "pieza"
        }
    }

    fun unitName(unit: PortionUnit) = when (unit) {
        PortionUnit.GRAM -> "Gramos"
        PortionUnit.MILLILITER -> "Mililitros"
        else -> unit(unit).replaceFirstChar { it.uppercase() }
    }

    fun source(source: NutritionSource) = when (source) {
        NutritionSource.USDA_FDC -> "USDA FoodData Central"
        NutritionSource.OPEN_FOOD_FACTS -> "Open Food Facts"
        NutritionSource.USER_ENTERED -> "Introducido por ti"
        NutritionSource.RECIPE -> "Receta aproximada"
    }

    fun error(error: AppError): String = when (error) {
        AppError.NoInternet -> "Necesitas conexión a Internet para analizar fotos. Mientras tanto puedes registrar la comida manualmente."
        AppError.ServiceUnavailable -> "El servicio de análisis no está disponible ahora mismo. Inténtalo más tarde o registra la comida manualmente."
        AppError.InvalidAiResponse -> "No pudimos interpretar el análisis. Intenta con otra foto o registra la comida manualmente."
        AppError.UnreadableImage -> "No pudimos analizar la imagen. Intenta tomar otra fotografía con mejor iluminación."
        is AppError.UnknownFood -> "No encontramos «${error.name}» en la base de datos. Elige un alimento parecido o créalo."
        is AppError.PermissionDenied -> "Para tomar fotos necesitamos permiso para usar la cámara. Puedes darlo en los ajustes del teléfono o elegir una foto de la galería."
        AppError.Storage -> "No pudimos guardar los datos. Inténtalo de nuevo."
        AppError.Unexpected -> "Algo salió mal. Inténtalo de nuevo."
    }

    fun kcal(value: Double) = "${value.roundToInt().formatThousands()} kcal"

    fun approxKcal(value: Double) = "≈ ${kcal(value)}"

    fun grams(value: Double) = "${formatNumber(value)} g"

    fun percent(value: Double) = "${(value * 100).roundToInt()}%"

    fun quantity(quantity: Double, unit: PortionUnit): String = when (unit) {
        PortionUnit.GRAM, PortionUnit.MILLILITER -> "${formatNumber(quantity)} ${unit(unit)}"
        else -> "${fraction(quantity)} ${unit(unit, quantity)}"
    }

    /** 0.5 → ½, 1.5 → 1½; el resto con un decimal como máximo. */
    fun fraction(value: Double): String {
        val whole = value.toInt()
        val rest = value - whole
        val symbol = when {
            abs(rest - 0.25) < 0.01 -> "¼"
            abs(rest - 0.5) < 0.01 -> "½"
            abs(rest - 0.75) < 0.01 -> "¾"
            rest < 0.01 -> ""
            else -> return formatNumber(value)
        }
        return if (whole == 0 && symbol.isNotEmpty()) symbol else "$whole$symbol"
    }

    fun formatNumber(value: Double): String =
        if (abs(value - value.roundToInt()) < 0.05) value.roundToInt().formatThousands()
        else String.format(es, "%.1f", value)

    fun weight(kg: Double) = String.format(es, "%.1f kg", kg)

    private fun Int.formatThousands(): String = String.format(es, "%,d", this)

    private val dayFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", es)
    private val shortFormatter = DateTimeFormatter.ofPattern("d MMM", es)

    fun day(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> date.format(dayFormatter).replaceFirstChar { it.uppercase() }
    }

    fun shortDate(date: LocalDate): String = date.format(shortFormatter)

    fun greeting(time: LocalTime = LocalTime.now()) = when (time.hour) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    fun defaultMealType(time: LocalTime = LocalTime.now()): MealType = when (time.hour) {
        in 5..10 -> MealType.BREAKFAST
        in 11..15 -> MealType.LUNCH
        in 18..22 -> MealType.DINNER
        else -> MealType.SNACK
    }
}
