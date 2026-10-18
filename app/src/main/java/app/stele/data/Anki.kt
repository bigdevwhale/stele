package app.stele.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.OpenableColumns
import io.airlift.compress.zstd.ZstdInputStream
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Anki decks (.apkg and .colpkg): a zip around an SQLite collection. A deck is read first, then the digger
 * says which field is the front, which the back and which an example, because decks differ: one keeps the
 * translation in its second field, another a sound there and the meaning in the third. Cloze notes become
 * phrases with [gaps]. Pictures come along from older packages, whose media list is plain JSON.
 */
object Anki {
    /** A note as it came: its fields as plain text, the cloze front if it has one, a picture's file in the unpacked deck. */
    class Raw(val fields: List<String>, val cloze: String?, val picture: String?)

    /** A deck read and waiting to be mapped; its pictures wait in [dir] until it is saved or dropped. */
    class Deck(val name: String, val fieldNames: List<String>, val notes: List<Raw>, val dir: File) {
        val fieldCount: Int get() = fieldNames.size
        /** Share of notes with something in each field. */
        val filled: List<Float> = fieldNames.indices.map { i -> notes.count { it.fields.getOrNull(i).orEmpty().isNotBlank() } / notes.size.coerceAtLeast(1).toFloat() }
        /** What a field holds, from the first note that has it. */
        fun sample(i: Int): String = notes.firstNotNullOfOrNull { n -> n.fields.getOrNull(i)?.takeIf { it.isNotBlank() } }.orEmpty()
    }

    /** Which field goes where; [example] is -1 when none. */
    data class Mapping(val front: Int, val back: Int, val example: Int = -1)

    class Note(val front: String, val back: String, val kind: Kind, val example: String = "", val picture: String? = null)

    fun read(context: Context, uri: Uri): Deck {
        val dir = File(context.cacheDir, "anki").apply { deleteRecursively(); mkdirs() }
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val e = zip.nextEntry ?: break
                        val name = File(e.name).name
                        if (e.isDirectory || name.isEmpty()) continue
                        // Sounds can be most of a deck's size and are never used: only the collection, the media list and pictures are kept.
                        if (name.startsWith("collection") || name == "media") { File(dir, name).outputStream().use { zip.copyTo(it) }; continue }
                        val head = ByteArray(12)
                        var n = 0
                        while (n < 12) { val r = zip.read(head, n, 12 - n); if (r <= 0) break; n += r }
                        if (isPicture(head, n)) File(dir, name).outputStream().use { it.write(head, 0, n); zip.copyTo(it) }
                    }
                }
            } ?: error("Can't read that")
            // The newest packages are compressed with zstd; they still carry an old "please update" collection beside it.
            val db = File(dir, "collection.anki21b").takeIf { it.exists() }?.let { packed ->
                File(dir, "collection.unpacked").also { out -> ZstdInputStream(packed.inputStream()).use { z -> out.outputStream().use { z.copyTo(it) } } }
            } ?: File(dir, "collection.anki21").takeIf { it.exists() } ?: File(dir, "collection.anki2").takeIf { it.exists() }
                ?: throw IllegalArgumentException("Not an Anki package")
            val media = runCatching { JSONObject(File(dir, "media").readText()) }.getOrNull()
            val byName = media?.let { m -> m.keys().asSequence().associateBy { m.getString(it) } }.orEmpty()
            val notes = mutableListOf<Raw>()
            var names = emptyList<String>()
            SQLiteDatabase.openDatabase(db.path, null, SQLiteDatabase.OPEN_READONLY).use { sql ->
                var topModel = 0L
                // The note type most notes use, whose field names are shown; a bare collection may not say.
                runCatching { sql.rawQuery("SELECT mid, count(*) c FROM notes GROUP BY mid ORDER BY c DESC LIMIT 1", null).use { c -> if (c.moveToFirst()) topModel = c.getLong(0) } }
                names = fieldNames(sql, topModel)
                sql.rawQuery("SELECT flds FROM notes ORDER BY id", null).use { c ->
                    while (c.moveToNext()) {
                        val fields = c.getString(0).split('\u001f')
                        val first = fields.firstOrNull().orEmpty()
                        val cloze = if (Regex("\\{\\{c\\d+::").containsMatchIn(first)) plain(cloze(first)) else null
                        val src = fields.firstNotNullOfOrNull { f -> Regex("<img[^>]+src=\"([^\"]+)\"").find(f)?.groupValues?.get(1) }
                        val picture = src?.let { byName[it] }?.takeIf { File(dir, it).exists() }
                        notes += Raw(fields.map { plain(it) }, cloze, picture)
                    }
                }
            }
            val count = maxOf(names.size, notes.maxOfOrNull { it.fields.size } ?: 0)
            val all = (0 until count).map { i -> names.getOrNull(i)?.takeIf { it.isNotBlank() } ?: "#${i + 1}" }
            return Deck(displayName(context, uri), all, notes, dir)
        } catch (e: Throwable) {
            dir.deleteRecursively()
            throw e
        }
    }

    /** The note type's field names: from the fields table in newer collections, from the models JSON in older ones. */
    private fun fieldNames(sql: SQLiteDatabase, model: Long): List<String> {
        runCatching {
            sql.rawQuery("SELECT name FROM fields WHERE ntid = ? ORDER BY ord", arrayOf(model.toString())).use { c ->
                val out = mutableListOf<String>()
                while (c.moveToNext()) out += c.getString(0)
                if (out.isNotEmpty()) return out
            }
        }
        return runCatching {
            sql.rawQuery("SELECT models FROM col", null).use { c ->
                if (!c.moveToFirst()) return emptyList()
                val models = JSONObject(c.getString(0))
                val m = models.optJSONObject(model.toString()) ?: models.optJSONObject(models.keys().next())
                val flds = m!!.getJSONArray("flds")
                (0 until flds.length()).map { flds.getJSONObject(it).optString("name") }
            }
        }.getOrDefault(emptyList())
    }

    private fun isPicture(b: ByteArray, n: Int): Boolean = n >= 4 && (
        (b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte()) ||
            (b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte() && b[2] == 'N'.code.toByte()) ||
            (b[0] == 'G'.code.toByte() && b[1] == 'I'.code.toByte() && b[2] == 'F'.code.toByte()) ||
            (n >= 12 && b[0] == 'R'.code.toByte() && b[8] == 'W'.code.toByte() && b[9] == 'E'.code.toByte() && b[10] == 'B'.code.toByte())
        )

    /**
     * A guess at the mapping: the front is the first field that is almost always filled, the back the next
     * such field (so a field holding only a sound, empty once cleaned, is skipped), the example the one after.
     */
    fun guess(d: Deck): Mapping {
        val good = d.filled.indices.filter { d.filled[it] >= 0.5f }.ifEmpty { d.filled.indices.filter { d.filled[it] > 0f } }
        val front = good.firstOrNull() ?: 0
        val back = good.firstOrNull { it != front } ?: (if (d.fieldCount > 1) 1 else 0)
        val example = good.firstOrNull { it != front && it != back } ?: -1
        return Mapping(front, back, example)
    }

    /** The finds a mapping makes; notes with nothing on the front are left out. */
    fun build(d: Deck, m: Mapping): List<Note> = d.notes.mapNotNull { n ->
        fun f(i: Int) = if (i < 0) "" else n.fields.getOrNull(i).orEmpty()
        if (n.cloze != null && m.front == 0) {
            return@mapNotNull if (n.cloze.isBlank()) null else Note(n.cloze, f(m.back).takeIf { m.back != 0 }.orEmpty(), Kind.PHRASE, f(m.example), n.picture)
        }
        val front = f(m.front)
        if (front.isBlank()) null else Note(front, if (m.back == m.front) "" else f(m.back), Kind.PAIR, if (m.example == m.front || m.example == m.back) "" else f(m.example), n.picture)
    }

    /** Kept for tests and simple decks: a note's fields with the default guess of first and second. */
    fun note(fields: List<String>, image: String = ""): Note? {
        val first = fields.firstOrNull().orEmpty()
        val raw = Raw(fields.map { plain(it) }, if (Regex("\\{\\{c\\d+::").containsMatchIn(first)) plain(cloze(first)) else null, image.ifBlank { null })
        return build(Deck("", fields.indices.map { "#$it" }, listOf(raw), File(".")), Mapping(0, 1, 2)).firstOrNull()
    }

    /** "{{c1::Paris::capital}} is…" → "[Paris] is…". */
    fun cloze(s: String): String = s.replace(Regex("\\{\\{c\\d+::(.*?)(::[^}]*)?\\}\\}")) { "[" + it.groupValues[1] + "]" }

    /** Field HTML as plain text: line breaks kept, tags, sounds and entities gone. */
    fun plain(html: String): String = html
        .replace(Regex("(?i)<br\\s*/?>|</div>|</p>"), "\n")
        .replace(Regex("\\[sound:[^]]*]"), "")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
        .lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")

    private fun displayName(context: Context, uri: Uri): String =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull().orEmpty().ifBlank { uri.lastPathSegment.orEmpty() }.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.').replace('_', ' ').trim().ifBlank { "Anki" }

    /**
     * Writes a mapped deck into a dig, a new one named [name] when [digId] is 0, bringing its pictures in,
     * and clears the unpacked deck. Returns the dig and the count.
     */
    suspend fun save(context: Context, dao: SteleDao, d: Deck, m: Mapping, name: String, digId: Long): Pair<Long, Int> {
        try {
            val notes = build(d, m)
            val now = System.currentTimeMillis()
            val dig = if (digId != 0L) digId else dao.insert(Dig(name = name.ifBlank { d.name }, symbol = 5, clay = (name.hashCode() and 7), createdAt = now))
            dao.insertFinds(notes.mapIndexed { i, n ->
                val image = n.picture?.let { File(d.dir, it) }?.takeIf { it.exists() }?.let { Images.keep(context, Uri.fromFile(it)) }.orEmpty()
                Find(digId = dig, kind = n.kind.name, front = n.front, back = n.back, example = n.example, image = image, origin = "Anki", createdAt = now + i)
            })
            return dig to notes.size
        } finally {
            drop(d)
        }
    }

    fun drop(d: Deck) {
        d.dir.deleteRecursively()
    }
}
