package app.stele

import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import app.stele.data.DemoSeed
import app.stele.data.Settings
import app.stele.ui.Handoff
import app.stele.ui.components.Hints
import app.stele.ui.SteleRoot
import app.stele.ui.theme.SteleTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the main screens with the demo museum to PNGs (run with `-Prender [-Plang=ru]`): a look without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xhdpi")
class RenderScreens {
    @get:Rule
    val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val lang = System.getProperty("render.lang") ?: "en"
    private val out = File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() }
    private val ru = lang == "ru"

    private fun settle(ms: Long = 3000) {
        rule.mainClock.autoAdvance = false
        var t = 0L
        while (t < ms) {
            Thread.sleep(40)
            rule.mainClock.advanceTimeBy(100)
            t += 100
        }
    }

    private fun shot(name: String) {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        rule.mainClock.advanceTimeByFrame()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val root = rule.activity.window.decorView
        val b = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(android.graphics.Canvas(b))
        File(out, "$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tap(text: String, ms: Long = 2000, exact: Boolean = false) {
        settle(500)
        // Lazy lists compose only what's on screen: walk down until the text appears.
        var i = 0
        while (rule.onAllNodesWithText(text, substring = !exact, useUnmergedTree = exact).fetchSemanticsNodes().isEmpty() && i < 30) {
            runCatching { rule.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(i) }
            settle(200)
            i += 2
        }
        val n = rule.onAllNodesWithText(text, substring = !exact, useUnmergedTree = exact)[0]
        runCatching { n.performScrollTo() }
        n.performClick()
        settle(ms)
    }

    private fun desc(d: String, ms: Long = 1500) {
        if (rule.onAllNodesWithContentDescription(d).fetchSemanticsNodes().isEmpty()) {
            runCatching { rule.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(0) }
            settle(600)
        }
        rule.onAllNodesWithContentDescription(d)[0].performClick()
        settle(ms)
    }

    private var scrolled = 0

    /** Scrolls the screen's list a few items further. */
    private fun up() {
        scrolled += 4
        runCatching { rule.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(scrolled) }
        settle(1200)
    }

    private fun back() = desc(t("Back", "Назад"), 1200)

    private fun t(en: String, ru: String) = if (this.ru) ru else en

    @Test
    fun screens() {
        if (ru) RuntimeEnvironment.setQualifiers("+ru")
        val app = ApplicationProvider.getApplicationContext<SteleApp>()
        runBlocking {
            DemoSeed.seedIfEmpty(app, app.container)
            if (System.getProperty("render.hints") != "true") app.container.settings.update { it.copy(hints = Hints.entries.map { h -> h.name }.toSet()) }
        }
        rule.setContent {
            val s: Settings? by app.container.settings.settings.collectAsState(initial = null)
            SteleTheme { s?.let { SteleRoot(it) } }
        }
        settle(4000)
        shot("1-museum")
        up()
        shot("2-museum-more")
        up()
        shot("3-museum-shelves")
        tap(t("Digs", "Раскопы"), exact = true)
        scrolled = 0
        shot("4-digs")
        tap(t("Spanish", "Испанский"))
        shot("5-dig")
        tap(t("The kitchen", "Кухня"))
        shot("6-vessel")
        back()
        back()
        tap(t("Museum", "Музей"), exact = true)
        runCatching { rule.onAllNodes(hasScrollToIndexAction())[0].performScrollToIndex(0) }
        settle(800)
        tap(t("Start cleaning", "Начать расчистку"))
        shot("7-clean-ask")
        tap(t("Fairly sure", "Скорее да"))
        settle(1500)
        shot("8-clean-revealed")
        back()
        tap(t("to dig up again", "в шурфе"))
        shot("9-pit")
        back()
        tap(t("The sealed door", "Запечатанная дверь"))
        shot("10-door")
        back()
        tap(t("Tell the genuine", "Отличи подлинную"))
        shot("11-forgery")
        runCatching { tap("embarazada", exact = true) }
        shot("12-forgery-picked")
        back()
        Handoff.text = t(
            "We got home in the small hours. Ojalá llueva mañana: la madrugada fue larga, y mañana empieza el viaje con la maleta nueva.",
            "Llegamos a casa de madrugada. Ojalá llueva mañana: la madrugada fue larga, y mañana empieza el viaje con la maleta nueva.",
        )
        desc(t("Sift a text", "Проверить текст"), 2500)
        shot("13-text")
        back()
        tap(t("Journal", "Журнал"), exact = true)
        scrolled = 0
        shot("14-journal")
        up()
        shot("15-journal-more")
        tap(t("Museum", "Музей"), exact = true)
        desc(t("New find", "Новая находка"), 2000)
        shot("16-add")
        back()
        desc(t("Settings", "Настройки"))
        shot("17-settings")
    }
}
