package com.nutriai.domain.nutrition

import com.nutriai.domain.model.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class EnergyFormulaTest {

    @Test
    fun `mifflin st jeor male`() {
        val bmr = MifflinStJeorFormula.basalMetabolicRate(BodyMetrics(Sex.MALE, 35, 80.0, 175.0))
        assertEquals(1723.75, bmr, 0.001)
    }

    @Test
    fun `mifflin st jeor female`() {
        val bmr = MifflinStJeorFormula.basalMetabolicRate(BodyMetrics(Sex.FEMALE, 30, 65.0, 165.0))
        assertEquals(1370.25, bmr, 0.001)
    }

    @Test
    fun `revised harris benedict male`() {
        val bmr = RevisedHarrisBenedictFormula.basalMetabolicRate(BodyMetrics(Sex.MALE, 35, 80.0, 175.0))
        assertEquals(1801.252, bmr, 0.001)
    }

    @Test
    fun `unknown formula id falls back to default`() {
        assertSame(MifflinStJeorFormula, EnergyFormulas.byId("nope"))
        assertSame(RevisedHarrisBenedictFormula, EnergyFormulas.byId("harris_benedict_revised"))
    }
}
