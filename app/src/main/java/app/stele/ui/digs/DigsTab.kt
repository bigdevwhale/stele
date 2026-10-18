package app.stele.ui.digs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Style
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.stele.container
import app.stele.data.Anki
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stele.R
import app.stele.domain.Rules
import app.stele.domain.Stage
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.T
import app.stele.ui.art.DigSeal
import app.stele.ui.art.ShardArt
import app.stele.ui.components.Empty
import app.stele.ui.components.PrimaryButton
import app.stele.ui.components.SoftButton
import app.stele.ui.components.StageBar
import app.stele.ui.components.UCard
import app.stele.ui.components.findsCount
import app.stele.ui.pluralStringResource
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType

@Composable
fun DigsTab(field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    val scope = rememberCoroutineScope()
    var importing by remember { mutableStateOf(false) }
    val anki = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { Anki.save(container.db.dao(), Anki.read(c, uri), 0) } }
            importing = false
            r.onSuccess { (dig, n) ->
                Toast.makeText(c, c.resources.getQuantityString(R.plurals.csv_imported, n, n), Toast.LENGTH_SHORT).show()
                go.dig(dig)
            }.onFailure { Toast.makeText(c, R.string.anki_failed, Toast.LENGTH_LONG).show() }
        }
    }
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.digs_eyebrow).uppercase(), style = UType.Eyebrow)
            Text(stringResource(R.string.digs_title), style = UType.Display)
            Text(stringResource(R.string.digs_text), style = UType.Sub)
        }
        if (field.digs.isEmpty()) {
            item {
                Empty(
                    stringResource(R.string.digs_empty), stringResource(R.string.digs_empty_text),
                    art = { ShardArt(Stage.GROUND, Modifier.size(72.dp)) },
                    action = { PrimaryButton(stringResource(R.string.dig_new), go::newDig, icon = Icons.Rounded.Add) },
                )
            }
            return@LazyColumn
        }
        items(field.digs, key = { it.id }) { d ->
            val finds = field.findsOf(d.id)
            val due = Rules.dusty(finds, field.now, field.retention).size
            UCard(onClick = { go.dig(d.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DigSeal(d.symbol, U.clay(d.clay), 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, style = UType.Name)
                        Text(
                            findsCount(finds.size) + (if (d.lang.isNotBlank() && !T.langName(c, d.lang).equals(d.name, true)) " · " + T.langName(c, d.lang) else "") +
                                " · " + pluralStringResource(R.plurals.vessels, field.vesselsOf(d.id).size, field.vesselsOf(d.id).size),
                            style = UType.Muted,
                        )
                    }
                    if (due > 0) Text("$due", style = UType.Strong.copy(fontSize = 15.sp, color = U.c.bronze), modifier = Modifier.clip(CircleShape).background(U.c.bronzeSoft).padding(horizontal = 10.dp, vertical = 4.dp))
                }
                if (finds.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    StageBar(Stage.entries.associateWith { s -> finds.count { it.askable && it.stage == s } }, legend = false)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton(stringResource(R.string.dig_new), go::newDig, Modifier.weight(1f), icon = Icons.Rounded.Add)
                SoftButton(stringResource(R.string.text_title), go::text, Modifier.weight(1f), color = U.c.bronze, soft = U.c.bronzeSoft, icon = Icons.AutoMirrored.Rounded.TextSnippet)
            }
            Spacer(Modifier.height(10.dp))
            SoftButton(
                stringResource(if (importing) R.string.anki_reading else R.string.anki_import), { if (!importing) anki.launch(arrayOf("*/*")) },
                Modifier.fillMaxWidth(), color = U.c.ochre, soft = U.c.ochreSoft, icon = Icons.Rounded.Style,
            )
            Text(stringResource(R.string.anki_hint), style = UType.Muted, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
            Spacer(Modifier.fillMaxWidth().height(4.dp))
        }
    }
}
