package de.dirkgerhardt.easymeldelist.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Die drei sich gegenseitig ausschließenden Anzeige-Modi.
 */
enum class AnzeigeModus {
    /** Sprüche wie bisher (Geheimtipp, Überraschungswaffe etc.) */
    MOTIVATION,

    /** Nüchterne Chancen-Einschätzung + wahrscheinlichste Medaille (Standard) */
    MEDAILLENCHANCEN,

    /** Exakte Prozentwerte für Gold/Silber/Bronze/Gesamt */
    PRO
}

/** Standard-Anzeige-Modus für frische Installationen. */
const val STANDARD_ANZEIGEMODUS = "MEDAILLENCHANCEN"

data class AppSettings(
    val modus: AnzeigeModus = AnzeigeModus.MEDAILLENCHANCEN
)

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        _settings.value = AppSettings(modus = leseModus())
    }

    private fun leseModus(): AnzeigeModus {
        val gespeichert = prefs.getString(KEY_MODUS, null)
        if (gespeichert != null) {
            return runCatching { AnzeigeModus.valueOf(gespeichert) }
                .getOrDefault(AnzeigeModus.MEDAILLENCHANCEN)
        }

        // Migration von den alten Boolean-Keys (erste Version der Settings)
        val altesPro = prefs.getBoolean(KEY_ALT_PRO, false)
        if (prefs.contains(KEY_ALT_PRO) && altesPro) return AnzeigeModus.PRO
        val alteChancen = prefs.getBoolean(KEY_ALT_CHANCEN, true)
        if (prefs.contains(KEY_ALT_CHANCEN) && !alteChancen) return AnzeigeModus.MOTIVATION

        return AnzeigeModus.MEDAILLENCHANCEN
    }

    fun setModus(neu: AnzeigeModus) {
        prefs.edit().putString(KEY_MODUS, neu.name).apply()
        _settings.value = AppSettings(modus = neu)
    }

    companion object {
        private const val PREFS_NAME = "easymeldelist_settings"
        private const val KEY_MODUS = "anzeige_modus"

        // Alte Keys der Boolean-Version, nur für Migration relevant
        private const val KEY_ALT_PRO = "pro_modus"
        private const val KEY_ALT_CHANCEN = "medaillen_chancen_anzeigen"

        private val _settings = MutableStateFlow(AppSettings())
        val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    }
}