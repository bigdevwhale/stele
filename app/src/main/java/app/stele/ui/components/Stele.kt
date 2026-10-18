package app.stele.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stele.R
import app.stele.data.Find
import app.stele.data.Kind
import app.stele.data.Vessel
import app.stele.domain.Shape
import app.stele.domain.Stage
import app.stele.domain.Words
import app.stele.ui.Field
import app.stele.ui.T
import app.stele.ui.art.ShardArt
import app.stele.ui.art.VesselArt
import app.stele.ui.art.chisel
import app.stele.ui.art.plinth
import app.stele.ui.pluralStringResource
import app.stele.ui.stringResource
import app.stele.ui.theme.Cormorant
import app.stele.ui.theme.U
import app.stele.ui.theme.UType

/** The collection by stage: one bar of earth, ochre, clay, bronze and gold, with its legend. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StageBar(byStage: Map<Stage, Int>, modifier: Modifier = Modifier, legend: Boolean = true) {
    val total = byStage.values.sum()
    val colors = Stage.entries.associateWith { U.stage(it) }
    val track = U.c.surface2
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)) {
            drawRect(track)
            if (total == 0) return@Canvas
            var x = 0f
            Stage.entries.forEach { s ->
                val w = size.width * (byStage[s] ?: 0) / total
                drawRect(colors.getValue(s), Offset(x, 0f), Size(w, size.height))
                x += w
            }
        }
        if (legend) {
            Spacer(Modifier.height(10.dp))
            val c = LocalContext.current
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Stage.entries.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(colors.getValue(s)))
                        Spacer(Modifier.width(6.dp))
                        Text("${T.stage(c, s)} ${byStage[s] ?: 0}", style = UType.Muted)
                    }
                }
            }
        }
    }
}

@Composable
fun StageBadge(stage: Stage, modifier: Modifier = Modifier) {
    val c = LocalContext.current
    Text(
        T.stage(c, stage).uppercase(),
        style = UType.Eyebrow.copy(fontSize = 10.sp, color = if (stage == Stage.EXHIBIT) U.c.goldDeep else U.stage(stage)),
        modifier = modifier.clip(U.RSm).background(U.stageSoft(stage)).padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

/** The front of a find as it reads in a list: a phrase shows without its brackets. */
fun Find.title(): String = if (kindOf == Kind.PHRASE) Words.plain(front) else front

/** A find in a list: its shard, its two sides, its stage and any dust. */
@Composable
fun FindRow(f: Find, field: Field, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val patina = field.patina(f)
    Row(
        modifier.fillMaxWidth().clip(U.RMd).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShardArt(f.stage, Modifier.size(40.dp), patina = patina, cracked = f.cracked, seed = f.id.toInt())
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(f.title(), style = UType.Name.copy(fontSize = 19.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = when {
                !f.askable -> stringResource(R.string.unlabelled)
                f.kindOf == Kind.PHRASE -> f.back
                else -> f.back
            }
            if (sub.isNotBlank()) Text(sub, style = UType.Muted.copy(color = if (!f.askable) U.c.clay else U.c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            StageBadge(f.stage)
            if (patina > 0f) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.dusty), style = UType.Small.copy(color = U.c.bronze))
            } else if (f.confirmed) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.confirmed_short), style = UType.Small.copy(color = U.c.goldDeep))
            }
        }
    }
}

/** A vessel on a shelf: drawn from its shards, with its name and how many are in. */
@Composable
fun VesselCard(v: Vessel, field: Field, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shards = field.shardsOf(v)
    val mended = field.mended(v)
    val ready = shards.count { it.stage >= Stage.CLEANED }
    Column(
        modifier.width(132.dp).clip(U.RMd).background(U.c.surface).border(1.dp, if (mended) U.c.gold else U.c.line, U.RMd).clickable(onClick = onClick).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VesselArt(Shape.of(v.shape), shards, mended, Modifier.size(104.dp), seed = v.id.toInt())
        Spacer(Modifier.height(6.dp))
        Text(v.name, style = UType.Name.copy(fontSize = 17.sp, textAlign = TextAlign.Center), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (mended) stringResource(R.string.vessel_whole) else stringResource(R.string.vessel_of, ready, shards.size),
            style = UType.Small.copy(color = if (mended) U.c.goldDeep else U.c.faint),
        )
    }
}

/** An exhibit on its plinth, with the placard below. */
@Composable
fun ExhibitTile(f: Find, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = U.c
    Column(modifier.width(104.dp).clip(U.RMd).clickable(onClick = onClick).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(84.dp, 70.dp), contentAlignment = Alignment.TopCenter) {
            Canvas(Modifier.size(84.dp, 70.dp)) {
                plinth(p, Rect(size.width * 0.12f, size.height * 0.56f, size.width * 0.88f, size.height))
            }
            ShardArt(Stage.EXHIBIT, Modifier.size(50.dp).padding(top = 2.dp), seed = f.id.toInt())
        }
        Text(f.title(), style = UType.Name.copy(fontSize = 15.sp, textAlign = TextAlign.Center), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Strikes of the chisel, 1..3, drawn as chisels in a row. */
@Composable
fun Chisels(n: Int, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size((18 * n + 4).dp, 22.dp)) {
        repeat(n) { i -> chisel(color, Rect(i * 18.dp.toPx(), 0f, i * 18.dp.toPx() + 22.dp.toPx(), 22.dp.toPx())) }
    }
}

/** A quiet message where a list would be: art, a line in the carved hand, and what to do. */
@Composable
fun Empty(title: String, text: String, modifier: Modifier = Modifier, art: @Composable () -> Unit = {}, action: @Composable () -> Unit = {}) {
    Column(modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        art()
        Spacer(Modifier.height(10.dp))
        Text(title, style = UType.Title.copy(fontSize = 24.sp, textAlign = TextAlign.Center))
        Spacer(Modifier.height(6.dp))
        Text(text, style = UType.Sub.copy(textAlign = TextAlign.Center))
        Spacer(Modifier.height(16.dp))
        action()
    }
}

/** A count in big carved numerals with its caption, for the journal. */
@Composable
fun Tally(value: String, label: String, modifier: Modifier = Modifier, color: Color = U.c.ink) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = UType.Number.copy(fontSize = 32.sp, color = color), maxLines = 1)
        Text(label, style = UType.Small.copy(textAlign = TextAlign.Center), maxLines = 2)
    }
}

@Composable
fun findsCount(n: Int): String = pluralStringResource(R.plurals.finds, n, n)

/** A find's picture from the app's files, decoded once. */
@Composable
fun rememberPicture(name: String): androidx.compose.ui.graphics.ImageBitmap? {
    val c = LocalContext.current
    return androidx.compose.runtime.remember(name) {
        if (name.isBlank()) null
        else runCatching { android.graphics.BitmapFactory.decodeFile(app.stele.data.Images.file(c, name).path)?.asImageBitmap() }.getOrNull()
    }
}
