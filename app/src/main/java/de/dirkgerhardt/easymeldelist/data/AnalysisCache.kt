package de.dirkgerhardt.easymeldelist.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Speichert das Analyseergebnis (Event, Vereine, Teilnehmer) als JSON
 * neben dem PDF, damit die PDF bei erneutem Öffnen nicht neu geparst
 * werden muss: filesDir/meldelisten/<name>.analysis.json
 */
class AnalysisCache(context: Context) {

    private val dir = File(context.filesDir, "meldelisten").apply { mkdirs() }

    private fun cacheFile(pdfFileName: String): File =
        File(dir, pdfFileName.removeSuffix(".pdf") + ".analysis.json")

    /** Speichert die Analyse – überschreibt einen evtl. vorhandenen alten Stand. */
    fun save(pdfFileName: String, analysis: MeldelistAnalysis) {
        val participants = JSONArray()
        analysis.participants.forEach { p ->
            participants.put(JSONObject().apply {
                put("nachname", p.nachname)
                put("vorname", p.vorname)
                put("verein", p.verein)
            })
        }
        val json = JSONObject().apply {
            put("eventName", analysis.eventName ?: "")
            put("clubs", JSONArray(analysis.clubs))
            put("participants", participants)
        }
        cacheFile(pdfFileName).writeText(json.toString())
    }

    /** Liest die gecachte Analyse, oder null wenn keine vorhanden. */
    fun load(pdfFileName: String): MeldelistAnalysis? {
        val file = cacheFile(pdfFileName)
        if (!file.exists()) return null
        return try {
            val json = JSONObject(file.readText())
            val clubs = json.getJSONArray("clubs").let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }
            val participants = json.getJSONArray("participants").let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    Participant(
                        o.getString("nachname"),
                        o.getString("vorname"),
                        o.getString("verein")
                    )
                }
            }
            MeldelistAnalysis(
                eventName = json.optString("eventName").takeIf { it.isNotBlank() },
                clubs = clubs,
                participants = participants
            )
        } catch (e: Exception) {
            // Defekte Cache-Datei → verhält sich wie "kein Cache"
            null
        }
    }

    /** Löscht den Cache (z. B. wenn die Datei neu importiert wird). */
    fun invalidate(pdfFileName: String) {
        cacheFile(pdfFileName).delete()
    }
}