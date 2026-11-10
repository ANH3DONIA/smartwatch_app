/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui

/** Opciones de ajustes compartidas por la pantalla del celular y la del reloj, para que sean idénticas. */
object SettingsOptions {
    val FOCUS_MINUTES = listOf(1, 15, 20, 25, 30, 45, 60)
    val SHORT_BREAK_MINUTES = listOf(1, 3, 5, 10)
    val LONG_BREAK_MINUTES = listOf(10, 15, 20, 30)
    val LONG_BREAK_INTERVAL = listOf(2, 3, 4, 5, 6)
    val DAILY_GOAL = listOf(4, 6, 8, 10, 12)

    /**
     * Las opciones más el valor actual si no está entre ellas (por ejemplo, el de un perfil de estudio
     * o uno recibido del otro dispositivo), para que siempre haya una opción marcada.
     */
    fun withCurrent(options: List<Int>, current: Int): List<Int> = (options + current).distinct().sorted()

    fun minutesLabel(minutes: Int): String = if (minutes == 1) "1 min (prueba)" else "$minutes min"
}
