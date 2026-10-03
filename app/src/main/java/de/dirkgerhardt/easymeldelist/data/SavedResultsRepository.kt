package de.dirkgerhardt.easymeldelist.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SavedParticipant(val name: String, val verein: String?)

/**
 * Speichert Suchergebnisse pro Meldeliste als JSON-Datei im
 * App-Speicher: filesDir/gespeichert/<pdfName>.json
 */
class SavedResultsRepository(context: Context) {

    private val dir = File(context.filesDir, "gespeichert").apply { mkdirs() }

    private fun jsonFile(pdfFileName: String) =
        File(dir, pdfFileName.removeSuffix(".pdf") + ".json")

    /** Speichert alle Meldungen eines Teilnehmers für eine Meldeliste. */
    fun save(pdfFileName: String, participant: String, verein: String?, entries: List<MeldeEntry>) {
        val file = jsonFile(pdfFileName)
        val root = if (file.exists()) JSONObject(file.readText()) else JSONObject()

        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("name", e.name)
                put("wettkampf", e.wettkampf)
                put("lauf", e.lauf)
                put("bahn", e.bahn)
                put("distanz", e.distanz)
                put("schwimmart", e.schwimmart)
                put("zeit", e.zeit ?: "")     // neu: Meldezeit
                put("verein", e.verein)
            })
        }
        root.put(participantKey(participant, verein), JSONObject().apply {
            put("name", participant)
            put("verein", verein ?: "")
            put("entries", arr)
        })
        file.writeText(root.toString())
    }

    /** Alle gespeicherten Teilnehmer einer Meldeliste. */
    fun loadParticipants(pdfFileName: String): List<SavedParticipant> {
        val file = jsonFile(pdfFileName)
        if (!file.exists()) return emptyList()
        val root = JSONObject(file.readText())
        return root.keys().asSequence().map { key ->
            val obj = root.getJSONObject(key)
            SavedParticipant(
                obj.getString("name"),
                obj.optString("verein").takeIf { it.isNotBlank() }
            )
        }.sortedBy { it.name.lowercase() }.toList()
    }

    /** Gespeicherte Meldungen eines Teilnehmers. */
    fun loadEntries(pdfFileName: String, participant: String, verein: String?): List<MeldeEntry> {
        val file = jsonFile(pdfFileName)
        if (!file.exists()) return emptyList()
        val root = JSONObject(file.readText())
        val obj = root.optJSONObject(participantKey(participant, verein)) ?: return emptyList()
        val arr = obj.getJSONArray("entries")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            MeldeEntry(
                name = o.getString("name"),
                wettkampf = o.getInt("wettkampf"),
                lauf = o.getInt("lauf"),
                bahn = o.getInt("bahn"),
                distanz = o.getInt("distanz"),
                schwimmart = o.getString("schwimmart"),
                zeit = o.optString("zeit").takeIf { it.isNotBlank() },   // neu
                verein = o.getString("verein")
            )
        }
    }

    private fun participantKey(participant: String, verein: String?) =
        participant.lowercase() + "|" + (verein ?: "").lowercase()
}