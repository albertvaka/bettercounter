package org.kde.bettercounter.extensions

import org.junit.Assert
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarExtensionTest {

    @Test
    fun `week truncation respects the first day of the week in US locale`() {
        Locale.setDefault(Locale.Category.FORMAT, Locale.US)
        Assert.assertEquals(Calendar.getInstance().firstDayOfWeek, Calendar.SUNDAY)
        val sundayDate = Date(1697995920502).toCalendar().truncated(Calendar.WEEK_OF_YEAR) // Sunday 22 October 2023 17:32:00.502 UTC
        val mondayDate = Date(1691995920502).toCalendar().truncated(Calendar.WEEK_OF_YEAR) // Monday 14 August 2023 6:52:00.502 UTC
        Assert.assertEquals(22, sundayDate.get(Calendar.DAY_OF_MONTH))
        Assert.assertEquals(13, mondayDate.get(Calendar.DAY_OF_MONTH))

    }

    @Test
    fun `week truncation respects the first day of the week in normal countries locale`() {
        Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY)
        Assert.assertEquals(Calendar.getInstance().firstDayOfWeek, Calendar.MONDAY)
        val sundayDate = Date(1697995920502).toCalendar().truncated(Calendar.WEEK_OF_YEAR) // Sunday 22 October 2023 17:32:00.502 UTC
        val mondayDate = Date(1691995920502).toCalendar().truncated(Calendar.WEEK_OF_YEAR) // Monday 14 August 2023 6:52:00.502 UTC
        Assert.assertEquals(16, sundayDate.get(Calendar.DAY_OF_MONTH))
        Assert.assertEquals(14, mondayDate.get(Calendar.DAY_OF_MONTH))

    }

}
