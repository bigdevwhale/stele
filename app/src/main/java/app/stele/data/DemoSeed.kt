package app.stele.data

import android.content.Context
import app.stele.AppContainer
import app.stele.domain.Grade
import app.stele.domain.Memory
import app.stele.domain.Shape
import kotlin.math.ln
import kotlin.math.max

/**
 * A museum half dug, for screenshots and trying the app out (`-Pdemo`): Spanish, the bones of the hand,
 * capitals and a few sayings, at every stage from still in the ground to on show.
 */
object DemoSeed {
    private class Item(val front: String, val back: String, val days: Double, val dusty: Boolean = false, val example: String = "", val hint: String = "", val origin: String = "")

    suspend fun seedIfEmpty(context: Context, c: AppContainer) {
        val dao = c.db.dao()
        if (dao.digs().isNotEmpty()) return
        val ru = context.resources.configuration.locales[0].language == "ru"
        val now = System.currentTimeMillis()
        val day = Memory.DAY
        val rnd = java.util.Random(11)

        suspend fun dig(name: String, symbol: Int, clay: Int, lang: String, ageDays: Int) =
            dao.insert(Dig(name = name, symbol = symbol, clay = clay, lang = lang, createdAt = now - ageDays * day))

        suspend fun vessel(digId: Long, name: String, shape: Shape, mendedAgo: Int? = null) =
            dao.insert(Vessel(digId = digId, name = name, shape = shape.name, createdAt = now - 60 * day, mendedAt = mendedAgo?.let { now - it * day }))

        suspend fun put(digId: Long, vesselId: Long, kind: Kind, items: List<Item>, ageDays: Int): List<Long> = items.mapIndexed { i, it ->
            val created = now - (ageDays - i) * day
            val f = if (it.days <= 0.0) {
                Find(digId = digId, vesselId = vesselId, kind = kind.name, front = it.front, back = it.back, example = it.example, hint = it.hint, origin = it.origin, createdAt = now - (i + 1) * day / 3)
            } else {
                // Reviewed long enough ago to be dusty, or recently enough to be clean.
                val interval = Memory.interval(it.days, 0.9)
                val ago = if (it.dusty) interval * 1.4 + 0.5 else interval * (0.15 + rnd.nextDouble() * 0.5)
                Find(
                    digId = digId, vesselId = vesselId, kind = kind.name, front = it.front, back = it.back, example = it.example, hint = it.hint, origin = it.origin,
                    stability = it.days, difficulty = 4.0 + rnd.nextDouble() * 3, reviewedAt = now - (ago * day).toLong(),
                    reps = max(1, (ln(it.days + 1) / ln(2.2)).toInt() + 1), lapses = rnd.nextInt(2), createdAt = created,
                )
            }
            val id = dao.insert(f)
            // A history of cleanings behind each one, closer together the further back.
            if (!f.inGround) {
                var at = f.reviewedAt
                var gap = max(1.0, f.stability / 2.5)
                repeat(f.reps) { k ->
                    dao.insert(Review(findId = id, at = at, mode = (if (k % 2 == 0) Mode.FORWARD else Mode.REVERSE).name, stake = 1 + rnd.nextInt(3), grade = if (rnd.nextInt(6) == 0) Grade.HARD.n else Grade.GOOD.n))
                    at -= (gap * day).toLong()
                    gap = max(0.6, gap / 2.3)
                }
            }
            id
        }

        // Spanish.
        val es = dig(if (ru) "Испанский" else "Spanish", 0, 0, "es", 70)
        val kitchen = vessel(es, if (ru) "Кухня" else "The kitchen", Shape.AMPHORA, mendedAgo = 3)
        val travel = vessel(es, if (ru) "В дорогу" else "On the road", Shape.JUG)
        put(es, kitchen, Kind.PAIR, listOf(
            Item("la cocina", if (ru) "кухня" else "kitchen", 160.0),
            Item("la cuchara", if (ru) "ложка" else "spoon", 130.0),
            Item("el tenedor", if (ru) "вилка" else "fork", 64.0),
            Item("el cuchillo", if (ru) "нож" else "knife", 41.0),
            Item("la olla", if (ru) "кастрюля" else "pot", 22.0, dusty = true),
            Item("la sartén", if (ru) "сковорода" else "frying pan", 15.0),
            Item("el horno", if (ru) "духовка" else "oven", 11.0),
            Item("la taza", if (ru) "чашка" else "cup", 9.0),
        ), 66)
        put(es, travel, Kind.PAIR, listOf(
            Item("la maleta", if (ru) "чемодан" else "suitcase", 18.0),
            Item("el billete", if (ru) "билет" else "ticket", 9.0, dusty = true),
            Item("el andén", if (ru) "перрон" else "platform", 3.0, dusty = true),
            Item("la aduana", if (ru) "таможня" else "customs", 2.0),
            Item("el equipaje", if (ru) "багаж" else "luggage", 0.0),
            Item("el retraso", if (ru) "задержка" else "delay", 0.0),
        ), 40)
        val loose = put(es, 0, Kind.PAIR, listOf(
            Item("embarazada", if (ru) "беременная" else "pregnant", 6.0, dusty = true, hint = if (ru) "не «смущённая»!" else "not «embarrassed»!"),
            Item("avergonzado", if (ru) "смущённый, пристыженный" else "embarrassed, ashamed", 4.0),
            Item("la madrugada", if (ru) "ранние часы до рассвета" else "the small hours before dawn", 34.0, example = if (ru) "Me levanté de madrugada para ver el eclipse." else "Me levanté de madrugada para ver el eclipse."),
            Item("la sobremesa", if (ru) "беседа за столом после еды" else "lingering talk at the table after a meal", 150.0, origin = if (ru) "сериал «Валерия»" else "the show «Valeria»"),
            Item("estrenar", if (ru) "использовать впервые, надеть обновку" else "to use or wear for the first time", 26.0),
            Item("aprovechar", if (ru) "воспользоваться, извлечь пользу" else "to make the most of", 12.0, dusty = true),
            Item("desarrollar", if (ru) "развивать" else "to develop", 8.0),
            Item("el éxito", if (ru) "успех" else "success", 5.0, hint = if (ru) "не «выход» — выход это salida" else "not «exit» — that's salida"),
            Item("la salida", if (ru) "выход" else "exit", 3.0),
            Item("ojalá", if (ru) "хоть бы, надеюсь что" else "hopefully, if only", 210.0, origin = if (ru) "от Лусии" else "from Lucía"),
            Item("despacio", if (ru) "медленно" else "slowly", 1.5, dusty = true),
            Item("el desayuno", if (ru) "завтрак" else "breakfast", 0.0),
            Item("la carpeta", if (ru) "папка" else "folder", 0.0),
            Item("recordar", if (ru) "помнить, вспоминать" else "to remember", 0.0),
        ), 60)
        dao.insert(Link(a = loose[0], b = loose[1], kind = LinkKind.CONFUSED.name, createdAt = now - 5 * day))
        dao.insert(Link(a = loose[7], b = loose[8], kind = LinkKind.CONFUSED.name, createdAt = now - 4 * day))
        dao.find(loose[2])?.let { dao.update(it.copy(quote = "Llegamos a casa de madrugada, cansados pero felices.", confirmedAt = now - 2 * day)) }
        dao.find(loose[0])?.let { dao.update(it.copy(cracked = true)) }
        dao.find(loose[10])?.let { dao.update(it.copy(cracked = true)) }

        // The bones of the hand.
        val anat = dig(if (ru) "Анатомия" else "Anatomy", 8, 5, "", 45)
        val hand = vessel(anat, if (ru) "Кисть руки" else "The hand", Shape.BOWL)
        put(anat, hand, Kind.TERM, if (ru) listOf(
            Item("Ладьевидная кость", "Кость запястья у основания большого пальца; ломается чаще других", 28.0),
            Item("Полулунная кость", "Кость запястья в форме полумесяца, в центре проксимального ряда", 14.0),
            Item("Гороховидная кость", "Маленькая кость запястья со стороны мизинца, сидит в сухожилии", 9.0, dusty = true),
            Item("Пястные кости", "Пять костей ладони между запястьем и пальцами", 45.0),
            Item("Фаланги", "Кости пальцев: по три в каждом, в большом — две; 14 на кисть", 70.0),
            Item("Тенар", "Мышечное возвышение у основания большого пальца", 2.5),
            Item("Гипотенар", "Мышечное возвышение у основания мизинца", 0.0),
        ) else listOf(
            Item("Scaphoid", "Wrist bone at the base of the thumb; the one most often broken", 28.0),
            Item("Lunate", "Crescent-shaped wrist bone in the middle of the near row", 14.0),
            Item("Pisiform", "Small pea-shaped wrist bone on the little-finger side, inside a tendon", 9.0, dusty = true),
            Item("Metacarpals", "The five bones of the palm, between wrist and fingers", 45.0),
            Item("Phalanges", "Finger bones: three in each finger, two in the thumb; 14 in a hand", 70.0),
            Item("Thenar eminence", "The mound of muscle at the base of the thumb", 2.5),
            Item("Hypothenar eminence", "The mound of muscle at the base of the little finger", 0.0),
        ), 44)

        // Capitals.
        val geo = dig(if (ru) "Столицы" else "Capitals", 11, 2, "", 35)
        val world = vessel(geo, if (ru) "Неочевидные столицы" else "Capitals that aren't the big city", Shape.COIN)
        put(geo, world, Kind.QUESTION, if (ru) listOf(
            Item("Столица Канады?", "Оттава", 50.0),
            Item("Столица Австралии?", "Канберра", 33.0),
            Item("Столица Марокко?", "Рабат", 12.0, dusty = true),
            Item("Столица Мьянмы?", "Нейпьидо", 3.0),
            Item("Столица Новой Зеландии?", "Веллингтон", 0.0),
        ) else listOf(
            Item("Capital of Canada?", "Ottawa", 50.0),
            Item("Capital of Australia?", "Canberra", 33.0),
            Item("Capital of Morocco?", "Rabat", 12.0, dusty = true),
            Item("Capital of Myanmar?", "Naypyidaw", 3.0),
            Item("Capital of New Zealand?", "Wellington", 0.0),
        ), 34)

        // Sayings.
        val words = dig(if (ru) "Изречения" else "Sayings", 9, 4, "", 30)
        put(words, 0, Kind.PHRASE, if (ru) listOf(
            Item("Жизнь без [исследования] не стоит того, чтобы её жить.", "Сократ, «Апология»", 18.0),
            Item("Память — [сокровищница] и хранительница всего.", "Цицерон", 7.5, dusty = true),
            Item("Festina [lente] — спеши медленно.", "Август", 130.0),
            Item("Повторение — мать [учения].", "", 0.0),
        ) else listOf(
            Item("The [unexamined] life is not worth living.", "Socrates, Apology", 18.0),
            Item("Memory is the [treasury] and guardian of all things.", "Cicero", 7.5, dusty = true),
            Item("Festina [lente] — make haste slowly.", "Augustus", 130.0),
            Item("Repetition is the mother of [learning].", "", 0.0),
        ), 28)

        c.settings.update {
            it.copy(
                onboarded = true, lastDig = es, halls = 2, hallSeen = 2, doorAt = now - 31 * day, doorScore = 10, doorOf = 12,
                soundingDay = -1,
            )
        }
    }
}
