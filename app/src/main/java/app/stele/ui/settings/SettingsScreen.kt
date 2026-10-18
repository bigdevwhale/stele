package app.stele.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.stele.BuildConfig
import app.stele.R
import app.stele.container
import app.stele.data.Backup
import app.stele.data.Settings
import app.stele.domain.Gender
import app.stele.notify.Bell
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.Language
import app.stele.ui.components.Chip
import app.stele.ui.components.ConfirmDialog
import app.stele.ui.components.InfoRow
import app.stele.ui.components.SectionTitle
import app.stele.ui.components.Stepper
import app.stele.ui.components.SwitchRow
import app.stele.ui.components.TimeDialog
import app.stele.ui.components.TopBar
import app.stele.ui.components.UCard
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(field: Field, go: Go) {
    val context = LocalContext.current
    val c = context.container
    val s = field.settings
    var status by remember { mutableStateOf<String?>(null) }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var timing by remember { mutableStateOf(false) }
    fun set(change: (Settings) -> Settings) = c.write { settings.update(change) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        c.scope.launch {
            val ok = runCatching { withContext(Dispatchers.IO) { Backup.write(context, uri, c.db.dao()) } }.isSuccess
            status = context.getString(if (ok) R.string.backup_done else R.string.backup_failed)
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreUri = uri }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.settings), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding()) {
            SectionTitle(stringResource(R.string.set_bell))
            UCard {
                Text(stringResource(R.string.set_bell_hint), style = UType.Muted)
                SwitchRow(stringResource(R.string.set_bell_on), null, s.bell, { on -> set { it.copy(bell = on) } })
                if (s.bell) InfoRow(Icons.Rounded.NotificationsActive, stringResource(R.string.set_at), hm(s.bellAt), onClick = { timing = true })
                Stepper(stringResource(R.string.set_pit_size), "${s.soundingSize}", { set { it.copy(soundingSize = (it.soundingSize - 1).coerceAtLeast(3)) } }, { set { it.copy(soundingSize = (it.soundingSize + 1).coerceAtMost(10)) } })
            }

            SectionTitle(stringResource(R.string.set_pace))
            UCard {
                Text(stringResource(R.string.set_new_hint), style = UType.Muted)
                Stepper(stringResource(R.string.set_new), "${s.newPerDay}", { set { it.copy(newPerDay = (it.newPerDay - 5).coerceAtLeast(0)) } }, { set { it.copy(newPerDay = (it.newPerDay + 5).coerceAtMost(100)) } })
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.set_retention), style = UType.Body)
                Text(stringResource(R.string.set_retention_hint), style = UType.Muted)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.85f to R.string.retention_light, 0.9f to R.string.retention_usual, 0.95f to R.string.retention_strict).forEach { (r, label) ->
                        Chip(stringResource(label), kotlin.math.abs(s.retention - r) < 0.001f, { set { it.copy(retention = r) } })
                    }
                }
            }

            SectionTitle(stringResource(R.string.set_language))
            UCard {
                val cur = Language.current()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.lang_system), cur == Language.SYSTEM, { Language.set(Language.SYSTEM) })
                    Chip("English", cur == Language.EN, { Language.set(Language.EN) })
                    Chip("Русский", cur == Language.RU, { Language.set(Language.RU) })
                }
                // Russian speaks to the digger as "уверен" or "уверена"; English has no need to ask.
                if (LocalConfiguration.current.locales[0].language == "ru") {
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.set_gender), style = UType.Body)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(stringResource(R.string.gender_male), s.gender == Gender.MALE, { set { it.copy(gender = Gender.MALE) } })
                        Chip(stringResource(R.string.gender_female), s.gender == Gender.FEMALE, { set { it.copy(gender = Gender.FEMALE) } })
                    }
                }
            }

            SectionTitle(stringResource(R.string.set_backup))
            UCard {
                Text(stringResource(R.string.set_backup_hint), style = UType.Muted)
                InfoRow(Icons.Rounded.Upload, stringResource(R.string.backup_export), stringResource(R.string.backup_export_hint), onClick = { export.launch("Stele.json") })
                InfoRow(Icons.Rounded.Download, stringResource(R.string.backup_import), stringResource(R.string.backup_import_hint), onClick = { import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
                status?.let { Text(it, style = UType.Muted.copy(color = U.c.clay)) }
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.about, BuildConfig.VERSION_NAME), style = UType.Small.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
    }

    if (timing) {
        TimeDialog(s.bellAt, onDismiss = { timing = false }) { m -> set { it.copy(bellAt = m) } }
    }

    restoreUri?.let { uri ->
        ConfirmDialog(
            stringResource(R.string.backup_import), stringResource(R.string.backup_import_confirm), stringResource(R.string.backup_import_do),
            onConfirm = {
                c.scope.launch {
                    val ok = runCatching { withContext(Dispatchers.IO) { Backup.restore(context, uri, c.db.dao()) } }.isSuccess
                    if (ok) c.settings.update { it.copy(onboarded = true) }
                    Bell.reschedule(context)
                    status = context.getString(if (ok) R.string.backup_restored else R.string.backup_bad)
                }
            },
            onDismiss = { restoreUri = null }, danger = true,
        )
    }
}

private fun hm(m: Int) = "%02d:%02d".format(m / 60, m % 60)
