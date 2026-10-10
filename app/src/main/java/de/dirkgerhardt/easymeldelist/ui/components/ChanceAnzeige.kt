package de.dirkgerhardt.easymeldelist.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.dirkgerhardt.easymeldelist.data.AnzeigeModus
import de.dirkgerhardt.easymeldelist.data.AppSettings
import de.dirkgerhardt.easymeldelist.data.ChancenInfo
import de.dirkgerhardt.easymeldelist.data.Medaille
import de.dirkgerhardt.easymeldelist.data.MedalEstimator
import kotlin.math.roundToInt

/**
 * Reine Anzeige der vorberechneten Chancen – identisch für
 * Ergebnis-Screen und gespeicherte Ergebnisse.
 *
 * MEDAILLENCHANCEN-Modus:
 * - Gesamt ≥ 50 %:  "🥈 Silber bei maximum Power – 🥉 Bronze ist stabil"
 * - Gesamt < 50 %:  Motivationstext (wenn in Settings aktiviert), sonst nichts
 *
 * PRO-Modus:
 * - IMMER alle exakten Prozentwerte (Gold, Silber, Bronze, Gesamt),
 *   auch bei 0% – keine Filter, keine Unterdrückung.
 */
@Composable
fun ChanceAnzeige(
    info: ChancenInfo?,
    wettkampfNr: Int = 0,
    settings: AppSettings = AppSettings()
) {
    if (info == null) {
        return
    }
    Spacer(Modifier.height(12.dp))

    // ZENTRALE STELLE: Perspektive mit Schwelle und 0%-Filter berechnen
    val perspektive = MedalEstimator.zweiteMedaille(
        info.chancen,
        info.medaille,
        settings.perspektiveSchwelle
    )

    when (settings.modus) {
        AnzeigeModus.MEDAILLENCHANCEN -> {
            if (info.medaille == null) {
                // Keine Medaille (Gesamtchance < 50 %)
                if (settings.motivationAnzeigen) {
                    if (info.chancen.gesamt > 0.0) {
                        Text(info.text, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(
                            info.motivationsSpruch ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Motivation aus: nichts anzeigen
                return
            }

            // Medaille vorhanden: kombinierte Anzeige als ein Text
            val text = nuechternerText(info, wettkampfNr, perspektive)
            Text(text, style = MaterialTheme.typography.bodySmall)
        }

        AnzeigeModus.PRO -> {
            Text(prozentZeile(info), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Nüchterner Text für den Medaillenchancen-Modus.
 * Perspektive LINKS (Vision), Primär RECHTS (Basis).
 * Format: "🥈 Silber bei maximum Power – 🥉 Bronze ist stabil"
 */
private fun nuechternerText(
    info: ChancenInfo,
    salt: Int,
    perspektive: Medaille?
): String {
    val medaille = info.medaille ?: return ""

    val hauptPool = when {
        info.chancen.gesamt >= 0.70 -> listOf(
            "on fire", "locked", "done deal", "im Kasten", "abgezählt", "going crazy"
        )
        info.chancen.gesamt >= 0.50 -> listOf(
            "solid", "drin", "real", "stabil", "im Plan", "im Flow"
        )
        else -> listOf(
            "on the table", "greifbar", "good shot", "im Spiel", "lowkey real", "nicht unwahrscheinlich"
        )
    }
    val schlussPool = listOf(
        "bei maximum Power", "bei Vollgas", "wenn alles aufgeht",
        "mit Turbo", "in Bestform", "im Rausch des Tages"
    )

    // Streuwert aus den Nachkommastellen der Chance – jeder Wettkampf hat
    // dort andere Werte, das bricht die Kopplung an die Wettkampf-Nummer
    val streu = (info.chancen.gesamt * 1000).toInt()

    val idxHaupt = poolIndex(salt, streu, hauptPool.size)
    val idxSchluss = poolIndex(salt + 1, streu, schlussPool.size)

    return if (perspektive != null) {
        "${perspektive.emoji} ${perspektive.displayName} ${schlussPool[idxSchluss]}" +
                " – " +
                "${medaille.emoji} ${medaille.displayName} ist ${hauptPool[idxHaupt]}"
    } else {
        "${medaille.emoji} ${medaille.displayName} ist ${hauptPool[idxHaupt]}"
    }
}

/**
 * PRO-Modus: Zeigt IMMER alle Prozentwerte, ohne Filter –
 * auch 0% wird explizit angezeigt (vollständige Transparenz).
 */
private fun prozentZeile(info: ChancenInfo): String {
    fun pct(wert: Double): Int = (wert * 100).roundToInt()
    return "Gold ${pct(info.chancen.gold)}% · Silber ${pct(info.chancen.silber)}% · " +
            "Bronze ${pct(info.chancen.bronze)}% · Gesamt ${pct(info.chancen.gesamt)}%"
}

/**
 * Deterministischer Index über einen Murmur-artigen Hash.
 * Verteilt auch bei nur ungeraden oder gehäuften Wettkampf-Nummern
 * gleichmäßig über den Pool, ohne dass sich Paarungen wiederholen.
 */
private fun poolIndex(salt: Int, streu: Int, size: Int): Int {
    var x = salt * 374761393 + streu * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return ((x % size) + size) % size
}