package app.stele

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.stele.data.DemoSeed
import app.stele.data.Keeper
import app.stele.data.SettingsStore
import app.stele.data.SteleDatabase
import app.stele.notify.Bell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(private val context: Context) {
    val db = SteleDatabase.create(context)
    val settings = SettingsStore(context)
    val keeper = Keeper(db)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch {
            block()
            Bell.reschedule(context)
        }
    }
}

class SteleApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Bell.ensureChannel(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@SteleApp, container)
            Bell.reschedule(this@SteleApp)
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as SteleApp).container

/** Resources in the in-app language, for receivers that AppCompat doesn't localise before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}
