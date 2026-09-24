package org.kde.bettercounter.test

import android.content.SharedPreferences
import org.junit.Before
import org.kde.bettercounter.persistence.FirstHourOfDay
import java.lang.reflect.Proxy

abstract class FirstHourOfDayTestBase {

    @Before
    fun initializeFirstHourOfDay() {
        @Suppress("UNCHECKED_CAST")
        val prefs = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getInt" -> 0
                else -> error("Unexpected SharedPreferences call: ${method.name}")
            }
        } as SharedPreferences

        FirstHourOfDay.prefs = prefs
    }
}
