package app.stele.domain

/** How Russian text addresses the digger: "ты уверен" or "ты уверена". English ignores it. */
enum class Gender { MALE, FEMALE;
    companion object {
        fun of(id: String?): Gender = entries.firstOrNull { it.name == id } ?: MALE
    }
}

private val genderMark = Regex("""\{([^{}|]*)\|([^{}|]*)\}""")

/** Resolves "{уверен|уверена}" marks: the first form for [Gender.MALE], the second for [Gender.FEMALE]. */
fun gendered(text: String, gender: Gender): String =
    if ('|' !in text) text else genderMark.replace(text) { m -> m.groupValues[if (gender == Gender.MALE) 1 else 2] }
