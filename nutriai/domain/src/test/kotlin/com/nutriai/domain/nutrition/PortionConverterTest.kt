package com.nutriai.domain.nutrition

import com.nutriai.domain.TestData
import com.nutriai.domain.model.NutritionSource
import com.nutriai.domain.model.PortionUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortionConverterTest {

    @Test
    fun `grams are used directly`() {
        assertEquals(150.0, PortionConverter.toBaseAmount(150.0, PortionUnit.GRAM, TestData.rice)!!, 0.0001)
    }

    @Test
    fun `known household portion is used`() {
        assertEquals(237.0, PortionConverter.toBaseAmount(1.5, PortionUnit.CUP, TestData.rice)!!, 0.0001)
    }

    @Test
    fun `tablespoon is derived from cup equivalence`() {
        val grams = PortionConverter.toBaseAmount(1.0, PortionUnit.TABLESPOON, TestData.rice)!!
        assertEquals(158.0 / 16, grams, 0.01)
    }

    @Test
    fun `unknown conversion returns null instead of guessing`() {
        assertNull(PortionConverter.toBaseAmount(1.0, PortionUnit.SLICE, TestData.rice))
        assertNull(PortionConverter.toBaseAmount(1.0, PortionUnit.CUP, TestData.chicken))
    }

    @Test
    fun `non positive quantity is rejected`() {
        assertNull(PortionConverter.toBaseAmount(0.0, PortionUnit.GRAM, TestData.rice))
        assertNull(PortionConverter.toBaseAmount(-2.0, PortionUnit.GRAM, TestData.rice))
    }

    @Test
    fun `build item scales nutrients from per 100 g`() {
        val item = PortionConverter.buildItem(TestData.rice, 150.0, PortionUnit.GRAM)
        assertNotNull(item)
        assertEquals(150.0, item!!.nutrients.energyKcal, 0.0001)
        assertEquals(15.0, item.nutrients.proteinG, 0.0001)
        assertEquals(NutritionSource.USDA_FDC, item.source)
    }

    @Test
    fun `supported units only lists convertible units`() {
        val units = PortionConverter.supportedUnits(TestData.chicken)
        assertEquals(listOf(PortionUnit.GRAM), units)
        assertTrue(PortionUnit.TEASPOON in PortionConverter.supportedUnits(TestData.rice))
    }
}
