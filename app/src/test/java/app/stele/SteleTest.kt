package app.stele

import app.stele.data.Backup
import app.stele.data.Dig
import app.stele.data.Find
import app.stele.data.Kind
import app.stele.data.Lines
import app.stele.data.Link
import app.stele.data.LinkKind
import app.stele.data.Mode
import app.stele.data.Review
import app.stele.data.Vessel
import app.stele.domain.Gender
import app.stele.domain.Grade
import app.stele.domain.Memory
import app.stele.domain.Rules
import app.stele.domain.Stage
import app.stele.domain.Words
import app.stele.domain.gendered
import app.stele.notify.Bell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SteleTest {
    private val day = Memory.DAY

    private fun find(id: Long, front: String, back: String = "x", s: Double = 0.0, at: Long = 0, dig: Long = 1, vessel: Long = 0, kind: Kind = Kind.PAIR) =
        Find(id = id, digId = dig, vesselId = vessel, kind = kind.name, front = front, back = back, stability = s, difficulty = 5.0, reviewedAt = at, createdAt = id)

    @Test
    fun recallIsNinetyPercentAfterOneStability() {
        val t = Memory.first(Grade.GOOD, 0)
        assertEquals(1.0, Memory.recall(t, 0), 1e-9)
        assertEquals(0.9, Memory.recall(t.copy(stability = 5.0), 5 * day), 1e-6)
        assertEquals(5.0, Memory.interval(5.0, 0.9), 1e-6)
    }

    @Test
    fun firstGradeSetsTheStartAndGoodCleaningsStretchIt() {
        assertTrue(Memory.first(Grade.EASY, 0).stability > Memory.first(Grade.AGAIN, 0).stability)
        var t = Memory.first(Grade.GOOD, 0)
        val seen = mutableListOf<Double>()
        repeat(6) {
            t = Memory.review(t, Grade.GOOD, Memory.due(t, 0.9))
            seen += t.stability
        }
        seen.zipWithNext().forEach { (a, b) -> assertTrue("$a then $b", b > a) }
        assertTrue(seen.last() > 30)
        assertTrue(Memory.review(t, Grade.AGAIN, Memory.due(t, 0.9)).stability < t.stability)
    }

    @Test
    fun stagesFollowStability() {
        assertEquals(Stage.GROUND, Stage.ofStability(0.0))
        assertEquals(Stage.SHARD, Stage.ofStability(2.0))
        assertEquals(Stage.CLEANED, Stage.ofStability(7.0))
        assertEquals(Stage.RESTORED, Stage.ofStability(45.0))
        assertEquals(Stage.EXHIBIT, Stage.ofStability(200.0))
        assertEquals(Stage.CLEANED, Stage.SHARD.next)
        assertNull(Stage.EXHIBIT.next)
    }

    @Test
    fun typedAnswersForgiveCaseVariantsAndOneSlip() {
        assertEquals(Words.Verdict.RIGHT, Words.check("House", "house"))
        assertEquals(Words.Verdict.RIGHT, Words.check("home", "house, home / dwelling"))
        assertEquals(Words.Verdict.RIGHT, Words.check("ёлка", "елка"))
        assertEquals(Words.Verdict.ALMOST, Words.check("hous", "house"))
        assertEquals(Words.Verdict.ALMOST, Words.check("embarazda", "embarazada"))
        assertEquals(Words.Verdict.WRONG, Words.check("cat", "dog"))
        assertEquals(Words.Verdict.WRONG, Words.check("", "dog"))
        // Brackets hold a note, not the answer.
        assertEquals(Words.Verdict.RIGHT, Words.check("to develop", "to develop (a skill)"))
    }

    @Test
    fun phrasesCutIntoGaps() {
        val p = Words.pieces("Memory is the [treasury] and [guardian] of all things.")
        assertEquals(listOf("treasury", "guardian"), p.filter { it.second }.map { it.first })
        assertEquals("Memory is the treasury and guardian of all things.", p.joinToString("") { it.first })
        assertEquals("Memory is the treasury", Words.plain("Memory is the [treasury]"))
        // No brackets: still something to fill in.
        assertTrue(Words.pieces("Repetition is the mother of learning").any { it.second })
    }

    @Test
    fun linesSplitOnDashesTabsAndEquals() {
        val rows = Lines.parse("casa — house\nperro\t dog\ngato = cat\n\nluz\n")
        assertEquals(listOf("casa", "perro", "gato", "luz"), rows.map { it.front })
        assertEquals(listOf("house", "dog", "cat", ""), rows.map { it.back })
    }

    @Test
    fun csvWithQuotesHeaderAndSemicolons() {
        val rows = Lines.csv("front,back,example\n\"hola, amigo\",\"hi, friend\",\"Dijo \"\"hola\"\"\"\nadiós,bye,\n")
        assertEquals(2, rows.size)
        assertEquals("hola, amigo", rows[0].front)
        assertEquals("hi, friend", rows[0].back)
        assertEquals("Dijo \"hola\"", rows[0].example)
        val semi = Lines.csv("uno;one\ndos;two")
        assertEquals(listOf("one", "two"), semi.map { it.back })
        val back = Lines.csv(Lines.toCsv(listOf(find(1, "a, b", "c \"d\""))))
        assertEquals("a, b", back[0].front)
        assertEquals("c \"d\"", back[0].back)
    }

    @Test
    fun aRoundTakesDustyFirstAndMixesInNew() {
        val now = 100 * day
        val finds = listOf(
            find(1, "old", s = 2.0, at = now - 30 * day),
            find(2, "fresh", s = 50.0, at = now - day),
            find(3, "new1"), find(4, "new2"), find(5, "unlabelled", back = ""),
        )
        val round = Rules.round(finds, now, 0.9, newLeft = 1)
        assertEquals(listOf(1L, 3L), round.map { it.id })
        assertTrue(Rules.patina(finds[0], now, 0.9) > 0.5f)
        assertEquals(0f, Rules.patina(finds[1], now, 0.9))
    }

    @Test
    fun newLeftCountsFirstCleaningsToday() {
        val today = LocalDate.of(2026, 10, 18)
        val t0 = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val reviews = listOf(Review(findId = 1, at = t0 + 1000, mode = "FORWARD", grade = 3), Review(findId = 2, at = t0 - day, mode = "FORWARD", grade = 3), Review(findId = 2, at = t0 + 5000, mode = "FORWARD", grade = 3))
        val left = Rules.newLeft(reviews, listOf(find(1, "a"), find(2, "b")), 10, today) { java.time.Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
        assertEquals(9, left)
    }

    @Test
    fun aVesselStandsWholeOnlyWhenEveryShardIsClean() {
        val clean = listOf(find(1, "a", s = 8.0), find(2, "b", s = 30.0), find(3, "c", s = 200.0))
        assertTrue(Rules.mended(clean))
        assertFalse(Rules.mended(clean.take(2)))
        assertFalse(Rules.mended(clean + find(4, "d", s = 3.0)))
    }

    @Test
    fun theTestPitHasARareFindAndStaysTheSameAllDay() {
        val now = 400 * day
        val finds = (1L..12L).map { find(it, "f$it", s = it * 12.0, at = now - it * day) }
        val a = Rules.sounding(finds, now, 0.9, 5, 400)
        val b = Rules.sounding(finds, now, 0.9, 5, 400)
        assertNotNull(a)
        assertEquals(5, a!!.finds.size)
        assertEquals(12L, a.rareId)
        assertEquals(a.finds.map { it.id }, b!!.finds.map { it.id })
        assertNull(Rules.sounding(finds.take(2), now, 0.9, 5, 400))
    }

    @Test
    fun theDoorWaitsAMonthAndEnoughFinds() {
        val now = 100 * day
        val finds = (1L..12L).map { find(it, "f$it", s = 5.0, at = now) }
        assertTrue(Rules.doorDue(finds, now, now - 31 * day))
        assertFalse(Rules.doorDue(finds, now, now - 10 * day))
        assertEquals(20, Rules.doorDaysLeft(finds, now, now - 10 * day))
        assertFalse(Rules.doorDue(finds.take(5), now, now - 40 * day))
        assertEquals(12, Rules.doorQuestions(finds, 1).size)
    }

    @Test
    fun forgeriesPreferWhatYouConfuse() {
        val finds = listOf(
            find(1, "embarazada", "pregnant"), find(2, "avergonzado", "embarrassed"),
            find(3, "casa", "house"), find(4, "perro", "dog"), find(5, "gato", "cat"),
        )
        val links = listOf(Link(a = 1, b = 2, kind = LinkKind.CONFUSED.name, createdAt = 0))
        val rounds = Rules.forgeries(finds, links, 3, 7)
        val first = rounds.first { it.genuine.id == 1L || it.genuine.id == 2L }
        assertTrue(first.choices.map { it.id }.containsAll(listOf(1L, 2L)))
        rounds.forEach { r -> assertTrue(r.genuine in r.choices); assertEquals(r.choices.size, r.choices.distinctBy { it.id }.size) }
    }

    @Test
    fun aBigDeckSortsWithoutBreaking() {
        // An imported Anki deck: thousands of finds, many alike, many never cleaned.
        val now = 400 * day
        val rnd = java.util.Random(3)
        val finds = (1L..3000L).map { i ->
            val s = if (i % 3 == 0L) 0.0 else rnd.nextDouble() * 200
            find(i, "word${i % 500}", "meaning ${i % 700}", s = s, at = now - (rnd.nextDouble() * 60 * day).toLong()).copy(lapses = rnd.nextInt(4))
        }
        val t0 = System.currentTimeMillis()
        repeat(20) { seed -> assertTrue(Rules.forgeries(finds, emptyList(), 8, seed.toLong()).isNotEmpty()) }
        assertNotNull(Rules.sounding(finds, now, 0.9, 5, 400))
        assertTrue(Rules.round(finds, now, 0.9, 10).isNotEmpty())
        Rules.sift("word1 word2 something else entirely", finds)
        assertTrue("took ${System.currentTimeMillis() - t0} ms", System.currentTimeMillis() - t0 < 20_000)
    }

    @Test
    fun siftingFindsKnownWordsAndOffersNewOnes() {
        val finds = listOf(find(1, "la madrugada", "small hours"), find(2, "ojalá", "hopefully"), find(3, "Memory is the [treasury]", kind = Kind.PHRASE))
        val s = Rules.sift("Llegamos de madrugada. Ojalá llueva mañana, mañana sin falta.", finds)
        assertEquals(setOf(1L, 2L), s.found.map { it.first.id }.toSet())
        assertEquals("mañana", s.fresh.first())
        assertTrue(s.known > 0f && s.known < 1f)
        assertTrue(s.sentenceOf("llueva").startsWith("Ojalá"))
    }

    @Test
    fun modesTakeTurns() {
        val f = find(1, "casa", s = 3.0, at = 1)
        assertEquals(Mode.FORWARD, Rules.mode(f.copy(reps = 0), canHear = true))
        assertEquals(Mode.REVERSE, Rules.mode(f.copy(reps = 1), canHear = true))
        assertEquals(Mode.ECHO, Rules.mode(f.copy(reps = 3), canHear = true))
        assertEquals(Mode.REVERSE, Rules.mode(f.copy(reps = 3), canHear = false))
        assertEquals(Mode.GAP, Rules.mode(f.copy(kind = Kind.PHRASE.name), canHear = true))
    }

    @Test
    fun calibrationCountsRightByStake() {
        val r = listOf(3, 3, 1).mapIndexed { i, g -> Review(findId = i.toLong(), at = 0, mode = "FORWARD", stake = 3, grade = g) } +
            Review(findId = 9, at = 0, mode = "FORWARD", stake = 1, grade = 1)
        val cal = Rules.calibration(r)
        assertEquals(2 to 3, cal[3])
        assertEquals(0 to 1, cal[1])
        assertEquals(0 to 0, cal[2])
    }

    @Test
    fun backupRoundTrips() {
        val text = Backup.encode(
            listOf(Dig(id = 1, name = "Español", lang = "es", createdAt = 5)),
            listOf(Vessel(id = 2, digId = 1, name = "Cocina", shape = "AMPHORA", createdAt = 6, mendedAt = 9)),
            listOf(find(3, "casa", "house", s = 12.0, at = 7, vessel = 2).copy(quote = "Mi casa", confirmedAt = 8, cracked = true)),
            listOf(Link(id = 4, a = 3, b = 3, kind = "OPPOSITE", createdAt = 1)),
            listOf(Review(id = 5, findId = 3, at = 7, mode = "ECHO", stake = 2, grade = 3, answer = "casa")),
            mapOf("p.jpg" to byteArrayOf(1, 2, 3)),
        )
        val c = Backup.decode(text)
        assertEquals("es", c.digs.single().lang)
        assertEquals(9L, c.vessels.single().mendedAt)
        val f = c.finds.single()
        assertEquals(12.0, f.stability, 1e-9)
        assertEquals(8L, f.confirmedAt)
        assertTrue(f.cracked)
        assertEquals(2L, f.vesselId)
        assertEquals("ECHO", c.reviews.single().mode)
        assertEquals(listOf<Byte>(1, 2, 3), c.images.getValue("p.jpg").toList())
        val failed = runCatching { Backup.decode("{\"app\":\"other\"}") }
        assertTrue(failed.isFailure)
    }

    @Test
    fun genderMarksResolve() {
        assertEquals("ты уверена", gendered("ты {уверен|уверена}", Gender.FEMALE))
        assertEquals("видел", gendered("видел{|а}", Gender.MALE))
        assertEquals("видела", gendered("видел{|а}", Gender.FEMALE))
        assertEquals("90%", gendered("90%", Gender.MALE))
    }

    @Test
    fun theBellRingsAfterNow() {
        val zone = ZoneOffset.UTC
        val now = LocalDate.of(2026, 10, 18).atTime(20, 0).toInstant(zone).toEpochMilli()
        assertEquals(LocalDate.of(2026, 10, 19).atTime(19, 0).toInstant(zone).toEpochMilli(), Bell.next(now, 19 * 60, zone))
        assertEquals(LocalDate.of(2026, 10, 18).atTime(21, 0).toInstant(zone).toEpochMilli(), Bell.next(now, 21 * 60, zone))
    }
}
