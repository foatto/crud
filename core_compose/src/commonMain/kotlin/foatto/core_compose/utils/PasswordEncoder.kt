package foatto.core_compose.utils

import java.security.MessageDigest

fun encodePassword(input: String): String {
    val HEX_CHARS = "0123456789ABCDEF"
    val bytes = MessageDigest
        .getInstance("SHA-1")
        .digest(input.toByteArray())

    var result = ""
    bytes.forEach { byte ->
        val i = byte.toInt()
        result += HEX_CHARS[i shr 4 and 0x0f]
        result += HEX_CHARS[i and 0x0f]
    }

    return result
}
