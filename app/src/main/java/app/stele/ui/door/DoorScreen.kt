package app.stele.ui.door

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stele.R
import app.stele.container
import app.stele.data.Kind
import app.stele.data.Mode
import app.stele.domain.Grade
import app.stele.domain.Hall
import app.stele.domain.Memory
import app.stele.domain.Rules
import app.stele.domain.Words
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.T
import app.stele.ui.art.SealedDoor
import app.stele.ui.components.Bar
import app.stele.ui.components.PrimaryButton
import app.stele.ui.components.SoftButton
import app.stele.ui.components.TopBar
import app.stele.ui.components.title
import app.stele.ui.pluralStringResource
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType

/**
 * The sealed door: once a month, questions from every dig with no hints and no stakes. Three in four
 * right and it opens onto a new hall of the museum.
 */
@Composable
fun DoorScreen(field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    val questions = remember { Rules.doorQuestions(field.finds, field.now / Memory.DAY) }
    var started by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }
    var shown by remember { mutableStateOf(false) }
    var right by remember { mutableIntStateOf(0) }
    val open = remember { Animatable(0f) }
    val halls = remember { field.settings.halls }
    val target = (halls + 1).coerceAtMost(Hall.entries.size - 1)
    val passed = questions.isNotEmpty() && right >= kotlin.math.ceil(questions.size * Rules.DOOR_PASS).toInt()
    val finished = started && index >= questions.size

    LaunchedEffect(finished) {
        if (!finished) return@LaunchedEffect
        val now = System.currentTimeMillis()
        container.write {
            settings.update {
                it.copy(
                    // A door that didn't give can be tried again in a week.
                    doorAt = if (passed) now else now - (Rules.DOOR_EVERY_DAYS - 7) * Memory.DAY,
                    doorScore = right, doorOf = questions.size,
                    halls = if (passed) (it.halls + 1).coerceAtMost(Hall.entries.size - 1) else it.halls,
                )
            }
        }
        if (passed) open.animateTo(1f, tween(1800))
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.door_title), go::back)
        if (started && !finished) Bar(index / questions.size.toFloat(), Modifier.padding(horizontal = 16.dp), color = U.c.ochre, track = U.c.surface2)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                !started -> {
                    SealedDoor(0f, Modifier.size(200.dp, 240.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.door_intro_title), style = UType.Title.copy(textAlign = TextAlign.Center))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.door_intro, questions.size), style = UType.Sub.copy(textAlign = TextAlign.Center))
                    if (halls + 1 < Hall.entries.size) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.door_behind, T.hall(c, Hall.entries[halls + 1])), style = UType.Italic.copy(textAlign = TextAlign.Center))
                    }
                }
                finished -> {
                    SealedDoor(open.value, Modifier.size(200.dp, 240.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("$right / ${questions.size}", style = UType.Display)
                    Text(
                        if (passed) stringResource(R.string.door_opened, T.hall(c, Hall.entries[target]))
                        else stringResource(R.string.door_held),
                        style = UType.Sub.copy(textAlign = TextAlign.Center),
                    )
                }
                else -> {
                    val f = questions[index]
                    val reverse = f.kindOf == Kind.PAIR && index % 2 == 1
                    Text(field.dig(f.digId)?.name.orEmpty().uppercase(), style = UType.Eyebrow.copy(color = U.c.muted))
                    Spacer(Modifier.height(10.dp))
                    Column(
                        Modifier.fillMaxWidth().clip(U.RLg).background(U.c.surface).border(1.dp, U.c.line, U.RLg).padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val clay = U.c.clay
                        val ask = when {
                            f.kindOf == Kind.PHRASE -> androidx.compose.ui.text.buildAnnotatedString {
                                Words.pieces(f.front).forEach { (t, gap) ->
                                    when {
                                        !gap -> append(t)
                                        shown -> { pushStyle(androidx.compose.ui.text.SpanStyle(color = clay)); append(t); pop() }
                                        else -> append("_".repeat(t.length.coerceIn(3, 10)))
                                    }
                                }
                            }
                            reverse -> androidx.compose.ui.text.AnnotatedString(f.back)
                            else -> androidx.compose.ui.text.AnnotatedString(f.front)
                        }
                        Text(ask, style = UType.Find.copy(textAlign = TextAlign.Center, fontSize = if (ask.length > 40) 24.sp else 32.sp))
                        if (shown && f.kindOf != Kind.PHRASE) {
                            Spacer(Modifier.height(14.dp))
                            Text(if (reverse) f.title() else f.back, style = UType.Italic.copy(fontSize = 22.sp, color = U.c.clay, textAlign = TextAlign.Center))
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding()) {
            when {
                !started -> PrimaryButton(stringResource(R.string.door_knock), { started = true }, Modifier.fillMaxWidth(), enabled = questions.isNotEmpty(), color = U.c.ochre)
                finished -> PrimaryButton(stringResource(R.string.done), { go.back() }, Modifier.fillMaxWidth())
                !shown -> PrimaryButton(stringResource(R.string.show_answer), { shown = true }, Modifier.fillMaxWidth())
                else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    fun answer(ok: Boolean) {
                        val f = questions[index]
                        container.write { keeper.clean(f.id, Mode.DOOR, if (ok) Grade.GOOD else Grade.AGAIN, 0, "") }
                        if (ok) right++
                        shown = false
                        index++
                    }
                    SoftButton(stringResource(R.string.door_forgot), { answer(false) }, Modifier.weight(1f), color = U.c.danger, soft = U.c.claySoft)
                    PrimaryButton(stringResource(R.string.door_knew), { answer(true) }, Modifier.weight(1f), color = U.c.bronze)
                }
            }
            if (started && !finished) {
                Spacer(Modifier.height(6.dp))
                Text(pluralStringResource(R.plurals.door_left, questions.size - index, questions.size - index), style = UType.Small, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}
