package com.nutriai.domain.model

/**
 * Valores nutricionales de una cantidad concreta de alimento.
 *
 * [fiberG] es null cuando la fuente no aporta el dato: nunca se rellena con un valor inventado.
 */
data class Nutrients(
    val energyKcal: Double,
    val proteinG: Double,
    val carbohydratesG: Double,
    val fatG: Double,
    val fiberG: Double?,
) {
    init {
        require(energyKcal >= 0 && proteinG >= 0 && carbohydratesG >= 0 && fatG >= 0) {
            "Los valores nutricionales no pueden ser negativos"
        }
        require(fiberG == null || fiberG >= 0) { "La fibra no puede ser negativa" }
    }

    /** Suma dos cantidades. Si a alguna le falta la fibra, el total de fibra pasa a ser desconocido. */
    operator fun plus(other: Nutrients) = Nutrients(
        energyKcal = energyKcal + other.energyKcal,
        proteinG = proteinG + other.proteinG,
        carbohydratesG = carbohydratesG + other.carbohydratesG,
        fatG = fatG + other.fatG,
        fiberG = if (fiberG != null && other.fiberG != null) fiberG + other.fiberG else null,
    )

    fun scaledBy(factor: Double): Nutrients {
        require(factor >= 0) { "El factor de escala no puede ser negativo" }
        return Nutrients(
            energyKcal = energyKcal * factor,
            proteinG = proteinG * factor,
            carbohydratesG = carbohydratesG * factor,
            fatG = fatG * factor,
            fiberG = fiberG?.times(factor),
        )
    }

    companion object {
        val ZERO = Nutrients(0.0, 0.0, 0.0, 0.0, 0.0)

        fun sum(items: Iterable<Nutrients>): Nutrients = items.fold(ZERO, Nutrients::plus)
    }
}
