package app.stele.ui.forgery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.stele.R
import app.stele.ui.components.Hint
import app.stele.ui.components.Hints
import app.stele.container
import app.stele.data.Find
import app.stele.domain.Memory
import app.stele.domain.Rules
import app.stele.domain.Stage
import app.stele.ui.Field
import app.stele.ui.Go
import app.stele.ui.art.ShardArt
import app.stele.ui.components.Bar
import app.stele.ui.components.PrimaryButton
import app.stele.ui.components.TopBar
import app.stele.ui.components.title
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType

/** Forgeries: the meaning on a placard, the genuine front among look-alikes. A mix-up is remembered as one. */
@Composable
fun ForgeryScreen(digId: Long, field: Field, go: Go) {
    val c = LocalContext.current
    val container = c.container
    val rounds = remember {
        Rules.forgeries(if (digId != 0L) field.findsOf(digId) else field.finds, field.links, 8, field.now / (Memory.DAY / 24))
    }
    var index by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Find?>(null) }
    val mixed = remember { mutableStateListOf<Pair<Find, Find>>() }
    var right by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.forgery_title), go::back)
        if (index < rounds.size) Bar(index / rounds.size.toFloat(), Modifier.padding(horizontal = 16.dp), color = U.c.ochre, track = U.c.surface2)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (index >= rounds.size) {
                Text(stringResource(R.string.forgery_done_title), style = UType.Display)
                Text(stringResource(R.string.forgery_done, right, rounds.size), style = UType.Sub)
                if (mixed.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.forgery_mixed).uppercase(), style = UType.Eyebrow)
                    mixed.forEach { (g, p) ->
                        Text("${g.title()} ≠ ${p.title()}", style = UType.Name.copy(fontSize = 19.sp), modifier = Modifier.padding(vertical = 4.dp))
                    }
                    Text(stringResource(R.string.forgery_mixed_text), style = UType.Muted)
                }
                return@Column
            }
            val r = rounds[index]
            if (index == 0) Hint(Hints.FORGERY, field)
            Text(stringResource(R.string.forgery_ask), style = UType.Sub)
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier.fillMaxWidth().clip(U.RLg).background(U.c.night).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.forgery_placard).uppercase(), style = UType.Eyebrow.copy(color = U.c.gold))
                Spacer(Modifier.height(6.dp))
                Text(r.genuine.back, style = UType.Find.copy(color = U.c.limestone, fontSize = 28.sp, textAlign = TextAlign.Center))
            }
            Spacer(Modifier.height(16.dp))
            r.choices.forEach { ch ->
                val p = picked
                val isGenuine = ch.id == r.genuine.id
                val border = when {
                    p == null -> U.c.line
                    isGenuine -> U.c.gold
                    ch.id == p.id -> U.c.danger
                    else -> U.c.line
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(U.RMd)
                        .background(if (p != null && isGenuine) U.c.goldSoft else U.c.limestone)
                        .border(if (p != null && (isGenuine || ch.id == p.id)) 2.dp else 1.dp, border, U.RMd)
                        .clickable(enabled = p == null) {
                            picked = ch
                            if (isGenuine) right++ else mixed += r.genuine to ch
                            container.write { keeper.forgery(r.genuine, ch) }
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShardArt(if (p != null && isGenuine) Stage.EXHIBIT else Stage.CLEANED, Modifier.size(30.dp), seed = ch.id.toInt())
                    Spacer(Modifier.width(12.dp))
                    Text(ch.title(), style = UType.Name.copy(fontSize = 22.sp), modifier = Modifier.weight(1f))
                    if (p != null && isGenuine) Icon(Icons.Rounded.Verified, null, tint = U.c.goldDeep)
                    if (p != null && !isGenuine && ch.id == p.id) Icon(Icons.Rounded.Cancel, null, tint = U.c.danger)
                }
            }
            picked?.let { p ->
                Spacer(Modifier.height(10.dp))
                Text(
                    if (p.id == r.genuine.id) stringResource(R.string.forgery_right) else stringResource(R.string.forgery_wrong, p.title(), p.back),
                    style = UType.Sub.copy(color = if (p.id == r.genuine.id) U.c.bronze else U.c.danger),
                )
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                index >= rounds.size -> PrimaryButton(stringResource(R.string.done), { go.back() }, Modifier.fillMaxWidth())
                picked != null -> PrimaryButton(stringResource(R.string.next), { picked = null; index++ }, Modifier.fillMaxWidth())
            }
        }
    }
}
