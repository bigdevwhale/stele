package app.stele.ui

import androidx.annotation.ArrayRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import app.stele.domain.Gender
import app.stele.domain.gendered

/** The digger's gender, for the Russian "{м|ж}" marks in the strings. */
val LocalGender = staticCompositionLocalOf { Gender.MALE }

/*
 * The app's own string lookups: the same as Compose's, with gender marks resolved. Screens import
 * these instead of androidx.compose.ui.res's, so every string goes through them.
 */

@Composable
fun stringResource(@StringRes id: Int, vararg args: Any): String = gendered(
    // Without arguments the text is not a format: "90%" must come back as it is.
    if (args.isEmpty()) androidx.compose.ui.res.stringResource(id) else androidx.compose.ui.res.stringResource(id, *args),
    LocalGender.current,
)

@Composable
fun pluralStringResource(@PluralsRes id: Int, count: Int, vararg args: Any): String = gendered(
    if (args.isEmpty()) androidx.compose.ui.res.pluralStringResource(id, count) else androidx.compose.ui.res.pluralStringResource(id, count, *args),
    LocalGender.current,
)

@Composable
fun stringArrayResource(@ArrayRes id: Int): List<String> =
    androidx.compose.ui.res.stringArrayResource(id).map { gendered(it, LocalGender.current) }
