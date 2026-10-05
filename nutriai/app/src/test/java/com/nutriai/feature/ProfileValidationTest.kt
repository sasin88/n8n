package com.nutriai.feature

import com.nutriai.feature.profile.ProfileFormState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidationTest {
    private val valid = ProfileFormState(name = "Adrian", age = "35", weight = "80", height = "175")

    @Test fun `valid form passes`() = assertTrue(valid.validation.isValid)

    @Test fun `comma decimals are accepted`() = assertTrue(valid.copy(weight = "79,5").validation.isValid)

    @Test fun `empty name fails`() = assertNotNull(valid.copy(name = " ").validation.nameError)

    @Test fun `out of range values fail`() {
        assertNotNull(valid.copy(age = "8").validation.ageError)
        assertNotNull(valid.copy(weight = "900").validation.weightError)
        assertNotNull(valid.copy(height = "40").validation.heightError)
    }

    @Test fun `minor is flagged`() {
        assertTrue(valid.copy(age = "16").validation.isMinor)
        assertFalse(valid.validation.isMinor)
    }
}
