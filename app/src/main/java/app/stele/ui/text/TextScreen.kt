package app.stele.ui.text

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stele.R
import app.stele.container
import app.stele.data.Find
import app.stele.data.Kind
import app.stele.domain.Rules
import app.stele.domain.Words
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.Handoff
import app.stele.ui.components.Bar
import app.stele.ui.components.Chip
import app.stele.ui.components.PrimaryButton
import app.stele.ui.components.SectionTitle
import app.stele.ui.components.SoftButton
import app.stele.ui.components.TopBar
import app.stele.ui.components.UCard
import app.stele.ui.components.UTextField
import app.stele.ui.components.title
import app.stele.ui.pluralStringResource
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType
import kotlin.math.roundToInt

/**
 * Sifting a text: paste any text and see which finds turn up in it (they get a real sentence and a seal)
 * and which of its words are not in the museum yet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextScreen(field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf(Handoff.text.also { Handoff.text = "" }) }
    var sifted by remember { mutableStateOf<Rules.Sifted?>(null) }
    var siftedText by remember { mutableStateOf("") }
    var picked by rememberSaveable { mutableStateOf(listOf<String>()) }
    var digId by rememberSaveable { mutableLongStateOf(field.settings.lastDig.takeIf { field.dig(it) != null } ?: field.digs.firstOrNull()?.id ?: 0L) }
    var confirmedNow by rememberSaveable { mutableStateOf(false) }

    fun sift() {
        sifted = Rules.sift(text, field.finds)
        siftedText = text
        picked = emptyList()
        confirmedNow = false
    }
    if (sifted == null && text.isNotBlank() && siftedText.isEmpty()) sift()

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.text_title), go::back)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            item {
                Text(stringResource(R.string.text_intro), style = UType.Sub)
                Spacer(Modifier.height(10.dp))
                UTextField(text, { text = it }, placeholder = stringResource(R.string.text_placeholder), minLines = 5)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftButton(stringResource(R.string.paste), { scope.launch { clipboard.getClipEntry()?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(c)?.toString()?.let { text = it } } }, Modifier.weight(1f), icon = Icons.Rounded.ContentPaste)
                    PrimaryButton(stringResource(R.string.text_sift), ::sift, Modifier.weight(1f), enabled = text.isNotBlank())
                }
            }
            val s = sifted ?: return@LazyColumn
            item {
                SectionTitle(stringResource(R.string.text_known_title))
                UCard {
                    Row {
                        Text("${(s.known * 100).roundToInt()}%", style = UType.Number.copy(fontSize = 40.sp, color = U.c.bronze))
                    }
                    Text(stringResource(R.string.text_known), style = UType.Muted)
                    Spacer(Modifier.height(8.dp))
                    Bar(s.known, color = U.c.bronze, track = U.c.bronzeSoft)
                }
            }
            if (s.found.isNotEmpty()) {
                item {
                    SectionTitle(pluralStringResource(R.plurals.text_found, s.found.size, s.found.size))
                    val stems = s.found.flatMap { (f, _) -> Words.tokens(if (f.kindOf == Kind.PHRASE) Words.plain(f.front) else f.front).filter { Words.isContent(it) }.map { Words.stem(it) } }.toSet()
                    UCard {
                        Text(
                            buildAnnotatedString {
                                var at = 0
                                Regex("[\\p{L}\\p{N}][\\p{L}\\p{N}'’-]*").findAll(siftedText.take(1500)).forEach { m ->
                                    append(siftedText.substring(at, m.range.first))
                                    if (stems.any { Words.sameStem(Words.stem(m.value), it) }) {
                                        pushStyle(SpanStyle(color = U.c.goldDeep, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
                                        append(m.value); pop()
                                    } else append(m.value)
                                    at = m.range.last + 1
                                }
                                if (at < siftedText.length.coerceAtMost(1500)) append(siftedText.substring(at, siftedText.length.coerceAtMost(1500)))
                                if (siftedText.length > 1500) append("…")
                            },
                            style = UType.Body,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    s.found.take(30).forEach { (f, sentence) ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            Text(f.title() + (if (f.back.isNotBlank()) " — " + f.back else ""), style = UType.Name.copy(fontSize = 18.sp))
                            Text("«$sentence»", style = UType.Muted, maxLines = 2)
                        }
                    }
                    val fresh = s.found.count { !it.first.confirmed }
                    Spacer(Modifier.height(8.dp))
                    if (confirmedNow || fresh == 0) {
                        Text(stringResource(R.string.text_confirmed), style = UType.Strong.copy(color = U.c.goldDeep))
                    } else {
                        PrimaryButton(pluralStringResource(R.plurals.text_confirm, fresh, fresh), {
                            val list = s.found.filter { !it.first.confirmed }
                            container.write { list.forEach { (f, sentence) -> keeper.confirm(f.id, sentence) } }
                            confirmedNow = true
                        }, Modifier.fillMaxWidth(), color = U.c.goldDeep)
                        Text(stringResource(R.string.text_confirm_hint), style = UType.Muted, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            if (s.fresh.isNotEmpty()) {
                item {
                    SectionTitle(stringResource(R.string.text_fresh))
                    Text(stringResource(R.string.text_fresh_hint), style = UType.Muted)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        s.fresh.forEach { w -> Chip(w, w in picked, { picked = if (w in picked) picked - w else picked + w }, U.c.clay) }
                    }
                    if (picked.isNotEmpty() && field.digs.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            field.digs.forEach { d -> Chip(d.name, d.id == digId, { digId = d.id }, U.clay(d.clay)) }
                        }
                        Spacer(Modifier.height(10.dp))
                        PrimaryButton(pluralStringResource(R.plurals.text_take, picked.size, picked.size), {
                            val now = System.currentTimeMillis()
                            val list = picked.mapIndexed { i, w -> Find(digId = digId, front = w, quote = s.sentenceOf(w), confirmedAt = now, createdAt = now + i) }
                            container.write { db.dao().insertFinds(list) }
                            Toast.makeText(c, c.resources.getQuantityString(R.plurals.text_taken, list.size, list.size), Toast.LENGTH_LONG).show()
                            picked = emptyList()
                        }, Modifier.fillMaxWidth())
                    } else if (field.digs.isEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        SoftButton(stringResource(R.string.dig_new), go::newDig)
                    }
                }
            }
        }
    }
}

