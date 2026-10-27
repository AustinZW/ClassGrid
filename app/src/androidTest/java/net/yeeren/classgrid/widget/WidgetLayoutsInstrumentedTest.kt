package net.yeeren.classgrid.widget

import android.appwidget.AppWidgetManager
import android.view.LayoutInflater
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.yeeren.classgrid.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetLayoutsInstrumentedTest {
    @Test
    fun widgetPickerOffersThreeDistinctSizes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val providers = AppWidgetManager.getInstance(context).installedProviders
            .filter { it.provider.packageName == context.packageName }
        assertEquals(
            setOf(
                NextClassWidgetProvider::class.java.name,
                NextClassWideWidgetProvider::class.java.name,
                NextClassTallWidgetProvider::class.java.name
            ),
            providers.map { it.provider.className }.toSet()
        )
        assertEquals(
            setOf(R.layout.widget_next_compact, R.layout.widget_next_wide, R.layout.widget_next_tall),
            providers.map { it.initialLayout }.toSet()
        )
        val byClass = providers.associateBy { it.provider.className }
        assertEquals(1, byClass[NextClassWidgetProvider::class.java.name]?.targetCellHeight)
        assertEquals(2, byClass[NextClassWideWidgetProvider::class.java.name]?.targetCellHeight)
        assertEquals(4, byClass[NextClassTallWidgetProvider::class.java.name]?.targetCellHeight)
    }

    @Test
    fun compactWideAndTallSizesInflateExpectedFields() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val inflater = LayoutInflater.from(context)
        val sizes = listOf(
            110 to 48 to R.layout.widget_next_compact,
            260 to 110 to R.layout.widget_next_wide,
            260 to 220 to R.layout.widget_next_tall
        )
        sizes.forEach { (size, expectedLayout) ->
            assertEquals(expectedLayout, WidgetUpdater.layoutForDimensions(size.first, size.second))
            assertEquals(
                expectedLayout,
                WidgetUpdater.layoutForWidget(expectedLayout, size.first, size.second)
            )
            val view = inflater.inflate(expectedLayout, null)
            if (expectedLayout == R.layout.widget_next_tall) {
                assertNotNull(view.findViewById<android.widget.ListView>(R.id.widget_day_list))
                assertNotNull(view.findViewById<android.view.View>(R.id.widget_day_header))
                assertNull(view.findViewById<android.view.View>(R.id.widget_title))
            } else {
                assertNotNull(view.findViewById<android.view.View>(R.id.widget_title))
                assertNotNull(view.findViewById<android.view.View>(R.id.widget_time))
                assertNotNull(view.findViewById<android.view.View>(R.id.widget_accent))
                if (expectedLayout == R.layout.widget_next_wide) {
                    assertNotNull(view.findViewById<android.view.View>(R.id.widget_secondary))
                }
            }
        }
        val row = inflater.inflate(R.layout.widget_day_item, null)
        assertNotNull(row.findViewById<android.view.View>(R.id.widget_day_title))
        assertNotNull(row.findViewById<android.view.View>(R.id.widget_day_meta))
        assertNotNull(row.findViewById<android.view.View>(R.id.widget_day_teacher))
        assertEquals(
            R.layout.widget_next_wide,
            WidgetUpdater.layoutForWidget(R.layout.widget_next_wide, 110, 48)
        )
        assertEquals(
            R.layout.widget_next_tall,
            WidgetUpdater.layoutForWidget(R.layout.widget_next_tall, 110, 48)
        )
    }
}
