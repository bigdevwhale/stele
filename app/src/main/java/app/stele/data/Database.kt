package app.stele.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import app.stele.domain.Memory
import app.stele.domain.Stage
import app.stele.domain.Trace
import kotlinx.coroutines.flow.Flow

/** What a find holds, which decides how it is asked. */
enum class Kind {
    /** A word and its translation, or any two things that go together. Asked both ways. */
    PAIR,
    /** A term and what it means. */
    TERM,
    /** A sentence, rule or quotation with [gaps] to fill in. */
    PHRASE,
    /** A question and its answer. */
    QUESTION;

    companion object {
        fun of(id: String?): Kind = entries.firstOrNull { it.name == id } ?: PAIR
    }
}

/** How two finds are tied. */
enum class LinkKind { KIN, CONFUSED, OPPOSITE;
    companion object {
        fun of(id: String?): LinkKind = entries.firstOrNull { it.name == id } ?: KIN
    }
}

/** A dig: one collection of finds, a language, a subject, a field of work. */
@Entity(tableName = "digs")
data class Dig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** An index into the glyphs carved on its marker. */
    val symbol: Int = 0,
    /** An index into the clays. */
    val clay: Int = 0,
    /** BCP-47 tag of the fronts' language, for speaking and listening; empty when it isn't a language. */
    val lang: String = "",
    val note: String = "",
    val createdAt: Long,
)

/** A vessel: a few finds on one theme that glue together into one object once all are cleaned. */
@Entity(tableName = "vessels", indices = [Index("digId")])
data class Vessel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val digId: Long,
    val name: String,
    /** [app.stele.domain.Shape] name. */
    val shape: String,
    val createdAt: Long,
    /** When it first stood whole; null while shards are missing. */
    val mendedAt: Long? = null,
)

/**
 * A find: one thing to keep in memory. Before its first cleaning it is still in the ground
 * ([stability] is 0); after that its trace says how deep the dust lies.
 */
@Entity(tableName = "finds", indices = [Index("digId"), Index("vesselId")])
data class Find(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val digId: Long,
    /** 0 when it lies loose in the dig. */
    val vesselId: Long = 0,
    /** [Kind] name. */
    val kind: String = Kind.PAIR.name,
    val front: String,
    /** Empty when it was dug up without a label (from a text): not asked until labelled. */
    val back: String = "",
    /** A sentence of one's own with it. */
    val example: String = "",
    /** An association, a mnemonic, an image in words. */
    val hint: String = "",
    /** Where it was met: a show, a colleague, a sign. */
    val origin: String = "",
    /** A picture in the app's files, or empty. */
    val image: String = "",
    /** A real sentence it was found in, when a text confirmed it. */
    val quote: String = "",
    val confirmedAt: Long? = null,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val reviewedAt: Long = 0,
    /** Cleanings that held, and those that didn't. */
    val reps: Int = 0,
    val lapses: Int = 0,
    /** Forgotten while sure of it (three strikes staked): in the workshop until cleaned well. */
    val cracked: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
) {
    val kindOf: Kind get() = Kind.of(kind)
    val inGround: Boolean get() = stability <= 0.0
    val trace: Trace get() = Trace(stability, difficulty, reviewedAt)
    val stage: Stage get() = Stage.of(this)
    /** Labelled enough to be asked. */
    val askable: Boolean get() = front.isNotBlank() && (kindOf == Kind.PHRASE || back.isNotBlank())
    fun recall(now: Long): Double = if (inGround) 0.0 else Memory.recall(trace, now)
    fun due(retention: Double): Long = if (inGround) createdAt else Memory.due(trace, retention)
    val confirmed: Boolean get() = confirmedAt != null
}

@Entity(tableName = "links", indices = [Index("a"), Index("b")])
data class Link(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val a: Long,
    val b: Long,
    /** [LinkKind] name. */
    val kind: String = LinkKind.KIN.name,
    val createdAt: Long,
) {
    val kindOf: LinkKind get() = LinkKind.of(kind)
    fun other(id: Long) = if (a == id) b else a
}

/** How a find was asked. */
enum class Mode {
    /** Front shown, back recalled. */
    FORWARD,
    /** Back shown, front recalled. */
    REVERSE,
    /** A phrase with its gaps covered. */
    GAP,
    /** Heard, not read: the front is spoken and written down. */
    ECHO,
    /** A question behind the sealed door. */
    DOOR,
    /** Told apart from its forgeries. */
    FORGERY;

    companion object {
        fun of(id: String?): Mode = entries.firstOrNull { it.name == id } ?: FORWARD
    }
}

@Entity(tableName = "reviews", indices = [Index("findId")])
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val findId: Long,
    val at: Long,
    /** [Mode] name. */
    val mode: String,
    /** Chisel strikes staked before looking, 1..3; 0 when none was staked. */
    val stake: Int = 0,
    /** [app.stele.domain.Grade.n]. */
    val grade: Int,
    /** What was written, if anything. */
    val answer: String = "",
)

@Dao
abstract class SteleDao {
    @Query("SELECT * FROM digs ORDER BY createdAt ASC") abstract fun observeDigs(): Flow<List<Dig>>
    @Query("SELECT * FROM vessels ORDER BY createdAt ASC") abstract fun observeVessels(): Flow<List<Vessel>>
    @Query("SELECT * FROM finds ORDER BY createdAt ASC") abstract fun observeFinds(): Flow<List<Find>>
    @Query("SELECT * FROM links ORDER BY createdAt ASC") abstract fun observeLinks(): Flow<List<Link>>
    @Query("SELECT * FROM reviews ORDER BY at ASC") abstract fun observeReviews(): Flow<List<Review>>

    @Query("SELECT * FROM digs") abstract suspend fun digs(): List<Dig>
    @Query("SELECT * FROM vessels") abstract suspend fun vessels(): List<Vessel>
    @Query("SELECT * FROM finds") abstract suspend fun finds(): List<Find>
    @Query("SELECT * FROM links") abstract suspend fun links(): List<Link>
    @Query("SELECT * FROM reviews") abstract suspend fun reviews(): List<Review>

    @Query("SELECT * FROM finds WHERE id = :id") abstract suspend fun find(id: Long): Find?
    @Query("SELECT * FROM finds WHERE vesselId = :vesselId") abstract suspend fun findsIn(vesselId: Long): List<Find>
    @Query("SELECT * FROM finds WHERE digId = :digId") abstract suspend fun findsOf(digId: Long): List<Find>
    @Query("SELECT * FROM vessels WHERE id = :id") abstract suspend fun vessel(id: Long): Vessel?

    @Insert abstract suspend fun insert(d: Dig): Long
    @Update abstract suspend fun update(d: Dig)
    @Delete abstract suspend fun delete(d: Dig)

    @Insert abstract suspend fun insert(v: Vessel): Long
    @Update abstract suspend fun update(v: Vessel)
    @Delete abstract suspend fun delete(v: Vessel)

    @Insert abstract suspend fun insert(f: Find): Long
    @Insert abstract suspend fun insertFinds(l: List<Find>)
    @Update abstract suspend fun update(f: Find)
    @Delete abstract suspend fun delete(f: Find)

    @Insert abstract suspend fun insert(l: Link): Long
    @Delete abstract suspend fun delete(l: Link)

    @Insert abstract suspend fun insert(r: Review): Long

    @Query("DELETE FROM links WHERE a = :findId OR b = :findId") abstract suspend fun deleteLinksOf(findId: Long)
    @Query("DELETE FROM reviews WHERE findId = :findId") abstract suspend fun deleteReviewsOf(findId: Long)
    @Query("UPDATE finds SET vesselId = 0 WHERE vesselId = :vesselId") abstract suspend fun loosen(vesselId: Long)
    @Query("DELETE FROM vessels WHERE digId = :digId") abstract suspend fun deleteVesselsOf(digId: Long)

    @Transaction
    open suspend fun deleteFind(f: Find) {
        deleteLinksOf(f.id)
        deleteReviewsOf(f.id)
        delete(f)
    }

    /** The vessel goes, its finds stay loose in the dig. */
    @Transaction
    open suspend fun deleteVessel(v: Vessel) {
        loosen(v.id)
        delete(v)
    }

    @Transaction
    open suspend fun deleteDig(d: Dig) {
        findsOf(d.id).forEach { deleteFind(it) }
        deleteVesselsOf(d.id)
        delete(d)
    }

    @Query("DELETE FROM digs") protected abstract suspend fun clearDigs()
    @Query("DELETE FROM vessels") protected abstract suspend fun clearVessels()
    @Query("DELETE FROM finds") protected abstract suspend fun clearFinds()
    @Query("DELETE FROM links") protected abstract suspend fun clearLinks()
    @Query("DELETE FROM reviews") protected abstract suspend fun clearReviews()
    @Insert protected abstract suspend fun insertDigs(l: List<Dig>)
    @Insert protected abstract suspend fun insertVessels(l: List<Vessel>)
    @Insert protected abstract suspend fun insertLinks(l: List<Link>)
    @Insert protected abstract suspend fun insertReviews(l: List<Review>)

    @Transaction
    open suspend fun replaceAll(digs: List<Dig>, vessels: List<Vessel>, finds: List<Find>, links: List<Link>, reviews: List<Review>) {
        clearReviews()
        clearLinks()
        clearFinds()
        clearVessels()
        clearDigs()
        insertDigs(digs)
        insertVessels(vessels)
        insertFinds(finds)
        insertLinks(links)
        insertReviews(reviews)
    }
}

@Database(
    entities = [Dig::class, Vessel::class, Find::class, Link::class, Review::class],
    version = 1,
    exportSchema = true,
)
abstract class SteleDatabase : RoomDatabase() {
    abstract fun dao(): SteleDao

    companion object {
        fun create(context: Context): SteleDatabase =
            Room.databaseBuilder(context.applicationContext, SteleDatabase::class.java, "stele.db").build()
    }
}
