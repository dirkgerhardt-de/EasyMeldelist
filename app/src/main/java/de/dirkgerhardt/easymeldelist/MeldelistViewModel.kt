package de.dirkgerhardt.easymeldelist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import de.dirkgerhardt.easymeldelist.data.Participant

/**
 * Hält den Wizard-Zustand (geladenes PDF, Event, Vereine, Teilnehmer,
 * Eingaben) über die Navigation hinweg, damit die Suche nach dem
 * Speichern/Abbrechen mit erhaltenem Zustand fortgesetzt werden kann.
 */
class MeldelistViewModel : ViewModel() {
    var importedFile by mutableStateOf<String?>(null)
    var eventName by mutableStateOf<String?>(null)
    var clubs by mutableStateOf<List<String>>(emptyList())
    var participants by mutableStateOf<List<Participant>>(emptyList())
    var selectedClub by mutableStateOf<String?>(null)
    var vorname by mutableStateOf("")
    var nachname by mutableStateOf("")
}