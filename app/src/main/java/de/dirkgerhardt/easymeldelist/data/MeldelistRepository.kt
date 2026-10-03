package de.dirkgerhardt.easymeldelist.data

import android.content.Context
import java.io.File

/**
 * Verwaltet die hochgeladenen PDFs im app-eigenen Speicher
 * (filesDir/meldelisten) – kein Storage-Permission nötig.
 * Speichert zusätzlich Meta-Dateien (.name) mit dem Eventnamen.
 */
class MeldelistRepository(private val context: Context) {

    private val dir get() = File(context.filesDir, "meldelisten").apply { mkdirs() }

    /** Kopiert die ausgewählte Datei in den App-Speicher, gibt den Dateinamen zurück. */
    fun import(uri: android.net.Uri): String {
        val displayName = queryDisplayName(uri)
        val target = File(dir, displayName)
        context.contentResolver.openInputStream(uri)!!.use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
        return displayName
    }

    /** Liefert alle hochgeladenen PDF-Dateinamen, alphabetisch sortiert. */
    fun listFiles(): List<String> =
        dir.listFiles()?.filter { it.isFile && it.extension == "pdf" }
            ?.map { it.name }?.sorted() ?: emptyList()

    /** Öffnet eine gespeicherte PDF zum Parsen. */
    fun open(fileName: String): java.io.InputStream =
        File(dir, fileName).inputStream()

    /** Speichert den Anzeigenamen (Event) für eine hochgeladene Datei. */
    fun saveEventName(pdfFileName: String, eventName: String?) {
        val meta = File(dir, pdfFileName + ".name")
        if (eventName.isNullOrBlank()) meta.delete()
        else meta.writeText(eventName)
    }

    /** Liefert den gespeicherten Event-Namen oder fällt auf den Dateinamen zurück. */
    fun displayName(pdfFileName: String): String {
        val meta = File(dir, pdfFileName + ".name")
        return if (meta.exists()) meta.readText().trim() else pdfFileName
    }

    private fun queryDisplayName(uri: android.net.Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) return cursor.getString(idx)
        }
        return "meldeliste_${System.currentTimeMillis()}.pdf"
    }
}