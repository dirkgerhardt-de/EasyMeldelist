package de.dirkgerhardt.easymeldelist.util

import java.net.URLDecoder
import java.net.URLEncoder

/** URL-kodiert einen String für die Verwendung in einer Navigation-Route. */
fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

/** Dekodiert ein Pfadargument aus einer Navigation-Route. */
fun dec(s: String?): String = s?.let { URLDecoder.decode(it, "UTF-8") } ?: ""