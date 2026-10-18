package app.stele.ui.find

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stele.R
import app.stele.ui.components.Hint
import app.stele.ui.components.Hints
import app.stele.container
import app.stele.data.Images
import app.stele.data.Kind
import app.stele.data.Mode
import app.stele.domain.Grade
import app.stele.domain.Memory
import app.stele.domain.Stage
import app.stele.domain.Words
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.Source
import app.stele.ui.T
import app.stele.ui.art.ShardArt
import app.stele.ui.components.Chisels
import app.stele.ui.components.ConfirmDialog
import app.stele.ui.components.SectionTitle
import app.stele.ui.components.SoftButton
import app.stele.ui.components.StageBadge
import app.stele.ui.components.TopBar
import app.stele.ui.components.UCard
import app.stele.ui.components.rememberPicture
import app.stele.ui.components.title
import app.stele.ui.rememberVoice
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType
import kotlin.math.roundToInt

@Composable
fun FindScreen(id: Long, field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    val f = field.find(id) ?: return
    val dig = field.dig(f.digId)
    val voice = rememberVoice()
    var deleting by rememberSaveable { mutableStateOf(false) }
    val stage = f.stage
    val patina = field.patina(f)
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(dig?.name.orEmpty(), go::back) {
            IconButton(onClick = { go.edit(id) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.find_edit), tint = U.c.muted) }
            IconButton(onClick = { deleting = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = U.c.muted) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            item { Hint(Hints.FIND, field) }
            item {
                Column(
                    Modifier.fillMaxWidth().clip(U.RLg).background(U.c.surface).border(1.dp, if (stage == Stage.EXHIBIT) U.c.gold else U.c.line, U.RLg).padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShardArt(stage, Modifier.size(56.dp), patina = patina, cracked = f.cracked, seed = f.id.toInt())
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            StageBadge(stage)
                            Spacer(Modifier.height(4.dp))
                            Text(T.kind(c, f.kindOf) + (if (f.cracked) " · " + stringResource(R.string.cracked) else ""), style = UType.Small)
                        }
                        if (dig != null && voice.speaks(dig.lang)) {
                            IconButton(onClick = { voice.say(f.title(), dig.lang) }) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.listen), tint = U.c.clay) }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    if (f.kindOf == Kind.PHRASE) {
                        Text(gapsShown(f.front), style = UType.Find.copy(fontSize = 26.sp, lineHeight = 32.sp))
                    } else {
                        Text(f.front, style = UType.Find)
                    }
                    if (f.back.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(f.back, style = UType.Italic.copy(fontSize = 21.sp, lineHeight = 26.sp, color = U.c.ink))
                    } else if (!f.askable) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.unlabelled_text), style = UType.Muted.copy(color = U.c.clay))
                        Spacer(Modifier.height(8.dp))
                        SoftButton(stringResource(R.string.label_it), { go.edit(id) })
                    }
                }
            }
            item {
                SectionTitle(stringResource(R.string.find_way))
                UCard {
                    Ladder(stage)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.find_stage, T.stage(c, stage), stage.ordinal + 1), style = UType.Strong.copy(color = if (stage == Stage.EXHIBIT) U.c.goldDeep else U.stage(stage)))
                    Spacer(Modifier.height(10.dp))
                    Text(T.stageHint(c, stage), style = UType.Sub)
                    if (!f.inGround) {
                        Spacer(Modifier.height(10.dp))
                        val r = (f.recall(field.now) * 100).roundToInt()
                        val nextDays = (f.due(field.retention) - field.now) / Memory.DAY.toDouble()
                        Text(stringResource(R.string.find_recall, r), style = UType.Strong)
                        Text(
                            if (nextDays <= 0) stringResource(R.string.find_dusty_now) else stringResource(R.string.find_next, T.inTime(c, nextDays)),
                            style = UType.Muted,
                        )
                        stage.next?.let { nx ->
                            Text(stringResource(R.string.find_holds, T.span(c, f.stability), T.stage(c, nx), T.span(c, nx.fromDays)), style = UType.Muted)
                        }
                    }
                    if (f.askable) {
                        Spacer(Modifier.height(12.dp))
                        SoftButton(stringResource(R.string.clean_now), { go.clean(Source.find(id)) })
                    }
                }
            }
            if (f.example.isNotBlank() || f.hint.isNotBlank() || f.origin.isNotBlank() || f.quote.isNotBlank() || f.image.isNotBlank()) {
                item {
                    SectionTitle(stringResource(R.string.find_notes))
                    UCard {
                        val pic = rememberPicture(f.image)
                        if (pic != null) {
                            Image(pic, null, Modifier.fillMaxWidth(1f).aspectRatio((pic.width.toFloat() / pic.height).coerceAtLeast(0.75f)).clip(U.RMd), contentScale = ContentScale.Fit)
                            Spacer(Modifier.height(12.dp))
                        }
                        Note(stringResource(R.string.example), f.example)
                        Note(stringResource(R.string.hint), f.hint)
                        Note(stringResource(R.string.origin), f.origin)
                        if (f.quote.isNotBlank()) {
                            Text(stringResource(R.string.quote).uppercase(), style = UType.Eyebrow.copy(color = U.c.goldDeep))
                            Text("«" + f.quote + "»", style = UType.Italic.copy(color = U.c.ink))
                            f.confirmedAt?.let { Text(stringResource(R.string.confirmed_on, T.date(c, it)), style = UType.Small.copy(color = U.c.goldDeep)) }
                        }
                    }
                }
            }
            item {
                SectionTitle(stringResource(R.string.links))
                val links = field.linksOf(id)
                if (links.isEmpty()) Text(stringResource(R.string.links_none), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
                links.forEach { l ->
                    val other = field.find(l.other(id)) ?: return@forEach
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(T.link(c, l.kindOf), style = UType.Small.copy(color = U.c.bronze), modifier = Modifier.width(96.dp))
                        Column(Modifier.weight(1f)) {
                            Text(other.title(), style = UType.Name.copy(fontSize = 18.sp), modifier = Modifier.padding(vertical = 2.dp))
                            if (other.back.isNotBlank()) Text(other.back, style = UType.Muted, maxLines = 1)
                        }
                        IconButton(onClick = { container.write { db.dao().delete(l) } }) { Icon(Icons.Rounded.Close, stringResource(R.string.link_remove), tint = U.c.faint) }
                    }
                }
                Spacer(Modifier.height(6.dp))
                SoftButton(stringResource(R.string.link_add), { go.link(id) }, icon = Icons.Rounded.Add, color = U.c.bronze, soft = U.c.bronzeSoft)
            }
            val history = field.reviewsOf(id)
            if (history.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.history)) }
                items(history.take(30), key = { it.id }) { r ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(if (r.grade >= Grade.GOOD.n) U.c.bronze else if (r.grade == Grade.HARD.n) U.c.ochre else U.c.danger))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(T.date(c, r.at) + " · " + T.mode(c, Mode.of(r.mode)), style = UType.Body)
                            if (r.answer.isNotBlank()) Text("«${r.answer}»", style = UType.Muted.copy(fontStyle = FontStyle.Italic), maxLines = 1)
                        }
                        if (r.stake > 0) Chisels(r.stake, U.c.faint)
                    }
                }
            }
        }
    }
    if (deleting) {
        ConfirmDialog(
            stringResource(R.string.find_delete), stringResource(R.string.find_delete_text), stringResource(R.string.delete),
            onConfirm = {
                container.write { db.dao().deleteFind(f); Images.drop(c, f.image) }
                go.back()
            },
            onDismiss = { deleting = false }, danger = true,
        )
    }
}

@Composable
private fun Note(label: String, text: String) {
    if (text.isBlank()) return
    Text(label.uppercase(), style = UType.Eyebrow)
    Text(text, style = UType.Body)
    Spacer(Modifier.height(12.dp))
}

/** A phrase with its gaps underlined in clay. */
@Composable
fun gapsShown(phrase: String) = androidx.compose.ui.text.buildAnnotatedString {
    Words.pieces(phrase).forEach { (t, gap) ->
        if (gap) {
            pushStyle(androidx.compose.ui.text.SpanStyle(color = U.c.clay, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
            append(t)
            pop()
        } else append(t)
    }
}

/** The five stages as steps, the current one lit. */
@Composable
private fun Ladder(stage: Stage) {
    val c = LocalContext.current
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Stage.entries.forEach { s ->
            val reached = s <= stage
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(if (reached) U.stage(s) else U.c.surface2))
            }
        }
    }
}
