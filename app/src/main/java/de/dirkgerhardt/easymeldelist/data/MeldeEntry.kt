package de.dirkgerhardt.easymeldelist.data

data class MeldeEntry(
    val name: String,
    val wettkampf: Int,
    val lauf: Int,
    val bahn: Int,
    val distanz: Int,
    val schwimmart: String,
    val zeit: String,
    val verein: String,
    val jahrgang: Int,
    val tag: Int
)