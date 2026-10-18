package app.stele.domain

import app.stele.data.Find

/**
 * The life of a find, read off how long it holds in memory: dug up, then a dusty shard, then cleaned,
 * restored and finally on show. Each step needs memory to hold a few times longer.
 */
enum class Stage(val fromDays: Double) {
    /** Written down, never cleaned yet. */
    GROUND(0.0),
    SHARD(0.0),
    CLEANED(7.0),
    RESTORED(30.0),
    EXHIBIT(120.0);

    val next: Stage? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun ofStability(s: Double): Stage = when {
            s <= 0.0 -> GROUND
            s < CLEANED.fromDays -> SHARD
            s < RESTORED.fromDays -> CLEANED
            s < EXHIBIT.fromDays -> RESTORED
            else -> EXHIBIT
        }

        fun of(f: Find): Stage = ofStability(f.stability)
    }
}

/** The forms a vessel can take once its shards are glued. */
enum class Shape {
    AMPHORA, KRATER, JUG, BOWL, LAMP, MASK, COIN, TABLET;

    companion object {
        fun of(id: String?): Shape = entries.firstOrNull { it.name == id } ?: AMPHORA
    }
}

/** The halls of the museum, opened one by one behind the sealed door. The atrium is open from the start. */
enum class Hall { ATRIUM, AMPHORAE, MASKS, COINS, TABLETS, MOSAICS, BRONZES, TREASURY }
