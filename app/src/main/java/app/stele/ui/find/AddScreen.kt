package app.stele.ui.find

import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.stele.R
import app.stele.ui.components.Hint
import app.stele.ui.components.Hints
import app.stele.container
import app.stele.data.Find
import app.stele.data.Images
import app.stele.data.Kind
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.Handoff
import app.stele.ui.T
import app.stele.ui.art.DigSeal
import app.stele.ui.canListen
import app.stele.ui.components.Chip
import app.stele.ui.components.PrimaryButton
import app.stele.ui.components.SectionTitle
import app.stele.ui.components.Segmented
import app.stele.ui.components.SoftButton
import app.stele.ui.components.TopBar
import app.stele.ui.components.UTextField
import app.stele.ui.components.rememberPicture
import app.stele.ui.listenIntent
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A new find, or one being relabelled: the two sides first, everything else folded away. */
@Composable
fun AddScreen(digArg: Long, vesselArg: Long, findId: Long, field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    val old = field.find(findId)
    val scope = rememberCoroutineScope()
    val firstDig = old?.digId ?: digArg.takeIf { field.dig(it) != null } ?: field.settings.lastDig.takeIf { field.dig(it) != null } ?: field.digs.firstOrNull()?.id ?: 0L
    var digId by rememberSaveable { mutableLongStateOf(firstDig) }
    var vesselId by rememberSaveable { mutableLongStateOf(old?.vesselId ?: vesselArg) }
    var kind by rememberSaveable { mutableStateOf(old?.kindOf ?: field.findsOf(firstDig).lastOrNull()?.kindOf ?: Kind.PAIR) }
    var front by rememberSaveable { mutableStateOf(old?.front ?: Handoff.front.also { Handoff.front = "" }) }
    var back by rememberSaveable { mutableStateOf(old?.back.orEmpty()) }
    var example by rememberSaveable { mutableStateOf(old?.example.orEmpty()) }
    var hint by rememberSaveable { mutableStateOf(old?.hint.orEmpty()) }
    var origin by rememberSaveable { mutableStateOf(old?.origin.orEmpty()) }
    var image by rememberSaveable { mutableStateOf(old?.image.orEmpty()) }
    var more by rememberSaveable { mutableStateOf(old != null && (old.example + old.hint + old.origin + old.image).isNotBlank()) }
    var added by rememberSaveable { mutableStateOf(0) }
    val focus = remember { FocusRequester() }
    val dig = field.dig(digId)

    if (field.digs.isEmpty()) {
        LaunchedEffect(Unit) { go.back(); go.newDig() }
        return
    }

    val listen = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { front = it }
    }
    fun keep(uri: Uri?) {
        uri ?: return
        scope.launch {
            val name = withContext(Dispatchers.IO) { Images.keep(c, uri) }
            if (name != null) image = name
        }
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { keep(it) }
    var shot by remember { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) keep(shot) }

    fun save(again: Boolean) {
        val now = System.currentTimeMillis()
        val f = (old ?: Find(digId = digId, front = "", createdAt = now)).copy(
            digId = digId, vesselId = if (field.vessel(vesselId)?.digId == digId) vesselId else 0L, kind = kind.name,
            front = front.trim(), back = back.trim(), example = example.trim(), hint = hint.trim(), origin = origin.trim(), image = image, updatedAt = now,
        )
        container.write {
            if (old == null) db.dao().insert(f) else db.dao().update(f)
            if (old != null && old.image.isNotBlank() && old.image != image) Images.drop(c, old.image)
            settings.update { it.copy(lastDig = digId) }
            keeper.glue(f.vesselId)
        }
        if (again) {
            front = ""; back = ""; example = ""; hint = ""; image = ""
            added++
            Toast.makeText(c, R.string.find_saved, Toast.LENGTH_SHORT).show()
            runCatching { focus.requestFocus() }
        } else go.back()
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(if (old == null) R.string.new_find else R.string.find_edit), go::back)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Hint(Hints.ADD, field)
            // Which dig it goes to.
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                field.digs.forEach { d -> Chip(d.name, d.id == digId, { digId = d.id }, U.clay(d.clay)) }
            }
            Spacer(Modifier.height(12.dp))
            Segmented(Kind.entries, kind, { T.kind(c, it) }, { kind = it })
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        when (kind) {
                            Kind.PAIR -> R.string.front_pair
                            Kind.TERM -> R.string.front_term
                            Kind.PHRASE -> R.string.front_phrase
                            Kind.QUESTION -> R.string.front_question
                        }
                    ),
                    style = UType.Strong, modifier = Modifier.weight(1f),
                )
                if (canListen(c)) {
                    IconButton(onClick = { runCatching { listen.launch(listenIntent(dig?.lang.orEmpty(), c.getString(R.string.say_it))) } }) {
                        Icon(Icons.Rounded.Mic, stringResource(R.string.say_it), tint = U.c.clay)
                    }
                }
            }
            UTextField(
                front, { front = it }, Modifier.focusRequester(focus),
                placeholder = stringResource(if (kind == Kind.PHRASE) R.string.front_phrase_hint else R.string.front_hint),
                minLines = if (kind == Kind.PHRASE || kind == Kind.QUESTION) 2 else 1,
            )
            if (kind == Kind.PHRASE) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.phrase_gaps_hint), style = UType.Muted)
                Spacer(Modifier.height(6.dp))
                SoftButton(stringResource(R.string.phrase_wrap_last), {
                    // Puts brackets round the last word that has none, for a quick gap without fiddly selection.
                    val m = Regex("[\\p{L}\\p{N}'’-]+(?![^\\[]*])").findAll(front).lastOrNull()
                    if (m != null) front = front.replaceRange(m.range, "[" + m.value + "]")
                }, color = U.c.ochre, soft = U.c.ochreSoft)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(
                    when (kind) {
                        Kind.PAIR -> R.string.back_pair
                        Kind.TERM -> R.string.back_term
                        Kind.PHRASE -> R.string.back_phrase
                        Kind.QUESTION -> R.string.back_question
                    }
                ),
                style = UType.Strong,
            )
            Spacer(Modifier.height(6.dp))
            UTextField(back, { back = it }, placeholder = stringResource(if (kind == Kind.PAIR) R.string.back_pair_hint else R.string.back_hint), minLines = if (kind == Kind.PAIR) 1 else 2)
            if (kind == Kind.PAIR) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.back_variants), style = UType.Muted)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clip(U.RSm).clickable { more = !more }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.find_more), style = UType.Strong.copy(color = U.c.clay), modifier = Modifier.weight(1f))
                Icon(if (more) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = U.c.clay)
            }
            if (more) {
                Text(stringResource(R.string.example), style = UType.Strong)
                Text(stringResource(R.string.example_hint), style = UType.Muted)
                Spacer(Modifier.height(6.dp))
                UTextField(example, { example = it }, placeholder = stringResource(R.string.example_placeholder), minLines = 2)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.hint), style = UType.Strong)
                Text(stringResource(R.string.hint_hint), style = UType.Muted)
                Spacer(Modifier.height(6.dp))
                UTextField(hint, { hint = it }, placeholder = stringResource(R.string.hint_placeholder))
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.origin), style = UType.Strong)
                Spacer(Modifier.height(6.dp))
                UTextField(origin, { origin = it }, placeholder = stringResource(R.string.origin_placeholder))
                val origins = field.finds.map { it.origin }.filter { it.isNotBlank() && it != origin }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(6).map { it.key }
                if (origins.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        origins.forEach { o -> Chip(o, false, { origin = o }) }
                    }
                }
                val vessels = field.vesselsOf(digId)
                if (vessels.isNotEmpty()) {
                    SectionTitle(stringResource(R.string.vessel))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Chip(stringResource(R.string.vessel_none), vesselId == 0L || vessels.none { it.id == vesselId }, { vesselId = 0 })
                        vessels.forEach { v -> Chip(v.name, v.id == vesselId, { vesselId = v.id }, U.c.bronze) }
                    }
                }
                SectionTitle(stringResource(R.string.picture))
                if (image.isNotBlank()) {
                    val bmp = rememberPicture(image)
                    if (bmp != null) {
                        Row(verticalAlignment = Alignment.Top) {
                            Image(bmp, null, Modifier.size(120.dp).clip(U.RMd), contentScale = ContentScale.Crop)
                            IconButton(onClick = { image = "" }) { Icon(Icons.Rounded.Close, stringResource(R.string.picture_remove), tint = U.c.muted) }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SoftButton(stringResource(R.string.picture_camera), {
                            val u = Images.captureUri(c)
                            shot = u
                            runCatching { camera.launch(u) }
                        }, Modifier.weight(1f), icon = Icons.Rounded.PhotoCamera)
                        SoftButton(stringResource(R.string.picture_gallery), {
                            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }, Modifier.weight(1f), icon = Icons.Rounded.Image)
                    }
                }
            }
            if (added > 0) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    dig?.let { DigSeal(it.symbol, U.clay(it.clay), 22.dp) }
                    Spacer(Modifier.width(8.dp))
                    Text(app.stele.ui.pluralStringResource(R.plurals.added_now, added, added), style = UType.Muted)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        val ok = front.isNotBlank()
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (old == null) SoftButton(stringResource(R.string.save_and_next), { if (ok) save(true) }, Modifier.weight(1f).height(54.dp))
            PrimaryButton(stringResource(R.string.save), { save(false) }, Modifier.weight(1f), enabled = ok)
        }
    }
}
