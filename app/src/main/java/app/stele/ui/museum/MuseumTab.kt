package app.stele.ui.museum

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stele.R
import app.stele.container
import app.stele.domain.Hall
import app.stele.domain.Memory
import app.stele.domain.Stage
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.Source
import app.stele.ui.T
import app.stele.ui.art.MuseumFront
import app.stele.ui.art.SealedDoor
import app.stele.ui.art.ShardArt
import app.stele.ui.components.Empty
import app.stele.ui.components.ExhibitTile
import app.stele.ui.components.PrimaryButton
import app.stele.ui.components.SectionTitle
import app.stele.ui.components.SoftButton
import app.stele.ui.components.StageBar
import app.stele.ui.components.UCard
import app.stele.ui.components.VesselCard
import app.stele.ui.components.title
import app.stele.ui.pluralStringResource
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuseumTab(field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    val halls = field.settings.halls.coerceAtMost(Hall.entries.size - 1)
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(T.longDay(c, field.now).uppercase(), style = UType.Eyebrow)
                    Text(stringResource(R.string.museum_title), style = UType.Display)
                }
                IconButton(onClick = go::text) { Icon(Icons.AutoMirrored.Rounded.TextSnippet, stringResource(R.string.text_title), tint = U.c.muted) }
                IconButton(onClick = go::settings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.muted) }
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            val shelf = (field.vessels.sortedByDescending { field.mended(it) } ).map { field.look(it) }
            Column(Modifier.fillMaxWidth().clip(U.RLg).border(1.dp, U.c.line, U.RLg)) {
                MuseumFront(halls, shelf, Modifier.fillMaxWidth().height(206.dp))
                Row(Modifier.fillMaxWidth().background(U.c.surface).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(T.hall(c, Hall.entries[halls]), style = UType.Name)
                        Text(
                            pluralStringResource(R.plurals.museum_holds, field.exhibits.size, field.exhibits.size) + " · " +
                                pluralStringResource(R.plurals.vessels_whole, field.mendedVessels.size, field.mendedVessels.size),
                            style = UType.Muted,
                        )
                    }
                    Text(pluralStringResource(R.plurals.halls_open, halls + 1, halls + 1), style = UType.Small)
                }
            }
        }
        if (field.finds.isEmpty()) {
            item {
                Empty(
                    stringResource(R.string.museum_empty),
                    stringResource(if (field.digs.isEmpty()) R.string.museum_empty_text else R.string.museum_empty_add),
                    art = { ShardArt(Stage.GROUND, Modifier.size(72.dp)) },
                    action = {
                        if (field.digs.isEmpty()) PrimaryButton(stringResource(R.string.dig_new), go::newDig)
                        else PrimaryButton(stringResource(R.string.new_find), { go.add() })
                    },
                )
            }
            return@LazyColumn
        }
        field.newHall?.let { n ->
            item {
                Spacer(Modifier.height(14.dp))
                UCard(Modifier.border(1.5.dp, U.c.gold, U.RMd)) {
                    Text(stringResource(R.string.hall_opened).uppercase(), style = UType.Eyebrow.copy(color = U.c.goldDeep))
                    Spacer(Modifier.height(4.dp))
                    Text(T.hall(c, Hall.entries[n.coerceAtMost(Hall.entries.size - 1)]), style = UType.Title)
                    Text(stringResource(R.string.hall_opened_text), style = UType.Sub)
                    Spacer(Modifier.height(10.dp))
                    SoftButton(stringResource(R.string.hall_walk_in), { container.write { settings.update { it.copy(hallSeen = it.halls) } } }, color = U.c.goldDeep, soft = U.c.goldSoft)
                }
            }
        }
        item {
            Spacer(Modifier.height(14.dp))
            CleanCard(field, go)
        }
        val pit = field.sounding
        if (!field.soundingDone && pit != null) {
            item {
                Spacer(Modifier.height(12.dp))
                UCard(onClick = { go.clean(Source.SOUNDING) }) {
                    Text(stringResource(R.string.pit_title).uppercase(), style = UType.Eyebrow.copy(color = U.c.ochre))
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                            pit.finds.forEachIndexed { i, f ->
                                ShardArt(if (f.id == pit.rareId) Stage.EXHIBIT else Stage.GROUND, Modifier.size(38.dp), seed = i + 40)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pluralStringResource(R.plurals.pit_text, pit.finds.size, pit.finds.size), style = UType.Strong)
                            Text(stringResource(if (pit.rareId != null) R.string.pit_rare else R.string.pit_plain), style = UType.Muted)
                        }
                    }
                }
            }
        }
        if (field.doorDue) {
            item {
                Spacer(Modifier.height(12.dp))
                UCard(onClick = go::door) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SealedDoor(0f, Modifier.size(64.dp, 76.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.door_title), style = UType.Name)
                            Text(stringResource(R.string.door_waiting), style = UType.Muted)
                        }
                    }
                }
            }
        }
        if (field.cracked.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                UCard(onClick = { go.clean(Source.WORKSHOP) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Build, null, tint = U.c.clay, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.workshop_title), style = UType.Name, modifier = Modifier.weight(1f))
                        Text("${field.cracked.size}", style = UType.Strong.copy(fontSize = 15.sp, color = U.c.clay), modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(U.c.claySoft).padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    Text(stringResource(R.string.workshop_text), style = UType.Muted)
                    Spacer(Modifier.height(6.dp))
                    Text(field.cracked.take(5).joinToString(" · ") { it.title() }, style = UType.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (field.forgeriesOpen) {
            item {
                Spacer(Modifier.height(12.dp))
                UCard(onClick = { go.forgery() }) {
                    Text(stringResource(R.string.forgery_title), style = UType.Name)
                    Text(stringResource(R.string.forgery_card), style = UType.Muted)
                }
            }
        }
        item {
            SectionTitle(stringResource(R.string.collection))
            UCard {
                StageBar(field.byStage)
                if (field.unlabelled.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(pluralStringResource(R.plurals.unlabelled_count, field.unlabelled.size, field.unlabelled.size), style = UType.Muted.copy(color = U.c.clay))
                }
                val left = field.doorDaysLeft
                if (!field.doorDue && left != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(pluralStringResource(R.plurals.door_in, left, left), style = UType.Muted)
                }
            }
        }
        if (field.vessels.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.vessels))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(field.vessels.sortedWith(compareByDescending<app.stele.data.Vessel> { field.mended(it) }.thenBy { it.createdAt }), key = { it.id }) { v ->
                        VesselCard(v, field, { go.vessel(v.id) })
                    }
                }
            }
        }
        item {
            SectionTitle(stringResource(R.string.exhibits))
            if (field.exhibits.isEmpty()) {
                Text(stringResource(R.string.exhibits_none), style = UType.Sub, modifier = Modifier.padding(horizontal = 4.dp))
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    field.exhibits.take(12).forEach { f -> ExhibitTile(f, { go.find(f.id) }) }
                }
                if (field.exhibits.size > 12) {
                    Text(pluralStringResource(R.plurals.exhibits_more, field.exhibits.size - 12, field.exhibits.size - 12), style = UType.Muted, modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}

@Composable
private fun CleanCard(field: Field, go: Go) {
    val c = LocalContext.current
    val dusty = field.dusty.size
    val fresh = field.newToday
    UCard {
        Text(stringResource(R.string.clean_title).uppercase(), style = UType.Eyebrow)
        Spacer(Modifier.height(6.dp))
        if (dusty + fresh > 0) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("$dusty", style = UType.Number.copy(fontSize = 40.sp, color = U.c.bronze))
                    Text(stringResource(R.string.label_dusty), style = UType.Muted)
                }
                Column(Modifier.weight(1f)) {
                    Text("$fresh", style = UType.Number.copy(fontSize = 40.sp, color = U.c.earth))
                    Text(stringResource(R.string.label_fresh), style = UType.Muted)
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(stringResource(R.string.clean_start), { go.clean(Source.DUE) }, Modifier.fillMaxWidth())
        } else {
            Text(stringResource(R.string.clean_none), style = UType.Name)
            val next = field.finds.filter { !it.inGround && it.askable }.minOfOrNull { it.due(field.retention) }
            val text = when {
                next != null -> stringResource(R.string.clean_next, T.inTime(c, (next - field.now) / Memory.DAY.toDouble()))
                field.buried.isNotEmpty() -> stringResource(R.string.clean_new_tomorrow)
                else -> stringResource(R.string.clean_add_more)
            }
            Text(text, style = UType.Muted)
            if (field.cleanedToday > 0) {
                Spacer(Modifier.height(4.dp))
                Text(pluralStringResource(R.plurals.cleaned_today, field.cleanedToday, field.cleanedToday), style = UType.Small.copy(color = U.c.bronze))
            }
        }
    }
}
