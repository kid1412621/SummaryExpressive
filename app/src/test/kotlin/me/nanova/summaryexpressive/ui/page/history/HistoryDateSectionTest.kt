package me.nanova.summaryexpressive.ui.page.history

import android.text.format.DateUtils
import androidx.compose.ui.unit.dp
import me.nanova.summaryexpressive.ui.page.history.section.GroupPosition
import me.nanova.summaryexpressive.ui.page.history.section.getDateSectionTitle
import me.nanova.summaryexpressive.ui.page.history.section.groupShape
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Calendar

class HistoryDateSectionTest {

    @Test
    fun `test getDateSectionTitle for today and yesterday`() {
        val now = System.currentTimeMillis()
        assertEquals("Today", getDateSectionTitle(now))

        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        assertEquals("Yesterday", getDateSectionTitle(yesterday))

        val threeDaysAgo = System.currentTimeMillis() - (3 * DateUtils.DAY_IN_MILLIS)
        assertEquals("Previous 7 Days", getDateSectionTitle(threeDaysAgo))
    }

    @Test
    fun `test groupShape radii values`() {
        val singleShape = groupShape(GroupPosition.SINGLE, outerRadius = 24.dp, innerRadius = 4.dp)
        assertEquals(24.dp, singleShape.topStart.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)).dp)

        val topShape = groupShape(GroupPosition.TOP, outerRadius = 24.dp, innerRadius = 4.dp)
        assertEquals(24.dp, topShape.topStart.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)).dp)
        assertEquals(4.dp, topShape.bottomStart.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)).dp)

        val bottomShape = groupShape(GroupPosition.BOTTOM, outerRadius = 24.dp, innerRadius = 4.dp)
        assertEquals(4.dp, bottomShape.topStart.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)).dp)
        assertEquals(24.dp, bottomShape.bottomStart.toPx(androidx.compose.ui.geometry.Size(100f, 100f), androidx.compose.ui.unit.Density(1f)).dp)
    }
}
