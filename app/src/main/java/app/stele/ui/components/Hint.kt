package app.stele.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stele.R
import app.stele.container
import app.stele.ui.Field
import app.stele.ui.art.DigSeal
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType

/** The screens that explain themselves on a first visit. */
enum class Hints(val title: Int, val text: Int, val glyph: Int) {
    MUSEUM(R.string.hint_museum_title, R.string.hint_museum, 9),
    DIGS(R.string.hint_digs_title, R.string.hint_digs, 11),
    DIG(R.string.hint_dig_title, R.string.hint_dig, 0),
    VESSEL(R.string.hint_vessel_title, R.string.hint_vessel, 2),
    FIND(R.string.hint_find_title, R.string.hint_find, 1),
    ADD(R.string.hint_add_title, R.string.hint_add, 8),
    BULK(R.string.hint_bulk_title, R.string.hint_bulk, 5),
    CLEAN(R.string.hint_clean_title, R.string.hint_clean, 6),
    DOOR(R.string.hint_door_title, R.string.hint_door, 5),
    FORGERY(R.string.hint_forgery_title, R.string.hint_forgery, 1),
    TEXT(R.string.hint_text_title, R.string.hint_text, 3),
    JOURNAL(R.string.hint_journal_title, R.string.hint_journal, 10),
    LINK(R.string.hint_link_title, R.string.hint_link, 7),
    ANKI(R.string.hint_anki_title, R.string.hint_anki, 4),
}

/**
 * A museum placard that explains the screen on a first visit: what happens here and what to do.
 * "Got it" puts it away for good; Settings can bring them all back.
 */
@Composable
fun Hint(hint: Hints, field: Field, modifier: Modifier = Modifier) {
    val container = LocalContext.current.container
    var gone by remember { mutableStateOf(false) }
    AnimatedVisibility(hint.name !in field.settings.hints && !gone, exit = fadeOut() + shrinkVertically()) {
        Column(
            modifier.fillMaxWidth().padding(vertical = 6.dp).clip(U.RMd).background(U.c.limestone).border(1.dp, U.c.line, U.RMd).padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DigSeal(hint.glyph, U.c.clay, 30.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.hint_eyebrow).uppercase(), style = UType.Eyebrow)
                    Text(stringResource(hint.title), style = UType.Name.copy(fontSize = 19.sp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(hint.text), style = UType.Sub.copy(color = U.c.ink))
            Spacer(Modifier.height(10.dp))
            SoftButton(stringResource(R.string.hint_ok), {
                gone = true
                container.write { settings.update { it.copy(hints = it.hints + hint.name) } }
            }, Modifier.height(42.dp))
        }
    }
}
