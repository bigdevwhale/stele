package app.stele.ui.find

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stele.R
import app.stele.container
import app.stele.data.Anki
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.components.Hint
import app.stele.ui.components.Hints
import app.stele.ui.components.PrimaryButton
import app.stele.ui.components.SectionTitle
import app.stele.ui.components.TopBar
import app.stele.ui.components.UTextField
import app.stele.ui.pluralStringResource
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * An Anki deck on its way in. Reading and saving run on the app's own scope, so leaving the screen, or the
 * app, doesn't cut an import of thousands of notes short.
 */
object AnkiImport {
    var reading by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set
    /** A deck read and waiting for its fields to be mapped. */
    var deck by mutableStateOf<Anki.Deck?>(null)
        private set
    /** The dig it goes into; 0 for a new one. */
    var into: Long = 0
        private set
    /** A finished import: the dig and how many finds, for the screen to open once. */
    var saved by mutableStateOf<Pair<Long, Int>?>(null)

    fun start(context: Context, uri: Uri, digId: Long) {
        if (reading || saving) return
        val app = context.applicationContext
        reading = true
        into = digId
        app.container.scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { Anki.read(app, uri) } }
            withContext(Dispatchers.Main) {
                reading = false
                r.onSuccess { d ->
                    if (d.notes.isEmpty()) { Anki.drop(d); Toast.makeText(app, R.string.anki_empty, Toast.LENGTH_LONG).show() } else deck = d
                }.onFailure { Toast.makeText(app, R.string.anki_failed, Toast.LENGTH_LONG).show() }
            }
        }
    }

    fun save(context: Context, m: Anki.Mapping, name: String) {
        val d = deck ?: return
        val app = context.applicationContext
        saving = true
        app.container.scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { Anki.save(app, app.container.db.dao(), d, m, name, into) } }
            withContext(Dispatchers.Main) {
                saving = false
                deck = null
                r.onSuccess { saved = it }.onFailure { Toast.makeText(app, R.string.anki_failed, Toast.LENGTH_LONG).show() }
            }
        }
    }

    fun cancel() {
        deck?.let { Anki.drop(it) }
        deck = null
    }
}

/** Which field of the deck is the front, which the back, which an example; with a preview of what that makes. */
@Composable
fun AnkiScreen(field: Field, go: Go) {
    val c = LocalContext.current
    val d = AnkiImport.deck
    if (d == null) {
        // Saving, or saved and on the way into the dig (the root opens it); nothing here navigates by itself.
        Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
            TopBar(stringResource(R.string.anki_title), go::back)
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (AnkiImport.saving) CircularProgressIndicator(color = U.c.clay)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.anki_saving), style = UType.Sub)
            }
        }
        return
    }
    val guess = remember(d) { Anki.guess(d) }
    var front by rememberSaveable { mutableIntStateOf(guess.front) }
    var back by rememberSaveable { mutableIntStateOf(guess.back) }
    var example by rememberSaveable { mutableIntStateOf(guess.example) }
    var name by rememberSaveable { mutableStateOf(d.name) }
    val mapping = Anki.Mapping(front, back, example)
    val preview = remember(d, mapping) { Anki.build(d, mapping) }
    val leave = { AnkiImport.cancel(); go.back() }
    BackHandler { leave() }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.anki_title), { leave() })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            item {
                Hint(Hints.ANKI, field)
                Text(pluralStringResource(R.plurals.anki_notes, d.notes.size, d.notes.size), style = UType.Strong)
                if (AnkiImport.into == 0L) {
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.dig_name), style = UType.Muted)
                    Spacer(Modifier.height(4.dp))
                    UTextField(name, { name = it })
                } else {
                    Text(stringResource(R.string.anki_into, field.dig(AnkiImport.into)?.name.orEmpty()), style = UType.Muted)
                }
                SectionTitle(stringResource(R.string.anki_front))
                Fields(d, front, allowNone = false) { front = it }
                SectionTitle(stringResource(R.string.anki_back))
                Fields(d, back, allowNone = true) { back = it }
                SectionTitle(stringResource(R.string.anki_example))
                Fields(d, example, allowNone = true) { example = it }
                SectionTitle(pluralStringResource(R.plurals.anki_preview, preview.size, preview.size))
            }
            items(preview.take(6)) { n ->
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(n.front, style = UType.Name.copy(fontSize = 19.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(n.back.ifBlank { stringResource(R.string.unlabelled) }, style = UType.Muted.copy(color = if (n.back.isBlank()) U.c.clay else U.c.muted), maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (n.example.isNotBlank()) Text(n.example, style = UType.Small, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        PrimaryButton(
            if (AnkiImport.saving) stringResource(R.string.anki_saving) else pluralStringResource(R.plurals.bulk_save, preview.size, preview.size),
            { AnkiImport.save(c, mapping, name.trim()) },
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding(),
            enabled = preview.isNotEmpty() && !AnkiImport.saving,
        )
    }
}

/** The deck's fields as a list to pick one from, each with what it holds in the first note. */
@Composable
private fun Fields(d: Anki.Deck, selected: Int, allowNone: Boolean, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(U.RMd).border(1.dp, U.c.line, U.RMd).background(U.c.surface)) {
        if (allowNone) Option(stringResource(R.string.anki_none), "", selected == -1) { onPick(-1) }
        d.fieldNames.forEachIndexed { i, name ->
            val empty = d.filled.getOrElse(i) { 0f } == 0f
            Option(name, if (empty) stringResource(R.string.anki_field_empty) else d.sample(i), selected == i) { onPick(i) }
        }
    }
}

@Composable
private fun Option(name: String, sample: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(if (on) U.c.claySoft else U.c.surface).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = UType.Strong.copy(color = if (on) U.c.clay else U.c.ink), modifier = Modifier.width(96.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        Text(sample.replace('\n', ' '), style = UType.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}
