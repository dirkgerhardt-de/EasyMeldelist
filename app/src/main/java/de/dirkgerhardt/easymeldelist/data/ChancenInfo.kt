package de.dirkgerhardt.easymeldelist.data

data class ChancenInfo(
    val chancen: MedaillenChancen,
    val medaille: Medaille?,
    val zweiteMedaille: Medaille? = null,
    val text: String,
    val motivationsSpruch: String?
)