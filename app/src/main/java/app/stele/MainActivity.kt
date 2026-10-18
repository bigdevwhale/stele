package app.stele

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.stele.data.Settings
import app.stele.notify.Bell
import app.stele.ui.Request
import app.stele.ui.SteleRoot
import app.stele.ui.theme.SteleTheme

class MainActivity : AppCompatActivity() {
    /** What the app was opened for (a bell, a shortcut, shared text), with a counter so each one opens again. */
    private var request by mutableStateOf<Request?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) take(intent)
        // Sand: dark icons in the bars.
        val style = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            SteleTheme {
                // Wait for settings so the welcome doesn't flash for someone with a museum.
                settings?.let { SteleRoot(it, request) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    private fun take(intent: Intent?) {
        intent ?: return
        val n = (request?.n ?: 0) + 1
        request = when (intent.action) {
            Bell.OPEN -> Request(Request.SOUNDING, n = n)
            ADD -> Request(Request.ADD, n = n)
            CLEAN -> Request(Request.CLEAN, n = n)
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let { Request(Request.TEXT, it, n) }
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.let { Request(Request.TEXT, it, n) }
            else -> null
        } ?: request
    }

    companion object {
        const val ADD = "app.stele.ADD"
        const val CLEAN = "app.stele.CLEAN"
    }
}
