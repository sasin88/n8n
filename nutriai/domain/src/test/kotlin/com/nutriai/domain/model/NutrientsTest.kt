package com.nutriai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NutrientsTest {

    private val a = Nutrients(energyKcal = 100.0, proteinG = 10.0, carbohydratesG = 20.0, fatG = 5.0, fiberG = 2.0)
    private val b = Nutrients(energyKcal = 50.0, proteinG = 1.0, carbohydratesG = 4.0, fatG = 3.0, fiberG = 1.0)

    @Test
    fun `plus sums every nutrient`() {
        assertEquals(Nutrients(150.0, 11.0, 24.0, 8.0, 3.0), a + b)
    }

    @Test
    fun `plus makes fiber unknown when one side has no fiber data`() {
        assertNull((a + b.copy(fiberG = null)).fiberG)
    }

    @Test
    fun `scaledBy multiplies every nutrient and keeps unknown fiber unknown`() {
        assertEquals(Nutrients(150.0, 15.0, 30.0, 7.5, 3.0), a.scaledBy(1.5))
        assertNull(a.copy(fiberG = null).scaledBy(2.0).fiberG)
    }

    @Test
    fun `sum of empty list is zero`() {
        assertEquals(Nutrients.ZERO, Nutrients.sum(emptyList()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative values are rejected`() {
        Nutrients(-1.0, 0.0, 0.0, 0.0, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative scale factor is rejected`() {
        a.scaledBy(-1.0)
    }
}
