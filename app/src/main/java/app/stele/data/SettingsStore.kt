package app.stele.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.stele.domain.Gender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** A find wants cleaning when the chance of recalling it drops below this. */
    val retention: Float = 0.9f,
    /** New finds brought up for their first cleaning each day. */
    val newPerDay: Int = 10,
    /** The evening bell: the day's test pit and the dust, if any. */
    val bell: Boolean = true,
    val bellAt: Int = 19 * 60,
    /** Finds in the day's test pit. */
    val soundingSize: Int = 5,
    /** Epoch day the test pit was last dug. */
    val soundingDay: Long = -1,
    /** For the Russian "{м|ж}" marks. */
    val gender: Gender = Gender.MALE,
    /** The dig new finds go to, unless chosen otherwise. */
    val lastDig: Long = 0,
    /** When the sealed door last opened, and how its last knock went; -1 before the first. */
    val doorAt: Long = 0,
    val doorScore: Int = -1,
    val doorOf: Int = 0,
    /** Halls opened behind the door, beyond the atrium. */
    val halls: Int = 0,
    /** The last hall whose opening has been shown. */
    val hallSeen: Int = 0,
    /** Screens whose first-visit placard has been put away. */
    val hints: Set<String> = emptySet(),
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        retention = this[RETENTION] ?: 0.9f,
        newPerDay = this[NEW_PER_DAY] ?: 10,
        bell = this[BELL] ?: true,
        bellAt = this[BELL_AT] ?: (19 * 60),
        soundingSize = this[SOUNDING_SIZE] ?: 5,
        soundingDay = this[SOUNDING_DAY] ?: -1L,
        gender = Gender.of(this[GENDER]),
        lastDig = this[LAST_DIG] ?: 0L,
        doorAt = this[DOOR_AT] ?: 0L,
        doorScore = this[DOOR_SCORE] ?: -1,
        doorOf = this[DOOR_OF] ?: 0,
        halls = this[HALLS] ?: 0,
        hallSeen = this[HALL_SEEN] ?: 0,
        hints = this[HINTS] ?: emptySet(),
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[RETENTION] = s.retention
            it[NEW_PER_DAY] = s.newPerDay
            it[BELL] = s.bell
            it[BELL_AT] = s.bellAt
            it[SOUNDING_SIZE] = s.soundingSize
            it[SOUNDING_DAY] = s.soundingDay
            it[GENDER] = s.gender.name
            it[LAST_DIG] = s.lastDig
            it[DOOR_AT] = s.doorAt
            it[DOOR_SCORE] = s.doorScore
            it[DOOR_OF] = s.doorOf
            it[HALLS] = s.halls
            it[HALL_SEEN] = s.hallSeen
            it[HINTS] = s.hints
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val RETENTION = floatPreferencesKey("retention")
        val NEW_PER_DAY = intPreferencesKey("new_per_day")
        val BELL = booleanPreferencesKey("bell")
        val BELL_AT = intPreferencesKey("bell_at")
        val SOUNDING_SIZE = intPreferencesKey("sounding_size")
        val SOUNDING_DAY = longPreferencesKey("sounding_day")
        val GENDER = stringPreferencesKey("gender")
        val LAST_DIG = longPreferencesKey("last_dig")
        val DOOR_AT = longPreferencesKey("door_at")
        val DOOR_SCORE = intPreferencesKey("door_score")
        val DOOR_OF = intPreferencesKey("door_of")
        val HALLS = intPreferencesKey("halls")
        val HALL_SEEN = intPreferencesKey("hall_seen")
        val HINTS = stringSetPreferencesKey("hints")
    }
}
