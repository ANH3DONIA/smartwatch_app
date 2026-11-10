package com.example.focuspause.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsOptionsTest {
    @Test fun currentValueIsAddedWhenMissing() {
        assertEquals(listOf(1, 15, 20, 25, 30, 45, 50, 60), SettingsOptions.withCurrent(SettingsOptions.FOCUS_MINUTES, 50))
    }

    @Test fun currentValueIsNotDuplicatedWhenPresent() {
        assertEquals(SettingsOptions.FOCUS_MINUTES, SettingsOptions.withCurrent(SettingsOptions.FOCUS_MINUTES, 25))
    }

    @Test fun oneMinuteIsLabelledAsTest() {
        assertEquals("1 min (prueba)", SettingsOptions.minutesLabel(1))
        assertEquals("25 min", SettingsOptions.minutesLabel(25))
    }
}
