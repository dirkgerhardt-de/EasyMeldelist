package de.dirkgerhardt.easymeldelist.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.dirkgerhardt.easymeldelist.data.ChancenInfo

/**
 * Reine Anzeige der vorberechneten Chancen – identisch für
 * Ergebnis-Screen und gespeicherte Ergebnisse.
 */
@Composable
fun ChanceAnzeige(info: ChancenInfo?) {
    Spacer(Modifier.height(12.dp))

    if (info == null) {
        return   // nichts berechenbar -> nichts anzeigen
    }

    if (info.chancen.gesamt <= 0.0) {
        Text(
            info.motivationsSpruch ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else if (info.medaille == null) {
        Text(info.text, style = MaterialTheme.typography.bodySmall)
    } else {
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
    }
}