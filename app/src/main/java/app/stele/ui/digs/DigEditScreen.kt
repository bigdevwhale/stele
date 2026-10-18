package app.stele.ui.digs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.stele.R
import app.stele.container
import app.stele.data.Dig
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.T
import app.stele.ui.art.DigSeal
import app.stele.ui.art.GLYPHS
import app.stele.ui.components.Chip
import app.stele.ui.components.ConfirmDialog
import app.stele.ui.components.PrimaryButton
import app.stele.ui.components.SectionTitle
import app.stele.ui.components.TopBar
import app.stele.ui.components.UTextField
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DigEditScreen(id: Long, field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    val old = field.dig(id)
    var name by rememberSaveable { mutableStateOf(old?.name.orEmpty()) }
    var symbol by rememberSaveable { mutableIntStateOf(old?.symbol ?: (field.digs.size * 5) % GLYPHS) }
    var clay by rememberSaveable { mutableIntStateOf(old?.clay ?: field.digs.size % U.clays.size) }
    var lang by rememberSaveable { mutableStateOf(old?.lang.orEmpty()) }
    var note by rememberSaveable { mutableStateOf(old?.note.orEmpty()) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun save() {
        val d = (old ?: Dig(name = "", createdAt = System.currentTimeMillis())).copy(name = name.trim(), symbol = symbol, clay = clay, lang = lang, note = note.trim())
        scope.launch {
            val newId = withContext(Dispatchers.IO) {
                val dao = container.db.dao()
                if (old == null) dao.insert(d).also { nid -> container.settings.update { it.copy(lastDig = nid) } } else { dao.update(d); d.id }
            }
            if (old == null) go.replaceWithDig(newId) else go.back()
        }
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(if (old == null) R.string.dig_new else R.string.dig_edit), go::back) {
            if (old != null) IconButton(onClick = { deleting = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = U.c.muted) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) { DigSeal(symbol, U.clay(clay), 84.dp) }
            Text(stringResource(R.string.dig_name), style = UType.Strong)
            Spacer(Modifier.height(6.dp))
            UTextField(name, { name = it }, placeholder = stringResource(R.string.dig_name_hint))
            SectionTitle(stringResource(R.string.dig_glyph))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(GLYPHS) { i ->
                    Box(
                        Modifier.clip(CircleShape).border(2.dp, if (i == symbol) U.c.ink else U.c.bg, CircleShape).clickable { symbol = i }.padding(3.dp),
                    ) { DigSeal(i, U.clay(clay), 42.dp) }
                }
            }
            SectionTitle(stringResource(R.string.dig_clay))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                U.clays.forEachIndexed { i, col ->
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(col).border(3.dp, if (i == clay) U.c.ink else col, CircleShape).clickable { clay = i },
                    )
                }
            }
            SectionTitle(stringResource(R.string.dig_lang))
            Text(stringResource(R.string.dig_lang_hint), style = UType.Muted)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                T.langs.forEach { t -> Chip(T.langName(c, t), t == lang, { lang = t }) }
            }
            SectionTitle(stringResource(R.string.dig_note))
            UTextField(note, { note = it }, placeholder = stringResource(R.string.dig_note_hint), minLines = 2)
            Spacer(Modifier.height(24.dp))
        }
        PrimaryButton(
            stringResource(R.string.save), ::save,
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding(),
            enabled = name.isNotBlank(),
        )
    }
    if (deleting && old != null) {
        ConfirmDialog(
            stringResource(R.string.dig_delete), stringResource(R.string.dig_delete_text, field.findsOf(old.id).size), stringResource(R.string.delete),
            onConfirm = {
                container.write { db.dao().deleteDig(old) }
                go.back(); go.back()
            },
            onDismiss = { deleting = false }, danger = true,
        )
    }
}
