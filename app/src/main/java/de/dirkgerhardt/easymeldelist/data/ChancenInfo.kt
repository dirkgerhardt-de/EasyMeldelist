package de.dirkgerhardt.easymeldelist.data

data class ChancenInfo(
    val chancen: MedaillenChancen,
    val medaille: Medaille?,
    val text: String,
    val motivationsSpruch: String?
)