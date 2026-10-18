package app.stele.domain

import kotlin.math.exp
import kotlin.math.pow

/** How a cleaning went, in the digger's own judgement. */
enum class Grade(val n: Int) { AGAIN(1), HARD(2), GOOD(3), EASY(4) }

/** Where a find stands in memory: how long it holds ([stability], days) and how hard it is to hold ([difficulty], 1..10). */
data class Trace(val stability: Double, val difficulty: Double, val reviewedAt: Long)

/**
 * The forgetting curve and how each cleaning bends it: FSRS-4.5 with its published default weights.
 * The dust on a find is how far its retrievability, the chance of recalling it right now, has fallen.
 */
object Memory {
    private val w = doubleArrayOf(
        0.4872, 1.4003, 3.7145, 13.8206, 5.1618, 1.2298, 0.8975, 0.031, 1.6474,
        0.1367, 1.0461, 2.1072, 0.0793, 0.3246, 1.587, 0.2272, 2.8755,
    )
    private const val DECAY = -0.5
    private const val FACTOR = 19.0 / 81.0
    const val DAY = 86_400_000L

    /** The first cleaning of a find still in the ground: its grade sets where memory starts. */
    fun first(g: Grade, at: Long) = Trace(stability = w[g.n - 1], difficulty = initialDifficulty(g), reviewedAt = at)

    /** The chance of recalling it [at] this moment, 0..1. */
    fun recall(t: Trace, at: Long): Double {
        val days = ((at - t.reviewedAt).coerceAtLeast(0L)) / DAY.toDouble()
        return (1 + FACTOR * days / t.stability).pow(DECAY)
    }

    /** Days after a review until recall drops to [retention]. */
    fun interval(stability: Double, retention: Double): Double = stability / FACTOR * (retention.pow(1 / DECAY) - 1)

    /** When the find dusts over to [retention] and wants cleaning. */
    fun due(t: Trace, retention: Double): Long = t.reviewedAt + (interval(t.stability, retention) * DAY).toLong().coerceAtLeast(DAY / 3)

    fun review(t: Trace, g: Grade, at: Long): Trace {
        val r = recall(t, at)
        val d = nextDifficulty(t.difficulty, g)
        val s = if (g == Grade.AGAIN) forget(t.difficulty, t.stability, r) else remember(t.difficulty, t.stability, r, g)
        return Trace(stability = s.coerceIn(0.1, 36_500.0), difficulty = d, reviewedAt = at)
    }

    private fun initialDifficulty(g: Grade) = (w[4] - (g.n - 3) * w[5]).coerceIn(1.0, 10.0)

    private fun nextDifficulty(d: Double, g: Grade): Double {
        val next = d - w[6] * (g.n - 3)
        return (w[7] * initialDifficulty(Grade.EASY) + (1 - w[7]) * next).coerceIn(1.0, 10.0)
    }

    private fun remember(d: Double, s: Double, r: Double, g: Grade): Double {
        val hard = if (g == Grade.HARD) w[15] else 1.0
        val easy = if (g == Grade.EASY) w[16] else 1.0
        return s * (exp(w[8]) * (11 - d) * s.pow(-w[9]) * (exp(w[10] * (1 - r)) - 1) * hard * easy + 1)
    }

    private fun forget(d: Double, s: Double, r: Double): Double =
        (w[11] * d.pow(-w[12]) * ((s + 1).pow(w[13]) - 1) * exp(w[14] * (1 - r))).coerceAtMost(s)
}
