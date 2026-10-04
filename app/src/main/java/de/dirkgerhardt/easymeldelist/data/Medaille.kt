package de.dirkgerhardt.easymeldelist.data

/** Medaillen-Typ für die Anzeige (reine Enumeration). */
enum class Medaille(
    val bezeichnung: String,
    val emoji: String
) {
    GOLD("Gold", "🥇"),
    SILBER("Silber", "🥈"),
    BRONZE("Bronze", "🥉")
}