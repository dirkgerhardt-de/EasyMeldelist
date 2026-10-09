package de.dirkgerhardt.easymeldelist.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.dirkgerhardt.easymeldelist.data.AppSettings
import de.dirkgerhardt.easymeldelist.data.AnzeigeModus
import de.dirkgerhardt.easymeldelist.data.ChancenInfo
import kotlin.math.roundToInt

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

    when (settings.modus) {
        AnzeigeModus.MOTIVATION -> zeigeMotivation(info)

        AnzeigeModus.MEDAILLENCHANCEN -> {
            // Nur anzeigen wenn Medaille vorhanden ist (≥50%)
            if (info.medaille == null) return

            val text = nuechternerText(info, wettkampfNr)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(info.medaille.emoji, fontSize = 28.sp, lineHeight = 32.sp)
                info.zweiteMedaille?.let { zweite ->
                    Spacer(Modifier.width(4.dp))
                    Text(
                        zweite.emoji,
                        fontSize = 20.sp,
                        lineHeight = 32.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnzeigeModus.PRO -> {
            val prozentZeile = prozentZeile(info) ?: return

            if (info.medaille == null) {
                Text(prozentZeile, style = MaterialTheme.typography.bodySmall)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        prozentZeile,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(info.medaille.emoji, fontSize = 28.sp, lineHeight = 32.sp)
                }
            }
        }
    }
}

@Composable
private fun zeigeMotivation(info: ChancenInfo) {
    if (info.medaille != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                info.text,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Text(info.medaille.emoji, fontSize = 28.sp, lineHeight = 32.sp)
        }
    } else if (info.chancen.gesamt > 0.0) {
        Text(info.text, style = MaterialTheme.typography.bodySmall)
    } else {
        Text(
            info.motivationsSpruch ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Nüchterner Text für den Medaillenchancen-Modus.
 * Nennt die wahrscheinlichste Medaille plus die nächsthöhere
 * als Perspektive ("bei maximum Power" etc.).
 */
private fun nuechternerText(info: ChancenInfo, salt: Int): String {
    val (_, besteName) = listOf(
        info.chancen.gold to "Gold",
        info.chancen.silber to "Silber",
        info.chancen.bronze to "Bronze"
    ).maxByOrNull { it.first } ?: return ""

    val naechsteHoehere = when (besteName) {
        "Gold" -> null
        "Silber" -> "Gold"
        else -> "Silber"
    }

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

    return if (naechsteHoehere != null) {
        "$besteName ist ${hauptPool[idxHaupt]}, $naechsteHoehere ${schlussPool[idxSchluss]}"
    } else {
        "$besteName ist ${hauptPool[idxHaupt]}"
    }
}

private fun prozentZeile(info: ChancenInfo): String? {
    val teile = mutableListOf<String>()
    if (info.chancen.gold > 0.0) teile += "Gold ${(info.chancen.gold * 100).roundToInt()}%"
    if (info.chancen.silber > 0.0) teile += "Silber ${(info.chancen.silber * 100).roundToInt()}%"
    if (info.chancen.bronze > 0.0) teile += "Bronze ${(info.chancen.bronze * 100).roundToInt()}%"
    if (info.chancen.gesamt > 0.0) teile += "Gesamt ${(info.chancen.gesamt * 100).roundToInt()}%"
    if (teile.isEmpty()) return null
    return teile.joinToString(" · ")
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