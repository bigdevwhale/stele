package app.stele.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.stele.MainActivity
import app.stele.R
import app.stele.container
import app.stele.domain.Rules
import app.stele.domain.gendered
import app.stele.localized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * One bell a day, at the hour the digger chose: the day's test pit is ready, or dust has settled on
 * some finds. It rings only when there is something to do.
 */
object Bell {
    private const val CHANNEL = "bell_v1"
    private const val ACTION = "app.stele.notify.BELL"

    /** Opens the app straight into the day's test pit (or the cleaning, if there is no pit). */
    const val OPEN = "app.stele.SOUNDING"

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) != null) return
        val l = context.localized()
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, l.getString(R.string.channel_bell), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = l.getString(R.string.channel_bell_desc)
            }
        )
    }

    private fun pi(context: Context) = PendingIntent.getBroadcast(
        context, 1, Intent(context, BellReceiver::class.java).setAction(ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** The next moment a daily bell at [minutes] after midnight rings, strictly after [now]. */
    fun next(now: Long, minutes: Int, zone: ZoneId): Long {
        val n = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone)
        var at = n.toLocalDate().atTime(minutes / 60, minutes % 60).atZone(zone)
        if (!at.isAfter(n)) at = at.plusDays(1)
        return at.toInstant().toEpochMilli()
    }

    suspend fun reschedule(context: Context) {
        val s = context.container.settings.now()
        val am = context.getSystemService(AlarmManager::class.java)
        val p = pi(context)
        // A few minutes either way is fine for a bell; this avoids the exact-alarm permission.
        if (!s.bell || !s.onboarded) am.cancel(p)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next(System.currentTimeMillis(), s.bellAt, ZoneId.systemDefault()), p)
    }

    suspend fun ring(context: Context) {
        val c = context.container
        val s = c.settings.now()
        if (!canNotify(context) || !s.onboarded) return
        val now = System.currentTimeMillis()
        val finds = c.db.dao().finds()
        val today = LocalDate.now().toEpochDay()
        val l = context.localized()
        val dusty = Rules.dusty(finds, now, s.retention.toDouble()).size
        val pit = if (s.soundingDay == today) null else Rules.sounding(finds, now, s.retention.toDouble(), s.soundingSize, today)
        val plain = when {
            pit != null && dusty > 0 -> l.resources.getQuantityString(R.plurals.bell_pit_dust, dusty, dusty)
            pit != null -> l.getString(R.string.bell_pit)
            dusty > 0 -> l.resources.getQuantityString(R.plurals.bell_dust, dusty, dusty)
            else -> return
        }
        val text = gendered(plain, s.gender)
        val open = Intent(context, MainActivity::class.java).setAction(OPEN)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(l.getString(if (pit != null) R.string.bell_title_pit else R.string.bell_title_dust))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(PendingIntent.getActivity(context, 11, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1, n)
        } catch (_: SecurityException) {
        }
    }

    fun clear(context: Context) = NotificationManagerCompat.from(context).cancelAll()

    internal const val RING = ACTION
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class BellReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Bell.RING) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Bell.ring(context)
                Bell.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Bell.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
