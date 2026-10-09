package de.dirkgerhardt.easymeldelist.data

import java.util.Random

object MedalEstimator {

    const val MEDAILLE_SCHWELLE = 0.50          // Gesamtchance, ab der überhaupt eine Medaille gezeigt wird
    const val PRIMAER_MEDAILLE_SCHWELLE = 0.50  // Eigenchance, ab der die primäre Medaille gezeigt wird (NEU)
    const val ZWEITE_MEDAILLE_SCHWELLE = 0.20   // Mindestchance der besseren Medaille (NEU)

    fun parseZeitMs(zeit: String): Long? {
        val m = Regex("""^(\d{2}):(\d{2}),(\d{2})$""").find(zeit.trim()) ?: return null
        val (min, sek, hundert) = m.destructured
        return (min.toLong() * 60_000) + (sek.toLong() * 1000) + (hundert.toLong() * 10)
    }

    fun medaillenChancen(
        eigeneZeitMs: Long,
        eigeneJahrgang: Int,
        alleLaufe: Map<Int, List<RivalenZeit>>,
        relSigma: Double = 0.02,
        runs: Int = 5_000,
        rng: Random = Random(42)
    ): MedaillenChancen {
        val rivalenZeiten = alleLaufe.values.flatten()
            .filter { it.jahrgang == eigeneJahrgang }
            .map { it.zeitMs }

        if (rivalenZeiten.isEmpty()) {
            return MedaillenChancen(1.0, 0.0, 0.0)
        }

        var gold = 0
        var silber = 0
        var bronze = 0
        repeat(runs) {
            val own = rng.nextGaussian() * relSigma * eigeneZeitMs + eigeneZeitMs
            var rank = 1
            for (t in rivalenZeiten) {
                val rival = rng.nextGaussian() * relSigma * t + t
                if (rival < own) rank++
            }
            when (rank) {
                1 -> gold++
                2 -> silber++
                3 -> bronze++
            }
        }
        return MedaillenChancen(
            gold.toDouble() / runs,
            silber.toDouble() / runs,
            bronze.toDouble() / runs
        )
    }

    fun besteMedaille(chancen: MedaillenChancen): Medaille? {
        if (chancen.gesamt <= MEDAILLE_SCHWELLE) return null

        val optionen = listOf(
            chancen.gold to Medaille.GOLD,
            chancen.silber to Medaille.SILBER,
            chancen.bronze to Medaille.BRONZE
        )
        // Alle Medaillen mit mind. 30%-Anteil an der Gesamtchance –
        // falls keine das schafft, alle berücksichtigen
        return optionen
            .filter { it.first >= chancen.gesamt * 0.30 }
            .ifEmpty { optionen }
            .maxBy { it.first }
            .second
    }

    /** Die zweitwahrscheinlichste Medaille (nur relevant, wenn es eine Erste gibt). */

    fun zweiteMedaille(chancen: MedaillenChancen, beste: Medaille?): Medaille? {
        if (beste == null) return null
        // Nur BESSERE Medaillen anzeigen (nicht schlechtere!)
        return listOf(
            Medaille.GOLD to chancen.gold,
            Medaille.SILBER to chancen.silber,
            Medaille.BRONZE to chancen.bronze
        )
            .filter { it.first.ordinal < beste.ordinal && it.second >= ZWEITE_MEDAILLE_SCHWELLE }
            .maxByOrNull { it.second }
            ?.first
    }

    fun personalWertung(gesamt: Double, medaille: Medaille?, salt: Int = 0): String {
        val varianten: List<String> = when {
            gesamt < 0.10 -> listOf(
                "Ein kleiner Funke Hoffnung aufs Treppchen glimmt",
                "Ein Funke Hoffnung bleibt bestehen",
                "Ein Hauch von Hoffnung aufs Podium",
                "Nichts ist unmöglich – glaub daran!"
            )
            gesamt < 0.20 -> listOf(
                "Du bist der Geheimtipp",
                "Für dieses Rennen bist du der Geheimtipp",
                "Ein echter Geheimtipp heute",
                "Niemand hat dich auf dem Zettel, zeig was in dir steckt"
            )
            gesamt < 0.30 -> listOf(
                "Du bist die Überraschungswaffe",
                "Heute kannst du alle überraschen",
                "Viele rechnen noch nicht mit dir",
                "Zeig dem Feld, wer du bist"
            )
            gesamt < 0.40 -> listOf(
                "Eine echte Möglichkeit aufs Treppchen hast du",
                "Das Treppchen ist heute wirklich drin",
                "Ein Platz auf dem Podium ist möglich",
                "Bronze liegt in deiner Reichweite"
            )
            medaille == null -> listOf(
                "Eine realistische Chance aufs Treppchen hast du",
                "Mit etwas Glück landest du vorne",
                "Dein Platz auf dem Podium ist heute denkbar",
                "Die Medaillenränge sind zum Greifen nah"
            )
            gesamt < 0.60 -> listOf(
                "Du hast gute Aussichten auf",
                "Die Chancen stehen gut für",
                "Da wäre etwas zu holen:",
                "Ein Medaillenplatz ist zum Greifen nah:"
            )
            gesamt < 0.70 -> listOf(
                "Du hast eine hohe Chance auf",
                "Sehr wahrscheinlich gibt es heute",
                "Bereite dich aufs Treppchen vor –",
                "Heute gehört dir mit hoher Wahrscheinlichkeit"
            )
            gesamt < 0.80 -> listOf(
                "Du hast richtig gute Karten für",
                "Die Voraussetzungen sind hervorragend für",
                "Deine Position im Feld ist stark –",
                "Fast schon ein sicherer Platz für"
            )
            gesamt < 0.90 -> listOf(
                "Du hast beste Aussichten auf",
                "So gut wie sicher gibt es heute",
                "Das Feld muss dich fürchten um",
                "Praktisch niemand nimmt dir"
            )
            else -> return "${medaille!!.displayName} ist zum Greifen nah"
        }
        return varianten[((salt % varianten.size) + varianten.size) % varianten.size]
    }

    fun motivationsSpruch(salt: Int = 0): String {
        return motivationsSprueche[((salt % motivationsSprueche.size) + motivationsSprueche.size) % motivationsSprueche.size]
    }

    private val motivationsSprueche = listOf(
        "Wenn du alles gibst, ist alles möglich!",
        "Große Schwimmer fingen auch mit einem Start an.",
        "Heute ist der Tag, an dem du überraschst!",
        "Jedes Rennen ist eine neue Chance – zeig, was in dir steckt!",
        "Der Wille zählt mehr als die Zeit auf dem Papier.",
        "Wasser ist dein Element – schwimm frei und mutig!",
        "Übung macht den Meister, Mut macht den Sieger!",
        "Die Besten waren auch mal Außenseiter!",
        "Glaub an dich – die Uhr tickt für alle gleich!",
        "Deine Bestzeit wartet noch auf dich!",
        "Jeder Start bringt dich näher ans Ziel!",
        "Nicht die Zeit entscheidet, sondern dein Einsatz!",
        "Trau dir etwas zu – du hast das Potential!",
        "Heute zählt jeder Meter, jede Bewegung!",
        "Dein Team glaubt an dich – jetzt sei du selbst!"
    )

    fun chancenInfoBerechnen(
        eigeneEntries: List<MeldeEntry>,
        feld: Map<Int, List<RivalenZeit>>,
        runs: Int = 5_000
    ): Map<Int, ChancenInfo> {
        val result = HashMap<Int, ChancenInfo>()
        for (entry in eigeneEntries) {
            if (result.containsKey(entry.wettkampf)) continue
            val zeitMs = entry.zeit?.let { parseZeitMs(it) } ?: continue

            val chancen = medaillenChancen(
                zeitMs, entry.jahrgang,
                mapOf(entry.wettkampf to (feld[entry.wettkampf] ?: emptyList())),
                runs = runs
            )
            val medaille = besteMedaille(chancen)
            result[entry.wettkampf] = ChancenInfo(
                chancen = chancen,
                medaille = medaille,
                zweiteMedaille = zweiteMedaille(chancen, medaille),
                text = personalWertung(chancen.gesamt, medaille, entry.wettkampf),
                motivationsSpruch = if (chancen.gesamt <= 0.0)
                    motivationsSpruch(entry.wettkampf) else null
            )
        }
        return result
    }
}