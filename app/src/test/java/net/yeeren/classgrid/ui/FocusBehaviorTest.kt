package net.yeeren.classgrid.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusBehaviorTest {
    @Test
    fun `stationary completed single pointer clears focus`() {
        assertTrue(isFocusClearingTap(2f, 8f, completed = true, pointerCount = 1))
    }

    @Test
    fun `horizontal or vertical drag keeps focus`() {
        assertFalse(isFocusClearingTap(24f, 8f, completed = true, pointerCount = 1))
    }

    @Test
    fun `cancelled and multi touch gestures keep focus`() {
        assertFalse(isFocusClearingTap(0f, 8f, completed = false, pointerCount = 1))
        assertFalse(isFocusClearingTap(0f, 8f, completed = true, pointerCount = 2))
    }
}
