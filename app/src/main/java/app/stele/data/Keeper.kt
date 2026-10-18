package app.stele.data

import app.stele.domain.Grade
import app.stele.domain.Memory
import app.stele.domain.Rules
import app.stele.domain.Stage

/** The writes that carry rules with them: a cleaning bends a trace, cracks a find, glues a vessel. */
class Keeper(private val db: SteleDatabase) {
    private val dao get() = db.dao()

    /** What a cleaning changed, for the screen to show. */
    class Outcome(val before: Stage, val after: Stage, val cracked: Boolean, val mended: Vessel?)

    suspend fun clean(findId: Long, mode: Mode, grade: Grade, stake: Int, answer: String, now: Long = System.currentTimeMillis()): Outcome? {
        val f = dao.find(findId) ?: return null
        val t = if (f.inGround) Memory.first(grade, now) else Memory.review(f.trace, grade, now)
        // Sure of it and wrong: the error that, once put right, sticks best. Out of the workshop once cleaned well.
        val cracked = when {
            grade == Grade.AGAIN && stake == 3 -> true
            grade.n >= Grade.GOOD.n -> false
            else -> f.cracked
        }
        val next = f.copy(
            stability = t.stability, difficulty = t.difficulty, reviewedAt = t.reviewedAt,
            reps = f.reps + if (grade == Grade.AGAIN) 0 else 1,
            lapses = f.lapses + if (grade == Grade.AGAIN) 1 else 0,
            cracked = cracked,
        )
        dao.update(next)
        dao.insert(Review(findId = findId, at = now, mode = mode.name, stake = stake, grade = grade.n, answer = answer))
        return Outcome(f.stage, next.stage, cracked && !f.cracked, glue(next.vesselId, now))
    }

    /** A vessel whose last shard was just cleaned stands whole for the first time. */
    suspend fun glue(vesselId: Long, now: Long = System.currentTimeMillis()): Vessel? {
        if (vesselId == 0L) return null
        val v = dao.vessel(vesselId) ?: return null
        if (v.mendedAt != null || !Rules.mended(dao.findsIn(vesselId))) return null
        val mended = v.copy(mendedAt = now)
        dao.update(mended)
        return mended
    }

    /** Told apart from forgeries, or not: kept for the journal, and a mix-up is remembered as one. */
    suspend fun forgery(genuine: Find, picked: Find, now: Long = System.currentTimeMillis()) {
        val right = genuine.id == picked.id
        dao.insert(Review(findId = genuine.id, at = now, mode = Mode.FORGERY.name, grade = if (right) Grade.GOOD.n else Grade.AGAIN.n, answer = picked.front))
        if (!right) {
            val linked = dao.links().any { (it.a == genuine.id && it.b == picked.id) || (it.b == genuine.id && it.a == picked.id) }
            if (!linked) dao.insert(Link(a = genuine.id, b = picked.id, kind = LinkKind.CONFUSED.name, createdAt = now))
        }
    }

    /** A text has shown the find in use: its sentence stays with it. */
    suspend fun confirm(findId: Long, sentence: String, now: Long = System.currentTimeMillis()) {
        val f = dao.find(findId) ?: return
        dao.update(f.copy(quote = f.quote.ifBlank { sentence }, confirmedAt = f.confirmedAt ?: now, updatedAt = now))
    }
}
