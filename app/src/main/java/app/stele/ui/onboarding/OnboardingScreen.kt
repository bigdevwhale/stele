package app.stele.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.stele.R
import app.stele.domain.Gender
import app.stele.domain.Shape
import app.stele.domain.Stage
import app.stele.ui.LocalGender
import app.stele.ui.art.Dust
import app.stele.ui.art.ShardArt
import app.stele.ui.art.ShardLook
import app.stele.ui.art.VesselArt
import app.stele.ui.components.Chip
import app.stele.ui.components.PrimaryButton
import app.stele.ui.stringResource
import app.stele.ui.theme.U
import app.stele.ui.theme.UType

/** Three steps: everything worth keeping is a find; clean it until it shines; glue vessels, open halls. */
@Composable
fun OnboardingScreen(onDone: (Gender) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var gender by rememberSaveable { mutableStateOf(Gender.MALE) }
    val russian = LocalConfiguration.current.locales[0].language == "ru"
    CompositionLocalProvider(LocalGender provides gender) {
        Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.app_name).uppercase(), style = UType.Eyebrow, modifier = Modifier.align(Alignment.CenterHorizontally))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnimatedContent(step, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) }, label = "step") { s ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.height(220.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            when (s) {
                                0 -> Sweep()
                                1 -> Ladder()
                                else -> Mend()
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            stringResource(listOf(R.string.welcome_1_title, R.string.welcome_2_title, R.string.welcome_3_title)[s]),
                            style = UType.Display, textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(listOf(R.string.welcome_1_text, R.string.welcome_2_text, R.string.welcome_3_text)[s]),
                            style = UType.Sub.copy(textAlign = TextAlign.Center),
                        )
                        if (s == 0 && russian) {
                            Spacer(Modifier.height(18.dp))
                            Text(stringResource(R.string.set_gender), style = UType.Muted)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Chip(stringResource(R.string.gender_male), gender == Gender.MALE, { gender = Gender.MALE })
                                Chip(stringResource(R.string.gender_female), gender == Gender.FEMALE, { gender = Gender.FEMALE })
                            }
                        }
                    }
                }
            }
            Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i -> Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == step) U.c.clay else U.c.line)) }
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                stringResource(if (step < 2) R.string.next else R.string.welcome_start),
                { if (step < 2) step++ else onDone(gender) }, Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** A word on a stone, and the brush sweeping the dust off it, over and over. */
@Composable
private fun Sweep() {
    val t by rememberInfiniteTransition(label = "sweep").animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "brush")
    Box(Modifier.size(260.dp, 130.dp).clip(U.RLg).border(1.dp, U.c.line, U.RLg)) {
        Column(Modifier.matchParentSize().background(U.c.limestone).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.welcome_word), style = UType.Find)
            Text(stringResource(R.string.welcome_word_back), style = UType.Italic)
        }
        Dust((t * 1.25f).coerceAtMost(1f), Modifier.matchParentSize(), seed = 3)
    }
}

/** The five stages side by side. */
@Composable
private fun Ladder() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        Stage.entries.forEachIndexed { i, s ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ShardArt(s, Modifier.size((40 + i * 8).dp), seed = i * 3 + 1)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.size(6.dp).clip(CircleShape).background(U.stage(s)))
            }
        }
    }
}

/** A vessel filling in shard by shard, then its seams turning gold. */
@Composable
private fun Mend() {
    val t by rememberInfiniteTransition(label = "mend").animateFloat(0f, 9f, infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart), label = "n")
    val n = t.toInt()
    val shards = List(7) { i -> ShardLook(if (i < n) Stage.CLEANED else Stage.GROUND) }
    VesselArt(Shape.AMPHORA, shards, n >= 8, Modifier.size(210.dp), seed = 2)
}
