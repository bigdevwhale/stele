package app.stele.ui

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** The phone's own voice, for hearing a find in its language. Nothing is sent anywhere. */
class Voice(context: Context) {
    private var ready by mutableStateOf(false)
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { ready = it == TextToSpeech.SUCCESS }

    /** Whether the phone has a voice for [lang]. */
    fun speaks(lang: String): Boolean {
        if (!ready || lang.isBlank()) return false
        return tts.isLanguageAvailable(Locale.forLanguageTag(lang)) >= TextToSpeech.LANG_AVAILABLE
    }

    fun say(text: String, lang: String, slow: Boolean = false) {
        if (!speaks(lang)) return
        tts.language = Locale.forLanguageTag(lang)
        tts.setSpeechRate(if (slow) 0.7f else 0.95f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "stele")
    }

    fun close() = tts.shutdown()
}

@Composable
fun rememberVoice(): Voice {
    val context = LocalContext.current
    val voice = remember { Voice(context) }
    DisposableEffect(voice) { onDispose { voice.close() } }
    return voice
}

/** The phone's recogniser, asked for one word or phrase in [lang] (or the phone's language). */
fun listenIntent(lang: String, prompt: String): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    .apply { if (lang.isNotBlank()) putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang) }

fun canListen(context: Context): Boolean =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).resolveActivity(context.packageManager) != null
