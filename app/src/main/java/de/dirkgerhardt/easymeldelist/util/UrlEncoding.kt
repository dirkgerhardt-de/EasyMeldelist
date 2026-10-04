package de.dirkgerhardt.easymeldelist.util

import java.net.URLEncoder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

fun urlEncode(s: String): String =
    URLEncoder.encode(s, StandardCharsets.UTF_8.toString()).replace("+", "%20")

fun urlDecode(s: String): String =
    URLDecoder.decode(s, StandardCharsets.UTF_8.toString()).replace("%20", "+")