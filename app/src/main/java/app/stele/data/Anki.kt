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
 * Anki decks (.apkg and .colpkg): a zip around an SQLite collection. Notes come in as finds — the first
 * field as the front, the second as the back; cloze notes become phrases with [gaps]. Pictures come along
 * from older packages, whose media list is plain JSON.
 */
object Anki {
    class Note(val front: String, val back: String, val kind: Kind, val example: String = "", val image: String = "")

    class Deck(val name: String, val notes: List<Note>)

    fun read(context: Context, uri: Uri): Deck {
        val dir = File(context.cacheDir, "anki").apply { deleteRecursively(); mkdirs() }
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val e = zip.nextEntry ?: break
                        val name = File(e.name).name
                        if (!e.isDirectory && name.isNotEmpty()) File(dir, name).outputStream().use { zip.copyTo(it) }
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
            val notes = mutableListOf<Note>()
            SQLiteDatabase.openDatabase(db.path, null, SQLiteDatabase.OPEN_READONLY).use { sql ->
                sql.rawQuery("SELECT flds FROM notes ORDER BY id", null).use { c ->
                    while (c.moveToNext()) {
                        val fields = c.getString(0).split('\u001f')
                        val picture = fields.firstNotNullOfOrNull { f -> Regex("<img[^>]+src=\"([^\"]+)\"").find(f)?.groupValues?.get(1) }
                        val image = picture?.let { byName[it] }?.let { File(dir, it) }?.takeIf { it.exists() }?.let { f -> Images.keep(context, Uri.fromFile(f)) }.orEmpty()
                        note(fields, image)?.let { notes += it }
                    }
                }
            }
            return Deck(displayName(context, uri), notes)
        } finally {
            dir.deleteRecursively()
        }
    }

    /** One note as a find, or null when it has nothing to show. */
    fun note(fields: List<String>, image: String = ""): Note? {
        val clean = fields.map { plain(it) }
        val first = fields.firstOrNull().orEmpty()
        if (Regex("\\{\\{c\\d+::").containsMatchIn(first)) {
            val front = plain(cloze(first))
            return if (front.isBlank()) null else Note(front, clean.getOrNull(1).orEmpty(), Kind.PHRASE, image = image)
        }
        val front = clean.getOrNull(0).orEmpty()
        if (front.isBlank() && image.isBlank()) return null
        return Note(front.ifBlank { "?" }, clean.getOrNull(1).orEmpty(), Kind.PAIR, clean.drop(2).firstOrNull { it.isNotBlank() }.orEmpty(), image)
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

    /** Writes a deck into a dig: a new one named after the deck when [digId] is 0. Returns the dig and the count. */
    suspend fun save(dao: SteleDao, deck: Deck, digId: Long): Pair<Long, Int> {
        val now = System.currentTimeMillis()
        val dig = if (digId != 0L) digId else dao.insert(Dig(name = deck.name, symbol = 5, clay = (deck.name.hashCode() and 7), createdAt = now))
        dao.insertFinds(deck.notes.mapIndexed { i, n ->
            Find(digId = dig, kind = n.kind.name, front = n.front, back = n.back, example = n.example, image = n.image, origin = "Anki", createdAt = now + i)
        })
        return dig to deck.notes.size
    }
}
