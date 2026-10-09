package de.dirkgerhardt.easymeldelist.data

/** Medaillen-Typ für die Anzeige (reine Enumeration). */
enum class Medaille(val displayName: String) {
    GOLD("Gold"),
    SILBER("Silber"),
    BRONZE("Bronze");

    val emoji get() = when(this) {
        GOLD -> "🥇"
        SILBER -> "🥈"
        BRONZE -> "🥉"
    }
}