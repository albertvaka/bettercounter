package org.kde.bettercounter.extensions

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.temporal.ChronoUnit
import java.util.Date

class ChronoUnitExtensionTest {

    @Test
    fun `millis with less than one second diff`() {
        val from = Date(1697146570502) // 12 October 2023 21:36:10.502 UTC
        val to = Date(1700780933742) // 23 November 2023 23:08:53.742 UTC
        val count = ChronoUnit.DAYS.count(from, to)
        assertEquals(44, count)
    }

    @Test
    fun `millis with more than one second diff`() {
        val from = Date(1697146570502) // 12 October 2023 21:36:10.502 UTC
        val to = Date(1700780934441) // 23 November 2023 23:08:54.441 UTC
        val count = ChronoUnit.DAYS.count(from, to)
        assertEquals(44, count)
    }

    @Test
    fun `one second later is one day`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1700781096000) // 23 November 2023 23:11:36 UTC
        val count = ChronoUnit.DAYS.count(from, to)
        assertEquals(1, count)
    }

    @Test
    fun `one exact day later are two days`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1700867495000) // 24 November 2023 23:11:35 UTC
        val count = ChronoUnit.DAYS.count(from, to)
        assertEquals(2, count)
    }

    @Test
    fun `one second later is one week`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1700781096000) // 23 November 2023 23:11:36 UTC
        val count = ChronoUnit.WEEKS.count(from, to)
        assertEquals(1, count)
    }

    @Test
    fun `one exact week later are two weeks`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1701385895000) // 30 November 2023 23:11:35 UTC
        val count = ChronoUnit.WEEKS.count(from, to)
        assertEquals(2, count)
    }

    @Test
    fun `one second later is one month`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1700781096000) // 23 November 2023 23:11:36 UTC
        val count = ChronoUnit.MONTHS.count(from, to)
        assertEquals(1, count)
    }

    @Test
    fun `one exact month later are two months`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1703373095000) // 23 December 2023 23:11:35 UTC
        val count = ChronoUnit.MONTHS.count(from, to)
        assertEquals(2, count)
    }

    @Test
    fun `one second later is one year`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1700781096000) // 23 November 2023 23:11:36 UTC
        val count = ChronoUnit.YEARS.count(from, to)
        assertEquals(1, count)
    }

    @Test
    fun `one exact year later are two years`() {
        val from = Date(1700781095000) // 23 November 2023 23:11:35 UTC
        val to = Date(1732403495000) // 23 November 2024 23:11:35 UTC
        val count = ChronoUnit.YEARS.count(from, to)
        assertEquals(2, count)
    }
}
