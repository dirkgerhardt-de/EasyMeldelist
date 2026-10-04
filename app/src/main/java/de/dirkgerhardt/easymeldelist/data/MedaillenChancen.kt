package de.dirkgerhardt.easymeldelist.data

/** Wahrscheinlichkeiten pro Metall und gesamt. */
data class MedaillenChancen(
    val gold: Double,
    val silber: Double,
    val bronze: Double
) {
    val gesamt: Double get() = gold + silber + bronze
}