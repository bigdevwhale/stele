package app.stele.ui.digs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import app.stele.data.Vessel
import app.stele.domain.Rules
import app.stele.domain.Shape
import app.stele.domain.Stage
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.Source
import app.stele.ui.T
import app.stele.ui.art.ShardLook
import app.stele.ui.art.VesselArt
import app.stele.ui.components.ConfirmDialog
import app.stele.ui.components.FindRow
import app.stele.ui.components.PrimaryButton
import app.stele.ui.components.SectionTitle
import app.stele.ui.components.SoftButton
import app.stele.ui.components.TopBar
import app.stele.ui.components.UTextField
import app.stele.ui.components.title
import app.stele.ui.pluralStringResource
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VesselEditScreen(digId: Long, id: Long, field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    val old = field.vessel(id)
    var name by rememberSaveable { mutableStateOf(old?.name.orEmpty()) }
    var shape by rememberSaveable { mutableStateOf(old?.shape ?: Shape.entries[field.vessels.size % Shape.entries.size].name) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val preview = (0 until 6).map { ShardLook(Stage.entries[(it % 4) + 1]) }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(if (old == null) R.string.vessel_new else R.string.vessel_edit), go::back) {
            if (old != null) IconButton(onClick = { deleting = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = U.c.muted) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(stringResource(R.string.vessel_text), style = UType.Sub)
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.vessel_name), style = UType.Strong)
            Spacer(Modifier.height(6.dp))
            UTextField(name, { name = it }, placeholder = stringResource(R.string.vessel_name_hint))
            SectionTitle(stringResource(R.string.vessel_shape))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Shape.entries.forEach { s ->
                    val on = s.name == shape
                    Column(
                        Modifier.width(100.dp).clip(U.RMd).background(if (on) U.c.claySoft else U.c.surface)
                            .border(if (on) 1.5.dp else 1.dp, if (on) U.c.clay else U.c.line, U.RMd).clickable { shape = s.name }.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        VesselArt(s, preview, false, Modifier.size(72.dp), seed = s.ordinal)
                        Text(T.shape(c, s), style = UType.Small.copy(color = if (on) U.c.clay else U.c.muted, textAlign = TextAlign.Center))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton(
            stringResource(R.string.save),
            {
                scope.launch {
                    val v = (old ?: Vessel(digId = digId, name = "", shape = shape, createdAt = System.currentTimeMillis())).copy(name = name.trim(), shape = shape)
                    val newId = withContext(Dispatchers.IO) { if (old == null) container.db.dao().insert(v) else { container.db.dao().update(v); v.id } }
                    go.back()
                    if (old == null) go.vessel(newId)
                }
            },
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding(),
            enabled = name.isNotBlank(),
        )
    }
    if (deleting && old != null) {
        ConfirmDialog(
            stringResource(R.string.vessel_delete), stringResource(R.string.vessel_delete_text), stringResource(R.string.delete),
            onConfirm = { container.write { db.dao().deleteVessel(old) }; go.back(); go.back() },
            onDismiss = { deleting = false }, danger = true,
        )
    }
}

@Composable
fun VesselScreen(id: Long, field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    val v = field.vessel(id) ?: return
    val finds = field.findsIn(id)
    val mended = field.mended(v)
    val due = field.round(vesselId = id).size
    val ready = finds.count { it.stage >= Stage.CLEANED }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(v.name, go::back) {
            IconButton(onClick = { go.editVessel(v.digId, id) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.vessel_edit), tint = U.c.muted) }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            item {
                Box(Modifier.fillMaxWidth().clip(U.RLg).background(U.c.surface).border(1.dp, if (mended) U.c.gold else U.c.line, U.RLg).padding(16.dp), contentAlignment = Alignment.Center) {
                    VesselArt(Shape.of(v.shape), field.shardsOf(v), mended, Modifier.size(240.dp), seed = v.id.toInt())
                }
                Spacer(Modifier.height(12.dp))
                Text(T.shape(c, Shape.of(v.shape)).uppercase() + " · " + field.dig(v.digId)?.name.orEmpty(), style = UType.Eyebrow)
                Text(
                    when {
                        mended -> stringResource(R.string.vessel_mended_text)
                        finds.size < Rules.VESSEL_MIN -> pluralStringResource(R.plurals.vessel_needs, Rules.VESSEL_MIN - finds.size, Rules.VESSEL_MIN - finds.size)
                        else -> stringResource(R.string.vessel_progress, ready, finds.size)
                    },
                    style = UType.Sub,
                )
                v.mendedAt?.let { Text(stringResource(R.string.vessel_mended_on, T.date(c, it)), style = UType.Small.copy(color = U.c.goldDeep)) }
                Spacer(Modifier.height(14.dp))
                if (due > 0) {
                    PrimaryButton(pluralStringResource(R.plurals.clean_dig, due, due), { go.clean(Source.vessel(id)) }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftButton(stringResource(R.string.new_find), { go.add(v.digId, id) }, Modifier.weight(1f), icon = Icons.Rounded.Add)
                    SoftButton(stringResource(R.string.vessel_put), { go.pick(id) }, Modifier.weight(1f), color = U.c.bronze, soft = U.c.bronzeSoft, icon = Icons.AutoMirrored.Rounded.PlaylistAdd)
                }
                SectionTitle(stringResource(R.string.vessel_shards))
            }
            items(finds, key = { it.id }) { f ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { FindRow(f, field, { go.find(f.id) }) }
                    IconButton(onClick = { container.write { db.dao().update(f.copy(vesselId = 0)) } }) {
                        Icon(Icons.Rounded.RemoveCircleOutline, stringResource(R.string.vessel_take_out), tint = U.c.faint)
                    }
                }
            }
        }
    }
}

/** Loose finds of the dig, to put into a vessel. */
@Composable
fun PickScreen(vesselId: Long, field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    val v = field.vessel(vesselId) ?: return
    val loose = field.findsOf(v.digId).filter { it.vesselId == 0L }.sortedBy { it.title().lowercase() }
    var picked by rememberSaveable { mutableStateOf(listOf<Long>()) }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.vessel_put), go::back)
        if (loose.isEmpty()) {
            Text(stringResource(R.string.vessel_no_loose), style = UType.Sub, modifier = Modifier.padding(16.dp))
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp)) {
            items(loose, key = { it.id }) { f ->
                val on = f.id in picked
                Row(
                    Modifier.fillMaxWidth().clip(U.RMd).clickable { picked = if (on) picked - f.id else picked + f.id }.padding(vertical = 10.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (on) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = if (on) U.c.clay else U.c.faint)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(f.title(), style = UType.Name.copy(fontSize = 18.sp))
                        if (f.back.isNotBlank()) Text(f.back, style = UType.Muted, maxLines = 1)
                    }
                }
            }
        }
        PrimaryButton(
            pluralStringResource(R.plurals.vessel_put_n, picked.size, picked.size),
            {
                val ids = picked.toSet()
                container.write {
                    loose.filter { it.id in ids }.forEach { db.dao().update(it.copy(vesselId = vesselId)) }
                    keeper.glue(vesselId)
                }
                go.back()
            },
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding(),
            enabled = picked.isNotEmpty(),
        )
    }
}
