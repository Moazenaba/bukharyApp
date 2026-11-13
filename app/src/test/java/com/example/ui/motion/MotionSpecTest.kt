package com.example.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionSpecTest {
    @Test
    fun `leaving is faster than arriving`() {
        assertTrue(MotionSpec.SHORT < MotionSpec.MEDIUM && MotionSpec.MEDIUM < MotionSpec.LONG)
    }

    @Test
    fun `a list shows in one after another but never for long`() {
        assertEquals(0, MotionSpec.staggerDelay(0))
        assertEquals(30, MotionSpec.staggerDelay(1))
        assertEquals(150, MotionSpec.staggerDelay(5))
        assertEquals(150, MotionSpec.staggerDelay(60))
        assertEquals(0, MotionSpec.staggerDelay(-3))
    }

    @Test
    fun `remove animations on the device means no animation`() {
        assertTrue(MotionSpec.isReduced(0f))
        assertFalse(MotionSpec.isReduced(1f))
        assertFalse(MotionSpec.isReduced(0.5f))
        assertEquals(0, MotionSpec.duration(250, reduced = true))
        assertEquals(250, MotionSpec.duration(250, reduced = false))
    }

    @Test
    fun `pressing shrinks a little, not a lot`() {
        assertTrue(MotionSpec.PRESSED_SCALE in 0.9f..0.99f)
    }
}
