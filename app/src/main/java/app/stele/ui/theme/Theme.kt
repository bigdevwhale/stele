package app.stele.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.stele.R
import app.stele.domain.Stage

/** Lining figures: Cormorant's old-style 1 reads as an I. */
private const val LINING = "lnum"

/**
 * A dig in the sun and a museum in the shade: sand and limestone, umber earth, fired terracotta,
 * ochre, the verdigris of old bronze and a little gold for what is on show.
 */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val clay: Color,
    val claySoft: Color,
    val ochre: Color,
    val ochreSoft: Color,
    val bronze: Color,
    val bronzeSoft: Color,
    val gold: Color,
    val goldDeep: Color,
    val goldSoft: Color,
    val earth: Color,
    val stone: Color,
    val limestone: Color,
    val night: Color,
    val danger: Color,
)

val Sand = Palette(
    bg = Color(0xFFEDE3D1),
    surface = Color(0xFFF8F2E7),
    surface2 = Color(0xFFE3D5BC),
    line = Color(0xFFD5C3A4),
    ink = Color(0xFF2B2119),
    muted = Color(0xFF6E5C49),
    faint = Color(0xFFA08B72),
    clay = Color(0xFFB5552F),
    claySoft = Color(0xFFF1D7C6),
    ochre = Color(0xFFC48A2C),
    ochreSoft = Color(0xFFF3E2BC),
    bronze = Color(0xFF3F7A70),
    bronzeSoft = Color(0xFFD4E5DF),
    gold = Color(0xFFCBA43F),
    goldDeep = Color(0xFF94701F),
    goldSoft = Color(0xFFF4E6B5),
    earth = Color(0xFF8F7A62),
    stone = Color(0xFFC9BCA6),
    limestone = Color(0xFFF2EADB),
    night = Color(0xFF2A211A),
    danger = Color(0xFFA8322A),
)

val LocalPalette = staticCompositionLocalOf { Sand }

/** The carved hand: titles, the finds themselves. */
val Cormorant = FontFamily(
    Font(R.font.cormorant_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_bold, FontWeight.Bold),
    Font(R.font.cormorant_italic, FontWeight.Normal, FontStyle.Italic),
)

/** The plain hand of the field notebook: buttons, counts, labels. */
val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(24.dp)
    val RLg = RoundedCornerShape(18.dp)
    val RMd = RoundedCornerShape(12.dp)
    val RSm = RoundedCornerShape(8.dp)

    /** The colour of each stage, from earth to gold. */
    @Composable
    fun stage(s: Stage): Color = when (s) {
        Stage.GROUND -> c.earth
        Stage.SHARD -> c.ochre
        Stage.CLEANED -> c.clay
        Stage.RESTORED -> c.bronze
        Stage.EXHIBIT -> c.gold
    }

    @Composable
    fun stageSoft(s: Stage): Color = when (s) {
        Stage.GROUND -> c.surface2
        Stage.SHARD -> c.ochreSoft
        Stage.CLEANED -> c.claySoft
        Stage.RESTORED -> c.bronzeSoft
        Stage.EXHIBIT -> c.goldSoft
    }

    /** The clays a dig can be marked with. */
    val clays = listOf(
        Color(0xFFB5552F), Color(0xFFC48A2C), Color(0xFF3F7A70), Color(0xFF3D5C8F),
        Color(0xFF7A4E33), Color(0xFFA9566A), Color(0xFF6C7838), Color(0xFF5B6573),
    )

    fun clay(i: Int): Color = clays[Math.floorMod(i, clays.size)]
}

object UType {
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontFeatureSettings = LINING, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontFeatureSettings = LINING, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 31.sp, color = U.c.ink)
    val Name: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontFeatureSettings = LINING, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 24.sp, color = U.c.ink)
    /** A find as it is carved. */
    val Find: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontFeatureSettings = LINING, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 39.sp, color = U.c.ink)
    val Italic: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontFeatureSettings = LINING, fontStyle = FontStyle.Italic, fontSize = 19.sp, lineHeight = 24.sp, color = U.c.muted)
    /** Small capitals for labels, like a museum placard. */
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, letterSpacing = 0.16.em, color = U.c.clay)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontSize = 14.sp, lineHeight = 20.sp, color = U.c.muted)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontSize = 15.sp, lineHeight = 22.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontSize = 13.sp, lineHeight = 18.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Cormorant, fontFeatureSettings = LINING, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = U.c.ink)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 15.sp)
}

@Composable
fun SteleTheme(content: @Composable () -> Unit) {
    val p = Sand
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = p.clay,
                onPrimary = p.surface,
                primaryContainer = p.claySoft,
                onPrimaryContainer = p.ink,
                secondary = p.bronze,
                secondaryContainer = p.bronzeSoft,
                onSecondaryContainer = p.ink,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface2,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
                error = p.danger,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 14.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
