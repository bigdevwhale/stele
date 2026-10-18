package app.stele

import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.stele.data.Anki
import app.stele.data.Kind
import io.airlift.compress.zstd.ZstdCompressor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnkiTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun fieldsBecomePlainText() {
        assertEquals("casa\nhouse", Anki.plain("<div>casa</div><br>house&nbsp;"))
        assertEquals("perro", Anki.plain("perro[sound:perro.mp3]"))
        assertEquals("[Paris] is the capital of [France]", Anki.cloze("{{c1::Paris}} is the capital of {{c2::France::country}}"))
        val n = Anki.note(listOf("{{c1::Ottawa}} is the capital of Canada", "Not Toronto"))!!
        assertEquals(Kind.PHRASE, n.kind)
        assertEquals("[Ottawa] is the capital of Canada", n.front)
        assertEquals(Kind.PAIR, Anki.note(listOf("<b>gato</b>", "cat", "El gato duerme."))!!.kind)
        assertEquals("El gato duerme.", Anki.note(listOf("gato", "cat", "El gato duerme."))!!.example)
    }

    private fun collection(file: File, notes: List<String>) {
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL("CREATE TABLE notes (id INTEGER PRIMARY KEY, flds TEXT)")
            notes.forEachIndexed { i, f -> db.execSQL("INSERT INTO notes (id, flds) VALUES (?, ?)", arrayOf<Any>(i + 1, f)) }
        }
    }

    private fun pack(name: String, entries: Map<String, ByteArray>): Uri {
        val f = File(context.cacheDir, name)
        ZipOutputStream(f.outputStream()).use { z -> entries.forEach { (n, b) -> z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry() } }
        return Uri.fromFile(f)
    }

    @Test
    fun readsAnOldPackage() {
        val db = File(context.cacheDir, "c.anki2")
        collection(db, listOf("casa\u001fhouse", "perro\u001fdog", "{{c1::Ojalá}} llueva\u001fhopefully"))
        val deck = Anki.read(context, pack("Spanish_words.apkg", mapOf("collection.anki2" to db.readBytes(), "media" to "{}".toByteArray())))
        assertEquals(3, deck.notes.size)
        assertEquals("house", deck.notes[0].back)
        assertEquals("[Ojalá] llueva", deck.notes[2].front)
        assertEquals("Spanish words", deck.name)
    }

    @Test
    fun readsTheNewestZstdPackageOverItsPlaceholder() {
        val real = File(context.cacheDir, "real.db")
        collection(real, listOf("hola\u001fhello"))
        val placeholder = File(context.cacheDir, "old.db")
        collection(placeholder, listOf("Please update to the latest Anki version\u001f"))
        val raw = real.readBytes()
        val z = ZstdCompressor()
        val out = ByteArray(z.maxCompressedLength(raw.size))
        val n = z.compress(raw, 0, raw.size, out, 0, out.size)
        val deck = Anki.read(context, pack("new.apkg", mapOf("collection.anki2" to placeholder.readBytes(), "collection.anki21b" to out.copyOf(n))))
        assertEquals(listOf("hola"), deck.notes.map { it.front })
    }

    @Test
    fun aZipWithoutACollectionIsRefused() {
        val r = runCatching { Anki.read(context, pack("x.apkg", mapOf("readme.txt" to "hi".toByteArray()))) }
        assertTrue(r.isFailure)
    }
}
