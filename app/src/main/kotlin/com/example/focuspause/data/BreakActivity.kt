/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.data

/** Para qué tipo de pausa sirve una actividad. */
enum class BreakKind { SHORT, LONG, BOTH }

/** Una pausa activa: algo concreto y breve que hacer en lugar de seguir mirando la pantalla. */
data class BreakActivity(
    val id: String,
    val title: String,
    val instruction: String,
    val kind: BreakKind
)

object BreakActivities {

    val all: List<BreakActivity> = listOf(
        BreakActivity(
            "eyes_20_20_20", "Regla 20-20-20",
            "Mira algo a 6 metros de distancia durante 20 segundos. Parpadea varias veces.",
            BreakKind.SHORT
        ),
        BreakActivity(
            "water", "Hidrátate",
            "Toma un vaso de agua despacio. Tu cerebro rinde mejor hidratado.",
            BreakKind.BOTH
        ),
        BreakActivity(
            "neck", "Estira el cuello",
            "Inclina la cabeza hacia un lado 15 segundos y luego hacia el otro. Sin rebotes.",
            BreakKind.SHORT
        ),
        BreakActivity(
            "shoulders", "Hombros arriba y atrás",
            "Sube los hombros hacia las orejas, sostén 3 segundos y suéltalos. Repite 8 veces.",
            BreakKind.SHORT
        ),
        BreakActivity(
            "wrists", "Muñecas y manos",
            "Gira las muñecas 10 veces en cada sentido y abre y cierra las manos con fuerza.",
            BreakKind.SHORT
        ),
        BreakActivity(
            "breathing", "Respira profundo",
            "Inhala 4 segundos por la nariz, sostén 4 y exhala 6 por la boca. Repite 5 veces.",
            BreakKind.BOTH
        ),
        BreakActivity(
            "stand_stretch", "Levántate y estírate",
            "Ponte de pie, entrelaza las manos y estira los brazos hacia arriba. Sostén 20 segundos.",
            BreakKind.BOTH
        ),
        BreakActivity(
            "walk", "Da un paseo",
            "Camina unos minutos, a otra habitación o por la calle. Deja el celular en el escritorio.",
            BreakKind.LONG
        ),
        BreakActivity(
            "back_twist", "Torsión de espalda",
            "Sentado, gira el torso hacia un lado agarrando el respaldo. 20 segundos por lado.",
            BreakKind.LONG
        ),
        BreakActivity(
            "snack", "Come algo ligero",
            "Una fruta o frutos secos. Evita pantallas mientras comes.",
            BreakKind.LONG
        )
    )

    /**
     * Elige la actividad para la pausa número [index], rotando entre las que sirven
     * para ese tipo de pausa. Devuelve null en modo enfoque.
     */
    fun forMode(mode: PomodoroMode, index: Int): BreakActivity? {
        val wanted = when (mode) {
            PomodoroMode.FOCUS -> return null
            PomodoroMode.SHORT_BREAK -> BreakKind.SHORT
            PomodoroMode.LONG_BREAK -> BreakKind.LONG
        }
        val pool = all.filter { it.kind == wanted || it.kind == BreakKind.BOTH }
        return pool[Math.floorMod(index, pool.size)]
    }
}
