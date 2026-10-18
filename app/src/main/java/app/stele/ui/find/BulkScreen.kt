package app.stele.ui.find

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stele.R
import app.stele.ui.components.Hint
import app.stele.ui.components.Hints
import app.stele.container
import app.stele.data.Find
import app.stele.data.Kind
import app.stele.data.Lines
import app.stele.data.Link
import app.stele.data.LinkKind
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.T
import app.stele.ui.components.Chip
import app.stele.ui.components.PrimaryButton
import app.stele.ui.components.Segmented
import app.stele.ui.components.TopBar
import app.stele.ui.components.UTextField
import app.stele.ui.components.title
import app.stele.ui.pluralStringResource
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType

/** Many finds at once, one per line: "casa — house". */
@Composable
fun BulkScreen(digArg: Long, field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    var digId by rememberSaveable { mutableLongStateOf(digArg.takeIf { field.dig(it) != null } ?: field.digs.firstOrNull()?.id ?: 0L) }
    var kind by rememberSaveable { mutableStateOf(Kind.PAIR) }
    var text by rememberSaveable { mutableStateOf("") }
    val rows = Lines.parse(text)
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.bulk_title), go::back)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp)) {
            item {
                Hint(Hints.BULK, field)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    field.digs.forEach { d -> Chip(d.name, d.id == digId, { digId = d.id }, U.clay(d.clay)) }
                }
                Spacer(Modifier.height(10.dp))
                Segmented(listOf(Kind.PAIR, Kind.TERM, Kind.QUESTION), kind, { T.kind(c, it) }, { kind = it })
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.bulk_text), style = UType.Sub)
                Spacer(Modifier.height(8.dp))
                UTextField(text, { text = it }, placeholder = stringResource(R.string.bulk_placeholder), minLines = 8)
                Spacer(Modifier.height(12.dp))
                if (rows.isNotEmpty()) Text(pluralStringResource(R.plurals.bulk_preview, rows.size, rows.size).uppercase(), style = UType.Eyebrow)
                Spacer(Modifier.height(6.dp))
            }
            items(rows.take(50)) { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(r.front, style = UType.Name.copy(fontSize = 17.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(10.dp))
                    Text(r.back.ifBlank { stringResource(R.string.unlabelled) }, style = UType.Muted.copy(color = if (r.back.isBlank()) U.c.clay else U.c.muted), modifier = Modifier.weight(1f), maxLines = 1)
                }
            }
        }
        PrimaryButton(
            pluralStringResource(R.plurals.bulk_save, rows.size, rows.size),
            {
                val now = System.currentTimeMillis()
                val list = rows.mapIndexed { i, r -> Find(digId = digId, kind = kind.name, front = r.front, back = r.back, createdAt = now + i) }
                container.write {
                    db.dao().insertFinds(list)
                    settings.update { it.copy(lastDig = digId) }
                }
                Toast.makeText(c, c.resources.getQuantityString(R.plurals.csv_imported, list.size, list.size), Toast.LENGTH_SHORT).show()
                go.back()
            },
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding(),
            enabled = rows.isNotEmpty() && digId != 0L,
        )
    }
}

/** Ties a find to another: kin, confused with, or opposite. */
@Composable
fun LinkScreen(id: Long, field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    val f = field.find(id) ?: return
    var kind by rememberSaveable { mutableStateOf(LinkKind.KIN) }
    var query by rememberSaveable { mutableStateOf("") }
    val linked = field.linksOf(id).map { it.other(id) }.toSet()
    val pool = field.finds.filter { it.id != id && it.id !in linked }
        .filter { query.isBlank() || it.title().contains(query, true) || it.back.contains(query, true) }
        // The same dig first, then the rest.
        .sortedWith(compareBy<Find> { if (it.digId == f.digId) 0 else 1 }.thenBy { it.title().lowercase() })
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.link_add), go::back)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Hint(Hints.LINK, field)
            Text(f.title(), style = UType.Name)
            Spacer(Modifier.height(10.dp))
            Segmented(LinkKind.entries, kind, { T.link(c, it) }, { kind = it })
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    when (kind) {
                        LinkKind.KIN -> R.string.link_kin_hint
                        LinkKind.CONFUSED -> R.string.link_confused_hint
                        LinkKind.OPPOSITE -> R.string.link_opposite_hint
                    }
                ),
                style = UType.Muted,
            )
            Spacer(Modifier.height(10.dp))
            UTextField(query, { query = it }, placeholder = stringResource(R.string.search))
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            items(pool.take(200), key = { it.id }) { o ->
                Row(
                    Modifier.fillMaxWidth().clip(U.RMd).clickable {
                        container.write { db.dao().insert(Link(a = id, b = o.id, kind = kind.name, createdAt = System.currentTimeMillis())) }
                        go.back()
                    }.padding(vertical = 10.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.RadioButtonUnchecked, null, tint = U.c.faint)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(o.title(), style = UType.Name.copy(fontSize = 18.sp))
                        Text(listOf(o.back, field.dig(o.digId)?.name.orEmpty()).filter { it.isNotBlank() }.joinToString(" · "), style = UType.Muted, maxLines = 1)
                    }
                }
            }
        }
    }
}
