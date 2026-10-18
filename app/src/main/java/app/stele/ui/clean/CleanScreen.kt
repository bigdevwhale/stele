package app.stele.ui.clean

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stele.R
import app.stele.container
import app.stele.data.Find
import app.stele.data.Keeper
import app.stele.data.Kind
import app.stele.data.Mode
import app.stele.data.Vessel
import app.stele.domain.Grade
import app.stele.domain.Memory
import app.stele.domain.Rules
import app.stele.domain.Shape
import app.stele.domain.Stage
import app.stele.domain.Words
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.Source
import app.stele.ui.T
import app.stele.ui.art.Dust
import app.stele.ui.art.ShardArt
import app.stele.ui.art.VesselArt
import app.stele.ui.art.chisel
import app.stele.ui.components.Bar
import app.stele.ui.components.PrimaryButton
import app.stele.ui.components.SoftButton
import app.stele.ui.components.UTextField
import app.stele.ui.components.rememberPicture
import app.stele.ui.components.title
import app.stele.ui.pluralStringResource
import app.stele.ui.rememberVoice
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** What a finished find left behind, for the banner and the summary. */
private class Done(val find: Find, val outcome: Keeper.Outcome?, val grade: Grade)

/**
 * A cleaning: one find at a time. Recall it (writing it down if you like), stake one to three chisel
 * strikes on being right, then the brush sweeps the dust off the answer and you grade yourself.
 */
@Composable
fun CleanScreen(source: String, field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    val scope = rememberCoroutineScope()
    val voice = rememberVoice()
    val queue = remember {
        mutableStateListOf<Long>().apply {
            addAll(
                when {
                    source == Source.DUE -> field.round()
                    source == Source.WORKSHOP -> field.cracked
                    source == Source.SOUNDING -> field.sounding?.finds.orEmpty()
                    source.startsWith("dig-") -> field.round(digId = source.removePrefix("dig-").toLongOrNull() ?: 0)
                    source.startsWith("vessel-") -> field.round(vesselId = source.removePrefix("vessel-").toLongOrNull() ?: 0)
                    source.startsWith("find-") -> listOfNotNull(field.find(source.removePrefix("find-").toLongOrNull() ?: 0))
                    else -> emptyList()
                }.filter { it.askable }.map { it.id }
            )
        }
    }
    val rareId = remember { if (source == Source.SOUNDING) field.sounding?.rareId else null }
    var index by remember { mutableIntStateOf(0) }
    val again = remember { mutableStateListOf<Long>() }
    val done = remember { mutableStateListOf<Done>() }
    var busy by remember { mutableStateOf(false) }

    // The pit counts as dug once it has been opened at all.
    LaunchedEffect(Unit) {
        if (source == Source.SOUNDING) container.write { settings.update { it.copy(soundingDay = LocalDate.now().toEpochDay()) } }
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = go::back) { Icon(Icons.Rounded.Close, stringResource(R.string.back), tint = U.c.ink) }
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    stringResource(if (source == Source.SOUNDING) R.string.pit_title else if (source == Source.WORKSHOP) R.string.workshop_title else R.string.clean_title).uppercase(),
                    style = UType.Eyebrow,
                )
                Spacer(Modifier.height(4.dp))
                Bar(if (queue.isEmpty()) 1f else index / queue.size.toFloat(), color = U.c.clay, track = U.c.surface2)
            }
            if (index < queue.size) Text("${index + 1} / ${queue.size}", style = UType.Small, modifier = Modifier.padding(end = 12.dp))
        }
        val f = queue.getOrNull(index)?.let { field.find(it) }
        if (f == null) {
            Summary(done, queue.isEmpty(), go)
            return@Column
        }
        val dig = field.dig(f.digId)
        val lang = dig?.lang.orEmpty()
        val mode = remember(index, f.id) { Rules.mode(f, voice.speaks(lang)).let { if (it == Mode.ECHO && !voice.speaks(lang)) Mode.FORWARD else it } }
        Card(
            key = index, f = f, mode = mode, lang = lang, rare = f.id == rareId, field = field, last = done.lastOrNull(),
            speak = { t -> voice.say(t, lang) }, canSpeak = voice.speaks(lang), busy = busy,
        ) { grade, stake, answer ->
            if (busy) return@Card
            busy = true
            scope.launch {
                val out = withContext(Dispatchers.IO) { container.keeper.clean(f.id, mode, grade, stake, answer) }
                done += Done(f, out, grade)
                // Forgotten: once more at the end of the round, while it's still fresh.
                if (grade == Grade.AGAIN && f.id !in again && source != Source.find(f.id)) {
                    again += f.id
                    queue += f.id
                }
                index++
                busy = false
            }
        }
    }
}

@Composable
private fun Card(
    key: Int, f: Find, mode: Mode, lang: String, rare: Boolean, field: Field, last: Done?,
    speak: (String) -> Unit, canSpeak: Boolean, busy: Boolean, onGrade: (Grade, Int, String) -> Unit,
) {
    val c = LocalContext.current
    var typing by remember(key) { mutableStateOf(mode == Mode.ECHO) }
    var answer by remember(key) { mutableStateOf("") }
    var stake by remember(key) { mutableIntStateOf(-1) }
    val sweep = remember(key) { Animatable(0f) }
    val revealed = stake >= 0
    LaunchedEffect(key, revealed) {
        if (revealed) sweep.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(key) { if (mode == Mode.ECHO) speak(f.front) }
    val asked = when (mode) {
        Mode.REVERSE -> f.back
        else -> f.front
    }
    val expected = when (mode) {
        Mode.REVERSE, Mode.ECHO -> f.front
        Mode.GAP -> Words.pieces(f.front).filter { it.second }.joinToString(" ") { it.first }
        else -> f.back
    }
    val verdict = if (answer.isNotBlank()) Words.check(answer, expected) else null
    val suggested = when {
        verdict != null -> Words.suggest(verdict)
        stake == 0 -> Grade.AGAIN
        else -> null
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            last?.let { Banner(it) }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShardArt(if (rare) Stage.EXHIBIT else f.stage, Modifier.size(30.dp), patina = field.patina(f), cracked = f.cracked, seed = f.id.toInt())
                Spacer(Modifier.width(8.dp))
                Text(
                    (field.dig(f.digId)?.name.orEmpty() + " · " + T.mode(c, mode)).uppercase(), style = UType.Eyebrow.copy(color = U.c.muted),
                    modifier = Modifier.weight(1f),
                )
                if (rare) Text(stringResource(R.string.pit_rare_badge), style = UType.Eyebrow.copy(color = U.c.goldDeep))
            }
            Spacer(Modifier.height(24.dp))
            // The question.
            Column(
                Modifier.fillMaxWidth().clip(U.RLg).background(U.c.surface).border(1.dp, U.c.line, U.RLg).padding(horizontal = 20.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (mode) {
                    Mode.ECHO -> {
                        Box(Modifier.size(84.dp).clip(CircleShape).background(U.c.claySoft).clickable { speak(f.front) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.listen), Modifier.size(40.dp), tint = U.c.clay)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.echo_prompt), style = UType.Sub.copy(textAlign = TextAlign.Center))
                    }
                    Mode.GAP -> {
                        Text(
                            buildAnnotatedString {
                                Words.pieces(f.front).forEach { (t, gap) ->
                                    if (!gap) append(t)
                                    else if (revealed) { pushStyle(SpanStyle(color = U.c.clay)); append(t); pop() }
                                    else { pushStyle(SpanStyle(color = U.c.faint)); append("_".repeat(t.length.coerceIn(3, 10))); pop() }
                                }
                            },
                            style = UType.Find.copy(fontSize = 26.sp, lineHeight = 33.sp, textAlign = TextAlign.Center),
                        )
                        if (f.back.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(f.back, style = UType.Italic.copy(textAlign = TextAlign.Center))
                        }
                    }
                    else -> {
                        Text(asked, style = UType.Find.copy(textAlign = TextAlign.Center, fontSize = if (asked.length > 40) 24.sp else 34.sp, lineHeight = if (asked.length > 40) 30.sp else 39.sp))
                        if (mode == Mode.FORWARD && canSpeak && f.kindOf != Kind.QUESTION) {
                            IconButton(onClick = { speak(f.front) }) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.listen), tint = U.c.clay) }
                        }
                        Text(
                            stringResource(
                                when {
                                    mode == Mode.REVERSE && f.kindOf == Kind.TERM -> R.string.ask_term
                                    mode == Mode.REVERSE -> R.string.ask_front
                                    f.kindOf == Kind.QUESTION -> R.string.ask_answer
                                    f.kindOf == Kind.TERM -> R.string.ask_meaning
                                    else -> R.string.ask_back
                                }
                            ),
                            style = UType.Muted,
                        )
                    }
                }
                if (!revealed && f.hint.isNotBlank()) {
                    var hint by remember(key) { mutableStateOf(false) }
                    Spacer(Modifier.height(8.dp))
                    if (hint) Text(f.hint, style = UType.Italic.copy(textAlign = TextAlign.Center))
                    else Text(stringResource(R.string.show_hint), style = UType.Small.copy(color = U.c.ochre), modifier = Modifier.clip(U.RSm).clickable { hint = true }.padding(6.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            if (!revealed) {
                if (typing) {
                    UTextField(answer, { answer = it }, placeholder = stringResource(if (mode == Mode.GAP) R.string.type_gaps else R.string.type_answer))
                } else {
                    Row(
                        Modifier.clip(U.RSm).clickable { typing = true }.padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp), tint = U.c.clay)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.write_it), style = UType.Strong.copy(color = U.c.clay))
                    }
                }
            } else {
                // The answer, under the dust the brush sweeps away.
                Box(Modifier.fillMaxWidth().clip(U.RLg)) {
                    Column(
                        Modifier.fillMaxWidth().background(U.c.limestone).border(1.dp, U.c.line, U.RLg).padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(expected.ifBlank { f.back }, style = UType.Find.copy(fontSize = 28.sp, lineHeight = 34.sp, textAlign = TextAlign.Center, color = U.c.ink))
                        if (mode == Mode.ECHO && f.back.isNotBlank()) Text(f.back, style = UType.Italic.copy(textAlign = TextAlign.Center))
                        if (verdict != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(
                                    when (verdict) {
                                        Words.Verdict.RIGHT -> R.string.verdict_right
                                        Words.Verdict.ALMOST -> R.string.verdict_almost
                                        Words.Verdict.WRONG -> R.string.verdict_wrong
                                    }, answer.trim(),
                                ),
                                style = UType.Strong.copy(
                                    color = when (verdict) {
                                        Words.Verdict.RIGHT -> U.c.bronze
                                        Words.Verdict.ALMOST -> U.c.ochre
                                        Words.Verdict.WRONG -> U.c.danger
                                    },
                                    textAlign = TextAlign.Center,
                                ),
                            )
                        }
                        // The answer is the front itself: hear how it sounds.
                        if (canSpeak && (mode == Mode.REVERSE || mode == Mode.ECHO)) IconButton(onClick = { speak(f.front) }) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.listen), tint = U.c.clay) }
                        if (f.example.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text(f.example, style = UType.Italic.copy(textAlign = TextAlign.Center)) }
                        if (f.quote.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text("«${f.quote}»", style = UType.Muted.copy(textAlign = TextAlign.Center)) }
                        val pic = rememberPicture(f.image)
                        if (pic != null) {
                            Spacer(Modifier.height(10.dp))
                            Image(pic, null, Modifier.fillMaxWidth().heightIn(max = 180.dp).clip(U.RMd), contentScale = ContentScale.Crop)
                        }
                    }
                    Dust(sweep.value, Modifier.matchParentSize(), seed = f.id.toInt())
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.fillMaxWidth().background(U.c.surface).padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding()) {
            if (!revealed) {
                Text(stringResource(R.string.stake_title), style = UType.Strong)
                Text(stringResource(R.string.stake_text), style = UType.Muted)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StakeButton(0, stringResource(R.string.stake_0), Modifier.weight(1f)) { stake = 0 }
                    StakeButton(1, stringResource(R.string.stake_1), Modifier.weight(1f)) { stake = 1 }
                    StakeButton(2, stringResource(R.string.stake_2), Modifier.weight(1f)) { stake = 2 }
                    StakeButton(3, stringResource(R.string.stake_3), Modifier.weight(1f)) { stake = 3 }
                }
            } else {
                Text(stringResource(R.string.grade_title), style = UType.Strong)
                if (stake == 3 && suggested == Grade.AGAIN) Text(stringResource(R.string.grade_crack_warning), style = UType.Muted.copy(color = U.c.danger))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Grade.entries.forEach { g ->
                        val next = if (f.inGround) Memory.first(g, field.now) else Memory.review(f.trace, g, field.now)
                        val days = Memory.interval(next.stability, field.retention).coerceAtLeast(1.0 / 3)
                        GradeButton(
                            stringResource(
                                when (g) {
                                    Grade.AGAIN -> R.string.grade_again
                                    Grade.HARD -> R.string.grade_hard
                                    Grade.GOOD -> R.string.grade_good
                                    Grade.EASY -> R.string.grade_easy
                                }
                            ),
                            if (g == Grade.AGAIN) stringResource(R.string.grade_again_sub) else T.span(c, days),
                            when (g) {
                                Grade.AGAIN -> U.c.danger
                                Grade.HARD -> U.c.ochre
                                Grade.GOOD -> U.c.bronze
                                Grade.EASY -> U.c.goldDeep
                            },
                            g == suggested, busy || sweep.value < 0.6f, Modifier.weight(1f),
                        ) { onGrade(g, stake, answer.trim()) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StakeButton(n: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    val p = U.c
    Column(
        modifier.height(76.dp).clip(U.RMd).background(if (n == 0) p.surface2 else p.claySoft.copy(alpha = 0.4f + n * 0.2f)).clickable(onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        if (n > 0) {
            androidx.compose.foundation.Canvas(Modifier.size((14 * n + 12).dp, 26.dp)) {
                repeat(n) { i ->
                    val s = 14.dp.toPx()
                    chisel(p.clay, androidx.compose.ui.geometry.Rect(i * s, 0f, i * s + 26.dp.toPx(), 26.dp.toPx()))
                }
            }
        } else Text("?", style = UType.Number.copy(fontSize = 22.sp, color = p.muted))
        Spacer(Modifier.height(4.dp))
        Text(label, style = UType.Small.copy(color = if (n == 0) p.muted else p.clay, textAlign = TextAlign.Center), maxLines = 2)
    }
}

@Composable
private fun GradeButton(label: String, sub: String, color: Color, suggested: Boolean, disabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(64.dp).clip(U.RMd)
            .background(if (suggested) color.copy(alpha = 0.22f) else color.copy(alpha = 0.09f))
            .border(if (suggested) 1.5.dp else 0.dp, if (suggested) color else Color.Transparent, U.RMd)
            .clickable(enabled = !disabled, onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text(label, style = UType.Button.copy(color = color, fontSize = 14.sp), maxLines = 1)
        Text(sub, style = UType.Small.copy(fontSize = 11.sp), maxLines = 1)
    }
}

/** What the last find came to: a step up, a crack, a vessel made whole. */
@Composable
private fun Banner(d: Done) {
    val c = LocalContext.current
    val o = d.outcome ?: return
    val (text, color) = when {
        o.mended != null -> stringResource(R.string.banner_mended, o.mended.name) to U.c.goldDeep
        o.cracked -> stringResource(R.string.banner_cracked, d.find.title()) to U.c.danger
        o.after > o.before && o.before != Stage.GROUND -> stringResource(R.string.banner_up, d.find.title(), T.stage(c, o.after).lowercase(T.locale(c))) to U.stage(o.after)
        o.before == Stage.GROUND -> stringResource(R.string.banner_dug, d.find.title()) to U.c.ochre
        else -> return
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp).clip(U.RMd).background(color.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShardArt(o.after, Modifier.size(22.dp), cracked = o.cracked, seed = d.find.id.toInt())
        Spacer(Modifier.width(8.dp))
        Text(text, style = UType.Muted.copy(color = color))
    }
}

@Composable
private fun Summary(done: List<Done>, empty: Boolean, go: Go) {
    val c = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (empty) {
            Spacer(Modifier.height(40.dp))
            ShardArt(Stage.CLEANED, Modifier.size(80.dp))
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.clean_none), style = UType.Title.copy(textAlign = TextAlign.Center))
            Text(stringResource(R.string.clean_none_text), style = UType.Sub.copy(textAlign = TextAlign.Center))
            Spacer(Modifier.height(20.dp))
            PrimaryButton(stringResource(R.string.done), { go.back() }, Modifier.fillMaxWidth())
            return@Column
        }
        val finds = done.distinctBy { it.find.id }
        val ups = done.mapNotNull { d -> d.outcome?.takeIf { it.after > it.before && it.before != Stage.GROUND }?.let { d.find to it.after } }
        val mended: List<Vessel> = done.mapNotNull { it.outcome?.mended }
        val cracked = done.filter { it.outcome?.cracked == true }.map { it.find }
        val dug = done.count { it.outcome?.before == Stage.GROUND }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.summary_title), style = UType.Display.copy(textAlign = TextAlign.Center))
        Text(pluralStringResource(R.plurals.summary_cleaned, finds.size, finds.size), style = UType.Sub.copy(textAlign = TextAlign.Center))
        Spacer(Modifier.height(16.dp))
        mended.forEach { v ->
            Column(
                Modifier.fillMaxWidth().clip(U.RLg).background(U.c.goldSoft.copy(alpha = 0.5f)).border(1.5.dp, U.c.gold, U.RLg).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.summary_mended).uppercase(), style = UType.Eyebrow.copy(color = U.c.goldDeep))
                VesselArt(Shape.of(v.shape), List(6) { app.stele.ui.art.ShardLook(Stage.CLEANED) }, true, Modifier.size(150.dp), seed = v.id.toInt())
                Text(v.name, style = UType.Title)
                Text(stringResource(R.string.summary_mended_text), style = UType.Muted.copy(textAlign = TextAlign.Center))
            }
            Spacer(Modifier.height(12.dp))
        }
        if (dug > 0) Line(pluralStringResource(R.plurals.summary_dug, dug, dug), U.c.ochre)
        ups.forEach { (f, s) -> Line(stringResource(R.string.banner_up, f.title(), T.stage(c, s).lowercase(T.locale(c))), U.stage(s)) }
        cracked.forEach { f -> Line(stringResource(R.string.banner_cracked, f.title()), U.c.danger) }
        val right = done.count { it.grade.n >= Grade.GOOD.n }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.summary_right, right, done.size), style = UType.Muted)
        Spacer(Modifier.height(20.dp))
        PrimaryButton(stringResource(R.string.done), { go.back() }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SoftButton(stringResource(R.string.new_find), { go.back(); go.add() }, Modifier.fillMaxWidth())
    }
}

@Composable
private fun Line(text: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(text, style = UType.Body)
    }
}
