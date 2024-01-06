package foatto.core_compose.utils

import io.ktor.util.*
import java.security.MessageDigest

fun encodePassword(input: String): String {
    val bytes = MessageDigest
        .getInstance("SHA-1")
        .digest(input.toByteArray())

    return bytes.encodeBase64()
}
