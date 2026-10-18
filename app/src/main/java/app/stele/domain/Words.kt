package app.stele.domain

/**
 * Plain word-work without a dictionary: tokens, a rough stem that forgives English and Russian
 * endings alike, typo distance for typed answers, and the [gaps] of a phrase.
 */
object Words {
    private val word = Regex("[\\p{L}\\p{N}][\\p{L}\\p{N}'’-]*")
    private val gap = Regex("\\[([^\\[\\]]+)]")

    private val stop = setOf(
        "the", "and", "that", "this", "with", "from", "they", "them", "then", "than", "there", "their", "what", "when", "which",
        "will", "would", "could", "should", "have", "has", "had", "been", "being", "were", "was", "are", "you", "your", "for",
        "not", "but", "its", "can", "into", "onto", "about", "just", "only", "also", "very", "more", "most", "some",
        "such", "each", "every", "other", "over", "under", "does", "did", "done", "how", "why", "who", "whom", "where",
        "because", "while", "after", "before", "until", "again", "once", "here", "these", "those", "one", "all", "any",
        "это", "этот", "эта", "эти", "того", "тому", "тот", "чтобы", "что", "как", "так", "когда", "если", "или", "только",
        "уже", "ещё", "еще", "тоже", "также", "очень", "потому", "поэтому", "чем", "где", "кто", "его", "её", "ее", "них",
        "они", "она", "оно", "мне", "меня", "мой", "моя", "свой", "своё", "свои", "себя", "себе", "был", "была", "было",
        "были", "будет", "есть", "нет", "для", "при", "про", "над", "под", "без", "после", "перед", "через", "между",
        "все", "всё", "всех", "весь", "вся", "который", "которая", "которое", "которые", "можно", "нужно", "надо",
        "даже", "лишь", "вот", "там", "тут", "здесь", "сам", "сама", "само", "сами", "один", "одна", "одно",
        "los", "las", "una", "uno", "del", "que", "por", "con", "para", "como", "pero", "más", "der", "die", "das", "und",
        "ein", "eine", "les", "des", "une", "est", "pas", "qui", "dans", "pour", "avec",
    )

    fun tokens(text: String): List<String> = word.findAll(text).map { it.value }.toList()

    fun normal(w: String): String = w.lowercase().replace('ё', 'е').trim('\'', '’', '-')

    /** Worth offering as a new find: not a number, not a little word. */
    fun isContent(w: String): Boolean {
        val l = normal(w)
        return l.length > 2 && l !in stop && !l.any { it.isDigit() }
    }

    /** A rough stem: long enough to tell words apart, short enough to forgive endings. */
    fun stem(w: String): String {
        val l = normal(w)
        return if (l.length <= 4) l else l.take(maxOf(4, l.length - 2).coerceAtMost(7))
    }

    /** One stem stands for another when either starts the other and both are long enough to mean it. */
    fun sameStem(a: String, b: String): Boolean = a == b || (minOf(a.length, b.length) >= 4 && (a.startsWith(b) || b.startsWith(a)))

    /** Edit distance, for forgiving a typo. */
    fun distance(a: String, b: String): Int {
        if (a == b) return 0
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = i
            for (j in 1..b.length) {
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            prev = cur
        }
        return prev[b.length]
    }

    /** 0 when equal, 1 when nothing alike. */
    fun unlikeness(a: String, b: String): Double {
        val x = squash(a)
        val y = squash(b)
        val n = maxOf(x.length, y.length)
        return if (n == 0) 0.0 else distance(x, y) / n.toDouble()
    }

    private fun squash(s: String) = tokens(s).joinToString(" ") { normal(it) }

    /** The ways an answer may be written: "car, automobile / auto" is three. Brackets are left out. */
    fun variants(answer: String): List<String> =
        answer.replace(Regex("\\([^)]*\\)"), " ").split(',', ';', '/', '|', '\n').map { squash(it) }.filter { it.isNotEmpty() }.ifEmpty { listOf(squash(answer)) }

    /** How a typed answer compares with the right one. */
    enum class Verdict { RIGHT, ALMOST, WRONG }

    fun check(typed: String, answer: String): Verdict {
        val t = squash(typed)
        if (t.isEmpty()) return Verdict.WRONG
        val vs = variants(answer)
        if (vs.any { it == t }) return Verdict.RIGHT
        val best = vs.minOf { distance(it, t) }
        val len = vs.minByOrNull { distance(it, t) }!!.length
        // One slip in a short word, two in a long one.
        return if (best <= if (len >= 8) 2 else if (len >= 4) 1 else 0) Verdict.ALMOST else Verdict.WRONG
    }

    fun suggest(v: Verdict): Grade = when (v) {
        Verdict.RIGHT -> Grade.GOOD
        Verdict.ALMOST -> Grade.HARD
        Verdict.WRONG -> Grade.AGAIN
    }

    /** A phrase's gaps, as the words inside its brackets. */
    fun gaps(phrase: String): List<String> = gap.findAll(phrase).map { it.groupValues[1].trim() }.toList()

    /** The phrase as it reads, brackets gone. */
    fun plain(phrase: String): String = gap.replace(phrase) { it.groupValues[1] }

    /**
     * A phrase cut into pieces of plain text and gaps. Without brackets the longest content word of
     * each four becomes a gap, so a phrase always has something to fill in.
     */
    fun pieces(phrase: String): List<Pair<String, Boolean>> {
        if (gap.containsMatchIn(phrase)) {
            val out = mutableListOf<Pair<String, Boolean>>()
            var at = 0
            gap.findAll(phrase).forEach { m ->
                if (m.range.first > at) out += phrase.substring(at, m.range.first) to false
                out += m.groupValues[1] to true
                at = m.range.last + 1
            }
            if (at < phrase.length) out += phrase.substring(at) to false
            return out
        }
        val ms = word.findAll(phrase).filter { isContent(it.value) }.toList()
        val picks = ms.chunked(4).mapNotNull { run -> run.maxByOrNull { it.value.length } }.map { it.range }.toSet()
        val out = mutableListOf<Pair<String, Boolean>>()
        var at = 0
        picks.sortedBy { it.first }.forEach { r ->
            if (r.first > at) out += phrase.substring(at, r.first) to false
            out += phrase.substring(r) to true
            at = r.last + 1
        }
        if (at < phrase.length) out += phrase.substring(at) to false
        return out
    }

    /** Sentences of a text, kept short enough to sit under a find. */
    fun sentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?…])\\s+|\\n+")).map { it.trim() }.filter { it.isNotEmpty() }.map { if (it.length > 240) it.take(237).trimEnd() + "…" else it }
}
