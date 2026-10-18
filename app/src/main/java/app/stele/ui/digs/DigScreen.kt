package app.stele.ui.digs

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TheaterComedy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.stele.R
import app.stele.ui.components.Hint
import app.stele.ui.components.Hints
import app.stele.container
import app.stele.data.Find
import app.stele.data.Kind
import app.stele.data.Lines
import app.stele.domain.Rules
import app.stele.domain.Stage
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.Source
import app.stele.ui.T
import app.stele.ui.art.DigSeal
import app.stele.ui.art.ShardArt
import app.stele.ui.components.Chip
import app.stele.ui.components.Empty
import app.stele.ui.components.FindRow
import app.stele.ui.components.PrimaryButton
import app.stele.ui.components.SectionTitle
import app.stele.ui.components.SoftButton
import app.stele.ui.components.StageBar
import app.stele.ui.components.TopBar
import app.stele.ui.components.UTextField
import app.stele.ui.components.VesselCard
import app.stele.ui.components.findsCount
import app.stele.ui.components.title
import app.stele.ui.pluralStringResource
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DigScreen(id: Long, field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    val dig = field.dig(id) ?: return
    val finds = field.findsOf(id)
    val due = field.round(digId = id).size
    // Comparing spellings across a big dig is not free: once per change, not on every keystroke in the search.
    val forgeable = remember(finds, field.links) { Rules.forgeries(finds, field.links, 1, 0).isNotEmpty() }
    var query by rememberSaveable { mutableStateOf("") }
    var stage by rememberSaveable { mutableStateOf<Stage?>(null) }
    var unlabelledOnly by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { c.contentResolver.openOutputStream(uri)?.use { it.write(Lines.toCsv(finds).toByteArray()) } }.isSuccess
            }
            Toast.makeText(c, if (ok) R.string.csv_saved else R.string.csv_failed, Toast.LENGTH_SHORT).show()
        }
    }
    val anki = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { app.stele.data.Anki.save(container.db.dao(), app.stele.data.Anki.read(c, uri), id).second } }
            r.onSuccess { n -> Toast.makeText(c, c.resources.getQuantityString(R.plurals.csv_imported, n, n), Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(c, R.string.anki_failed, Toast.LENGTH_LONG).show() }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val n = withContext(Dispatchers.IO) {
                runCatching {
                    val text = c.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }.orEmpty()
                    val now = System.currentTimeMillis()
                    val rows = Lines.csv(text)
                    container.db.dao().insertFinds(rows.mapIndexed { i, r ->
                        Find(digId = id, kind = Kind.PAIR.name, front = r.front, back = r.back, example = r.example, hint = r.hint, createdAt = now + i)
                    })
                    rows.size
                }.getOrDefault(-1)
            }
            if (n < 0) Toast.makeText(c, R.string.csv_failed, Toast.LENGTH_SHORT).show()
            else Toast.makeText(c, c.resources.getQuantityString(R.plurals.csv_imported, n, n), Toast.LENGTH_SHORT).show()
        }
    }

    val shown = finds.filter { f ->
        (stage == null || f.stage == stage) && (!unlabelledOnly || !f.askable) &&
            (query.isBlank() || f.title().contains(query, true) || f.back.contains(query, true))
    }.sortedByDescending { it.createdAt }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(dig.name, go::back) {
            IconButton(onClick = { go.editDig(id) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.dig_edit), tint = U.c.muted) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.more), tint = U.c.muted) }
                DropdownMenu(menu, { menu = false }, containerColor = U.c.surface) {
                    DropdownMenuItem({ Text(stringResource(R.string.csv_import)) }, { menu = false; import.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) })
                    DropdownMenuItem({ Text(stringResource(R.string.anki_import_here)) }, { menu = false; anki.launch(arrayOf("*/*")) })
                    DropdownMenuItem({ Text(stringResource(R.string.csv_export)) }, { menu = false; export.launch(dig.name.replace(Regex("[^\\p{L}\\p{N}]+"), "-") + ".csv") })
                }
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            item { Hint(Hints.DIG, field) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DigSeal(dig.symbol, U.clay(dig.clay), 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(findsCount(finds.size), style = UType.Strong)
                        Text(
                            listOfNotNull(dig.lang.takeIf { it.isNotBlank() }?.let { T.langName(c, it) }, dig.note.takeIf { it.isNotBlank() }).joinToString(" · ")
                                .ifEmpty { stringResource(R.string.dig_no_lang) },
                            style = UType.Muted,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                StageBar(Stage.entries.associateWith { s -> finds.count { it.askable && it.stage == s } })
                Spacer(Modifier.height(14.dp))
                if (due > 0) {
                    PrimaryButton(pluralStringResource(R.plurals.clean_dig, due, due), { go.clean(Source.dig(id)) }, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftButton(stringResource(R.string.new_find), { go.add(id) }, icon = Icons.Rounded.Add)
                    SoftButton(stringResource(R.string.bulk_title), { go.bulk(id) }, color = U.c.bronze, soft = U.c.bronzeSoft, icon = Icons.AutoMirrored.Rounded.PlaylistAdd)
                    if (forgeable) {
                        SoftButton(stringResource(R.string.forgery_title), { go.forgery(id) }, color = U.c.ochre, soft = U.c.ochreSoft, icon = Icons.Rounded.TheaterComedy)
                    }
                }
            }
            item {
                SectionTitle(stringResource(R.string.vessels))
                Text(stringResource(R.string.vessels_hint), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(field.vesselsOf(id), key = { it.id }) { v -> VesselCard(v, field, { go.vessel(v.id) }) }
                    item {
                        Column(
                            Modifier.width(132.dp).height(166.dp).border(1.dp, U.c.line, U.RMd).clickable { go.newVessel(id) }.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = U.c.clay)
                            Text(stringResource(R.string.vessel_new), style = UType.Strong.copy(color = U.c.clay))
                        }
                    }
                }
            }
            item {
                SectionTitle(stringResource(R.string.finds))
                if (finds.isEmpty()) {
                    Empty(
                        stringResource(R.string.dig_empty), stringResource(R.string.dig_empty_text),
                        art = { ShardArt(Stage.GROUND, Modifier.size(64.dp)) },
                        action = { PrimaryButton(stringResource(R.string.new_find), { go.add(id) }, icon = Icons.Rounded.Add) },
                    )
                } else {
                    UTextField(query, { query = it }, placeholder = stringResource(R.string.search))
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip(stringResource(R.string.all), stage == null && !unlabelledOnly, { stage = null; unlabelledOnly = false })
                        Stage.entries.forEach { s -> Chip(T.stage(c, s), stage == s, { stage = if (stage == s) null else s; unlabelledOnly = false }, U.stage(s)) }
                        if (finds.any { !it.askable }) Chip(stringResource(R.string.unlabelled), unlabelledOnly, { unlabelledOnly = !unlabelledOnly; stage = null })
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            items(shown, key = { it.id }) { f -> FindRow(f, field, { go.find(f.id) }) }
            if (finds.isNotEmpty() && shown.isEmpty()) {
                item {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Search, null, tint = U.c.faint)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.nothing_found), style = UType.Sub)
                    }
                }
            }
        }
    }
}
