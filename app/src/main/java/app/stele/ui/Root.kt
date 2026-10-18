package app.stele.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.stele.BuildConfig
import app.stele.R
import app.stele.container
import app.stele.data.Dig
import app.stele.data.Find
import app.stele.data.Link
import app.stele.data.Review
import app.stele.data.Settings
import app.stele.data.Vessel
import app.stele.domain.Rules
import app.stele.domain.Shape
import app.stele.domain.Stage
import app.stele.notify.Bell
import app.stele.ui.art.ShardLook
import app.stele.ui.clean.CleanScreen
import app.stele.ui.components.rememberNow
import app.stele.ui.digs.DigEditScreen
import app.stele.ui.digs.DigScreen
import app.stele.ui.digs.DigsTab
import app.stele.ui.digs.PickScreen
import app.stele.ui.digs.VesselEditScreen
import app.stele.ui.digs.VesselScreen
import app.stele.ui.door.DoorScreen
import app.stele.ui.find.AddScreen
import app.stele.ui.find.BulkScreen
import app.stele.ui.find.FindScreen
import app.stele.ui.find.LinkScreen
import app.stele.ui.forgery.ForgeryScreen
import app.stele.ui.journal.JournalTab
import app.stele.ui.museum.MuseumTab
import app.stele.ui.onboarding.OnboardingScreen
import app.stele.ui.settings.SettingsScreen
import app.stele.ui.text.TextScreen
import app.stele.ui.theme.Manrope
import app.stele.ui.theme.U
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Tab { MUSEUM, DIGS, JOURNAL }

/** What the app was opened for from outside. */
class Request(val kind: String, val text: String = "", val n: Int = 0) {
    companion object {
        const val SOUNDING = "sounding"
        const val ADD = "add"
        const val CLEAN = "clean"
        const val TEXT = "text"
    }
}

/** Everything stored, plus what is derived from it, recomputed every minute or on change. */
@Immutable
class Field(
    val digs: List<Dig>,
    val vessels: List<Vessel>,
    val finds: List<Find>,
    val links: List<Link>,
    val reviews: List<Review>,
    val settings: Settings,
    val now: Long,
) {
    val zone: ZoneId = ZoneId.systemDefault()
    val today: LocalDate = LocalDate.now(zone)
    val retention: Double get() = settings.retention.toDouble()
    fun day(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    private val byDig = finds.groupBy { it.digId }
    private val byVessel = finds.filter { it.vesselId != 0L }.groupBy { it.vesselId }
    private val reviewsBy = reviews.groupBy { it.findId }

    fun dig(id: Long) = digs.firstOrNull { it.id == id }
    fun vessel(id: Long) = vessels.firstOrNull { it.id == id }
    fun find(id: Long) = finds.firstOrNull { it.id == id }
    fun findsOf(digId: Long): List<Find> = byDig[digId].orEmpty()
    fun findsIn(vesselId: Long): List<Find> = byVessel[vesselId].orEmpty().sortedBy { it.createdAt }
    fun vesselsOf(digId: Long): List<Vessel> = vessels.filter { it.digId == digId }
    fun linksOf(findId: Long): List<Link> = links.filter { it.a == findId || it.b == findId }
    fun reviewsOf(findId: Long): List<Review> = reviewsBy[findId].orEmpty().sortedByDescending { it.at }

    fun patina(f: Find): Float = Rules.patina(f, now, retention)
    fun shardsOf(v: Vessel): List<ShardLook> = findsIn(v.id).map { ShardLook(it.stage, patina(it)) }
    fun mended(v: Vessel): Boolean = Rules.mended(findsIn(v.id))
    fun look(v: Vessel): Pair<Shape, List<ShardLook>> = Shape.of(v.shape) to shardsOf(v)

    val dusty: List<Find> = Rules.dusty(finds, now, retention)
    val buried: List<Find> = Rules.buried(finds)
    val newLeft: Int = Rules.newLeft(reviews, finds, settings.newPerDay, today, ::day)
    val newToday: Int get() = minOf(newLeft, buried.size)
    val unlabelled: List<Find> = finds.filter { !it.askable }
    val cracked: List<Find> = finds.filter { it.cracked }
    val byStage: Map<Stage, Int> = Stage.entries.associateWith { s -> finds.count { it.askable && it.stage == s } }
    val exhibits: List<Find> = finds.filter { it.stage == Stage.EXHIBIT }.sortedByDescending { it.stability }
    val mendedVessels: List<Vessel> = vessels.filter { it.mendedAt != null }

    fun round(digId: Long = 0, vesselId: Long = 0): List<Find> {
        val pool = when {
            vesselId != 0L -> findsIn(vesselId)
            digId != 0L -> findsOf(digId)
            else -> finds
        }
        return Rules.round(pool, now, retention, newLeft)
    }

    val soundingDone: Boolean = settings.soundingDay == today.toEpochDay()
    val sounding: Rules.Sounding? = Rules.sounding(finds, now, retention, settings.soundingSize, today.toEpochDay())
    val doorDue: Boolean = Rules.doorDue(finds, now, settings.doorAt)
    val doorDaysLeft: Int? = Rules.doorDaysLeft(finds, now, settings.doorAt)
    val forgeriesOpen: Boolean by lazy { Rules.forgeries(finds, links, 1, 0).isNotEmpty() }
    val fieldDays: Set<LocalDate> = Rules.fieldDays(finds, reviews, ::day)
    val cleanedToday: Int = reviews.count { day(it.at) == today && it.mode != app.stele.data.Mode.FORGERY.name }
    /** A hall opened behind the door that hasn't been shown yet. */
    val newHall: Int? = if (settings.halls > settings.hallSeen) settings.halls else null
}

/** Where a cleaning's finds come from. */
object Source {
    const val DUE = "due"
    const val SOUNDING = "sounding"
    const val WORKSHOP = "workshop"
    fun dig(id: Long) = "dig-$id"
    fun vessel(id: Long) = "vessel-$id"
    fun find(id: Long) = "find-$id"
}

/** Text handed from outside to a screen that opens next. */
object Handoff {
    var front: String = ""
    var text: String = ""
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController) {
    fun newDig() = nav.navigate("digedit/0") { launchSingleTop = true }
    fun editDig(id: Long) = nav.navigate("digedit/$id") { launchSingleTop = true }
    fun dig(id: Long) = nav.navigate("dig/$id") { launchSingleTop = true }
    fun newVessel(digId: Long) = nav.navigate("vesseledit/$digId/0") { launchSingleTop = true }
    fun editVessel(digId: Long, id: Long) = nav.navigate("vesseledit/$digId/$id") { launchSingleTop = true }
    fun vessel(id: Long) = nav.navigate("vessel/$id") { launchSingleTop = true }
    fun pick(vesselId: Long) = nav.navigate("pick/$vesselId") { launchSingleTop = true }
    fun add(digId: Long = 0, vesselId: Long = 0) = nav.navigate("add/$digId/$vesselId/0") { launchSingleTop = true }
    fun edit(findId: Long) = nav.navigate("add/0/0/$findId") { launchSingleTop = true }
    fun bulk(digId: Long) = nav.navigate("bulk/$digId") { launchSingleTop = true }
    fun find(id: Long) = nav.navigate("find/$id") { launchSingleTop = true }
    fun link(findId: Long) = nav.navigate("link/$findId") { launchSingleTop = true }
    fun clean(source: String) = nav.navigate("clean/$source") { launchSingleTop = true }
    fun door() = nav.navigate("door") { launchSingleTop = true }
    fun forgery(digId: Long = 0) = nav.navigate("forgery/$digId") { launchSingleTop = true }
    fun text() = nav.navigate("text") { launchSingleTop = true }
    fun settings() = nav.navigate("settings") { launchSingleTop = true }
    fun back() = nav.popBackStack()

    /** After saving a new dig: straight into it, with the editor gone from the back stack. */
    fun replaceWithDig(id: Long) {
        nav.popBackStack()
        nav.navigate("dig/$id") { launchSingleTop = true }
    }
}

@Composable
fun SteleRoot(settings: Settings, request: Request? = null) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    var tab by rememberSaveable { mutableStateOf(Tab.MUSEUM) }
    val start = remember { if (settings.onboarded) "main" else "welcome" }
    val dao = container.db.dao()

    val digs by dao.observeDigs().collectAsStateWithLifecycle(initialValue = null)
    val vessels by dao.observeVessels().collectAsStateWithLifecycle(initialValue = null)
    val finds by dao.observeFinds().collectAsStateWithLifecycle(initialValue = null)
    val links by dao.observeLinks().collectAsStateWithLifecycle(initialValue = null)
    val reviews by dao.observeReviews().collectAsStateWithLifecycle(initialValue = null)
    val tick = rememberNow(60_000)
    val field = remember(digs, vessels, finds, links, reviews, settings, tick) {
        Field(digs.orEmpty(), vessels.orEmpty(), finds.orEmpty(), links.orEmpty(), reviews.orEmpty(), settings, System.currentTimeMillis())
    }
    val go = remember(nav) { Go(nav) }

    // Opened from outside: a bell, a shortcut, the tile, shared or selected text.
    var handled by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(request, settings.onboarded, digs != null) {
        val r = request ?: return@LaunchedEffect
        if (r.n <= handled || !settings.onboarded || digs == null) return@LaunchedEffect
        handled = r.n
        when (r.kind) {
            Request.SOUNDING -> {
                Bell.clear(context)
                go.clean(if (!field.soundingDone && field.sounding != null) Source.SOUNDING else Source.DUE)
            }
            Request.CLEAN -> go.clean(Source.DUE)
            Request.ADD -> go.add()
            Request.TEXT -> {
                val t = r.text.trim()
                // A word or a short phrase is a find; anything longer is a text to sift.
                if (t.length <= 60 && t.split(Regex("\\s+")).size <= 5 && '\n' !in t) {
                    Handoff.front = t
                    go.add()
                } else {
                    Handoff.text = t
                    go.text()
                }
            }
        }
    }

    // Onboarded elsewhere (a restored backup, the demo seed): leave the welcome.
    LaunchedEffect(settings.onboarded) {
        if (settings.onboarded && nav.currentDestination?.route == "welcome") nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
    }

    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(field.finds.isNotEmpty()) {
        // Asked when it first matters: once there is a find that will gather dust.
        if (field.finds.isNotEmpty() && settings.bell && Build.VERSION.SDK_INT >= 33 && !Bell.canNotify(context) && !BuildConfig.DEMO) {
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CompositionLocalProvider(LocalGender provides settings.gender) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.background(U.c.bg),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            composable("welcome") {
                OnboardingScreen { gender ->
                    container.write { this.settings.update { it.copy(onboarded = true, gender = gender) } }
                    nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
                    go.newDig()
                }
            }
            composable("main") { MainTabs(tab, { tab = it }, go, field) }
            composable("digedit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                DigEditScreen(e.arguments?.getLong("id") ?: 0L, field, go)
            }
            composable("dig/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                DigScreen(e.arguments?.getLong("id") ?: 0L, field, go)
            }
            composable(
                "vesseledit/{dig}/{id}",
                arguments = listOf(navArgument("dig") { type = NavType.LongType }, navArgument("id") { type = NavType.LongType }),
            ) { e -> VesselEditScreen(e.arguments?.getLong("dig") ?: 0L, e.arguments?.getLong("id") ?: 0L, field, go) }
            composable("vessel/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                VesselScreen(e.arguments?.getLong("id") ?: 0L, field, go)
            }
            composable("pick/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                PickScreen(e.arguments?.getLong("id") ?: 0L, field, go)
            }
            composable(
                "add/{dig}/{vessel}/{find}",
                arguments = listOf(
                    navArgument("dig") { type = NavType.LongType },
                    navArgument("vessel") { type = NavType.LongType },
                    navArgument("find") { type = NavType.LongType },
                ),
            ) { e -> AddScreen(e.arguments?.getLong("dig") ?: 0L, e.arguments?.getLong("vessel") ?: 0L, e.arguments?.getLong("find") ?: 0L, field, go) }
            composable("bulk/{dig}", arguments = listOf(navArgument("dig") { type = NavType.LongType })) { e ->
                BulkScreen(e.arguments?.getLong("dig") ?: 0L, field, go)
            }
            composable("find/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                FindScreen(e.arguments?.getLong("id") ?: 0L, field, go)
            }
            composable("link/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                LinkScreen(e.arguments?.getLong("id") ?: 0L, field, go)
            }
            composable("clean/{source}", arguments = listOf(navArgument("source") { type = NavType.StringType })) { e ->
                CleanScreen(e.arguments?.getString("source").orEmpty(), field, go)
            }
            composable("door") { DoorScreen(field, go) }
            composable("forgery/{dig}", arguments = listOf(navArgument("dig") { type = NavType.LongType })) { e ->
                ForgeryScreen(e.arguments?.getLong("dig") ?: 0L, field, go)
            }
            composable("text") { TextScreen(field, go) }
            composable("settings") { SettingsScreen(field, go) }
        }
    }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, field: Field) {
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.MUSEUM -> MuseumTab(field, go)
                Tab.DIGS -> DigsTab(field, go)
                Tab.JOURNAL -> JournalTab(field, go)
            }
        }
        HorizontalDivider(color = U.c.line)
        Row(
            Modifier.fillMaxWidth().background(U.c.surface).navigationBarsPadding().height(66.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Rounded.AccountBalance, stringResource(R.string.tab_museum), tab == Tab.MUSEUM) { onTab(Tab.MUSEUM) }
            NavItem(Icons.Rounded.Landscape, stringResource(R.string.tab_digs), tab == Tab.DIGS) { onTab(Tab.DIGS) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(50.dp).shadow(3.dp, CircleShape).clip(CircleShape).background(U.c.clay).clickable { if (field.digs.isEmpty()) go.newDig() else go.add() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, stringResource(R.string.new_find), Modifier.size(28.dp), tint = U.c.surface) }
            }
            NavItem(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.tab_journal), tab == Tab.JOURNAL) { onTab(Tab.JOURNAL) }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) U.c.clay else U.c.faint
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(if (selected) U.c.claySoft else Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp),
        ) { Icon(icon, null, Modifier.size(22.dp), tint = color) }
        Spacer(Modifier.height(3.dp))
        Text(label, fontFamily = Manrope, fontSize = 11.5.sp, color = color, maxLines = 1)
    }
}
