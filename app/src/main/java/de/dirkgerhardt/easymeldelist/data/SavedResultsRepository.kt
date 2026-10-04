package de.dirkgerhardt.easymeldelist.data

import android.content.Context
import org.json.JSONObject
import java.io.File

class SavedResultsRepository(private val context: Context) {

    private val dir: File
        get() = File(context.filesDir, "gespeichert").apply { mkdirs() }

    /** Normalisiert Namen/Dateinamen für robutes Vergleichen. */
    private fun norm(s: String): String =
        s.trim()
            .replace("%2C", ",")
            .replace("%20", " ")
            .replace("+", " ")
            .replace("\\s+".toRegex(), " ")
            .lowercase()

    /** Findet die gespeicherte Datei unabhängig von .json-Suffix und Sonderzeichen. */
    private fun findeDatei(dateiName: String): File? {
        val target = norm(dateiName.removeSuffix(".json"))
        android.util.Log.d("SavedRepo", "Suche Datei für '$dateiName' (norm='$target')")
        val treffer = dir.listFiles { f -> f.extension == "json" }
            ?.firstOrNull { norm(it.nameWithoutExtension) == target }
        android.util.Log.d("SavedRepo", "Treffer: ${treffer?.name ?: "KEINER"} — verfügbar: ${dir.listFiles()?.map { it.name }}")
        return treffer
    }

    /** Speichert (oder aktualisiert) einen Teilnehmer samt Konkurrenzdaten in der JSON. */
    fun save(
        dateiName: String,
        teilnehmerName: String,
        verein: String?,
        entries: List<MeldeEntry>,
        feld: Map<Int, List<RivalenZeit>>
    ) {
        val file = findeDatei(dateiName) ?: File(dir, dateiName.replace("/", "_") + ".json")

        val root = if (file.exists()) {
            runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject())
        } else JSONObject()

        val participants = root.optJSONArray("participants") ?: org.json.JSONArray()
        var pJson: JSONObject? = null
        for (i in 0 until participants.length()) {
            val p = participants.optJSONObject(i)
            if (p != null && norm(p.optString("name")) == norm(teilnehmerName)) {
                pJson = p
                break
            }
        }
        if (pJson == null) {
            pJson = JSONObject()
            participants.put(pJson)
        }

        pJson.put("name", teilnehmerName)
        pJson.put("verein", verein ?: "")
        pJson.put("entries", org.json.JSONArray().apply {
            entries.forEach { e ->
                put(org.json.JSONObject().apply {
                    put("name", e.name)
                    put("wettkampf", e.wettkampf)
                    put("lauf", e.lauf)
                    put("bahn", e.bahn)
                    put("distanz", e.distanz)
                    put("schwimmart", e.schwimmart)
                    put("zeit", e.zeit)
                    put("verein", e.verein)
                    put("jahrgang", e.jahrgang)
                    put("tag", e.tag)
                })
            }
        })
        pJson.put("feld", org.json.JSONObject().apply {
            feld.forEach { (wk, liste) ->
                put(wk.toString(), org.json.JSONArray().apply {
                    liste.forEach { r ->
                        put(org.json.JSONObject().apply {
                            put("jahrgang", r.jahrgang)
                            put("zeitMs", r.zeitMs)
                        })
                    }
                })
            }
        })

        root.put("datei", dateiName)
        root.put("participants", participants)
        file.writeText(root.toString())

        android.util.Log.d("SavedRepo", "Gespeichert: ${file.name}, Teilnehmer='${teilnehmerName}', Entries=${entries.size}")
    }

    /** Lädt alle gespeicherten Teilnehmer einer Datei. */
    fun loadParticipants(dateiName: String): List<GespeicherterTeilnehmer> {
        val file = findeDatei(dateiName) ?: return emptyList()
        val root = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return emptyList()
        val arr = root.optJSONArray("participants") ?: return emptyList()
        val result = mutableListOf<GespeicherterTeilnehmer>()
        for (i in 0 until arr.length()) {
            val p = arr.optJSONObject(i) ?: continue
            result.add(
                GespeicherterTeilnehmer(
                    name = p.optString("name"),
                    verein = p.optString("verein").ifEmpty { null }
                )
            )
        }
        android.util.Log.d("SavedRepo", "loadParticipants('${dateiName}'): ${result.map { it.name }}")
        return result
    }

    private fun findeTeilnehmer(dateiName: String, name: String): JSONObject? {
        val file = findeDatei(dateiName) ?: return null
        val root = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return null
        val arr = root.optJSONArray("participants") ?: return null
        for (i in 0 until arr.length()) {
            val p = arr.optJSONObject(i) ?: continue
            val gespeichert = p.optString("name")
            if (norm(gespeichert) == norm(name)) return p
        }
        android.util.Log.w("SavedRepo", "Teilnehmer '$name' nicht gefunden in '${dateiName}'. Vorhanden: ${(0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("name") }}")
        return null
    }

    /** Lädt die Meldungen eines Teilnehmers. */
    fun loadEntries(dateiName: String, name: String): List<MeldeEntry> {
        val p = findeTeilnehmer(dateiName, name) ?: return emptyList()
        val arr = p.optJSONArray("entries") ?: return emptyList()
        val result = mutableListOf<MeldeEntry>()
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            result.add(
                MeldeEntry(
                    name = e.optString("name"),
                    wettkampf = e.optInt("wettkampf"),
                    lauf = e.optInt("lauf"),
                    bahn = e.optInt("bahn"),
                    distanz = e.optInt("distanz"),
                    schwimmart = e.optString("schwimmart"),
                    zeit = e.optString("zeit"),
                    verein = e.optString("verein"),
                    jahrgang = e.optInt("jahrgang"),
                    tag = e.optInt("tag")
                )
            )
        }
        android.util.Log.d("SavedRepo", "loadEntries('$name'): ${result.size} Einträge")
        return result
    }

    /** Lädt das Konkurrenz-Feld eines Teilnehmers für die Chancen-Berechnung. */
    fun loadFeld(dateiName: String, name: String): Map<Int, List<RivalenZeit>> {
        val p = findeTeilnehmer(dateiName, name) ?: return emptyMap()
        val feldJson = p.optJSONObject("feld") ?: return emptyMap()
        val result = mutableMapOf<Int, List<RivalenZeit>>()
        for (key in feldJson.keys()) {
            val wk = key.toIntOrNull() ?: continue
            val arr = feldJson.optJSONArray(key) ?: continue
            val liste = mutableListOf<RivalenZeit>()
            for (i in 0 until arr.length()) {
                val r = arr.optJSONObject(i) ?: continue
                liste.add(RivalenZeit(jahrgang = r.optInt("jahrgang"), zeitMs = r.optLong("zeitMs")))
            }
            result[wk] = liste
        }
        android.util.Log.d("SavedRepo", "loadFeld('$name'): ${result.size} Wettkämpfe, ${result.values.sumOf { it.size }} Rivalen")
        return result
    }

    /** Liste aller gespeicherten Dateien (für 'Meldelisten'-Übersicht). */
    fun gespeicherteDateien(): List<String> =
        dir.listFiles { f -> f.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
}