package com.nutriai.domain.activity

import com.nutriai.domain.model.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityTest {
    @Test fun `exercise kcal uses MET formula`() {
        assertEquals(280, ExerciseCalculator.estimateKcal(ExerciseType.WALKING, 60, 80.0))
        assertEquals(392, ExerciseCalculator.estimateKcal(ExerciseType.RUNNING_10KMH, 30, 80.0))
        assertNull(ExerciseCalculator.estimateKcal(ExerciseType.OTHER, 30, 80.0))
        assertNull(ExerciseCalculator.estimateKcal(ExerciseType.YOGA, 0, 80.0))
    }

    @Test fun `fasting session progress and completion`() {
        val s = FastingSession.start(1, FastingProtocol.P16_8, 0)
        assertEquals(16 * 3_600_000L, s.plannedEndEpochMs)
        assertEquals(0.5f, s.progress(8 * 3_600_000L), 0.0001f)
        assertTrue(s.isActive)
        assertFalse(s.copy(endEpochMs = 3_600_000).completed)
        assertTrue(s.copy(endEpochMs = 17 * 3_600_000L).completed)
        assertEquals("16:8", FastingProtocol.P16_8.label)
    }

    @Test fun `unit conversions round trip`() {
        assertEquals(176.37, Units.kgToLb(80.0), 0.01)
        assertEquals(80.0, Units.lbToKg(Units.kgToLb(80.0)), 0.0001)
        assertEquals(5 to 9, Units.cmToFeetInches(175.0))
        assertEquals(175.26, Units.feetInchesToCm(5, 9), 0.01)
    }
}
