package net.yeeren.classgrid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class FocusBehaviorInstrumentedTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun blankTapClearsFocusAndAnotherInputReceivesFocus() {
        rule.setContent { FocusFixture() }

        rule.onNodeWithTag("first").performClick().assertIsFocused()
        rule.onNodeWithTag("blank").performClick()
        rule.onNodeWithTag("first").assertIsNotFocused()

        rule.onNodeWithTag("first").performClick().assertIsFocused()
        rule.onNodeWithTag("second").performClick().assertIsFocused()
        rule.onNodeWithTag("first").assertIsNotFocused()
    }

    @Test
    fun dragGestureDoesNotClearFocus() {
        rule.setContent { FocusFixture() }

        rule.onNodeWithTag("first").performClick().assertIsFocused()
        rule.onNodeWithTag("drag").performTouchInput { swipeRight(durationMillis = 400) }
        rule.onNodeWithTag("first").assertIsFocused()
    }
}

@androidx.compose.runtime.Composable
private fun FocusFixture() {
    val focusManager = LocalFocusManager.current
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().clearFocusOnTap(focusManager)) {
        OutlinedTextField(first, { first = it }, modifier = Modifier.testTag("first"))
        OutlinedTextField(second, { second = it }, modifier = Modifier.testTag("second"))
        Box(
            Modifier
                .testTag("blank")
                .width(120.dp)
                .height(80.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { }
        )
        Box(
            Modifier
                .testTag("drag")
                .width(240.dp)
                .height(80.dp)
                .pointerInput(Unit) { detectDragGestures { _, _ -> } }
        )
    }
}
