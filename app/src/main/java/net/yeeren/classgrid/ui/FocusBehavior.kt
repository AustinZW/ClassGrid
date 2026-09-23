package net.yeeren.classgrid.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput

internal fun isFocusClearingTap(
    maxDistance: Float,
    touchSlop: Float,
    completed: Boolean,
    pointerCount: Int
): Boolean = completed && pointerCount == 1 && maxDistance <= touchSlop

/** Observes taps without consuming child clicks, scrolling, paging, or drag gestures. */
internal fun Modifier.clearFocusOnTap(focusManager: FocusManager): Modifier = pointerInput(focusManager) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val pointerId = down.id
        val origin = down.position
        var maxDistance = 0f
        var maxPointers = 1
        var completed = false

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            maxPointers = maxOf(maxPointers, event.changes.count { it.pressed })
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            maxDistance = maxOf(maxDistance, (change.position - origin).getDistance())
            if (change.changedToUpIgnoreConsumed()) {
                completed = true
                break
            }
            if (!change.pressed) break
        }

        if (isFocusClearingTap(maxDistance, viewConfiguration.touchSlop, completed, maxPointers)) {
            focusManager.clearFocus()
        }
    }
}
