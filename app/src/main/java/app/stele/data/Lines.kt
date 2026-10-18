package app.stele.data

/**
 * Finds typed or pasted many at a time: one per line, front and back split by a dash, a tab, "=" or
 * ";". Also CSV and TSV files, such as an Anki or spreadsheet export.
 */
object Lines {
    /** The byte-order mark some spreadsheet apps put at the start of a CSV. */
    private const val BOM = "\uFEFF"

    class Row(val front: String, val back: String, val example: String = "", val hint: String = "")

    private val separators = listOf("\t", " — ", " – ", " = ", " - ", ";", "—", "–", "=")

    /** A typed list: "casa — house" per line; a line with no separator is a front with no label yet. */
    fun parse(text: String): List<Row> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
        val sep = separators.firstOrNull { it in line }
        if (sep == null) Row(line, "") else {
            val parts = line.split(sep, limit = 2)
            Row(parts[0].trim(), parts[1].trim())
        }
    }.filter { it.front.isNotEmpty() }

    /** A CSV or TSV file: columns front, back, example, hint. The delimiter is guessed; a header row is skipped. */
    fun csv(text: String): List<Row> {
        val body = text.removePrefix(BOM)
        val firstLine = body.lineSequence().firstOrNull { it.isNotBlank() && !it.startsWith("#") }.orEmpty()
        val delim = listOf('\t', ';', ',').maxByOrNull { d -> firstLine.count { it == d } } ?: ','
        val rows = records(body, delim).filter { r -> r.any { it.isNotBlank() } && !(r.firstOrNull()?.startsWith("#") ?: false) }
        val header = rows.firstOrNull()?.map { it.trim().lowercase() }
        val skip = header != null && header.any { it in setOf("front", "back", "word", "translation", "term", "слово", "перевод", "термин") }
        return rows.drop(if (skip) 1 else 0).mapNotNull { r ->
            val front = r.getOrNull(0)?.trim().orEmpty()
            if (front.isEmpty()) null else Row(front, r.getOrNull(1)?.trim().orEmpty(), r.getOrNull(2)?.trim().orEmpty(), r.getOrNull(3)?.trim().orEmpty())
        }
    }

    /** Splits delimited text into records, honouring quotes (and quotes doubled inside them). */
    fun records(text: String, delim: Char): List<List<String>> {
        val out = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && text.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                c == '"' && (quoted || cell.isEmpty()) -> quoted = !quoted
                !quoted && c == delim -> { row += cell.toString(); cell.clear() }
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    row += cell.toString(); cell.clear()
                    out += row; row = mutableListOf()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row += cell.toString(); out += row }
        return out
    }

    fun toCsv(finds: List<Find>): String {
        fun q(s: String) = if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
        return buildString {
            appendLine("front,back,example,hint")
            finds.forEach { appendLine(listOf(it.front, it.back, it.example, it.hint).joinToString(",") { s -> q(s) }) }
        }
    }
}
