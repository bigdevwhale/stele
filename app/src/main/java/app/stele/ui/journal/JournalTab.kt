package app.stele.ui.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stele.R
import app.stele.data.Mode
import app.stele.domain.Grade
import app.stele.domain.Hall
import app.stele.domain.Rules
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.T
import app.stele.ui.components.Bar
import app.stele.ui.components.Chisels
import app.stele.ui.components.SectionTitle
import app.stele.ui.components.Tally
import app.stele.ui.components.UCard
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

@Composable
fun JournalTab(field: Field, go: Go) {
    val c = LocalContext.current
    val cleanings = field.reviews.filter { it.mode != Mode.FORGERY.name }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    ) {
        item {
            Text(stringResource(R.string.journal_eyebrow).uppercase(), style = UType.Eyebrow)
            Text(stringResource(R.string.journal_title), style = UType.Display)
            Spacer(Modifier.height(14.dp))
            UCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Tally("${field.fieldDays.size}", stringResource(R.string.journal_days))
                    Tally("${field.finds.size}", stringResource(R.string.journal_finds))
                    Tally("${cleanings.size}", stringResource(R.string.journal_cleanings))
                    Tally("${field.mendedVessels.size}", stringResource(R.string.journal_vessels), color = U.c.goldDeep)
                }
            }
        }
        item {
            SectionTitle(stringResource(R.string.journal_field))
            UCard { Calendar(field) }
        }
        item {
            SectionTitle(stringResource(R.string.journal_forecast))
            UCard {
                val f = Rules.forecast(field.finds, field.now, field.retention, 7, field.today, field::day)
                Forecast(f, field)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.journal_forecast_text), style = UType.Muted)
            }
        }
        item {
            SectionTitle(stringResource(R.string.journal_eye))
            UCard {
                Text(stringResource(R.string.journal_eye_text), style = UType.Muted)
                Spacer(Modifier.height(10.dp))
                val cal = Rules.calibration(cleanings)
                (1..3).forEach { s ->
                    val (ok, n) = cal.getValue(s)
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(66.dp)) { Chisels(s, U.c.clay) }
                        Column(Modifier.weight(1f)) {
                            Bar(if (n == 0) 0f else ok / n.toFloat(), color = U.c.clay, track = U.c.claySoft)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(if (n == 0) "—" else "${(ok * 100f / n).roundToInt()}%", style = UType.Strong, modifier = Modifier.width(48.dp))
                    }
                }
                val (ok3, n3) = cal.getValue(3)
                if (n3 >= 5) {
                    Spacer(Modifier.height(6.dp))
                    val share = ok3 / n3.toFloat()
                    Text(
                        stringResource(if (share >= 0.9f) R.string.journal_eye_sharp else if (share >= 0.75f) R.string.journal_eye_fair else R.string.journal_eye_over),
                        style = UType.Sub,
                    )
                }
            }
        }
        item {
            SectionTitle(stringResource(R.string.journal_halls))
            UCard {
                Hall.entries.forEach { h ->
                    val open = h.ordinal <= field.settings.halls
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(if (open) U.c.gold else U.c.surface2))
                        Spacer(Modifier.width(10.dp))
                        Text(T.hall(c, h), style = UType.Body.copy(color = if (open) U.c.ink else U.c.faint))
                    }
                }
                if (field.settings.doorScore >= 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.journal_door_last, field.settings.doorScore, field.settings.doorOf, T.date(c, field.settings.doorAt)), style = UType.Muted)
                }
            }
        }
        item {
            val right = cleanings.count { it.grade >= Grade.GOOD.n }
            if (cleanings.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.journal_right, (right * 100f / cleanings.size).roundToInt()), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

/** The last eighteen weeks, a square a day, darker the more was cleaned. */
@Composable
private fun Calendar(field: Field) {
    val c = LocalContext.current
    val weeks = 18
    val start = field.today.minusWeeks((weeks - 1).toLong()).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val counts = (field.reviews.map { field.day(it.at) } + field.finds.map { field.day(it.createdAt) }).groupingBy { it }.eachCount()
    val clay = U.c.clay
    val empty = U.c.surface2
    Row {
        // A row is 16dp tall; Monday, Thursday and Sunday are named.
        Column(Modifier.padding(end = 6.dp)) {
            DayOfWeek.entries.forEach { d ->
                Box(Modifier.height(16.dp), contentAlignment = Alignment.CenterStart) {
                    if (d == DayOfWeek.MONDAY || d == DayOfWeek.THURSDAY || d == DayOfWeek.SUNDAY) Text(d.getDisplayName(TextStyle.SHORT, T.locale(c)), style = UType.Small.copy(fontSize = 10.sp))
                }
            }
        }
        Canvas(Modifier.weight(1f).height(112.dp)) {
            val cell = size.height / 7
            val gap = cell * 0.18f
            for (w in 0 until weeks) for (d in 0 until 7) {
                val day = start.plusDays((w * 7 + d).toLong())
                if (day.isAfter(field.today)) continue
                val n = counts[day] ?: 0
                val color = if (n == 0) empty else clay.copy(alpha = (0.3f + n / 30f).coerceAtMost(1f))
                drawRoundRect(color, Offset(w * cell, d * cell), Size(cell - gap, cell - gap), CornerRadius(cell * 0.2f))
            }
        }
    }
}

@Composable
private fun Forecast(counts: List<Int>, field: Field) {
    val c = LocalContext.current
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val bronze = U.c.bronze
    Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        counts.forEachIndexed { i, n ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$n", style = UType.Small)
                Canvas(Modifier.fillMaxWidth().height((70f * n / max).coerceAtLeast(3f).dp)) {
                    drawRoundRect(if (i == 0) bronze else bronze.copy(alpha = 0.55f), cornerRadius = CornerRadius(6f))
                }
                Text(field.today.plusDays(i.toLong()).dayOfWeek.getDisplayName(TextStyle.SHORT, T.locale(c)), style = UType.Small)
            }
        }
    }
}
