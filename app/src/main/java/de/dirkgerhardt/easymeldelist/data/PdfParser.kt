package de.dirkgerhardt.easymeldelist.data

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Participant(
    val nachname: String,
    val vorname: String,
    val verein: String
)

/** Ergebnis der Ein-Pass-Analyse einer Meldeliste. */
data class MeldelistAnalysis(
    val eventName: String?,
    val clubs: List<String>,
    val participants: List<Participant>
)

/**
 * Portierung des Desktop-MeldelistExtractor auf Android.
 * Arbeitet auf einem InputStream statt auf einem Dateipfad.
 */
class MeldelistParser {

    private val wettkampfRegex = Regex(
        """(?:noch\s+)?Wettkampf\s+(\d+)\s+-\s+(\d+)m\s+(Freistil|Brust|Rücken|Schmetterling|Lagen)"""
    )
    private val laufRegex = Regex("""Lauf\s+(\d+)/\d+""")

    // Gruppen: 1=Bahn, 2=Name, 3=Jahrgang, 4=Verein, 5=Meldezeit
    private val bahnRegex = Regex(
        """^Bahn\s+(\d+)\s+(.+?)\s+(\d{4})\s+(.+?)\s+(\d\d:\d\d,\d{2})"""
    )

    // Tag-Erkennung: "Tag 1", "Tag 2", "Samstag", "Sonntag", Datumsmuster
    private val tagRegex = Regex("""(?:Tag\s*[12]|Samstag|Sonntag|(?:\d{2}\.\d{2}\.\d{2,4}))""")

    private val styleMap = mapOf(
        "Freistil" to "F", "Brust" to "B", "Rücken" to "R",
        "Schmetterling" to "S", "Lagen" to "L"
    )

    /** Zeilen, die niemals der Veranstaltungsname sind. */
    private val genericLines = listOf(
        "easywk", "meldeergebnis", "stand:", "veranstaltet von",
        "auszug", "seite", "wettkampf", "lauf", "bahn"
    )

    /** Liest alle Textzeilen einer Meldeliste. */
    private fun readLines(input: java.io.InputStream): List<String> =
        PDDocument.load(input.readBytes()).use { document ->
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            stripper.getText(document).lineSequence()
                .map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }

    /** Bestimmt den Tag aus einer Textzeile (1, 2, oder 0 = unbekannt). */
    private fun bestimmeTagZeile(line: String): Int {
        val lower = line.lowercase()
        return when {
            "samstag" in lower || "stag 1" in lower || "tag 1" in lower || "stag" in lower -> 1
            "sonntag" in lower || "stag 2" in lower || "tag 2" in lower -> 2
            else -> 0
        }
    }

    /**
     * Analysiert eine Meldeliste in EINEM Durchlauf:
     * Veranstaltungsname, Vereine und Teilnehmer.
     */
    suspend fun analyze(input: java.io.InputStream): MeldelistAnalysis =
        withContext(Dispatchers.IO) {
            val lines = readLines(input)

            // Veranstaltungsname: erste "nicht-generische" Zeile der ersten Seite
            val eventName = lines.take(15).firstOrNull { line ->
                val l = line.lowercase()
                line.isNotBlank() && genericLines.none { l.contains(it) } && line.length > 5
            }?.trim()

            // Vereine
            val clubRegex = Regex("""^Bahn\s+\d+\s+.+?\s+\d{4}\s+(.+?)\s+\d\d:\d\d,\d{2}""")
            val clubs = lines.mapNotNull { line ->
                clubRegex.find(line)?.groupValues?.get(1)?.trim()
            }.toSortedSet(compareBy { it.lowercase().replace(Regex("[^a-zäöüß0-9]"), "") })

            // Teilnehmer
            val participantRegex = Regex(
                """^Bahn\s+\d+\s+(.+?),\s+(.+?)\s+\d{4}\s+(.+?)\s+\d\d:\d\d,\d{2}"""
            )
            val participants = lines.mapNotNull { line ->
                participantRegex.find(line)?.let { m ->
                    Participant(
                        m.groupValues[1].trim(),
                        m.groupValues[2].trim(),
                        m.groupValues[3].trim()
                    )
                }
            }.distinctBy { "${it.nachname}|${it.vorname}|${it.verein}" }

            MeldelistAnalysis(eventName, clubs.toList(), participants)
        }

    /**
     * Die Personensuche: alle Meldungen eines Schwimmers, optional nach
     * Verein gefiltert. Über [onHeatEntry] werden zusätzlich ALLE
     * Meldezeiten eines Laufs gemeldet (für die Medaillenchance-Berechnung).
     */
    suspend fun findSwimmer(
        input: java.io.InputStream,
        nachname: String,
        vorname: String,
        verein: String? = null,
        onHeatEntry: ((wettkampf: Int, lauf: Int, jahrgang: Int, zeitMs: Long, name: String) -> Unit)? = null
    ): List<MeldeEntry> = withContext(Dispatchers.IO) {
        val searchName = "$nachname, $vorname"
        android.util.Log.d("SEARCH_DEBUG", "searchName='$searchName', verein='$verein'")
        val entries = mutableListOf<MeldeEntry>()
        var wk = 0; var distanz = 0; var art = ""; var lauf = 0
        var aktuellesTag = 0               // aktueller Tag aus Kontext

        readLines(input).forEach { line ->
            val wkMatch = wettkampfRegex.find(line)

            // NEU: Tag-Kontext aus der Zeile extrahieren (z.B. "Tag 1" in Header)
            if (tagRegex.containsMatchIn(line)) {
                val erkanntesTag = bestimmeTagZeile(line)
                if (erkanntesTag > 0) {
                    aktuellesTag = erkanntesTag
                }
            }

            when {
                wkMatch != null -> {
                    wk = wkMatch.groupValues[1].toInt()
                    distanz = wkMatch.groupValues[2].toInt()
                    art = styleMap[wkMatch.groupValues[3]] ?: "?"
                    lauf = 0
                }
                laufRegex.find(line) != null -> {
                    lauf = laufRegex.find(line)!!.groupValues[1].toInt()
                }
                else -> bahnRegex.find(line)?.let { m ->
                    val name = m.groupValues[2].trim()
                    val club = m.groupValues[4].trim()
                    val jahrgang = m.groupValues[3].toInt()
                    val zeit = m.groupValues[5]

                    // NEU: Wenn kein expliziter Tag gesetzt, versuche ihn aus dem Namen zu ableiten
                    // Oft steht am Anfang des Dokuments "Tag 1" oder "Samstag" in Überschriften
                    val tag = aktuellesTag

                    // Meldezeit an die Heat-Sammlung melden (für Medaillenchance)
                    if (zeit.isNotBlank()) {
                        MedalEstimator.parseZeitMs(zeit)?.let { ms ->
                            onHeatEntry?.invoke(wk, lauf, jahrgang, ms, name)
                        }
                    }

                    if (isSameName(name, searchName) &&
                        (verein == null || club.contains(verein, ignoreCase = true))
                    ) {
                        entries.add(
                            MeldeEntry(
                                name = name,
                                wettkampf = wk,
                                lauf = lauf,
                                bahn = m.groupValues[1].toInt(),
                                distanz = distanz,
                                schwimmart = art,
                                zeit = zeit.ifBlank { "" },
                                verein = club,
                                jahrgang = jahrgang,
                                tag = tag
                            )
                        )
                    }
                }
            }
        }
        entries
    }

    private fun isSameName(found: String, searched: String): Boolean {
        val normalize: (String) -> String = { raw ->
            raw.split(",").joinToString(",") { part ->
                part.trim().replace(Regex("\\s+"), " ").lowercase()
            }
        }
        return normalize(found) == normalize(searched)
    }
}