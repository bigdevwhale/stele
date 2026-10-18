package app.stele.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** The whole museum as one JSON file, pictures included. Nothing leaves the phone unless the digger exports it. */
object Backup {
    private const val VERSION = 1

    fun encode(
        digs: List<Dig>, vessels: List<Vessel>, finds: List<Find>, links: List<Link>, reviews: List<Review>,
        images: Map<String, ByteArray> = emptyMap(),
    ): String = JSONObject()
        .put("app", "stele")
        .put("version", VERSION)
        .put("digs", JSONArray(digs.map {
            JSONObject().put("id", it.id).put("name", it.name).put("symbol", it.symbol).put("clay", it.clay).put("lang", it.lang)
                .put("note", it.note).put("createdAt", it.createdAt)
        }))
        .put("vessels", JSONArray(vessels.map {
            JSONObject().put("id", it.id).put("digId", it.digId).put("name", it.name).put("shape", it.shape).put("createdAt", it.createdAt)
                .put("mendedAt", it.mendedAt ?: JSONObject.NULL)
        }))
        .put("finds", JSONArray(finds.map {
            JSONObject().put("id", it.id).put("digId", it.digId).put("vesselId", it.vesselId).put("kind", it.kind).put("front", it.front)
                .put("back", it.back).put("example", it.example).put("hint", it.hint).put("origin", it.origin).put("image", it.image)
                .put("quote", it.quote).put("confirmedAt", it.confirmedAt ?: JSONObject.NULL)
                .put("stability", it.stability).put("difficulty", it.difficulty).put("reviewedAt", it.reviewedAt)
                .put("reps", it.reps).put("lapses", it.lapses).put("cracked", it.cracked).put("createdAt", it.createdAt).put("updatedAt", it.updatedAt)
        }))
        .put("links", JSONArray(links.map {
            JSONObject().put("id", it.id).put("a", it.a).put("b", it.b).put("kind", it.kind).put("createdAt", it.createdAt)
        }))
        .put("reviews", JSONArray(reviews.map {
            JSONObject().put("id", it.id).put("findId", it.findId).put("at", it.at).put("mode", it.mode).put("stake", it.stake)
                .put("grade", it.grade).put("answer", it.answer)
        }))
        .put("images", JSONObject().apply { images.forEach { (name, bytes) -> put(name, Base64.encodeToString(bytes, Base64.NO_WRAP)) } })
        .toString(1)

    class Contents(
        val digs: List<Dig>, val vessels: List<Vessel>, val finds: List<Find>, val links: List<Link>, val reviews: List<Review>,
        val images: Map<String, ByteArray>,
    )

    /** Throws [IllegalArgumentException] when the text is not a Stele backup. */
    fun decode(text: String): Contents {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "stele") { "Not a Stele backup" }
        fun list(key: String) = (root.optJSONArray(key) ?: JSONArray()).let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
        fun JSONObject.time(key: String): Long? = if (isNull(key)) null else optLong(key)
        val pics = root.optJSONObject("images")
        return Contents(
            digs = list("digs").map {
                Dig(id = it.getLong("id"), name = it.getString("name"), symbol = it.optInt("symbol"), clay = it.optInt("clay"), lang = it.optString("lang"),
                    note = it.optString("note"), createdAt = it.getLong("createdAt"))
            },
            vessels = list("vessels").map {
                Vessel(id = it.getLong("id"), digId = it.getLong("digId"), name = it.getString("name"), shape = it.optString("shape"),
                    createdAt = it.getLong("createdAt"), mendedAt = it.time("mendedAt"))
            },
            finds = list("finds").map {
                Find(
                    id = it.getLong("id"), digId = it.getLong("digId"), vesselId = it.optLong("vesselId"), kind = it.optString("kind", Kind.PAIR.name),
                    front = it.getString("front"), back = it.optString("back"), example = it.optString("example"), hint = it.optString("hint"),
                    origin = it.optString("origin"), image = it.optString("image"), quote = it.optString("quote"), confirmedAt = it.time("confirmedAt"),
                    stability = it.optDouble("stability", 0.0), difficulty = it.optDouble("difficulty", 0.0), reviewedAt = it.optLong("reviewedAt"),
                    reps = it.optInt("reps"), lapses = it.optInt("lapses"), cracked = it.optBoolean("cracked"),
                    createdAt = it.getLong("createdAt"), updatedAt = it.optLong("updatedAt", it.getLong("createdAt")),
                )
            },
            links = list("links").map {
                Link(id = it.getLong("id"), a = it.getLong("a"), b = it.getLong("b"), kind = it.optString("kind", LinkKind.KIN.name), createdAt = it.getLong("createdAt"))
            },
            reviews = list("reviews").map {
                Review(id = it.getLong("id"), findId = it.getLong("findId"), at = it.getLong("at"), mode = it.optString("mode", Mode.FORWARD.name),
                    stake = it.optInt("stake"), grade = it.getInt("grade"), answer = it.optString("answer"))
            },
            images = pics?.keys()?.asSequence()?.associateWith { Base64.decode(pics.getString(it), Base64.NO_WRAP) }.orEmpty(),
        )
    }

    suspend fun write(context: Context, uri: Uri, dao: SteleDao) {
        val finds = dao.finds()
        val images = finds.map { it.image }.filter { it.isNotBlank() }.distinct()
            .mapNotNull { name -> File(Images.dir(context), name).takeIf { it.exists() }?.let { name to it.readBytes() } }.toMap()
        val text = encode(dao.digs(), dao.vessels(), finds, dao.links(), dao.reviews(), images)
        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } ?: error("Can't write there")
    }

    suspend fun restore(context: Context, uri: Uri, dao: SteleDao) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } ?: error("Can't read that")
        val c = decode(text)
        c.images.forEach { (name, bytes) -> File(Images.dir(context), File(name).name).writeBytes(bytes) }
        dao.replaceAll(c.digs, c.vessels, c.finds, c.links, c.reviews)
    }
}
