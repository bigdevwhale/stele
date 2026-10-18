package app.stele.domain

import app.stele.data.Find
import app.stele.data.Kind
import app.stele.data.Link
import app.stele.data.LinkKind
import app.stele.data.Mode
import app.stele.data.Review
import java.time.LocalDate
import kotlin.math.abs

/** The rules of the dig, kept apart from storage and screens so they can be tested plainly. */
object Rules {
    /** A vessel stands whole when it has at least this many shards and all of them are cleaned. */
    const val VESSEL_MIN = 3
    /** The sealed door asks this many questions… */
    const val DOOR_QUESTIONS = 12
    /** …and opens for three in four right. */
    const val DOOR_PASS = 0.75
    const val DOOR_EVERY_DAYS = 30
    /** Finds that must have been cleaned at least once before the door shows. */
    const val DOOR_MIN = 12

    /** Dust laid over a find, 0..1: none while it holds above [retention], full by half recall. */
    fun patina(f: Find, now: Long, retention: Double): Float {
        if (f.inGround) return 0f
        val r = f.recall(now)
        if (r >= retention) return 0f
        return ((retention - r) / (retention - 0.5)).coerceIn(0.0, 1.0).toFloat()
    }

    /** Cleaned once or more and dusty again: due now, the dustiest first. */
    fun dusty(finds: List<Find>, now: Long, retention: Double): List<Find> =
        finds.filter { !it.inGround && it.askable && it.due(retention) <= now }.sortedBy { it.recall(now) }

    /** Still in the ground and labelled: waiting for a first cleaning, oldest first. */
    fun buried(finds: List<Find>): List<Find> = finds.filter { it.inGround && it.askable }.sortedBy { it.createdAt }

    /** How many new finds may still come up today, given the first cleanings already done. */
    fun newLeft(reviews: List<Review>, finds: List<Find>, perDay: Int, today: LocalDate, day: (Long) -> LocalDate): Int {
        val firstAt = reviews.groupBy { it.findId }.mapValues { (_, rs) -> rs.minOf { it.at } }
        val ids = finds.map { it.id }.toSet()
        val doneToday = firstAt.count { (id, at) -> id in ids && day(at) == today }
        return (perDay - doneToday).coerceAtLeast(0)
    }

    /** One cleaning round: dusty finds, then a few new ones mixed in so the round doesn't end on the hardest. */
    fun round(finds: List<Find>, now: Long, retention: Double, newLeft: Int, limit: Int = 40): List<Find> {
        val old = dusty(finds, now, retention).take(limit)
        val new = buried(finds).take(minOf(newLeft, (limit - old.size).coerceAtLeast(0)))
        if (new.isEmpty()) return old
        val out = old.toMutableList()
        new.forEachIndexed { i, f -> out.add(minOf(out.size, (i + 1) * 3), f) }
        return out
    }

    /** How a find is asked this time: pairs alternate sides and are sometimes only heard. */
    fun mode(f: Find, canHear: Boolean): Mode = when (f.kindOf) {
        Kind.PHRASE -> Mode.GAP
        Kind.QUESTION -> Mode.FORWARD
        Kind.TERM -> if (f.stage >= Stage.CLEANED && (f.reps + f.lapses) % 3 == 2) Mode.REVERSE else Mode.FORWARD
        Kind.PAIR -> {
            val turn = f.reps + f.lapses
            when {
                canHear && turn % 4 == 3 && !f.inGround -> Mode.ECHO
                turn % 2 == 1 -> Mode.REVERSE
                else -> Mode.FORWARD
            }
        }
    }

    /** Whether a vessel's shards are all in and cleaned. */
    fun mended(shards: List<Find>): Boolean = shards.size >= VESSEL_MIN && shards.all { it.stage >= Stage.CLEANED }

    /**
     * The day's test pit: a few finds that have not come up for longest, one about to dust over, and a
     * rare one — the find that has held longest — to be glad of. Picked the same way all day.
     */
    fun sounding(finds: List<Find>, now: Long, retention: Double, size: Int, epochDay: Long): Sounding? {
        val dug = finds.filter { !it.inGround && it.askable }
        if (dug.size < 3) return null
        val rnd = java.util.Random(epochDay * 7919)
        val rare = dug.filter { it.stage >= Stage.RESTORED }.maxByOrNull { it.stability }
        val pool = dug.filter { it.id != rare?.id }
        val nearDue = pool.filter { it.due(retention) > now }.minByOrNull { it.due(retention) }
        val forgotten = pool.filter { it.id != nearDue?.id }.sortedBy { it.reviewedAt }.take(size * 2).shuffled(rnd)
        val picks = (listOfNotNull(nearDue) + forgotten).distinctBy { it.id }.take(size - if (rare != null) 1 else 0)
        return Sounding(picks + listOfNotNull(rare), rare?.id)
    }

    class Sounding(val finds: List<Find>, val rareId: Long?)

    /** The door wants knocking a month after it last opened, once enough has been dug. */
    fun doorDue(finds: List<Find>, now: Long, lastDoor: Long): Boolean {
        val dug = finds.filter { !it.inGround && it.askable }
        if (dug.size < DOOR_MIN) return false
        val since = if (lastDoor > 0) lastDoor else dug.minOf { it.createdAt }
        return now - since >= DOOR_EVERY_DAYS * Memory.DAY
    }

    fun doorDaysLeft(finds: List<Find>, now: Long, lastDoor: Long): Int? {
        val dug = finds.filter { !it.inGround && it.askable }
        if (dug.size < DOOR_MIN) return null
        val since = if (lastDoor > 0) lastDoor else dug.minOf { it.createdAt }
        return (((since + DOOR_EVERY_DAYS * Memory.DAY) - now + Memory.DAY - 1) / Memory.DAY).toInt().coerceAtLeast(0)
    }

    /** The door's questions: from every dig, mostly finds not seen lately, never the same twice. */
    fun doorQuestions(finds: List<Find>, seed: Long): List<Find> {
        val dug = finds.filter { !it.inGround && it.askable }
        val rnd = java.util.Random(seed)
        return dug.groupBy { it.digId }.values.flatMap { it.sortedBy { f -> f.reviewedAt }.take(DOOR_QUESTIONS) }
            .shuffled(rnd).take(DOOR_QUESTIONS)
    }

    /** Days on which anything was dug or cleaned. */
    fun fieldDays(finds: List<Find>, reviews: List<Review>, day: (Long) -> LocalDate): Set<LocalDate> =
        (finds.map { day(it.createdAt) } + reviews.map { day(it.at) }).toSet()

    /** The restorer's eye: for each stake, how often it turned out right (grade good or better). */
    fun calibration(reviews: List<Review>): Map<Int, Pair<Int, Int>> =
        (1..3).associateWith { s ->
            val rs = reviews.filter { it.stake == s && it.mode != Mode.FORGERY.name }
            rs.count { it.grade >= Grade.GOOD.n } to rs.size
        }

    /** How many finds dust over on each of the next [days] days, today first (overdue counted today). */
    fun forecast(finds: List<Find>, now: Long, retention: Double, days: Int, today: LocalDate, day: (Long) -> LocalDate): List<Int> {
        val counts = IntArray(days)
        finds.filter { !it.inGround && it.askable }.forEach { f ->
            val d = day(f.due(retention))
            val i = if (d <= today) 0 else java.time.temporal.ChronoUnit.DAYS.between(today, d).toInt()
            if (i in 0 until days) counts[i]++
        }
        return counts.toList()
    }

    /**
     * Forgeries: a find's meaning and its front among look-alikes. Decoys are, in order, the finds it was
     * marked as confused with, fronts spelled alike, then any other front of the same dig.
     */
    fun forgeries(finds: List<Find>, links: List<Link>, rounds: Int, seed: Long): List<Forgery> {
        val pool = finds.filter { it.askable && it.kindOf != Kind.PHRASE && it.kindOf != Kind.QUESTION }
        val rnd = java.util.Random(seed)
        val byId = pool.associateBy { it.id }
        val confused = links.filter { it.kindOf == LinkKind.CONFUSED }
        // Those marked as confused go first, then the ones forgotten most.
        val targets = (confused.flatMap { listOf(it.a, it.b) }.mapNotNull { byId[it] }.distinctBy { it.id }.shuffled(rnd) +
            pool.map { it to it.lapses * 10 + rnd.nextInt(10) }.sortedByDescending { it.second }.map { it.first }).distinctBy { it.id }
        val out = mutableListOf<Forgery>()
        for (t in targets) {
            val sameDig = pool.filter { it.digId == t.digId && it.id != t.id && !Words.variants(it.back).any { v -> v in Words.variants(t.back) } }
            if (sameDig.size < 2) continue
            val marked = confused.filter { it.a == t.id || it.b == t.id }.mapNotNull { byId[it.other(t.id)] }.filter { it in sameDig }
            // The keys are drawn once: a sort whose keys change between comparisons breaks on big digs.
            val alike = sameDig.filter { it !in marked }.map { it to Words.unlikeness(it.front, t.front) + rnd.nextDouble() * 0.15 }.sortedBy { it.second }.map { it.first }
            val decoys = (marked + alike).distinctBy { it.id }.take(3)
            out += Forgery(t, (decoys + t).shuffled(rnd))
            if (out.size == rounds) break
        }
        return out
    }

    class Forgery(val genuine: Find, val choices: List<Find>)

    /** What a text says about the museum: the finds met in it and the words it has that the museum hasn't. */
    fun sift(text: String, finds: List<Find>): Sifted {
        val sentences = Words.sentences(text)
        val tokens = Words.tokens(text)
        val stems = tokens.map { Words.stem(it) }
        val found = mutableListOf<Pair<Find, String>>()
        val knownStems = mutableSetOf<String>()
        for (f in finds) {
            val front = if (f.kindOf == Kind.PHRASE) Words.plain(f.front) else f.front
            // Articles and little words ("la", "to") needn't be there for the find to count as met.
            val fs = Words.tokens(front).let { ts -> ts.filter { Words.isContent(it) }.ifEmpty { ts } }.map { Words.stem(it) }
            if (fs.isEmpty() || fs.size > 6) continue
            val hit = sentences.firstOrNull { s ->
                val ss = Words.tokens(s).map { Words.stem(it) }
                // Every word of the find in one sentence, in any order: "on the road" is met in "the road we went on".
                fs.all { f -> ss.any { Words.sameStem(it, f) } }
            } ?: continue
            found += f to hit
            knownStems += fs
        }
        val content = tokens.filter { Words.isContent(it) }
        val known = content.count { t -> knownStems.any { Words.sameStem(Words.stem(t), it) } }
        val counts = mutableMapOf<String, Int>()
        val first = mutableMapOf<String, String>()
        content.forEach { t ->
            val s = Words.stem(t)
            if (knownStems.any { Words.sameStem(s, it) }) return@forEach
            counts[s] = (counts[s] ?: 0) + 1
            first.putIfAbsent(s, t.lowercase())
        }
        val fresh = counts.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { stems.indexOf(it.key) })
            .take(40).map { first.getValue(it.key) }
        val sentenceOf = { w: String -> sentences.firstOrNull { s -> Words.tokens(s).any { it.equals(w, ignoreCase = true) } }.orEmpty() }
        return Sifted(found, fresh, if (content.isEmpty()) 0f else known / content.size.toFloat(), sentenceOf)
    }

    class Sifted(
        val found: List<Pair<Find, String>>,
        /** Words of the text not yet in the museum, most frequent first. */
        val fresh: List<String>,
        /** Share of the text's words the museum holds. */
        val known: Float,
        val sentenceOf: (String) -> String,
    )

    /** Days between two epoch days, for "a week ago" style counts. */
    fun daysBetween(a: Long, b: Long): Int = (abs(b - a) / Memory.DAY).toInt()
}
