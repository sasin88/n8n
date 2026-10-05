package com.nutriai.domain.ai

import com.nutriai.domain.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodMatcherTest {
    private val catalog = listOf(TestData.rice, TestData.chicken, TestData.beans)

    @Test
    fun `alias matches exactly`() {
        assertEquals("rice", FoodMatcher.bestMatch("Arroz", catalog)?.food?.id)
    }

    @Test
    fun `accents and case are ignored`() {
        assertEquals("beans", FoodMatcher.bestMatch("FRIJOLES negros", catalog)?.food?.id)
    }

    @Test
    fun `english names match aliases`() {
        assertEquals("chicken", FoodMatcher.bestMatch("grilled chicken breast", catalog)?.food?.id)
    }

    @Test
    fun `unrelated name has no match`() {
        assertNull(FoodMatcher.bestMatch("pizza", catalog))
    }
}
