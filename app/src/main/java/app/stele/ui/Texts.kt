package app.stele.ui

import android.content.Context
import app.stele.R
import app.stele.data.Kind
import app.stele.data.LinkKind
import app.stele.data.Mode
import app.stele.domain.Hall
import app.stele.domain.Shape
import app.stele.domain.Stage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Words and dates, in the app's language. Plain functions over a [Context] so receivers can use them too. */
object T {
    fun locale(c: Context): Locale = c.resources.configuration.locales[0]

    fun local(ms: Long, zone: ZoneId = ZoneId.systemDefault()) = Instant.ofEpochMilli(ms).atZone(zone)

    /** "17 Oct 2026". */
    fun date(c: Context, ms: Long): String = local(ms).format(DateTimeFormatter.ofPattern("d MMM yyyy", locale(c)))

    /** "Saturday, 17 October". */
    fun longDay(c: Context, ms: Long): String =
        local(ms).format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale(c))).replaceFirstChar { it.titlecase(locale(c)) }

    fun stage(c: Context, s: Stage): String = c.getString(
        when (s) {
            Stage.GROUND -> R.string.stage_ground
            Stage.SHARD -> R.string.stage_shard
            Stage.CLEANED -> R.string.stage_cleaned
            Stage.RESTORED -> R.string.stage_restored
            Stage.EXHIBIT -> R.string.stage_exhibit
        }
    )

    fun stageHint(c: Context, s: Stage): String = c.getString(
        when (s) {
            Stage.GROUND -> R.string.stage_ground_hint
            Stage.SHARD -> R.string.stage_shard_hint
            Stage.CLEANED -> R.string.stage_cleaned_hint
            Stage.RESTORED -> R.string.stage_restored_hint
            Stage.EXHIBIT -> R.string.stage_exhibit_hint
        }
    )

    fun kind(c: Context, k: Kind): String = c.getString(
        when (k) {
            Kind.PAIR -> R.string.kind_pair
            Kind.TERM -> R.string.kind_term
            Kind.PHRASE -> R.string.kind_phrase
            Kind.QUESTION -> R.string.kind_question
        }
    )

    fun mode(c: Context, m: Mode): String = c.getString(
        when (m) {
            Mode.FORWARD -> R.string.mode_forward
            Mode.REVERSE -> R.string.mode_reverse
            Mode.GAP -> R.string.mode_gap
            Mode.ECHO -> R.string.mode_echo
            Mode.DOOR -> R.string.mode_door
            Mode.FORGERY -> R.string.mode_forgery
        }
    )

    fun shape(c: Context, s: Shape): String = c.getString(
        when (s) {
            Shape.AMPHORA -> R.string.shape_amphora
            Shape.KRATER -> R.string.shape_krater
            Shape.JUG -> R.string.shape_jug
            Shape.BOWL -> R.string.shape_bowl
            Shape.LAMP -> R.string.shape_lamp
            Shape.MASK -> R.string.shape_mask
            Shape.COIN -> R.string.shape_coin
            Shape.TABLET -> R.string.shape_tablet
        }
    )

    fun hall(c: Context, h: Hall): String = c.getString(
        when (h) {
            Hall.ATRIUM -> R.string.hall_atrium
            Hall.AMPHORAE -> R.string.hall_amphorae
            Hall.MASKS -> R.string.hall_masks
            Hall.COINS -> R.string.hall_coins
            Hall.TABLETS -> R.string.hall_tablets
            Hall.MOSAICS -> R.string.hall_mosaics
            Hall.BRONZES -> R.string.hall_bronzes
            Hall.TREASURY -> R.string.hall_treasury
        }
    )

    fun link(c: Context, k: LinkKind): String = c.getString(
        when (k) {
            LinkKind.KIN -> R.string.link_kin
            LinkKind.CONFUSED -> R.string.link_confused
            LinkKind.OPPOSITE -> R.string.link_opposite
        }
    )

    /** "10 min", "3 days", "2 months". */
    fun span(c: Context, days: Double): String {
        val r = c.resources
        fun q(id: Int, n: Int) = r.getQuantityString(id, n, n)
        return when {
            days < 1.0 / 24 -> q(R.plurals.span_minutes, (days * 24 * 60).roundToInt().coerceAtLeast(1))
            days < 1 -> q(R.plurals.span_hours, (days * 24).roundToInt().coerceAtLeast(1))
            days < 45 -> q(R.plurals.span_days, days.roundToInt())
            days < 365 -> q(R.plurals.span_months, (days / 30).roundToInt())
            else -> q(R.plurals.span_years, (days / 365).roundToInt().coerceAtLeast(1))
        }
    }

    /** "in 3 days". */
    fun inTime(c: Context, days: Double): String = c.getString(R.string.in_time, span(c, days))

    /** Languages a dig can be in, for speaking; the first is "not a language". */
    val langs = listOf("", "en", "es", "de", "fr", "it", "pt", "nl", "pl", "cs", "uk", "ru", "tr", "el", "sv", "fi", "ar", "he", "hi", "zh", "ja", "ko", "la")

    fun langName(c: Context, tag: String): String =
        if (tag.isBlank()) c.getString(R.string.lang_none)
        else Locale.forLanguageTag(tag).getDisplayLanguage(locale(c)).replaceFirstChar { it.titlecase(locale(c)) }
}
